package com.sebastiannarvaez.marcador.torneo

// Datos de ejemplo para la consola y las pruebas de F2: equipos, jugadores y dos
// «guiones» de partido (la lista de cosas que pasan y en qué minuto).
object Ejemplo {
    val ana = Jugador("Ana", 9)
    val luis = Jugador("Luis", null)
    val marta = Jugador("Marta", 1)
    val ivan = Jugador("Iván", 7)
    val sofia = Jugador("Sofía", 10)
    val pedro = Jugador("Pedro", 4)
    val carla = Jugador("Carla", 5)
    val nico = Jugador("Nico", 11)

    val rayo = Equipo("Rayo FC", listOf(ana, luis, marta))
    val toros = Equipo("Toros", listOf(ivan, sofia))
    val lobos = Equipo("Lobos", listOf(pedro, carla))
    val aguilas = Equipo("Águilas", listOf(nico))

    val rayoContraToros = Partido(
        local = rayo,
        visitante = toros,
        eventos = listOf(
            Gol(12, ana, rayo),
            Tarjeta(30, ivan, ColorDeTarjeta.AMARILLA),
            Gol(55, ivan, toros),
            Gol(80, ana, rayo),
            Cambio(85, sale = ana, entra = luis),
        ),
    )

    val lobosContraAguilas = Partido(
        local = lobos,
        visitante = aguilas,
        eventos = listOf(
            Gol(20, pedro, lobos),
            Gol(33, nico, aguilas),
            Tarjeta(60, carla, ColorDeTarjeta.ROJA),
            Gol(70, nico, aguilas),
        ),
    )

    // Un torneo nuevo cada vez (el Torneo es mutable) con los dos partidos ya jugados.
    fun torneoConPartidos(): Torneo = Torneo("Copa Barrio", listOf(rayo, toros, lobos, aguilas)).apply {
        registrar(rayoContraToros).getOrThrow()
        registrar(lobosContraAguilas).getOrThrow()
    }
}
