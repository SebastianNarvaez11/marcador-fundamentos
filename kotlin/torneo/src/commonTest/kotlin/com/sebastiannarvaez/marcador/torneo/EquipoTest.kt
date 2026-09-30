package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EquipoTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", null)
    private val marta = Jugador("Marta", 1)

    @Test
    fun aceptaPlantillaSinDorsalesRepetidos() {
        val equipo = Equipo("Rayo", listOf(ana, luis, marta))
        assertEquals(3, equipo.cantidadDeJugadores)
    }

    @Test
    fun variosJugadoresSinDorsalNoSeConsideranRepetidos() {
        Equipo("Rayo", listOf(luis, Jugador("Pedro", null)))
    }

    @Test
    fun rechazaDorsalesRepetidos() {
        val error = assertFailsWith<IllegalArgumentException> {
            Equipo("Rayo", listOf(ana, Jugador("Otra", 9)))
        }
        assertEquals("Dorsales repetidos en Rayo: [9, 9]", error.message)
    }

    @Test
    fun rechazaNombreEnBlanco() {
        assertFailsWith<IllegalArgumentException> { Equipo(" ", emptyList()) }
    }

    @Test
    fun elCapitanEmpiezaSinAsignar() {
        assertNull(Equipo("Rayo", listOf(ana)).capitan)
    }

    @Test
    fun nombraCapitanDeLaPlantilla() {
        val equipo = Equipo("Rayo", listOf(ana, marta))
        equipo.nombrarCapitan(marta)
        assertEquals(marta, equipo.capitan)
    }

    @Test
    fun noSePuedeNombrarCapitanAUnForaneo() {
        val equipo = Equipo("Rayo", listOf(ana))
        assertFailsWith<IllegalArgumentException> { equipo.nombrarCapitan(marta) }
    }
}
