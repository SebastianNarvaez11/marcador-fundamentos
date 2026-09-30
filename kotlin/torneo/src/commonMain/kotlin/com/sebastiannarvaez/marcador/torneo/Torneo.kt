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

    // Registrar puede fallar (equipo no inscrito, gol de alguien ajeno…), y eso
    // no es una situación excepcional sino esperable. Por eso devuelve un
    // `Result<Partido>`: o el partido registrado, o el error que lo impidió.
    // `runCatching` ejecuta el bloque y convierte lo que lance en un Result.failure.
    fun registrar(partido: Partido): Result<Partido> = runCatching {
        require(partido.local !== partido.visitante) { "Un equipo no puede jugar contra sí mismo" }
        require(partido.local in equipos && partido.visitante in equipos) {
            "Los dos equipos deben estar inscritos en $nombre"
        }
        partido.eventos.forEach { validar(it, partido) }
        partido.also { partidosJugados.add(it) }
    }

    fun registrar(
        local: Equipo,
        visitante: Equipo,
        eventos: List<EventoDePartido> = emptyList(),
    ): Result<Partido> = registrar(Partido(local, visitante, eventos))

    private fun validar(evento: EventoDePartido, partido: Partido) {
        require(evento.minuto in 0..120) { "Minuto fuera del partido: ${evento.minuto}" }
        val jugadoresDelPartido = partido.local.plantilla + partido.visitante.plantilla
        // `when` como sentencia sobre un tipo sellado: también debe ser exhaustivo.
        when (evento) {
            is Gol -> {
                require(evento.equipo == partido.local || evento.equipo == partido.visitante) {
                    "El gol es de un equipo que no juega: ${evento.equipo.nombre}"
                }
                require(evento.jugador in evento.equipo.plantilla) {
                    "${evento.jugador.nombre} no juega en ${evento.equipo.nombre}"
                }
            }
            is Tarjeta -> require(evento.jugador in jugadoresDelPartido) {
                "${evento.jugador.nombre} no juega este partido"
            }
            is Cambio -> require(evento.sale in jugadoresDelPartido && evento.entra in jugadoresDelPartido) {
                "El cambio implica a alguien que no juega este partido"
            }
        }
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
