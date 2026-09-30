package com.sebastiannarvaez.marcador.torneo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Torneo con el error clásico: `val` con una lista mutable que se expone tal cual.
private class TorneoConFuga {
    val partidos = mutableListOf<Partido>()
}

// El mismo error, pero disfrazado: el tipo declarado es List, el objeto es MutableList.
private class TorneoConFugaDisfrazada {
    private val interna = mutableListOf<Partido>()
    val partidos: List<Partido> get() = interna
    fun agregar(partido: Partido) { interna.add(partido) }
}

class InmutabilidadTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))

    @Test
    fun valNoImpideCambiarLaListaPorDentro() {
        val torneo = TorneoConFuga()
        torneo.partidos.add(Partido(rayo, toros))   // la referencia no cambia, la lista sí
        assertEquals(1, torneo.partidos.size)
    }

    @Test
    fun elTipoListNoBastaSiElObjetoEsMutable() {
        val torneo = TorneoConFugaDisfrazada()
        torneo.agregar(Partido(rayo, toros))
        @Suppress("UNCHECKED_CAST")
        (torneo.partidos as MutableList<Partido>).clear()   // ¡se cuela!
        assertEquals(0, torneo.partidos.size)
    }

    @Test
    fun torneoDevuelveUnaFotoQueNoCambiaDespues() {
        val torneo = Torneo("Copa", listOf(rayo, toros))
        torneo.registrar(Partido(rayo, toros)).getOrThrow()
        val foto = torneo.partidos
        torneo.registrar(Partido(toros, rayo)).getOrThrow()
        assertEquals(1, foto.size)              // la copia no ve lo que llega después
        assertEquals(2, torneo.partidos.size)
    }

    @Test
    fun torneoNoSeVeAfectadoPorLaListaOriginalDeEquipos() {
        val inscritos = mutableListOf(rayo)
        val torneo = Torneo("Copa", inscritos)
        inscritos.add(toros)
        assertEquals(1, torneo.equipos.size)
    }

    @Test
    fun equipoHaceCopiaDefensivaDeLaPlantilla() {
        val lista = mutableListOf(ana)
        val equipo = Equipo("Rayo", lista)
        lista.add(Jugador("Otra", 9))   // dorsal repetido, por fuera del require
        assertEquals(1, equipo.plantilla.size)
    }

    @Test
    fun soloSePuedenAgregarPartidosDeEquiposInscritos() {
        val torneo = Torneo("Copa", listOf(rayo))
        val resultado = torneo.registrar(Partido(rayo, toros))
        assertTrue(resultado.isFailure)
        assertEquals(0, torneo.partidos.size)
    }

    @Test
    fun laTablaYLosGoleadoresSalenDeLosPartidos() {
        val torneo = Torneo("Copa", listOf(rayo, toros))
        torneo.registrar(Partido(rayo, toros).registrar(Gol(3, ana, rayo))).getOrThrow()
        assertEquals("Rayo", torneo.tablaDePosiciones().first().equipo.nombre)
        assertEquals(listOf(ana), torneo.goleadores().ordenados)
    }
}
