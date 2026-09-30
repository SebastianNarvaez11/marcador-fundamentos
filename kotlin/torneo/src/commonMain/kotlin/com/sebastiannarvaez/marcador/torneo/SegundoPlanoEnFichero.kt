package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

// Escribir el JSON es esperar al disco: IO. En commonMain hace falta `import kotlinx.coroutines.IO`:
// sin el, `Dispatchers.IO` da «Cannot access 'val IO'» (es `internal` en la parte comun).
suspend fun Torneo.guardarEnSegundoPlano(ruta: String, dispatcher: CoroutineDispatcher = Dispatchers.IO): Result<Unit> =
    withContext(dispatcher) { guardar(ruta) }

suspend fun cargarTorneoEnSegundoPlano(ruta: String): Result<Torneo> =
    withContext(Dispatchers.IO) { cargarTorneo(ruta) }

// Las dos cosas a la vez, cada una en su dispatcher. Si el guardado falla, se
// lanza el error (`getOrThrow`) al esperar el resultado.
suspend fun Torneo.cerrarJornada(ruta: String): List<FilaDePosicion> = coroutineScope {
    val guardado = async { guardarEnSegundoPlano(ruta) }
    val tabla = async { tablaEnSegundoPlano() }
    guardado.await().getOrThrow()
    tabla.await()
}
