package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SegundoPlanoTest {
    private fun ficheroTemporal(): File = File.createTempFile("torneo", ".json").apply { deleteOnExit() }

    @Test
    fun guardarYCargarEnSegundoPlanoDevuelveElMismoTorneo() = runBlocking {
        val fichero = ficheroTemporal()
        val torneo = Ejemplo.torneoConPartidos()
        torneo.guardarEnSegundoPlano(fichero).getOrThrow()
        val cargado = cargarTorneoEnSegundoPlano(fichero).getOrThrow()
        assertEquals(torneo.nombre, cargado.nombre)
        assertEquals(torneo.partidos.size, cargado.partidos.size)
        assertEquals(torneo.goleadores().map { it.nombre }, cargado.goleadores().map { it.nombre })
    }

    @Test
    fun laTablaEnSegundoPlanoEsLaMismaQueLaSincrona() = runBlocking {
        val torneo = Ejemplo.torneoConPartidos()
        assertEquals(torneo.tablaDePosiciones(), torneo.tablaEnSegundoPlano())
    }

    @Test
    fun cerrarJornadaGuardaYDevuelveLaTabla() = runBlocking {
        val fichero = ficheroTemporal()
        val torneo = Ejemplo.torneoConPartidos()
        val tabla = torneo.cerrarJornada(fichero)
        assertEquals(4, tabla.size)
        assertTrue(fichero.length() > 0)
    }

    @Test
    fun guardarEnUnaRutaImposibleDevuelveFalloSinLanzar() = runBlocking {
        val resultado = Ejemplo.torneoConPartidos().guardarEnSegundoPlano(File("/no/existe/torneo.json"))
        assertTrue(resultado.isFailure)
    }

    @Test
    fun cerrarJornadaPropagaElErrorDelGuardado() {
        assertFailsWith<Exception> {
            runBlocking { Ejemplo.torneoConPartidos().cerrarJornada(File("/no/existe/torneo.json")) }
        }
    }

    @Test
    fun withContextCambiaDeHiloYVuelve() = runBlocking {
        val antes = Thread.currentThread()
        val dentro = withContext(Dispatchers.Default) { Thread.currentThread() }
        assertTrue(dentro !== antes)
        assertEquals(antes, Thread.currentThread())
    }

    @Test
    fun enUnaConsolaNoHayDispatcherMain() {
        // No hay módulo con hilo de interfaz: Android o Swing lo aportarían.
        assertFailsWith<IllegalStateException> { runBlocking(Dispatchers.Main) { } }
    }
}
