package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals

class TablaTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val marta = Jugador("Marta", 1)
    private val rayo = Equipo("Rayo", listOf(ana, marta))
    private val toros = Equipo("Toros", listOf(luis))
    private val lobos = Equipo("Lobos", listOf(Jugador("Pedro", 4)))

    // Rayo 2-1 Toros
    private val primero = Partido(rayo, toros)
        .registrar(Gol(10, ana, rayo))
        .registrar(Gol(20, luis, toros))
        .registrar(Gol(30, marta, rayo))

    // Toros 0-0 Lobos
    private val segundo = Partido(toros, lobos)

    // Lobos 1-3 Rayo
    private val tercero = Partido(lobos, rayo)
        .registrar(Gol(5, Jugador("Pedro", 4), lobos))
        .registrar(Gol(15, ana, rayo))
        .registrar(Gol(25, ana, rayo))
        .registrar(Gol(35, marta, rayo))

    private val partidos = listOf(primero, segundo, tercero)

    @Test
    fun ordenaPorPuntosYCalculaCadaColumna() {
        val tabla = partidos.tablaDePosiciones()
        assertEquals(listOf("Rayo", "Toros", "Lobos"), tabla.map { it.equipo.nombre })
        val filaRayo = tabla.first()
        assertEquals(2, filaRayo.jugados)
        assertEquals(2, filaRayo.ganados)
        assertEquals(6, filaRayo.puntos)
        assertEquals(5, filaRayo.golesAFavor)
        assertEquals(2, filaRayo.golesEnContra)
        assertEquals(3, filaRayo.diferencia)
    }

    @Test
    fun desempataPorDiferenciaDeGoles() {
        // Toros: 1 punto, diferencia -1. Lobos: 1 punto, diferencia -3.
        val tabla = partidos.tablaDePosiciones()
        assertEquals(1, tabla[1].puntos)
        assertEquals(1, tabla[2].puntos)
        assertEquals("Toros", tabla[1].equipo.nombre)
    }

    @Test
    fun incluyeEquiposQueNoHanJugado() {
        val descansa = Equipo("Descansa", emptyList())
        val tabla = partidos.tablaDePosiciones(equipos = listOf(descansa, rayo))
        assertEquals(4, tabla.size)
        assertEquals(0, tabla.last().puntos)
        assertEquals(0, tabla.last().jugados)
    }

    @Test
    fun tablaVaciaSinPartidos() {
        assertEquals(emptyList(), emptyList<Partido>().tablaDePosiciones())
    }

    @Test
    fun goleadoresOrdenadosDeMasAMenos() {
        val tabla = partidos.goleadores()
        assertEquals(
            listOf(ana to 3, marta to 2, luis to 1, Jugador("Pedro", 4) to 1),
            tabla,
        )
    }
}
