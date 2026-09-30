package com.sebastiannarvaez.marcador.data.room

import com.sebastiannarvaez.marcador.data.DatosDeEjemplo
import com.sebastiannarvaez.marcador.domain.Goleador
import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.torneo.Equipo
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Partido
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// f45 · El repositorio con Room detras. Implementa LA MISMA interfaz que el de memoria (f43):
// ni los casos de uso ni los ViewModels cambian. Su trabajo es TRADUCIR entre filas
// (entidades) y dominio, y ser el unico que conoce el DAO.
class PartidosRepositoryRoom(
    private val dao: PartidoDao,
    alcance: CoroutineScope,
    override val equipos: List<Equipo> = DatosDeEjemplo.torneo.equipos,
) : PartidosRepository {

    // Primera vez que se abre la app: la base esta vacia y se llena con los partidos de
    // ejemplo. Es un Job: `partido(id)` ESPERA a que termine, o pediria el partido 1 antes
    // de que exista y la pantalla diria «No existe el partido».
    private val sembrado = alcance.launch {
        if (dao.contarPartidos() == 0) {
            dao.insertarPartidos(DatosDeEjemplo.partidos.map { PartidoEntity(it.id, it.partido.local.nombre, it.partido.visitante.nombre) })
            dao.insertarGoles(
                DatosDeEjemplo.partidos.flatMap { fila ->
                    fila.partido.eventos.filterIsInstance<Gol>().map {
                        GolEntity(partidoId = fila.id, minuto = it.minuto, jugador = it.jugador.nombre, equipo = it.equipo.nombre)
                    }
                },
            )
        }
    }

    // Dos flujos de tablas distintas; `combine` (f21) reconstruye la lista cada vez que
    // CUALQUIERA cambia. Asi un gol nuevo (INSERT en `gol`) refresca la lista sin recargar.
    override fun observarPartidos(): Flow<List<PartidoDeLista>> =
        combine(dao.observarPartidos(), dao.observarGoles()) { partidos, goles ->
            val golesPorPartido = goles.groupBy { it.partidoId }
            partidos.map { PartidoDeLista(it.id, aDominio(it, golesPorPartido[it.id].orEmpty())) }
        }

    override suspend fun partido(id: Int): Partido? {
        sembrado.join()
        val fila = dao.partido(id) ?: return null
        return aDominio(fila, dao.golesDe(id))
    }

    override suspend fun registrarGol(partidoId: Int, gol: Gol) {
        sembrado.join()
        dao.insertarGol(GolEntity(partidoId = partidoId, minuto = gol.minuto, jugador = gol.jugador.nombre, equipo = gol.equipo.nombre, aMano = true))
    }

    override fun observarGoleadores(): Flow<List<Goleador>> =
        dao.observarGoleadores().map { filas -> filas.map { Goleador(it.jugador, it.goles) } }

    // Fila -> dominio. Los equipos se buscan en el catalogo POR NOMBRE: `Equipo` no define
    // `equals`, asi que `Torneo` solo reconoce las MISMAS instancias (f04).
    private fun aDominio(fila: PartidoEntity, goles: List<GolEntity>): Partido {
        val local = equipo(fila.local)
        val visitante = equipo(fila.visitante)
        return Partido(
            local, visitante,
            goles.map { gol ->
                val equipo = equipo(gol.equipo)
                Gol(gol.minuto, equipo.plantilla.first { it.nombre == gol.jugador }, equipo)
            },
        )
    }

    private fun equipo(nombre: String): Equipo =
        equipos.firstOrNull { it.nombre == nombre } ?: error("Equipo desconocido: $nombre")
}
