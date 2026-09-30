package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

// El marcador en un instante. Inmutable: cada gol produce uno nuevo.
data class Marcador(val local: Int, val visitante: Int) {
    val goles: Int get() = local + visitante

    override fun toString() = "$local-$visitante"
}

fun Partido.marcador(): Marcador = Marcador(golesLocal, golesVisitante)

// El marcador tras un evento: solo un gol lo cambia.
fun Marcador.despuesDe(evento: EventoDePartido, partido: Partido): Marcador = when {
    evento !is Gol -> this
    evento.equipo == partido.local -> copy(local = local + 1)
    else -> copy(visitante = visitante + 1)
}

// `scan` es como `fold` pero emite cada paso: parte de 0-0 y va emitiendo el
// marcador tras cada evento. `distinctUntilChanged` deja pasar solo los cambios
// (una tarjeta no cambia el marcador, así que no emite nada).
fun PartidoEnVivo.marcadores(): Flow<Marcador> =
    eventos()
        .scan(Marcador(0, 0)) { marcador, evento -> marcador.despuesDe(evento, guion) }
        .distinctUntilChanged()

// Los dos marcadores de la jornada, en un solo Flow.
data class MarcadoresDeJornada(val primero: Marcador, val segundo: Marcador) {
    val golesTotales: Int get() = primero.goles + segundo.goles
}

// `combine` junta varios flujos: cada vez que CUALQUIERA emite, emite la
// combinación de los últimos valores de todos. No emite hasta que todos hayan
// emitido al menos una vez (por eso `scan` parte de 0-0).
fun marcadoresDeLaJornada(primero: PartidoEnVivo, segundo: PartidoEnVivo): Flow<MarcadoresDeJornada> =
    combine(primero.marcadores(), segundo.marcadores(), ::MarcadoresDeJornada)

// `flatMapLatest`: cada vez que llega un partido nuevo a seguir, CANCELA el
// marcador del anterior y empieza a recoger el nuevo. Es lo que quieres cuando
// el usuario cambia de partido en pantalla: no seguir mandando datos del viejo.
@OptIn(ExperimentalCoroutinesApi::class)
fun seguirPartido(elegido: Flow<PartidoEnVivo>): Flow<Marcador> = elegido.flatMapLatest { it.marcadores() }

// `debounce`: espera a que el que escribe se calle `pausaMs` antes de dejar
// pasar la última consulta. «r», «ra», «ray» seguidos producen una sola búsqueda.
fun buscarEquipos(consulta: Flow<String>, equipos: List<Equipo>, pausaMs: Long = 300): Flow<List<Equipo>> =
    consulta
        .debounce(pausaMs)
        .map { it.trim() }
        .distinctUntilChanged()
        .map { texto ->
            if (texto.isEmpty()) emptyList() else equipos.filter { it.nombre.contains(texto, ignoreCase = true) }
        }
