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
    val inicio = Partido(rayo, toros)
    val alFinal = inicio.conGolLocal().conGolLocal().conGolVisitante()
    val (_, _, golesLocal, golesVisitante) = alFinal
    println("Antes: ${inicio.golesLocal}-${inicio.golesVisitante}, después: $golesLocal-$golesVisitante")
    val ana = jugadores.first()
    println("¿Misma ficha? ${ana == Jugador("Ana", 9)} (==), ${ana === Jugador("Ana", 9)} (===)")
    println("Con otro dorsal: ${ana.copy(dorsal = 10)}")
}
