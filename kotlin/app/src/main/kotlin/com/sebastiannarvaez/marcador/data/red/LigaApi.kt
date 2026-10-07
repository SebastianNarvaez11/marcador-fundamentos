package com.sebastiannarvaez.marcador.data.red

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// LA API DE LA LIGA, CON RETROFIT
//
// El servidor es JSONPlaceholder, un servicio de pruebas gratuito: sus «usuarios» hacen
// aqui de arbitros de la liga. Los nombres son inventados y en ingles.
//
// DTO (Data Transfer Object) = la forma del JSON TAL COMO LLEGA. No es el modelo de la app:
// los campos se llaman como en el servidor (`name`, `address`) y solo se declaran los que
// se usan. El resto del JSON (email, phone, company...) se ignora con `ignoreUnknownKeys`.
// El repositorio traduce el DTO a `Arbitro` (dominio) en un solo sitio.
@Serializable
data class ArbitroDto(val id: Int, val name: String, val address: DireccionDto)

@Serializable
data class DireccionDto(val city: String)

// La interfaz: cada funcion es UNA peticion. Retrofit escribe la clase que la cumple.
// `suspend`: la llamada espera fuera del hilo principal (OkHttp tiene sus propios hilos),
// asi que no hace falta `withContext(Dispatchers.IO)`.
interface LigaApi {
    // GET https://jsonplaceholder.typicode.com/users
    @GET("users")
    suspend fun arbitros(): List<ArbitroDto>
}

// La URL base termina en «/»: Retrofit le pega la ruta de cada funcion («users»).
const val URL_LIGA = "https://jsonplaceholder.typicode.com/"

// OkHttp es el motor: abre las conexiones y espera las respuestas. 10 s de timeout: si el
// servidor no contesta en ese tiempo, la llamada falla con una IOException.
fun crearOkHttp(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

fun crearLigaApi(cliente: OkHttpClient, url: String = URL_LIGA): LigaApi {
    val json = Json { ignoreUnknownKeys = true }
    return Retrofit.Builder()
        .baseUrl(url)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
        .build()
        .create(LigaApi::class.java)
}
