package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ReglamentoTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))

    @Test
    fun laLigaDaTresUnoYCero() {
        assertEquals(3, ReglamentoLiga.puntosPor(2, 1))
        assertEquals(1, ReglamentoLiga.puntosPor(0, 0))
        assertEquals(0, ReglamentoLiga.puntosPor(0, 1))
    }

    @Test
    fun puntosPorAtajoCoincideConLaLiga() {
        assertEquals(ReglamentoLiga.puntosPor(3, 0), puntosPor(3, 0))
    }

    @Test
    fun objectEsUnaUnicaInstancia() {
        assertSame(ReglamentoLiga, ReglamentoLiga)
        assertEquals("Reglamento Liga (3/1/0)", ReglamentoLiga.toString())
    }

    @Test
    fun objetoAnonimoImplementaLaInterfaz() {
        val antiguo = object : Reglamento {
            override val puntosPorVictoria = 2
            override val puntosPorEmpate = 1
            override val puntosPorDerrota = -1
        }
        assertEquals(2, antiguo.puntosPor(1, 0))
        assertEquals(-1, antiguo.puntosPor(0, 1))
    }

    @Test
    fun laTablaUsaElReglamentoDelTorneo() {
        val reglamento = object : Reglamento {
            override val puntosPorVictoria = 2
            override val puntosPorEmpate = 1
        }
        val torneo = Torneo("Antigua", listOf(rayo, toros), reglamento)
        torneo.agregar(Partido(rayo, toros).registrar(Gol(1, ana, rayo)))
        assertEquals(2, torneo.tablaDePosiciones().first().puntos)
    }

    @Test
    fun companionCreaTorneosConMinimoDeEquipos() {
        assertEquals(2, Torneo.MINIMO_DE_EQUIPOS)
        assertEquals(2, Torneo.conEquipos("Copa", rayo, toros).equipos.size)
        assertFailsWith<IllegalArgumentException> { Torneo.conEquipos("Copa", rayo) }
    }
}
