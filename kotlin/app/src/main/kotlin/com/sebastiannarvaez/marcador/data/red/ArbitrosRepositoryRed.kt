package com.sebastiannarvaez.marcador.data.red

import com.sebastiannarvaez.marcador.domain.Arbitro
import com.sebastiannarvaez.marcador.domain.ArbitrosRepository

// El repositorio con la RED como unica fuente: pide la lista y traduce DTO -> dominio.
// Es el unico que conoce `LigaApi`; hacia fuera solo salen `Arbitro`.
class ArbitrosRepositoryRed(private val api: LigaApi) : ArbitrosRepository {
    // Los fallos tambien se traducen: hacia fuera no sale ninguna excepcion de Retrofit.
    override suspend fun arbitros(): List<Arbitro> = try {
        api.arbitros().map { Arbitro(id = it.id, nombre = it.name, ciudad = it.address.city) }
    } catch (error: Exception) {
        throw error.aErrorDeRed()
    }
}
