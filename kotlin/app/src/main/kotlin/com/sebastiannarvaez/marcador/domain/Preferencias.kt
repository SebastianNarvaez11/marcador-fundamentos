package com.sebastiannarvaez.marcador.domain

import kotlinx.coroutines.flow.Flow

// f46 · Los ajustes del usuario, como los ve el dominio: un Flow con el valor actual y una
// funcion `suspend` para cambiarlo. Igual que PartidosRepository: el dominio dice QUE, la
// capa de datos (DataStore) dice COMO.
interface PreferenciasRepository {
    // 90 o 60 minutos. Se emite el valor guardado y otra vez cada vez que cambia.
    val duracionDelPartido: Flow<Int>

    suspend fun cambiarDuracion(minutos: Int)

    companion object {
        val DURACIONES = listOf(90, 60)
        const val DURACION_POR_DEFECTO = 90
    }
}
