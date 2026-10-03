package com.sebastiannarvaez.marcador.torneo

// `Int?` es un Int que puede ser null: en el amateur hay jugadores sin dorsal.
// `data class`: el compilador genera equals, hashCode, toString, copy y
// componentN a partir de las propiedades del constructor primario.
data class Jugador(val nombre: String, val dorsal: Int?) {

    // `?.let`: el bloque solo corre si el dorsal no es null.
    // `?:` (elvis): el valor de reserva cuando lo de la izquierda es null.
    fun dorsalOGuion(): String = dorsal?.let { "#$it" } ?: "-"

    // `!!` promete «esto no es null». Si mientes, salta NullPointerException.
    // Existe para que la lección lea la pila de llamadas; no lo uses en código real.
    fun dorsalForzado(): Int = dorsal!!

    // Smart cast: tras comprobar `dorsal != null`, el compilador ya lo trata
    // como Int (no como Int?) dentro del `if`.
    fun esPortero(): Boolean {
        if (dorsal != null) {
            return dorsal == 1
        }
        return false
    }

    // `?.` encadena: si algo es null, todo el resultado es null.
    fun longitudDelNombre(): Int? = nombre.takeIf { it.isNotBlank() }?.length
}

// Smart cast sobre un parámetro: `jugador` es Jugador? y se convierte a Jugador.
fun presentar(jugador: Jugador?): String {
    if (jugador == null) return "Sin jugador"
    return "${jugador.nombre} (${jugador.dorsalOGuion()})"
}

// `try` como expresión: su valor es el del bloque que se ejecute.
// Aquí, un texto que no es un número produce null en vez de tumbar el programa.
fun dorsalDesdeTexto(texto: String): Int? =
    try {
        texto.trim().toInt()
    } catch (error: NumberFormatException) {
        null
    }
