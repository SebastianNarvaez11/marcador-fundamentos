package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals

class LambdasTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))
    private val partido = Partido(rayo, toros)
        .registrar(Gol(10, ana, rayo))
        .registrar(Tarjeta(30, luis, ColorDeTarjeta.AMARILLA))
        .registrar(Gol(50, luis, toros))
        .registrar(Gol(70, ana, rayo))

    @Test
    fun filtraEventosConTrailingLambda() {
        assertEquals(1, partido.eventos { it is Tarjeta }.size)
        assertEquals(3, partido.eventos(esGol).size)
    }

    @Test
    fun goleadoresRepiteAlQueMarcaVarias() {
        assertEquals(listOf(ana, luis, ana), partido.goleadores())
    }

    @Test
    fun minutosDeGolSaltaLosEventosQueNoSonGoles() {
        assertEquals(listOf(10, 70), partido.minutosDeGolDe(ana))
        assertEquals(listOf(50), partido.minutosDeGolDe(luis))
    }

    @Test
    fun funcionQueDevuelveFuncion() {
        assertEquals(1, partido.eventos(antesDelMinuto(20)).size)
        assertEquals(3, partido.eventos(antesDelMinuto(60)).size)
    }

    @Test
    fun repetirEjecutaLaAccionCadaVez() {
        val vueltas = mutableListOf<Int>()
        repetir(3) { vueltas += it }
        assertEquals(listOf(1, 2, 3), vueltas)
    }

    @Test
    fun referenciaLigadaAUnaFuncion() {
        val goles = listOf(rayo, toros).map(partido::golesDe)
        assertEquals(listOf(2, 1), goles)
        assertEquals(true, esDelMinutoOAntes(partido.eventos.first(), 10))
    }
}
