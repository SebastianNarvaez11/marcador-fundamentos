package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormatoTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", null)
    private val rayo = Equipo("Rayo", listOf(ana, luis))
    private val toros = Equipo("Toros", listOf(Jugador("Iván", 7)))

    @Test
    fun etiquetaDeJugador() {
        assertEquals("Ana (#9)", ana.etiqueta())
        assertEquals("Luis (-)", luis.etiqueta())
    }

    @Test
    fun capitanONingunoSeCalculaAlLeer() {
        assertEquals("sin capitán", rayo.capitanONinguno)
        rayo.nombrarCapitan(ana)
        assertEquals("Ana", rayo.capitanONinguno)
    }

    @Test
    fun extensionSobreListaDePartidos() {
        val partidos = listOf(Partido(rayo, toros).registrar(Gol(1, ana, rayo)))
        assertEquals("Rayo", partidos.tablaDePosiciones().first().equipo.nombre)
        assertEquals(listOf(ana to 1), partidos.goleadores())
    }

    @Test
    fun tablaComoTextoTieneEncabezadoYUnaFilaPorEquipo() {
        val torneo = Torneo("Copa", listOf(rayo, toros)).apply {
            agregar(Partido(rayo, toros).registrar(Gol(1, ana, rayo)))
        }
        val lineas = torneo.tablaComoTexto().lines()
        assertEquals(3, lineas.size)
        assertTrue(lineas[1].contains("Rayo"))
    }

    @Test
    fun resumenDeEquipoUsaLasFuncionesDeAlcance() {
        rayo.nombrarCapitan(ana)
        assertEquals(
            "Rayo tiene 2 jugadores. Capitán: Ana (#9) (1 con dorsal)",
            resumenDeEquipo(rayo),
        )
    }
}
