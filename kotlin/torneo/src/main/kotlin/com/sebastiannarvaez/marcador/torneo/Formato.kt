package com.sebastiannarvaez.marcador.torneo

// Extensiones sobre tipos propios: leen como métodos, pero viven aparte.
// Un miembro de la clase siempre gana a una extensión del mismo nombre, y la
// extensión NO puede ver lo `private` de la clase.
fun Jugador.etiqueta(): String = "$nombre (${dorsalOGuion()})"

// Extensión con propiedad: sin campo, solo calculada.
val Equipo.capitanONinguno: String
    get() = capitan?.nombre ?: "sin capitán"

fun FilaDePosicion.aTexto(puesto: Int): String =
    "%2d  %-8s %2d %2d %2d %2d %3d %2d %3d %3d".format(
        puesto, equipo.nombre, jugados, ganados, empatados, perdidos,
        golesAFavor, golesEnContra, diferencia, puntos,
    )

// `buildString` da un StringBuilder como `this` (lambda con receptor): dentro
// se llama a `appendLine` sin escribir el nombre del builder.
fun Torneo.tablaComoTexto(): String = buildString {
    appendLine("Pos Equipo    PJ  G  E  P  GF GC  DG Pts")
    tablaDePosiciones().forEachIndexed { indice, fila ->
        appendLine(fila.aTexto(indice + 1))
    }
}.trimEnd()

// Las cinco funciones de alcance (scope functions) en una misma función:
//   let   -> `it`   y devuelve el resultado de la lambda
//   run   -> `this` y devuelve el resultado de la lambda
//   with  -> `this` (el objeto va como argumento) y devuelve el resultado
//   apply -> `this` y devuelve EL OBJETO
//   also  -> `it`   y devuelve EL OBJETO
fun resumenDeEquipo(equipo: Equipo): String {
    val jugadoresConDorsal = equipo.plantilla.filter { it.dorsal != null }
    val lineaDeCapitan = equipo.capitan?.let { "Capitán: ${it.etiqueta()}" } ?: "Sin capitán"
    val texto = with(equipo) { "$nombre tiene $cantidadDeJugadores jugadores" }
    return StringBuilder()
        .apply {
            append(texto)
            append(". ")
            append(lineaDeCapitan)
        }
        .also { println("[traza] resumen de ${equipo.nombre} listo") }
        .run { toString() + " (${jugadoresConDorsal.size} con dorsal)" }
}
