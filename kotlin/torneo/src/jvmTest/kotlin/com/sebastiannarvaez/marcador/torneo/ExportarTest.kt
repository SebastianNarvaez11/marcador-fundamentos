package com.sebastiannarvaez.marcador.torneo

import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExportarTest {
    private val ana = Jugador("Ana", 9)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana))
    private val toros = Equipo("Toros", listOf(luis))

    private fun torneo() = Torneo("Copa", listOf(rayo, toros))

    @Test
    fun exportarTablaEscribeYCierraElFichero() {
        val carpeta = createTempDirectory("marcador").toFile()
        val destino = File(carpeta, "tabla.txt")
        val torneo = torneo().apply {
            registrar(rayo, toros, listOf(Gol(1, ana, rayo))).getOrThrow()
        }
        assertTrue(torneo.exportarTabla(destino.path).isSuccess)
        val lineas = destino.readLines()
        assertEquals(3, lineas.size)
        assertTrue(lineas[1].contains("Rayo"))
        carpeta.deleteRecursively()
    }

    @Test
    fun exportarEnUnaCarpetaQueNoExisteDevuelveFailure() {
        val destino = "/carpeta/que/no/existe/tabla.txt"
        val error = torneo().exportarTabla(destino).exceptionOrNull()
        assertIs<IOException>(error)
    }
}
