package com.sebastiannarvaez.marcador.torneo

// Una fila de la tabla de posiciones.
data class FilaDePosicion(
    val equipo: Equipo,
    val jugados: Int,
    val ganados: Int,
    val empatados: Int,
    val perdidos: Int,
    val golesAFavor: Int,
    val golesEnContra: Int,
    val puntos: Int,
) {
    val diferencia: Int get() = golesAFavor - golesEnContra
}

// Función de extensión: añade un método a `List<Partido>` sin tocar la clase List.
// Dentro, `this` es la lista de partidos. Se llama como si fuera de la lista:
//     partidos.tablaDePosiciones()
// `equipos` permite que salgan también los que aún no han jugado.
fun List<Partido>.tablaDePosiciones(
    equipos: List<Equipo> = emptyList(),
    reglamento: Reglamento = ReglamentoLiga,
): List<FilaDePosicion> {
    // Cada partido cuenta dos veces: una desde el lado local y otra desde el visitante.
    // Cada elemento es (equipo, goles a favor, goles en contra).
    val apariciones = flatMap { partido ->
        listOf(
            Triple(partido.local, partido.golesLocal, partido.golesVisitante),
            Triple(partido.visitante, partido.golesVisitante, partido.golesLocal),
        )
    }

    // groupBy con dos lambdas: la clave (equipo) y el valor (el marcador visto por él).
    // El resultado es un Map<Equipo, List<Pair<Int, Int>>>.
    val marcadoresPorEquipo = apariciones.groupBy({ it.first }, { it.second to it.third })

    // Un Set no admite repetidos: une los equipos dados con los que ya jugaron.
    val todos: Set<Equipo> = (equipos + marcadoresPorEquipo.keys).toSet()

    return todos
        .map { equipo ->
            val marcadores = marcadoresPorEquipo[equipo].orEmpty()
            FilaDePosicion(
                equipo = equipo,
                jugados = marcadores.size,
                ganados = marcadores.count { (aFavor, enContra) -> aFavor > enContra },
                empatados = marcadores.count { (aFavor, enContra) -> aFavor == enContra },
                perdidos = marcadores.count { (aFavor, enContra) -> aFavor < enContra },
                golesAFavor = marcadores.sumOf { it.first },
                golesEnContra = marcadores.sumOf { it.second },
                puntos = marcadores.sumOf { (aFavor, enContra) -> reglamento.puntosPor(aFavor, enContra) },
            )
        }
        .sortedWith(
            compareByDescending<FilaDePosicion> { it.puntos }
                .thenByDescending { it.diferencia }
                .thenByDescending { it.golesAFavor }
                .thenBy { it.equipo.nombre },
        )
}

// Tabla de goleadores: cada jugador con sus goles, de más a menos.
// Usa una Sequence: las operaciones se encadenan y se ejecutan elemento a
// elemento, sin crear una lista intermedia entre paso y paso.
fun List<Partido>.goleadores(): List<Pair<Jugador, Int>> =
    asSequence()
        .flatMap { it.eventos.asSequence() }
        .filterIsInstance<Gol>()
        .groupingBy { it.jugador }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<Jugador, Int>> { it.second }.thenBy { it.first.nombre })
