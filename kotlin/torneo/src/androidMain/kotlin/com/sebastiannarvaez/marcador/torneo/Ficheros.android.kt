package com.sebastiannarvaez.marcador.torneo

import java.io.File

// Android es una JVM: el actual es el mismo que el de jvmMain. Kotlin no lo comparte solo
// (jvm y android son dos targets distintos), asi que se repiten.
actual fun leerTexto(ruta: String): String = File(ruta).readText()

actual fun escribirTexto(ruta: String, texto: String) = File(ruta).writeText(texto)
