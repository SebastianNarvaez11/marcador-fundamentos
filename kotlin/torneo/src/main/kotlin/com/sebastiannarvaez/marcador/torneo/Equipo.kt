package com.sebastiannarvaez.marcador.torneo

// Una clase es `final` por defecto: nadie puede heredar de Equipo salvo que
// se escriba `open class`. Aquí no hace falta.
//
// `nombre` es `val` en el constructor primario: se convierte en propiedad.
// `plantilla` es solo un parámetro (sin val): existe durante la construcción.
class Equipo(val nombre: String, plantilla: List<Jugador>) {

    // El bloque init corre al construir. `require` lanza IllegalArgumentException
    // con el mensaje si la condición es falsa.
    init {
        require(nombre.isNotBlank()) { "El equipo necesita un nombre" }
        val dorsales = plantilla.mapNotNull { it.dorsal }
        require(dorsales.size == dorsales.toSet().size) {
            "Dorsales repetidos en $nombre: $dorsales"
        }
    }

    val plantilla: List<Jugador> = plantilla

    // Propiedad calculada: sin campo, se evalúa en cada lectura.
    val cantidadDeJugadores: Int
        get() = plantilla.size

    // `private set`: cualquiera lee el capitán, pero solo la clase lo cambia,
    // a través de nombrarCapitan (que valida).
    var capitan: Jugador? = null
        private set

    fun nombrarCapitan(jugador: Jugador) {
        require(jugador in plantilla) { "${jugador.nombre} no juega en $nombre" }
        capitan = jugador
    }

    // `private`: solo visible dentro de la clase. `internal` sería visible en
    // todo el módulo.
    private fun descripcion() = "$nombre ($cantidadDeJugadores jugadores)"

    override fun toString(): String = descripcion()
}
