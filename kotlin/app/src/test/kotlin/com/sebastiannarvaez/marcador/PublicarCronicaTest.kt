package com.sebastiannarvaez.marcador

import com.sebastiannarvaez.marcador.domain.CronicaPublicada
import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.domain.PublicarCronica
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// La regla de la cronica, probada SIN pantalla y SIN red: dos repositorios falsos.
class PublicarCronicaTest {

    @Test
    fun escribeElTituloYUnaLineaPorGol() = runTest {
        // partidoConGoles (Falsos.kt): Rayo FC 2-1 Toros, goles al 15, 30 y 60.
        val cronicas = FakeCronicasRepository()
        val publicar = PublicarCronica(FakePartidosRepository(listOf(PartidoDeLista(1, partidoConGoles()))), cronicas)

        val resultado = publicar(1)

        val texto = "15' Ana (Rayo FC)\n30' Iván (Toros)\n60' Ana (Rayo FC)"
        assertEquals(Result.success(CronicaPublicada(101, "Rayo FC 2-1 Toros", texto)), resultado)
        assertEquals(listOf("Rayo FC 2-1 Toros" to texto), cronicas.publicadas)
    }

    @Test
    fun unPartidoQueNoExisteFallaYNoPublicaNada() = runTest {
        val cronicas = FakeCronicasRepository()
        val resultado = PublicarCronica(FakePartidosRepository(), cronicas)(7)

        assertEquals("No existe el partido 7", resultado.exceptionOrNull()?.message)
        assertTrue(cronicas.publicadas.isEmpty())
    }
}
