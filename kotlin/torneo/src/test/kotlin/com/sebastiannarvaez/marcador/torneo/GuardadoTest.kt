package com.sebastiannarvaez.marcador.torneo

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuardadoTest {
    private val ana = Jugador("Ana", 9)
    private val marta = Jugador("Marta", null)
    private val luis = Jugador("Luis", 7)
    private val rayo = Equipo("Rayo", listOf(ana, marta)).apply { nombrarCapitan(ana) }
    private val toros = Equipo("Toros", listOf(luis))

    private fun torneoDeEjemplo(): Torneo = Torneo("Copa", listOf(rayo, toros)).apply {
        registrar(
            rayo, toros,
            listOf(
                Gol(10, ana, rayo),
                Tarjeta(30, luis, ColorDeTarjeta.AMARILLA),
                Cambio(60, sale = ana, entra = marta),
                Gol(75, luis, toros),
                Gol(80, ana, rayo),
            ),
        ).getOrThrow()
    }

    @Test
    fun elJsonTieneLaFormaEsperada() {
        val texto = torneoDeEjemplo().aJson()
        assertTrue("\"tipo\": \"gol\"" in texto)
        assertTrue("\"tipo\": \"tarjeta\"" in texto)
        assertTrue("\"tipo\": \"cambio\"" in texto)
        assertTrue("\"color\": \"AMARILLA\"" in texto)
        // Marta no tiene dorsal: el campo no aparece (null por defecto no se escribe).
        assertTrue("\"nombre\": \"Marta\"\n" in texto)
    }

    @Test
    fun guardarYCargarConservaElTorneo() {
        val original = torneoDeEjemplo()
        val recuperado = torneoDesdeJson(original.aJson()).getOrThrow()
        assertEquals(original.nombre, recuperado.nombre)
        assertEquals(2, recuperado.equipos.size)
        assertEquals("Ana", recuperado.equipos.first().capitan?.nombre)
        assertEquals(1, recuperado.partidos.size)
        assertEquals(original.partidos.single().eventos.map(::describir), recuperado.partidos.single().eventos.map(::describir))
        assertEquals(
            original.tablaDePosiciones().map { it.equipo.nombre to it.puntos },
            recuperado.tablaDePosiciones().map { it.equipo.nombre to it.puntos },
        )
    }

    @Test
    fun cargaDesdeFichero() {
        val carpeta = createTempDirectory("marcador").toFile()
        val fichero = carpeta.resolve("torneo.json")
        assertTrue(torneoDeEjemplo().guardar(fichero).isSuccess)
        val cargado = cargarTorneo(fichero).getOrThrow()
        assertEquals("Copa", cargado.nombre)
        carpeta.deleteRecursively()
    }

    @Test
    fun unFicheroQueNoExisteEsFailure() {
        assertTrue(cargarTorneo(java.io.File("/no/existe/torneo.json")).isFailure)
    }

    @Test
    fun jsonRotoEsFailure() {
        assertTrue(torneoDesdeJson("{ esto no es json").isFailure)
    }

    @Test
    fun jsonConEquipoInexistenteEsFailure() {
        val texto = """
            {"nombre":"X","equipos":[{"nombre":"A","plantilla":[]}],
             "partidos":[{"local":"A","visitante":"B","eventos":[]}]}
        """.trimIndent()
        assertTrue(torneoDesdeJson(texto).isFailure)
    }

    @Test
    fun laInstantaneaCalculaLaTablaUnaSolaVez() {
        val foto = torneoDeEjemplo().instantanea()
        assertEquals(0, foto.calculosDeLaTabla)
        foto.tabla
        foto.tabla
        assertEquals(1, foto.calculosDeLaTabla)
        assertEquals("Rayo", foto.tabla.first().equipo.nombre)
        assertEquals(ana, foto.goleadores.ordenados.first())
    }
}
