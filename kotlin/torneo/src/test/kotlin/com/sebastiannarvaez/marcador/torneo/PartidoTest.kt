package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PartidoTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))

    @Test
    fun registrarUnEventoDevuelveUnaCopia() {
        val inicio = Partido(rayo, toros)
        val despues = inicio.registrar(Gol(10, ana, rayo))
        assertEquals(0, inicio.eventos.size)
        assertEquals(1, despues.eventos.size)
        assertNotSame(inicio, despues)
    }

    @Test
    fun elMarcadorSeDeduceDeLosGoles() {
        val partido = Partido(rayo, toros)
            .registrar(Gol(10, ana, rayo))
            .registrar(Tarjeta(20, luis, ColorDeTarjeta.ROJA))
            .registrar(Gol(30, luis, toros))
            .registrar(Gol(40, ana, rayo))
        assertEquals(2, partido.golesLocal)
        assertEquals(1, partido.golesVisitante)
    }

    @Test
    fun copyCambiaSoloLoIndicado() {
        val partido = Partido(rayo, toros)
        val otro = partido.copy(eventos = listOf(Gol(1, ana, rayo)))
        assertSame(rayo, otro.local)
        assertEquals(1, otro.golesLocal)
    }

    @Test
    fun dobleIgualIgualCompruebaContenido() {
        val a = Jugador("Ana", 9)
        val b = Jugador("Ana", 9)
        assertTrue(a == b)      // equals generado: mismo contenido
        assertFalse(a === b)    // identidad: son dos objetos distintos en memoria
    }

    @Test
    fun copyDeJugadorCambiaElDorsal() {
        assertEquals(Jugador("Ana", 10), ana.copy(dorsal = 10))
    }

    @Test
    fun desestructuracionUsaComponentN() {
        val (nombre, dorsal) = Jugador("Ana", 9)
        assertEquals("Ana", nombre)
        assertEquals(9, dorsal)
        val (local, visitante, eventos) = Partido(rayo, toros)
        assertSame(rayo, local)
        assertSame(toros, visitante)
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun toStringDeUnDataClassMuestraSusPropiedades() {
        assertEquals("Jugador(nombre=Ana, dorsal=9)", Jugador("Ana", 9).toString())
    }
}
