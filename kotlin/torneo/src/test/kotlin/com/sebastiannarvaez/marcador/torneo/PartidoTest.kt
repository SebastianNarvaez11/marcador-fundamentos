package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PartidoTest {
    private val rayo = Equipo("Rayo", listOf(Jugador("Ana", 9)))
    private val toros = Equipo("Toros", listOf(Jugador("Luis", 7)))

    @Test
    fun registrarUnGolDevuelveUnaCopia() {
        val inicio = Partido(rayo, toros)
        val despues = inicio.conGolLocal()
        assertEquals(0, inicio.golesLocal)
        assertEquals(1, despues.golesLocal)
        assertNotSame(inicio, despues)
    }

    @Test
    fun copyCambiaSoloLoIndicado() {
        val partido = Partido(rayo, toros, golesLocal = 2, golesVisitante = 1)
        val otro = partido.copy(golesVisitante = 3)
        assertSame(rayo, otro.local)
        assertEquals(2, otro.golesLocal)
        assertEquals(3, otro.golesVisitante)
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
        val ana = Jugador("Ana", 9)
        assertEquals(Jugador("Ana", 10), ana.copy(dorsal = 10))
    }

    @Test
    fun desestructuracionUsaComponentN() {
        val (nombre, dorsal) = Jugador("Ana", 9)
        assertEquals("Ana", nombre)
        assertEquals(9, dorsal)
        val (local, visitante, golesLocal, golesVisitante) = Partido(rayo, toros, 2, 1)
        assertSame(rayo, local)
        assertSame(toros, visitante)
        assertEquals(2, golesLocal)
        assertEquals(1, golesVisitante)
    }

    @Test
    fun toStringDeUnDataClassMuestraSusPropiedades() {
        assertEquals("Jugador(nombre=Ana, dorsal=9)", Jugador("Ana", 9).toString())
    }
}
