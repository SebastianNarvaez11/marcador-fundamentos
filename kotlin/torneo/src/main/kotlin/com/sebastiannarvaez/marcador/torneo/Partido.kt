package com.sebastiannarvaez.marcador.torneo

// Un partido es un valor inmutable: ninguna propiedad es `var`.
// «Registrar un gol» no modifica el partido: devuelve OTRO con el marcador nuevo.
data class Partido(
    val local: Equipo,
    val visitante: Equipo,
    val golesLocal: Int = 0,
    val golesVisitante: Int = 0,
) {
    // `copy` clona el partido cambiando solo lo que se indica.
    fun conGolLocal(): Partido = copy(golesLocal = golesLocal + 1)

    fun conGolVisitante(): Partido = copy(golesVisitante = golesVisitante + 1)
}
