package com.sebastiannarvaez.marcador.torneo

import java.io.File

// Punto de entrada de consola: ./gradlew :torneo:run
fun main() {
    val nombreDelTorneo = "Copa Barrio"
    val totalDeJornadas = 3
    println("Torneo: $nombreDelTorneo")

    // Rangos y bucles: cada jornada del 1 al total.
    for (jornada in 1..totalDeJornadas) {
        // Argumento con nombre: se lee sin adivinar qué es cada número.
        println(encabezadoDeJornada(numero = jornada, totalDeJornadas = totalDeJornadas))
    }
    println(encabezadoDeJornada(1, 3, prefijo = "Fecha"))

    // Todos los marcadores posibles de 0 a 2 goles y los puntos del local.
    for (golesLocal in 0..2) {
        for (golesVisitante in 0..2) {
            val puntos = puntosPor(golesLocal, golesVisitante)
            println("$golesLocal-$golesVisitante -> $puntos puntos para el local")
        }
    }

    // `if` como expresión, `downTo`, `step` y `while`.
    val hayEmpates = puntosPor(1, 1) == 1
    println(if (hayEmpates) "Un empate vale 1 punto" else "Sin empates")
    for (minuto in 90 downTo 0 step 30) println("Minuto $minuto")
    var minuto = 0
    while (minuto < 90) minuto += 45
    println("El partido acaba en el minuto $minuto")

    // Nulabilidad: jugadores con y sin dorsal.
    val jugadores = listOf(Jugador("Ana", 9), Jugador("Luis", null), Jugador("Marta", 1))
    for (jugador in jugadores) println(presentar(jugador))
    println(presentar(null))

    // Clases: un equipo valida su plantilla al construirse.
    val rayo = Equipo("Rayo FC", jugadores)
    rayo.nombrarCapitan(jugadores.first())
    println("$rayo, capitán: ${rayo.capitan?.nombre}")
    val error = runCatching { Equipo("Toros", listOf(Jugador("A", 7), Jugador("B", 7))) }
    println("Toros: ${error.exceptionOrNull()?.message}")

    // data class: copy, == frente a ===, desestructuración.
    val toros = Equipo("Toros", listOf(Jugador("Iván", 7)))
    val ana = jugadores.first()
    val ivan = toros.plantilla.first()
    println("¿Misma ficha? ${ana == Jugador("Ana", 9)} (==), ${ana === Jugador("Ana", 9)} (===)")
    println("Con otro dorsal: ${ana.copy(dorsal = 10)}")

    // sealed interface y when exhaustivo: los eventos de un partido.
    val inicio = Partido(rayo, toros)
    val alFinal = inicio
        .registrar(Gol(12, ana, rayo))
        .registrar(Tarjeta(30, ivan, ColorDeTarjeta.AMARILLA))
        .registrar(Gol(55, ivan, toros))
        .registrar(Gol(80, ana, rayo))
        .registrar(Cambio(85, sale = ana, entra = jugadores[1]))
    println("Antes: ${inicio.eventos.size} eventos, después: ${alFinal.eventos.size}")
    alFinal.eventos.forEach { println(describir(it)) }
    println("Marcador: ${alFinal.golesLocal}-${alFinal.golesVisitante} -> ${alFinal.resultado().titular()}")

    // Lambdas: filtrar eventos y sacar goleadores.
    println("Tarjetas: ${alFinal.eventos { it is Tarjeta }.map(::describir)}")
    println("Primera mitad: ${alFinal.eventos(antesDelMinuto(45)).size} eventos")
    println("Goleadores: ${alFinal.goleadores().map { it.nombre }}")
    println("Minutos de gol de Ana: ${alFinal.minutosDeGolDe(ana)}")
    repetir(2) { vuelta -> println("Vuelta $vuelta") }

    // Colecciones y extensiones: una jornada completa en un torneo.
    val lobos = Equipo("Lobos", listOf(Jugador("Pedro", 4)))
    // `apply`: configura el objeto recién creado y lo devuelve.
    val torneo = Torneo(nombreDelTorneo, listOf(rayo, toros, lobos)).apply {
        registrar(alFinal).getOrThrow()
        registrar(Partido(toros, lobos).registrar(Gol(20, ivan, toros))).getOrThrow()
        registrar(Partido(lobos, rayo).registrar(Gol(40, ana, rayo))).getOrThrow()
    }
    println(torneo.tablaComoTexto())
    val goleadores = torneo.goleadores()
    for (jugador in goleadores) {
        println("${goleadores.puesto(jugador)}. ${jugador.etiqueta()}: ${goleadores.puntosDe(jugador)}")
    }
    println(resumenDeEquipo(rayo))
    println("Capitán de Toros: ${toros.capitanONinguno}")

    // Inmutabilidad: lo que devuelve el torneo es una copia.
    println("Partidos en el torneo: ${torneo.partidos.size}")

    // Interfaces y objetos: el reglamento se puede cambiar por otro.
    println(ReglamentoLiga)
    // Objeto anónimo: implementa la interfaz al vuelo, sin declarar una clase.
    val reglamentoAntiguo = object : Reglamento {
        override val puntosPorVictoria = 2
        override val puntosPorEmpate = 1
    }
    val torneoAntiguo = Torneo("Copa Antigua", listOf(rayo, toros), reglamentoAntiguo)
    torneoAntiguo.registrar(alFinal)
    println("Rayo con reglas antiguas: ${torneoAntiguo.tablaDePosiciones().first().puntos} puntos")
    println("Mínimo de equipos: ${Torneo.MINIMO_DE_EQUIPOS}")

    // Errores como valores: un registro inválido no tumba el programa.
    val intruso = Jugador("Intruso", 99)
    val golInvalido = torneo.registrar(lobos, toros, listOf(Gol(10, intruso, lobos)))
    golInvalido
        .onSuccess { println("Registrado: $it") }
        .onFailure { println("No se registró: ${it.message}") }
    val golValido = torneo.registrar(lobos, toros, listOf(Gol(10, lobos.plantilla.first(), lobos)))
    println("Marcador: " + golValido.fold(
        onSuccess = { "${it.golesLocal}-${it.golesVisitante}" },
        onFailure = { "error" },
    ))
    println("Partidos: " + torneo.registrar(lobos, lobos).map { it.eventos.size }.getOrElse { -1 })

    // `try` es una expresión: devuelve un valor.
    for (texto in listOf("10", " 7 ", "diez", "")) {
        println("«$texto» -> ${dorsalDesdeTexto(texto) ?: "no es un dorsal"}")
    }

    // `use` cierra el fichero aunque falle la escritura.
    val destino = File.createTempFile("tabla", ".txt")
    torneo.exportarTabla(destino)
        .onSuccess { println("Tabla escrita en ${destino.name} (${destino.length()} bytes)") }
        .onFailure { println("No se pudo exportar: ${it.message}") }
    destino.delete()

    // by lazy: la tabla de una foto del torneo se calcula la primera vez que se lee.
    val foto = torneo.instantanea()
    println("Tabla calculada ${foto.calculosDeLaTabla} veces antes de leerla")
    println("Líder: ${foto.tabla.first().equipo.nombre}")
    println("Líder otra vez: ${foto.tabla.first().equipo.nombre} (calculada ${foto.calculosDeLaTabla} vez)")

    // kotlinx.serialization: el torneo entero, a torneo.json y de vuelta.
    val fichero = File("torneo.json")
    torneo.guardar(fichero)
        .onSuccess { println("Guardado en ${fichero.absolutePath}") }
        .onFailure { println("No se pudo guardar: ${it.message}") }
    cargarTorneo(fichero)
        .onSuccess { println("Cargado: ${it.nombre}, ${it.partidos.size} partidos, líder ${it.tablaDePosiciones().first().equipo.nombre}") }
        .onFailure { println("No se pudo cargar: ${it.message}") }
}
