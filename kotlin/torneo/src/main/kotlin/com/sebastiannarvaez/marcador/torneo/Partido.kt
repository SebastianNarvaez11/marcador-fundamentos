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

    // Función de orden superior: recibe una función como parámetro.
    // `(EventoDePartido) -> Boolean` es un tipo función: toma un evento y
    // devuelve si cumple. Al llamarla, la lambda va fuera de los paréntesis
    // (trailing lambda): partido.eventos { it is Tarjeta }
    fun eventos(filtro: (EventoDePartido) -> Boolean): List<EventoDePartido> = eventos.filter(filtro)

    // `Gol::jugador` es una referencia a propiedad: equivale a { gol -> gol.jugador }.
    fun goleadores(): List<Jugador> = eventos.filterIsInstance<Gol>().map(Gol::jugador)

    // Retorno con etiqueta: `return@forEach` sale solo de la lambda actual
    // (salta ese evento), no de la función. Un `return` a secas saldría de la función.
    fun minutosDeGolDe(jugador: Jugador): List<Int> {
        val minutos = mutableListOf<Int>()
        eventos.forEach { evento ->
            if (evento !is Gol) return@forEach
            if (evento.jugador == jugador) minutos.add(evento.minuto)
        }
        return minutos
    }
}
