package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals

class EventoTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))

    @Test
    fun describeCadaTipoDeEvento() {
        assertEquals("12' Gol de Ana (Rayo)", describir(Gol(12, ana, rayo)))
        assertEquals("30' Tarjeta amarilla para Luis", describir(Tarjeta(30, luis, ColorDeTarjeta.AMARILLA)))
        assertEquals("85' Cambio: sale Ana, entra Luis", describir(Cambio(85, ana, luis)))
    }

    @Test
    fun victoriaDelLocal() {
        val partido = Partido(rayo, toros).registrar(Gol(5, ana, rayo))
        assertEquals(Resultado.Victoria(ganador = rayo, perdedor = toros), partido.resultado())
        assertEquals("Gana Rayo", partido.resultado().titular())
    }

    @Test
    fun victoriaDelVisitante() {
        val partido = Partido(rayo, toros).registrar(Gol(5, luis, toros))
        assertEquals("Gana Toros", partido.resultado().titular())
    }

    @Test
    fun empateSinGoles() {
        assertEquals(Resultado.Empate, Partido(rayo, toros).resultado())
        assertEquals("Empate", Resultado.Empate.titular())
    }

    @Test
    fun elEnumConoceTodosSusValores() {
        assertEquals(listOf("Amarilla", "Roja"), ColorDeTarjeta.entries.map { it.etiqueta })
        assertEquals(ColorDeTarjeta.ROJA, ColorDeTarjeta.valueOf("ROJA"))
    }
}
