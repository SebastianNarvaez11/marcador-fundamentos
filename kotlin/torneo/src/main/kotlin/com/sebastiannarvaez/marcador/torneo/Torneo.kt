package com.sebastiannarvaez.marcador.torneo

// El torneo es lo único mutable del dominio: va acumulando partidos.
// Por eso cuida qué enseña hacia fuera.
class Torneo(val nombre: String, equipos: List<Equipo>) {

    // Copia defensiva de lo que entra.
    val equipos: List<Equipo> = equipos.toList()

    // `val` NO significa inmutable: la referencia no cambia, pero la lista sí
    // puede cambiar por dentro. Es `private` para que nadie de fuera la toque.
    private val partidosJugados = mutableListOf<Partido>()

    // Lo que sale es una COPIA de solo lectura. Si devolviéramos
    // `partidosJugados` directamente, el tipo `List` engañaría: por dentro
    // sigue siendo una MutableList y un `as MutableList` la vaciaría.
    val partidos: List<Partido>
        get() = partidosJugados.toList()

    fun agregar(partido: Partido) {
        require(partido.local in equipos && partido.visitante in equipos) {
            "Los dos equipos deben estar inscritos en $nombre"
        }
        partidosJugados.add(partido)
    }

    fun tablaDePosiciones(): List<FilaDePosicion> = tablaDePosiciones(partidosJugados, equipos)

    fun goleadores(): List<Pair<Jugador, Int>> = tablaDeGoleadores(partidosJugados)
}
