package com.sebastiannarvaez.marcador.feature.partidos.data

import com.sebastiannarvaez.marcador.core.database.GolEntity
import com.sebastiannarvaez.marcador.core.database.JugadorEntity
import com.sebastiannarvaez.marcador.core.database.PartidoDao
import com.sebastiannarvaez.marcador.core.database.PartidoEntity
import com.sebastiannarvaez.marcador.feature.partidos.domain.Goleador
import com.sebastiannarvaez.marcador.feature.partidos.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.feature.partidos.domain.PartidosRepository
import com.sebastiannarvaez.marcador.torneo.Equipo
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.Jugador
import com.sebastiannarvaez.marcador.torneo.Partido
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

// El repositorio con Room detras. Implementa LA MISMA interfaz que el de memoria (ya borrado):
// ni los casos de uso ni los ViewModels cambiaron. Su trabajo es TRADUCIR entre filas
// (entidades) y dominio, y ser el unico que conoce el DAO.
//
// Desde la version 4 los equipos tambien salen de la base: su plantilla es la tabla `jugador`.
class PartidosRepositoryRoom(
    private val dao: PartidoDao,
    alcance: CoroutineScope,
) : PartidosRepository {

    // Primera vez que se abre la app: la base esta vacia y se llena con los datos de ejemplo.
    // Es un Job: quien lee ESPERA a que termine (`join`), o veria la base a medio llenar.
    // Los jugadores van PRIMERO y con su propia comprobacion: en un movil que ya tenia la
    // version 3, la migracion crea la tabla `jugador` vacia pero los partidos ya estan.
    private val sembrado = alcance.launch {
        if (dao.contarJugadores() == 0) {
            dao.insertarJugadores(
                DatosDeEjemplo.torneo.equipos.flatMap { equipo ->
                    equipo.plantilla.map { JugadorEntity(equipo = equipo.nombre, nombre = it.nombre, dorsal = it.dorsal) }
                },
            )
        }
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

    // TRES tablas; `combine` reconstruye la lista cada vez que CUALQUIERA cambia: un gol nuevo
    // (INSERT en `gol`) o un jugador nuevo (INSERT en `jugador`) refrescan la lista sin recargar.
    // `onStart { sembrado.join() }`: no se empieza a leer hasta que la siembra termina; sin
    // jugadores no hay equipos con los que montar los partidos.
    override fun observarPartidos(): Flow<List<PartidoDeLista>> =
        combine(dao.observarPartidos(), dao.observarGoles(), dao.observarJugadores()) { partidos, goles, jugadores ->
            val equipos = equiposDesde(jugadores)
            val golesPorPartido = goles.groupBy { it.partidoId }
            partidos.map { PartidoDeLista(it.id, aDominio(it, golesPorPartido[it.id].orEmpty(), equipos)) }
        }.onStart { sembrado.join() }

    override suspend fun partido(id: Int): Partido? {
        sembrado.join()
        val fila = dao.partido(id) ?: return null
        return aDominio(fila, dao.golesDe(id), equiposDesde(dao.jugadores()))
    }

    override suspend fun registrarGol(partidoId: Int, gol: Gol) {
        sembrado.join()
        dao.insertarGol(GolEntity(partidoId = partidoId, minuto = gol.minuto, jugador = gol.jugador.nombre, equipo = gol.equipo.nombre, aMano = true))
    }

    override fun observarGoleadores(): Flow<List<Goleador>> =
        dao.observarGoleadores().map { filas -> filas.map { Goleador(it.jugador, it.goles) } }

    // La plantilla de UN equipo: las filas de la tabla `jugador` con ese nombre, como Jugador.
    override fun observarPlantilla(equipo: String): Flow<List<Jugador>> =
        dao.observarJugadores()
            .map { filas -> filas.filter { it.equipo == equipo }.map { Jugador(it.nombre, it.dorsal) } }
            .onStart { sembrado.join() }

    // El formulario ya comprobo sus reglas. Aun asi, la del DOMINIO sigue mandando: se arma el
    // equipo con el jugador nuevo y, si su dorsal se repitiera, `Equipo` lanza y no se guarda nada.
    override suspend fun anadirJugador(equipo: String, jugador: Jugador) {
        sembrado.join()
        val plantilla = dao.jugadores().filter { it.equipo == equipo }.map { Jugador(it.nombre, it.dorsal) }
        Equipo(equipo, plantilla + jugador)
        dao.insertarJugador(JugadorEntity(equipo = equipo, nombre = jugador.nombre, dorsal = jugador.dorsal))
    }

    // Filas -> equipos. `groupBy` junta los jugadores de cada equipo y conserva el orden (el id).
    // `Equipo(...)` vuelve a pasar su `init`: si la tabla tuviera dos dorsales iguales, lanzaria.
    private fun equiposDesde(jugadores: List<JugadorEntity>): List<Equipo> =
        jugadores.groupBy { it.equipo }.map { (nombre, filas) -> Equipo(nombre, filas.map { Jugador(it.nombre, it.dorsal) }) }

    // Fila -> dominio. Los equipos se buscan POR NOMBRE en la lista que se acaba de construir:
    // `Equipo` no define `equals`, asi que `Torneo` solo reconoce las MISMAS instancias, y todos
    // los partidos de una lectura comparten los mismos equipos.
    private fun aDominio(fila: PartidoEntity, goles: List<GolEntity>, equipos: List<Equipo>): Partido {
        fun equipo(nombre: String): Equipo = equipos.firstOrNull { it.nombre == nombre } ?: error("Equipo desconocido: $nombre")
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
}
