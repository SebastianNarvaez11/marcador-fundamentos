package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

// Un Dispatcher decide EN QUÉ HILOS corre una corrutina:
//   Dispatchers.Default -> tantos hilos como núcleos; para CALCULAR.
//   Dispatchers.IO      -> muchos hilos, pensado para ESPERAR (disco, red).
//   Dispatchers.Main    -> el hilo de la interfaz. Solo existe donde hay UI
//                          (Android, Swing…): en una consola no lo hay.
//
// `withContext(dispatcher) { ... }` cambia de hilo durante el bloque, espera a
// que acabe y devuelve su resultado. La función que lo llama sigue siendo
// secuencial para quien la lee.

// Escribir el JSON es esperar al disco: IO.
suspend fun Torneo.guardarEnSegundoPlano(fichero: File): Result<Unit> =
    withContext(Dispatchers.IO) { guardar(fichero) }

suspend fun cargarTorneoEnSegundoPlano(fichero: File): Result<Torneo> =
    withContext(Dispatchers.IO) { cargarTorneo(fichero) }

// Calcular la tabla es CPU: Default. Se toma una copia de los partidos ANTES de
// cambiar de hilo: el Torneo es mutable y no está preparado para que otro hilo
// lo lea mientras alguien registra un partido.
suspend fun Torneo.tablaEnSegundoPlano(): List<FilaDePosicion> {
    val copiaDePartidos = partidos
    val equiposInscritos = equipos
    val reglas = reglamento
    return withContext(Dispatchers.Default) { copiaDePartidos.tablaDePosiciones(equiposInscritos, reglas) }
}

// Las dos cosas a la vez, cada una en su dispatcher. Si el guardado falla, se
// lanza el error (`getOrThrow`) al esperar el resultado.
suspend fun Torneo.cerrarJornada(fichero: File): List<FilaDePosicion> = coroutineScope {
    val guardado = async { guardarEnSegundoPlano(fichero) }
    val tabla = async { tablaEnSegundoPlano() }
    guardado.await().getOrThrow()
    tabla.await()
}
