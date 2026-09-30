package com.sebastiannarvaez.marcador.torneo

// El torneo es lo único mutable del dominio: va acumulando partidos.
// Por eso cuida qué enseña hacia fuera.
// Por composición: el torneo TIENE un reglamento (se lo dan de fuera) en vez de
// ser un tipo de reglamento. Cambiar de reglas no obliga a cambiar de clase.
class Torneo(
    val nombre: String,
    equipos: List<Equipo>,
    val reglamento: Reglamento = ReglamentoLiga,
) {

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

    fun tablaDePosiciones(): List<FilaDePosicion> = partidosJugados.tablaDePosiciones(equipos, reglamento)

    fun goleadores(): Ranking<Jugador> = partidosJugados.goleadores()

    // `companion object`: lo que en otros lenguajes es «estático». Se usa con el
    // nombre de la clase: Torneo.conEquipos(...), Torneo.MINIMO_DE_EQUIPOS.
    companion object {
        // `const val`: constante conocida al compilar (solo tipos básicos).
        const val MINIMO_DE_EQUIPOS = 2

        fun conEquipos(nombre: String, vararg equipos: Equipo): Torneo {
            require(equipos.size >= MINIMO_DE_EQUIPOS) {
                "Un torneo necesita al menos $MINIMO_DE_EQUIPOS equipos"
            }
            return Torneo(nombre, equipos.toList())
        }
    }
}
