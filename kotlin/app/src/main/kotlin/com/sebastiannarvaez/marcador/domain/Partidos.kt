package com.sebastiannarvaez.marcador.domain

import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Jugador
import com.sebastiannarvaez.marcador.torneo.Partido
import com.sebastiannarvaez.marcador.torneo.Torneo
import kotlinx.coroutines.flow.Flow

// La lista necesita un identificador ESTABLE para cada fila (la `key` de LazyColumn).
// `Partido` no tiene id (dos partidos pueden repetir equipos), asi que se envuelve.
data class PartidoDeLista(val id: Int, val partido: Partido)

// REPOSITORIO: la UNICA puerta a los datos.
//
// Es una INTERFAZ en la capa de dominio: el dominio dice QUE necesita («dame los
// partidos», «guarda este gol») y la capa de datos decide COMO (memoria, Room, red).
// Quien la usa (un caso de uso, un ViewModel) no sabe de donde salen los datos ni si
// cambian: puede ser una lista en memoria o Room, y nadie mas se entera.
//
// Reglas del repositorio:
//   - devuelve y recibe tipos de DOMINIO (Partido, Gol), nunca entidades de Room ni DTO;
//   - lo que cambia con el tiempo se expone como Flow; lo que ocurre una vez, como `suspend`;
//   - es la fuente de la verdad: quien quiera saber los partidos, los OBSERVA aqui.
interface PartidosRepository {
    fun observarPartidos(): Flow<List<PartidoDeLista>>

    suspend fun partido(id: Int): Partido?

    suspend fun registrarGol(partidoId: Int, gol: Gol)

    fun observarGoleadores(): Flow<List<Goleador>>

    // La plantilla de un equipo, observada: si alguien anade un jugador, vuelve a emitir.
    fun observarPlantilla(equipo: String): Flow<List<Jugador>>
}

// Cuantos goles lleva un jugador (la pantalla de goleadores los pinta). Se calcula en la base con GROUP BY.
data class Goleador(val jugador: String, val goles: Int)

// Las tablas y los goleadores no se guardan: se CALCULAN a partir de los partidos. Guardar
// ademas la tabla seria tener dos fuentes de verdad que se pueden contradecir.
// Los equipos salen de los PROPIOS partidos: son los mismos objetos que llevan dentro (Torneo
// compara equipos por identidad) y vienen de la misma lectura de la base.
fun armarTorneo(partidos: List<PartidoDeLista>): Torneo {
    val equipos = partidos.flatMap { listOf(it.partido.local, it.partido.visitante) }.distinct()
    return Torneo("Copa Barrio", equipos).also { torneo ->
        partidos.forEach { torneo.registrar(it.partido).getOrThrow() }
    }
}
