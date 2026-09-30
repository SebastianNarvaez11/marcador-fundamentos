package com.sebastiannarvaez.marcador.torneo

// Un partido acaba en victoria (de alguien) o en empate.
sealed interface Resultado {
    // Lleva datos: quién ganó y quién perdió.
    data class Victoria(val ganador: Equipo, val perdedor: Equipo) : Resultado

    // No lleva datos: basta un único objeto. `data object` da un toString legible.
    data object Empate : Resultado
}

fun Resultado.titular(): String = when (this) {
    is Resultado.Victoria -> "Gana ${ganador.nombre}"
    Resultado.Empate -> "Empate"
}
