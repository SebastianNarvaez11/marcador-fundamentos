package com.sebastiannarvaez.marcador.feature.cronica.data

import com.sebastiannarvaez.marcador.core.network.CronicaDto
import com.sebastiannarvaez.marcador.core.network.LigaApi
import com.sebastiannarvaez.marcador.core.network.aErrorDeRed
import com.sebastiannarvaez.marcador.feature.cronica.domain.CronicasRepository
import com.sebastiannarvaez.marcador.torneo.intentar
import javax.inject.Inject

// Publica con un POST. `intentar` atrapa el fallo (y relanza la cancelacion);
// `recoverCatching` cambia la excepcion de Retrofit por la de dominio.
class CronicasRepositoryRed @Inject constructor(private val api: LigaApi) : CronicasRepository {
    override suspend fun publicar(titulo: String, texto: String): Result<Int> = intentar {
        api.publicarCronica(CronicaDto(title = titulo, body = texto, userId = 1)).id
    }.recoverCatching { throw it.aErrorDeRed() }
}
