package com.sebastiannarvaez.marcador.feature.arbitros.data

import com.sebastiannarvaez.marcador.core.database.ArbitroDao
import com.sebastiannarvaez.marcador.core.database.ArbitroEntity
import com.sebastiannarvaez.marcador.core.network.LigaApi
import com.sebastiannarvaez.marcador.core.network.aErrorDeRed
import com.sebastiannarvaez.marcador.feature.arbitros.domain.Arbitro
import com.sebastiannarvaez.marcador.feature.arbitros.domain.ArbitrosRepository
import com.sebastiannarvaez.marcador.torneo.intentar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// El repositorio con DOS fuentes: Room (la local) y la red. Room MANDA: es la fuente de
// verdad. La red solo sirve para ponerla al dia. Vive en feature/arbitros/data y no en
// core/network ni en core/database porque conoce a las dos.
class ArbitrosRepositoryDosFuentes @Inject constructor(
    private val api: LigaApi,
    private val dao: ArbitroDao,
) : ArbitrosRepository {

    // Lo que ve la pantalla sale SIEMPRE de la base, traducido a dominio.
    override fun observarArbitros(): Flow<List<Arbitro>> =
        dao.observarArbitros().map { filas -> filas.map { Arbitro(it.id, it.nombre, it.ciudad) } }

    // Red -> base. Al guardar, el Flow de arriba vuelve a emitir y la pantalla se entera
    // sola. Si la red falla, la base no se toca y el fallo sale traducido.
    override suspend fun refrescar(): Result<Unit> = intentar {
        val dtos = api.arbitros()
        dao.guardar(dtos.map { ArbitroEntity(id = it.id, nombre = it.name, ciudad = it.address.city) })
    }.recoverCatching { throw it.aErrorDeRed() }
}
