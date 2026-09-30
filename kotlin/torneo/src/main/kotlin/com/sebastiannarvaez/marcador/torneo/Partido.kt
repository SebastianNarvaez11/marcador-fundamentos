package com.sebastiannarvaez.marcador.torneo

// Sigue siendo inmutable. El marcador ya no se guarda: se DEDUCE de los
// eventos, así que no puede contradecirlos.
data class Partido(
    val local: Equipo,
    val visitante: Equipo,
    val eventos: List<EventoDePartido> = emptyList(),
) {
    fun golesDe(equipo: Equipo): Int = eventos.count { it is Gol && it.equipo == equipo }

    val golesLocal: Int get() = golesDe(local)
    val golesVisitante: Int get() = golesDe(visitante)

    // Registrar un evento devuelve un partido nuevo con el evento al final.
    fun registrar(evento: EventoDePartido): Partido = copy(eventos = eventos + evento)

    fun resultado(): Resultado = when {
        golesLocal > golesVisitante -> Resultado.Victoria(ganador = local, perdedor = visitante)
        golesLocal < golesVisitante -> Resultado.Victoria(ganador = visitante, perdedor = local)
        else -> Resultado.Empate
    }
}
