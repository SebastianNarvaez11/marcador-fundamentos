package com.sebastiannarvaez.marcador.domain

import com.sebastiannarvaez.marcador.torneo.Equipo
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Partido
import com.sebastiannarvaez.marcador.torneo.Torneo
import kotlinx.coroutines.flow.Flow

// La lista necesita un identificador ESTABLE para cada fila (la `key` de LazyColumn, f35).
// `Partido` no tiene id (dos partidos pueden repetir equipos), asi que se envuelve.
data class PartidoDeLista(val id: Int, val partido: Partido)

// f43 · REPOSITORIO: la UNICA puerta a los datos.
//
// Es una INTERFAZ en la capa de dominio: el dominio dice QUE necesita («dame los
// partidos», «guarda este gol») y la capa de datos decide COMO (memoria, Room, red).
// Quien la usa (un caso de uso, un ViewModel) no sabe de donde salen los datos ni si
// cambian: hoy es una lista en memoria, en f45 sera Room, y nadie mas se entera.
//
// Reglas del repositorio:
//   - devuelve y recibe tipos de DOMINIO (Partido, Gol), nunca entidades de Room ni DTO;
//   - lo que cambia con el tiempo se expone como Flow; lo que ocurre una vez, como `suspend`;
//   - es la fuente de la verdad: quien quiera saber los partidos, los OBSERVA aqui.
interface PartidosRepository {
    val equipos: List<Equipo>

    fun observarPartidos(): Flow<List<PartidoDeLista>>

    suspend fun partido(id: Int): Partido?

    suspend fun registrarGol(partidoId: Int, gol: Gol)
}

// Las tablas y los goleadores no se guardan: se CALCULAN a partir de los partidos. Guardar
// ademas la tabla seria tener dos fuentes de verdad que se pueden contradecir.
fun armarTorneo(equipos: List<Equipo>, partidos: List<PartidoDeLista>): Torneo =
    Torneo("Copa Barrio", equipos).also { torneo ->
        partidos.forEach { torneo.registrar(it.partido).getOrThrow() }
    }
