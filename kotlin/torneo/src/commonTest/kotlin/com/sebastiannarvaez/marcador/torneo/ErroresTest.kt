package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ErroresTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val pedro = Jugador("Pedro", 4)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))
    private val lobos = Equipo("Lobos", listOf(pedro))

    private fun torneo() = Torneo("Copa", listOf(rayo, toros))

    @Test
    fun registrarUnPartidoValidoDevuelveSuccess() {
        val torneo = torneo()
        val resultado = torneo.registrar(rayo, toros, listOf(Gol(10, ana, rayo)))
        assertTrue(resultado.isSuccess)
        assertEquals(1, resultado.getOrThrow().golesLocal)
        assertEquals(1, torneo.partidos.size)
    }

    @Test
    fun equipoNoInscritoDevuelveFailure() {
        val torneo = torneo()
        val resultado = torneo.registrar(rayo, lobos)
        val error = resultado.exceptionOrNull()
        assertIs<IllegalArgumentException>(error)
        assertEquals("Los dos equipos deben estar inscritos en Copa", error.message)
        assertEquals(0, torneo.partidos.size)
    }

    @Test
    fun unEquipoNoJuegaContraSiMismo() {
        val error = torneo().registrar(rayo, rayo).exceptionOrNull()
        assertEquals("Un equipo no puede jugar contra sí mismo", error?.message)
    }

    @Test
    fun golDeAlguienAjenoALaPlantillaFalla() {
        val torneo = torneo()
        val resultado = torneo.registrar(rayo, toros, listOf(Gol(10, luis, rayo)))
        assertEquals("Luis no juega en Rayo", resultado.exceptionOrNull()?.message)
        assertEquals(0, torneo.partidos.size)
    }

    @Test
    fun minutoFueraDelPartidoFalla() {
        val resultado = torneo().registrar(rayo, toros, listOf(Gol(200, ana, rayo)))
        assertEquals("Minuto fuera del partido: 200", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun cambioConAlguienAjenoFalla() {
        val resultado = torneo().registrar(rayo, toros, listOf(Cambio(60, ana, pedro)))
        assertTrue(resultado.isFailure)
    }

    @Test
    fun resultCombinaConFoldYGetOrElse() {
        val torneo = torneo()
        assertEquals("ok", torneo.registrar(rayo, toros).fold({ "ok" }, { "error" }))
        assertEquals(-1, torneo.registrar(rayo, rayo).map { it.eventos.size }.getOrElse { -1 })
    }

    @Test
    fun tryComoExpresionDevuelveNullSiNoEsNumero() {
        assertEquals(10, dorsalDesdeTexto("10"))
        assertEquals(7, dorsalDesdeTexto(" 7 "))
        assertNull(dorsalDesdeTexto("diez"))
        assertNull(dorsalDesdeTexto(""))
    }
}
