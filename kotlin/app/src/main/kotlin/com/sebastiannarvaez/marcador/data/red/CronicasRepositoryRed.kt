package com.sebastiannarvaez.marcador.data.red

import com.sebastiannarvaez.marcador.domain.CronicasRepository
import com.sebastiannarvaez.marcador.torneo.intentar

// Publica con un POST. `intentar` atrapa el fallo (y relanza la cancelacion);
// `recoverCatching` cambia la excepcion de Retrofit por la de dominio.
class CronicasRepositoryRed(private val api: LigaApi) : CronicasRepository {
    override suspend fun publicar(titulo: String, texto: String): Result<Int> = intentar {
        api.publicarCronica(CronicaDto(title = titulo, body = texto, userId = 1)).id
    }.recoverCatching { throw it.aErrorDeRed() }
}
