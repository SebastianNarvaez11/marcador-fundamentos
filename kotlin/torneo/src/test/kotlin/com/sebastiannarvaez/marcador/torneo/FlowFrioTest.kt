package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

class FlowFrioTest {
    private val enVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 0)

    @Test
    fun losEventosSalenEnElOrdenDelGuion() = runBlocking {
        assertEquals(Ejemplo.rayoContraToros.eventos, enVivo.eventos().toList())
    }

    @Test
    fun laNarracionDescribeCadaEvento() = runBlocking {
        val lineas = enVivo.narracion().toList()
        assertEquals(5, lineas.size)
        assertEquals("12' Gol de Ana (Rayo FC)", lineas.first())
    }

    @Test
    fun soloSalenLosGoles() = runBlocking {
        assertEquals(3, enVivo.golesEnFrio().toList().size)
    }

    @Test
    fun elFlowEsFrioYCadaCollectLoRepiteEntero() = runBlocking {
        val arranques = AtomicInteger()
        val frio = flow { arranques.incrementAndGet(); emit(1) }
        assertEquals(0, arranques.get())
        frio.toList(); frio.toList()
        assertEquals(2, arranques.get())
    }

    @Test
    fun firstCancelaElRestoDelPartido() = runBlocking {
        val vistos = mutableListOf<EventoDePartido>()
        val lento = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 5)
        val ms = measureTimeMillis { lento.eventos().onEach { vistos += it }.first() }
        assertEquals(1, vistos.size)
        // El partido entero serían ~450 ms; el primer evento llega en el minuto 12.
        assertTrue(ms < 300, "Tardó $ms ms: no se cortó al primer evento")
    }

    @Test
    fun primerGolEsElDelMinuto12() = runBlocking {
        assertEquals(12, enVivo.primerGol().minuto)
    }
}
