package com.sebastiannarvaez.marcador.torneo

import java.io.File

// Guardar y cargar con java.io.File: solo existe en la JVM (y en Android, que es una JVM),
// no en iOS. Se queda en jvmMain; el resto de Guardado.kt (JSON <-> Torneo) es comun.
fun Torneo.guardar(fichero: File): Result<Unit> = runCatching { fichero.writeText(aJson()) }

fun cargarTorneo(fichero: File): Result<Torneo> = runCatching { fichero.readText() }.mapCatching {
    torneoDesdeJson(it).getOrThrow()
}
