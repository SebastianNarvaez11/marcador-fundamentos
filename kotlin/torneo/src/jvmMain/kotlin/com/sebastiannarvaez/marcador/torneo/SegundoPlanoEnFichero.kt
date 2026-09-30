package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

// Escribir el JSON es esperar al disco: IO.
suspend fun Torneo.guardarEnSegundoPlano(fichero: File, dispatcher: CoroutineDispatcher = Dispatchers.IO): Result<Unit> =
    withContext(dispatcher) { guardar(fichero) }

suspend fun cargarTorneoEnSegundoPlano(fichero: File): Result<Torneo> =
    withContext(Dispatchers.IO) { cargarTorneo(fichero) }

// Las dos cosas a la vez, cada una en su dispatcher. Si el guardado falla, se
// lanza el error (`getOrThrow`) al esperar el resultado.
suspend fun Torneo.cerrarJornada(fichero: File): List<FilaDePosicion> = coroutineScope {
    val guardado = async { guardarEnSegundoPlano(fichero) }
    val tabla = async { tablaEnSegundoPlano() }
    guardado.await().getOrThrow()
    tabla.await()
}
