package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JugadorTest {
    @Test
    fun dorsalOGuionMuestraElNumero() = assertEquals("#9", Jugador("Ana", 9).dorsalOGuion())

    @Test
    fun dorsalOGuionMuestraGuionSinDorsal() = assertEquals("-", Jugador("Luis", null).dorsalOGuion())

    @Test
    fun dorsalForzadoLanzaSiEsNull() {
        assertFailsWith<NullPointerException> { Jugador("Luis", null).dorsalForzado() }
    }

    @Test
    fun elPorteroLlevaElUno() {
        assertTrue(Jugador("Marta", 1).esPortero())
        assertFalse(Jugador("Ana", 9).esPortero())
        assertFalse(Jugador("Luis", null).esPortero())
    }

    @Test
    fun longitudDelNombreEsNullSiEstaEnBlanco() {
        assertNull(Jugador(" ", 3).longitudDelNombre())
        assertEquals(3, Jugador("Ana", 3).longitudDelNombre())
    }

    @Test
    fun presentarAceptaNull() {
        assertEquals("Sin jugador", presentar(null))
        assertEquals("Ana (#9)", presentar(Jugador("Ana", 9)))
    }
}
