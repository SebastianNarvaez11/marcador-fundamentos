package com.sebastiannarvaez.marcador.data

import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.torneo.Equipo
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Partido
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// f43 · La implementacion de datos: una lista en memoria dentro de un StateFlow.
// El StateFlow ES la fuente de la verdad; `observarPartidos` la ofrece de solo lectura.
// Se pierde al morir el proceso: f45 la sustituye por Room sin tocar la interfaz.
class PartidosRepositoryEnMemoria(
    override val equipos: List<Equipo> = DatosDeEjemplo.torneo.equipos,
    inicial: List<PartidoDeLista> = DatosDeEjemplo.partidos,
) : PartidosRepository {

    private val lista = MutableStateFlow(inicial)

    override fun observarPartidos(): Flow<List<PartidoDeLista>> = lista.asStateFlow()

    override suspend fun partido(id: Int): Partido? = lista.value.firstOrNull { it.id == id }?.partido

    override suspend fun registrarGol(partidoId: Int, gol: Gol) {
        lista.update { partidos ->
            partidos.map { if (it.id == partidoId) it.copy(partido = it.partido.registrar(gol)) else it }
        }
    }
}
