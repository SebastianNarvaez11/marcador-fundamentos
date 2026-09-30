package com.sebastiannarvaez.marcador.torneo

import java.io.File

actual fun leerTexto(ruta: String): String = File(ruta).readText()

actual fun escribirTexto(ruta: String, texto: String) = File(ruta).writeText(texto)
