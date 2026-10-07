package com.sebastiannarvaez.marcador.di

import com.sebastiannarvaez.marcador.data.PartidosRepositoryEnMemoria
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// UN MODULO de Hilt: recetas para lo que Hilt no sabe construir solo. Se llama
// DatosModule porque da piezas de la capa de datos (mas adelante, tambien la base de datos).
//
// PartidosRepository es una interfaz (no se puede construir) y su implementacion tiene parametros con valor
// por defecto, que Hilt no entiende. Asi que se escribe la receta a mano con @Provides.
//
// @InstallIn(SingletonComponent::class): estas recetas viven en el contenedor de la app,
// el que dura lo que el proceso.
@Module
@InstallIn(SingletonComponent::class)
object DatosModule {
    // @Singleton: UNA sola instancia para toda la app. Todos los que pidan el repositorio
    // reciben el mismo, y por eso ven los mismos goles.
    @Provides
    @Singleton
    fun partidos(): PartidosRepository = PartidosRepositoryEnMemoria()
}
