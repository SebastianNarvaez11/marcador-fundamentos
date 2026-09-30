package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SimulacionTest {
    // `runBlocking` para probar código suspendido sin más librerías. En f23
    // llega `runTest`, que además adelanta el reloj en vez de esperar de verdad.
    @Test
    fun jugarUnPartidoReproduceElGuion() = runBlocking {
        val jugado = jugarPartido(Ejemplo.rayoContraToros, msPorMinuto = 0)
        assertEquals(Ejemplo.rayoContraToros.eventos, jugado.eventos)
        assertEquals(2, jugado.golesLocal)
        assertEquals(1, jugado.golesVisitante)
    }

    @Test
    fun elPartidoNoModificaElGuion() = runBlocking {
        val guion = Ejemplo.rayoContraToros
        jugarPartido(guion, msPorMinuto = 0)
        assertEquals(5, guion.eventos.size)
    }

    @Test
    fun unPartidoSinEventosTerminaSinGoles() = runBlocking {
        val jugado = jugarPartido(Partido(Ejemplo.rayo, Ejemplo.toros), msPorMinuto = 0)
        assertEquals(0, jugado.eventos.size)
    }
}
