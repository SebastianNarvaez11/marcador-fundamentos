package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

// Punto de entrada de consola: ./gradlew :torneo:run
// (la demo de F1 sigue disponible: ./gradlew :torneo:run --args=kotlin)
//
// `runBlocking` es el puente entre el mundo normal (`main` no es suspend) y el
// de las corrutinas: bloquea el hilo actual hasta que el bloque termina.
fun main(args: Array<String>) {
    if ("kotlin" in args) return demoKotlin()
    runBlocking {
        println("== f15: corrutina frente a hilo ==")
        demoCorrutinasFrenteAHilos()
        println("== f15: un partido suspendido ==")
        val final = jugarPartido(Ejemplo.rayoContraToros)
        println("Final: ${final.golesLocal}-${final.golesVisitante} con ${final.eventos.size} eventos")

        println("== f16: dos partidos a la vez con launch ==")
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

        println("== f16: estadísticas con async ==")
        val (uno, otro) = jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)
        val tiempoEstadisticas = measureTimeMillis {
            println(estadisticasDe(uno))
            println(estadisticasDe(otro))
        }
        println("Dos estadísticas en $tiempoEstadisticas ms (cada una son 3 consultas de 50 ms en paralelo)")

        println("== f17: el cronómetro se cancela al pitar el final ==")
        val conReloj = jugarConCronometro(Ejemplo.rayoContraToros) { minuto ->
            if (minuto % 15 == 0) println("  reloj: minuto $minuto")
        }
        println("Final ${conReloj.golesLocal}-${conReloj.golesVisitante}; el reloj ya no corre")

        println("== f17: un alcance propio y cancelación cooperativa ==")
        demoTransmisorPropio()
        println("== f17: por qué no GlobalScope ==")
        demoGlobalScope()

        println("== f18: guardar en IO y calcular la tabla en Default ==")
        demoDispatchers()
    }
}

suspend fun demoDispatchers() {
    val torneo = Ejemplo.torneoConPartidos()
    val fichero = File("torneo.json")
    println("Empiezo en: ${Thread.currentThread().name}")
    withContext(Dispatchers.IO) { println("Guardar corre en: ${Thread.currentThread().name}") }
    withContext(Dispatchers.Default) { println("Calcular corre en: ${Thread.currentThread().name}") }
    val tabla = torneo.cerrarJornada(fichero)
    println("Guardado en ${fichero.absolutePath}; líder: ${tabla.first().equipo.nombre} con ${tabla.first().puntos} puntos")
    println("Vuelvo a: ${Thread.currentThread().name}")
    // Dispatchers.Main en consola: no existe.
    val main = runCatching { withContext(Dispatchers.Main) { } }
    println("Dispatchers.Main: ${main.exceptionOrNull()?.message}")
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

// GlobalScope no tiene padre: sus corrutinas no se cancelan con nadie. Aquí el
// padre se cancela y el huérfano sigue vivo hasta que lo cancelamos a mano.
@OptIn(DelicateCoroutinesApi::class)
suspend fun demoGlobalScope() = coroutineScope {
    var huerfano: Job? = null
    val padre = launch {
        huerfano = GlobalScope.launch { jugarPartido(Ejemplo.rayoContraToros) }
        delay(1_000)
    }
    delay(50)
    padre.cancelAndJoin()
    println("Padre cancelado: ${padre.isCancelled}. Huérfano activo: ${huerfano?.isActive}")
    huerfano?.cancel() // limpiarlo a mano es justo la carga que el alcance estructurado evita
}

// 10.000 esperas de 100 ms. Con corrutinas tardan ~100 ms en total, porque
// todas esperan a la vez sobre un puñado de hilos. Con un hilo cada una serían
// 10.000 hilos del sistema operativo.
suspend fun demoCorrutinasFrenteAHilos() {
    val ms = measureTimeMillis {
        // `coroutineScope` no termina hasta que terminan todas sus corrutinas hijas.
        coroutineScope { repeat(10_000) { launch { delay(100) } } }
    }
    println("10.000 corrutinas esperando 100 ms terminan en $ms ms")
}
