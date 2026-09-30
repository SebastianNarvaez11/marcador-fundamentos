package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Un Dispatcher decide EN QUÉ HILOS corre una corrutina:
//   Dispatchers.Default -> tantos hilos como núcleos; para CALCULAR.
//   Dispatchers.IO      -> muchos hilos, pensado para ESPERAR (disco, red).
//   Dispatchers.Main    -> el hilo de la interfaz. Solo existe donde hay UI
//                          (Android, Swing…): en una consola no lo hay.
//
// `withContext(dispatcher) { ... }` cambia de hilo durante el bloque, espera a
// que acabe y devuelve su resultado. La función que lo llama sigue siendo
// secuencial para quien la lee.

// Calcular la tabla es CPU: Default. Se toma una copia de los partidos ANTES de
// cambiar de hilo: el Torneo es mutable y no está preparado para que otro hilo
// lo lea mientras alguien registra un partido.
suspend fun Torneo.tablaEnSegundoPlano(dispatcher: CoroutineDispatcher = Dispatchers.Default): List<FilaDePosicion> {
    val copiaDePartidos = partidos
    val equiposInscritos = equipos
    val reglas = reglamento
    return withContext(dispatcher) { copiaDePartidos.tablaDePosiciones(equiposInscritos, reglas) }
}
