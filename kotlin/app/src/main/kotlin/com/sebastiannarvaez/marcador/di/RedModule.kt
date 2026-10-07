package com.sebastiannarvaez.marcador.di

import com.sebastiannarvaez.marcador.data.red.LigaApi
import com.sebastiannarvaez.marcador.data.red.crearLigaApi
import com.sebastiannarvaez.marcador.data.red.crearOkHttp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

// CAPA DE RED (ver el resumen de capas en DatosModule). OkHttpClient y Retrofit no son clases nuestras (no podemos ponerles
// @Inject): se dan con @Provides, que es «la receta» para construirlas. Las recetas
// reutilizan crearOkHttp() y crearLigaApi() de LigaApi.kt.
// @Singleton: UN solo cliente para toda la app (comparte conexiones e hilos).
@Module
@InstallIn(SingletonComponent::class)
object RedModule {
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = crearOkHttp()

    // Hilt ve que esta receta pide un OkHttpClient y le pasa el de arriba.
    @Provides
    @Singleton
    fun ligaApi(cliente: OkHttpClient): LigaApi = crearLigaApi(cliente)
}
