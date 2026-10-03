package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

// Punto de entrada de consola: ./gradlew :torneo:jvmRun
// (la demo del módulo de Kotlin sigue disponible: ./gradlew :torneo:jvmRun --args=kotlin)
//
// `runBlocking` es el puente entre el mundo normal (`main` no es suspend) y el
// de las corrutinas: bloquea el hilo actual hasta que el bloque termina.
fun main(args: Array<String>) {
    if ("kotlin" in args) {
        demoKotlin()
        return
    }
    runBlocking {
        println("== un partido suspendido ==")
        val final = jugarPartido(Ejemplo.rayoContraToros)
        println("Final: ${final.golesLocal}-${final.golesVisitante} con ${final.eventos.size} eventos")

        println("== dos partidos a la vez con launch ==")
        val tiempo = measureTimeMillis {
            // Cada `launch` arranca una corrutina hija y sigue sin esperarla.
            // `coroutineScope` espera a que terminen las dos antes de continuar.
            coroutineScope {
                for (guion in listOf(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)) {
                    launch {
                        val final = jugarPartido(guion)
                        println("Pitido final: ${final.local.nombre} ${final.golesLocal}-${final.golesVisitante} ${final.visitante.nombre}")
                    }
                }
            }
        }
        println("Los dos partidos duraron $tiempo ms (uno solo dura algo más de 1 s)")

        println("== jornada con async ==")
        val tiempoJornada = measureTimeMillis {
            // `jugarJornada` devuelve un `Pair`: se reparte en dos variables.
            val (uno, otro) = jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)
            println("Resultados: ${uno.golesLocal}-${uno.golesVisitante} y ${otro.golesLocal}-${otro.golesVisitante}")
        }
        println("La jornada con async duró $tiempoJornada ms")

        println("== estadísticas con async ==")
        val tiempoEstadisticas = measureTimeMillis {
            println(estadisticasDe(Ejemplo.rayoContraToros))
            println(estadisticasDe(Ejemplo.lobosContraAguilas))
        }
        println("Dos estadísticas en $tiempoEstadisticas ms (cada una son 3 consultas de 50 ms a la vez)")

        println("== el cronómetro se cancela al pitar el final ==")
        val conReloj = jugarConCronometro(Ejemplo.rayoContraToros) { minuto ->
            if (minuto % 15 == 0) println("  reloj: minuto $minuto")
        }
        println("Final ${conReloj.golesLocal}-${conReloj.golesVisitante}; el reloj ya no corre")

        println("== un alcance propio y cancelación cooperativa ==")
        demoTransmisorPropio()

        println("== guardar en IO y calcular la tabla en Default ==")
        demoDispatchers()

        println("== un partido que falla no tumba al otro ==")
        demoErrores()

        println("== un Flow frío, la narración del partido ==")
        demoFlowFrio()

        println("== la jornada, dos marcadores combinados ==")
        demoJornada()

        println("== dos partidos simultáneos con StateFlow y SharedFlow ==")
        demoEnVivo()
    }
}

// Los dos partidos a la vez, con la pantalla (marcadores) y el aviso de goles
// como espectadores, y un cronómetro. Al pitar el final se cancela todo: los
// espectadores son corrutinas infinitas y, si nadie las cancela, el programa no
// terminaría nunca.
suspend fun demoEnVivo() {
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val jornada = Jornada(PartidoEnVivo(Ejemplo.rayoContraToros), PartidoEnVivo(Ejemplo.lobosContraAguilas), alcance)

    val reloj = alcance.cronometro(msPorMinuto = 10) { minuto -> if (minuto % 30 == 0) println("  ⏱ minuto $minuto") }
    val goles = alcance.launch(start = CoroutineStart.UNDISPATCHED) {
        jornada.goles.collect { println("  ⚽ ${describir(it)}") }
    }
    val pantalla = alcance.launch(start = CoroutineStart.UNDISPATCHED) {
        jornada.marcadores.collect { println("  ${Ejemplo.rayo.nombre} ${it.primero} ${Ejemplo.toros.nombre} | ${Ejemplo.lobos.nombre} ${it.segundo} ${Ejemplo.aguilas.nombre}") }
    }

    jornada.iniciar().join()
    println("Pitido final: se cancelan el reloj y los espectadores")
    val espectadores = listOf(reloj, goles, pantalla)
    espectadores.forEach { it.cancelAndJoin() }
    println("Cancelados: ${espectadores.map { it.isCancelled }}")
    println("Marcadores finales: ${jornada.marcadores.value.primero} y ${jornada.marcadores.value.segundo}")
    alcance.cancel()
}


suspend fun demoJornada() {
    val uno = PartidoEnVivo(Ejemplo.rayoContraToros)
    val otro = PartidoEnVivo(Ejemplo.lobosContraAguilas)
    marcadoresDeLaJornada(uno, otro).collect { jornada ->
        println("  ${Ejemplo.rayo.nombre} ${jornada.primero} ${Ejemplo.toros.nombre} | ${Ejemplo.lobos.nombre} ${jornada.segundo} ${Ejemplo.aguilas.nombre}")
    }

    // flatMapLatest: cambiar de partido a los 300 ms cancela el seguimiento del primero.
    val elegidos = flow {
        emit(uno); delay(300); emit(otro)
    }
    println("Siguiendo un partido y cambiando a los 300 ms:")
    seguirPartido(elegidos).collect { println("  marcador visto: $it") }
}


suspend fun demoFlowFrio() {
    val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros)
    // Aún no ha pasado nada: crear el Flow no arranca el partido.
    val narracion = enVivo.narracion()
    println("Flow creado; nada ha corrido todavía")
    narracion.collect { println("  $it") }

    // La MISMA variable, recogida otra vez: el frío se repite entero.
    val ms = measureTimeMillis { narracion.collect { } }
    println("Segundo collect de la misma variable: otro partido completo ($ms ms)")

    val gol = enVivo.primerGol()
    println("Primer gol: ${describir(gol)} (el resto del partido se canceló)")
}


suspend fun demoErrores() {
    val resultados = jugarJornadaSupervisada(listOf(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)) { partido ->
        if (partido.local === Ejemplo.lobos) jugarConApagon(partido, minutoDelApagon = 30) else jugarPartido(partido)
    }
    for (resultado in resultados) {
        resultado
            .onSuccess { println("Terminó ${it.local.nombre}-${it.visitante.nombre}: ${it.golesLocal}-${it.golesVisitante}") }
            .onFailure { println("Falló: ${it.message}") }
    }

    // El mismo caso con un alcance propio: el error de un `launch` raíz no llega a
    // quien lo lanzó, llega al CoroutineExceptionHandler.
    val transmisor = Transmisor(alFallar = { println("  handler: ${it.message}") }, jugar = { partido, ms ->
        if (partido.local === Ejemplo.lobos) jugarConApagon(partido, ms, minutoDelApagon = 30) else jugarPartido(partido, ms)
    })
    listOf(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)
        .map { transmisor.transmitir(it) { final -> println("  terminó ${final.local.nombre}") } }
        .forEach { it.join() }
    println("Transmisor sigue activo tras el fallo: ${transmisor.activo}")
    transmisor.pitarElFinal()
}


suspend fun demoDispatchers() {
    val torneo = Ejemplo.torneoConPartidos()
    val ruta = "torneo.json"
    println("Empiezo en: ${Thread.currentThread().name}")
    withContext(Dispatchers.IO) { println("Guardar corre en: ${Thread.currentThread().name}") }
    withContext(Dispatchers.Default) { println("Calcular corre en: ${Thread.currentThread().name}") }
    val tabla = torneo.cerrarJornada(ruta)
    println("Guardado en $ruta; líder: ${tabla.first().equipo.nombre} con ${tabla.first().puntos} puntos")
    println("Vuelvo a: ${Thread.currentThread().name}")
    // Dispatchers.Main en consola: no existe.
    try {
        withContext(Dispatchers.Main) { }
    } catch (e: IllegalStateException) {
        println("Dispatchers.Main: ${e.message}")
    }
}

// El Transmisor tiene su propio alcance. Se transmiten dos partidos y, a mitad,
// se pita el final del día: los dos se cancelan sin que ninguno llegue al 90.
suspend fun demoTransmisorPropio() = coroutineScope {
    val transmisor = Transmisor()
    val trabajos = listOf(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas).map { guion ->
        transmisor.transmitir(guion) { println("Terminó ${it.local.nombre}: eso no debería verse") }
    }
    delay(300)
    transmisor.pitarElFinal()
    trabajos.forEach { it.join() }
    println("Cancelados: ${trabajos.map { it.isCancelled }}, transmisor activo: ${transmisor.activo}")

    // Un bucle de cálculo sin suspender solo se cancela si mira `ensureActive()`.
    // Va en Dispatchers.Default: en el hilo de runBlocking, el bucle ocuparía el
    // único hilo y ni siquiera podríamos llegar a llamar a cancelAndJoin().
    val calculo = launch(Dispatchers.Default) { println("resultado: ${probabilidadDeVictoria(Ejemplo.rayoContraToros, simulaciones = 2_000_000_000)}") }
    delay(100)
    val ms = measureTimeMillis { calculo.cancelAndJoin() }
    println("El cálculo pesado se canceló en $ms ms")
}
