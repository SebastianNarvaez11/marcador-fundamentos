package com.sebastiannarvaez.marcador.torneo

// Una «foto» del torneo en un momento dado: sus partidos ya no cambian.
// Por eso aquí SÍ tiene sentido calcular la tabla una sola vez y guardarla.
//
// `by lazy` es una propiedad delegada: en vez de guardar un valor, delega
// leer la propiedad en otro objeto (el Lazy). El bloque corre la PRIMERA vez
// que alguien lee `tabla`; las siguientes lecturas devuelven el resultado
// guardado.
class InstantaneaDelTorneo(
    val partidos: List<Partido>,
    val equipos: List<Equipo>,
    val reglamento: Reglamento,
) {
    // Solo para que los tests vean cuántas veces se calculó de verdad.
    internal var calculosDeLaTabla = 0
        private set

    val tabla: List<FilaDePosicion> by lazy {
        calculosDeLaTabla++
        partidos.tablaDePosiciones(equipos, reglamento)
    }

    val goleadores: Ranking<Jugador> by lazy { partidos.goleadores() }
}

// `partidos` ya devuelve una copia, así que la foto no se ve afectada por lo que
// se registre después.
fun Torneo.instantanea(): InstantaneaDelTorneo = InstantaneaDelTorneo(partidos, equipos, reglamento)
