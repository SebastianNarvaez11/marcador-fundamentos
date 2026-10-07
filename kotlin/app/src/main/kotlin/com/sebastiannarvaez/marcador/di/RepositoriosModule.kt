package com.sebastiannarvaez.marcador.di

import com.sebastiannarvaez.marcador.data.ArbitrosRepositoryDosFuentes
import com.sebastiannarvaez.marcador.data.red.CronicasRepositoryRed
import com.sebastiannarvaez.marcador.domain.ArbitrosRepository
import com.sebastiannarvaez.marcador.domain.CronicasRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// CAPA DE REPOSITORIOS (ver el resumen de capas en DatosModule).
// REPOSITORIOS: «quien pide la interfaz, recibe esta implementacion».
// @Binds sirve cuando la implementacion ya tiene @Inject constructor: Hilt sabe
// construirla y solo falta decir para que interfaz. Las funciones con @Binds no tienen
// cuerpo (son `abstract`), asi que el modulo es una `abstract class`, no un `object`.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    // Quien pida ArbitrosRepository recibe ArbitrosRepositoryDosFuentes. Cambiar de implementacion es
    // cambiar ESTA linea: ni el ViewModel ni la pantalla se enteran.
    @Binds
    @Singleton
    abstract fun arbitros(impl: ArbitrosRepositoryDosFuentes): ArbitrosRepository

    @Binds
    @Singleton
    abstract fun cronicas(impl: CronicasRepositoryRed): CronicasRepository
}
