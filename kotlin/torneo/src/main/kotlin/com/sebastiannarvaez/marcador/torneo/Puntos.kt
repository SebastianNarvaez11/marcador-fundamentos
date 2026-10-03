package com.sebastiannarvaez.marcador.torneo

// Función suelta: escrita directamente en el fichero.
// La regla vive ahora en el reglamento de la liga; esta función se queda
// como atajo y ya no repite los números.
fun puntosPor(golesAFavor: Int, golesEnContra: Int): Int =
    ReglamentoLiga.puntosPor(golesAFavor, golesEnContra)

// Parámetros con valor por defecto: se pueden omitir al llamar.
fun encabezadoDeJornada(numero: Int, totalDeJornadas: Int, prefijo: String = "Jornada"): String =
    "$prefijo $numero de $totalDeJornadas"
