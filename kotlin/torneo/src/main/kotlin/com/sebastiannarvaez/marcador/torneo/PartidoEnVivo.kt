package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

// Un partido que se está jugando. `guion` es todo lo que va a pasar.
class PartidoEnVivo(val guion: Partido, val msPorMinuto: Long = 10) {

    // Un Flow FRÍO: `flow { ... }` no hace nada hasta que alguien lo recoge con
    // `collect` (o `first`, `toList`, `fold`…). Y cada `collect` ejecuta el bloque
    // ENTERO otra vez, desde el principio: dos espectadores = dos partidos
    // independientes. `emit` entrega un valor y suspende hasta que el que recoge
    // lo ha procesado.
    //
    // `flowOn` cambia el dispatcher de lo que está ANTES (el `flow { }` y sus
    // operadores anteriores); quien recoge sigue en su propio contexto.
    fun eventos(): Flow<EventoDePartido> = flow {
        guion.eventos.filter { it.minuto == 0 }.forEach { emit(it) }
        val ultimoMinuto = maxOf(MINUTOS_DEL_PARTIDO, guion.eventos.maxOfOrNull { it.minuto } ?: 0)
        for (minuto in 1..ultimoMinuto) {
            delay(msPorMinuto)
            guion.eventos.filter { it.minuto == minuto }.forEach { emit(it) }
        }
    }.flowOn(Dispatchers.Default)

    // Operadores: cada uno devuelve OTRO Flow, y sigue sin ejecutarse nada hasta
    // que se recoja el resultado.
    fun narracion(): Flow<String> = eventos().map(::describir)

    fun goles(): Flow<Gol> = eventos().filterIsInstance<Gol>()

    // `first()` es un operador terminal que se queda con el primer valor y CANCELA
    // el resto del flujo: el partido no se sigue jugando por nada.
    suspend fun primerGol(): Gol = goles().first()
}
