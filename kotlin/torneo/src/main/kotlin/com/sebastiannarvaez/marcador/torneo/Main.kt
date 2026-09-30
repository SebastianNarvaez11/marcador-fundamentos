package com.sebastiannarvaez.marcador.torneo

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

    // Colecciones: una jornada completa en un torneo, la tabla y los goleadores.
    val lobos = Equipo("Lobos", listOf(Jugador("Pedro", 4)))
    val torneo = Torneo(nombreDelTorneo, listOf(rayo, toros, lobos))
    torneo.agregar(alFinal)
    torneo.agregar(Partido(toros, lobos).registrar(Gol(20, ivan, toros)))
    torneo.agregar(Partido(lobos, rayo).registrar(Gol(40, ana, rayo)))
    println("Pos Equipo  PJ  G  E  P  GF GC  DG Pts")
    torneo.tablaDePosiciones().forEachIndexed { indice, fila ->
        println(
            "%2d  %-7s %2d %2d %2d %2d %3d %2d %3d %3d".format(
                indice + 1, fila.equipo.nombre, fila.jugados, fila.ganados, fila.empatados,
                fila.perdidos, fila.golesAFavor, fila.golesEnContra, fila.diferencia, fila.puntos,
            ),
        )
    }
    torneo.goleadores().forEach { (jugador, goles) -> println("${jugador.nombre}: $goles") }

    // Inmutabilidad: lo que devuelve el torneo es una copia.
    println("Partidos en el torneo: ${torneo.partidos.size}")
}
