package com.sebastiannarvaez.marcador

import com.sebastiannarvaez.marcador.data.red.CronicaDto
import com.sebastiannarvaez.marcador.data.red.CronicasRepositoryRed
import com.sebastiannarvaez.marcador.domain.ErrorDeRed
import com.sebastiannarvaez.marcador.domain.FalloDeRed
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

// El repositorio de red, probado con la API falsa: sin servidor y sin esperar a nadie.
// No hace falta setMain: aqui no hay ViewModel ni viewModelScope.
class CronicasRepositoryTest {

    @Test
    fun publicaYDevuelveElNumero() = runTest {
        val api = FakeLigaApi()
        val resultado = CronicasRepositoryRed(api).publicar("Rayo FC 2-1 Toros", "12' Ana (Rayo FC)")
        assertEquals(Result.success(101), resultado)
        assertEquals(listOf(CronicaDto("Rayo FC 2-1 Toros", "12' Ana (Rayo FC)", userId = 1)), api.enviadas)
    }

    @Test
    fun sinRedElErrorEsSinConexion() = runTest {
        val api = FakeLigaApi(fallo = IOException("sin red"))
        val error = CronicasRepositoryRed(api).publicar("t", "x").exceptionOrNull()
        assertEquals(ErrorDeRed.SinConexion, (error as FalloDeRed).error)
    }

    @Test
    fun unErrorDelServidorLlevaSuCodigo() = runTest {
        // Una HttpException se construye con una respuesta de error de Retrofit.
        val api = FakeLigaApi(fallo = HttpException(Response.error<Any>(500, "".toResponseBody(null))))
        val error = CronicasRepositoryRed(api).publicar("t", "x").exceptionOrNull()
        assertEquals(ErrorDeRed.Servidor(500), (error as FalloDeRed).error)
    }
}
