package com.sebastiannarvaez.marcador.torneo

// Función de nivel superior: no vive dentro de ninguna clase.
// Función de expresión: el cuerpo es un único `when` que devuelve el valor.
fun puntosPor(golesAFavor: Int, golesEnContra: Int): Int = when {
    golesAFavor > golesEnContra -> 3
    golesAFavor == golesEnContra -> 1
    else -> 0
}

// Parámetros con valor por defecto: se pueden omitir al llamar.
fun encabezadoDeJornada(numero: Int, totalDeJornadas: Int, prefijo: String = "Jornada"): String =
    "$prefijo $numero de $totalDeJornadas"
