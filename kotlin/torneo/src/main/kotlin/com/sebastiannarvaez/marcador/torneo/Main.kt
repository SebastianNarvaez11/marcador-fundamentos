package com.sebastiannarvaez.marcador.torneo

// Punto de entrada de consola: ./gradlew :torneo:run
fun main() {
    val nombreDelTorneo = "Copa Barrio"
    var jornada = 1
    val equipos = 4
    val partidosPorJornada = equipos / 2
    println("Torneo: $nombreDelTorneo")
    println("Jornada $jornada de ${equipos - 1}: $partidosPorJornada partidos")
    jornada += 1
    println("La siguiente jornada será la $jornada")
}
