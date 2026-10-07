package com.sebastiannarvaez.marcador.di

import android.content.Context
import com.sebastiannarvaez.marcador.data.PreferenciasDataStore
import com.sebastiannarvaez.marcador.data.room.MarcadorDatabase
import com.sebastiannarvaez.marcador.data.room.PartidoDao
import com.sebastiannarvaez.marcador.data.room.PartidosRepositoryRoom
import com.sebastiannarvaez.marcador.data.room.crearBaseDeDatos
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.domain.PreferenciasRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// CAPA DE DATOS LOCALES: todo lo que guarda en el movil. Es un `object` con recetas
// (@Provides): ninguna de estas piezas se puede construir con un simple @Inject.
@Module
@InstallIn(SingletonComponent::class)
object DatosModule {
    // UNA sola base (@Singleton): abrir dos sobre el mismo fichero es un error clasico.
    // @ApplicationContext: Hilt da el Context de la Application, nunca el de una Activity
    // (guardarlo seria una fuga de memoria).
    @Provides
    @Singleton
    fun baseDeDatos(@ApplicationContext contexto: Context): MarcadorDatabase = crearBaseDeDatos(contexto)

    // Sin @Singleton: el DAO lo guarda la propia base; pedirlo dos veces es barato.
    @Provides
    fun partidoDao(base: MarcadorDatabase): PartidoDao = base.partidoDao()

    // Los ajustes, en DataStore. @Singleton: DataStore exige UNA instancia por fichero.
    // PreferenciasDataStore tiene dos constructores; la receta usa el del Context.
    @Provides
    @Singleton
    fun preferencias(@ApplicationContext contexto: Context): PreferenciasRepository = PreferenciasDataStore(contexto)

    // Un alcance que dura lo que el proceso: para trabajo que no es de ninguna pantalla
    // (sembrar la base). SupervisorJob: un fallo no cancela el resto.
    @Provides
    @Singleton
    @AlcanceDeApp
    fun alcanceDeApp(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // El repositorio de partidos, ahora con Room detras. Va con receta (@Provides) porque
    // PartidosRepositoryRoom tiene un parametro con valor por defecto (los equipos), que
    // Hilt no entiende, y recibe el alcance con su etiqueta. Quien pide PartidosRepository
    // no se entera del cambio.
    @Provides
    @Singleton
    fun partidos(dao: PartidoDao, @AlcanceDeApp alcance: CoroutineScope): PartidosRepository =
        PartidosRepositoryRoom(dao, alcance)
}
