package com.sebastiannarvaez.marcador.torneo

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Los tests que tocan disco con java.io.File: solo corren en la JVM.
class GuardadoEnFicheroTest {
    private fun torneoDeEjemplo(): Torneo = Ejemplo.torneoConPartidos()

    @Test
    fun cargaDesdeFichero() {
        val carpeta = createTempDirectory("marcador").toFile()
        val fichero = carpeta.resolve("torneo.json")
        assertTrue(torneoDeEjemplo().guardar(fichero).isSuccess)
        val cargado = cargarTorneo(fichero).getOrThrow()
        assertEquals(torneoDeEjemplo().nombre, cargado.nombre)
        carpeta.deleteRecursively()
    }

    @Test
    fun unFicheroQueNoExisteEsFailure() {
        assertTrue(cargarTorneo(java.io.File("/no/existe/torneo.json")).isFailure)
    }
}
