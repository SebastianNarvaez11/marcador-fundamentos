package com.sebastiannarvaez.marcador.feature.marcador.domain

import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.Partido
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// PRUEBAS DEL PARTIDO EN JUEGO (Kotlin puro: sin ViewModel ni Android).
//
// `backgroundScope` es un alcance que `runTest` cancela solo al terminar la prueba: hace de
// alcance de la app. Va con el mismo reloj VIRTUAL, asi que 90 minutos tardan milisegundos.
@OptIn(ExperimentalCoroutinesApi::class)
class PartidoEnJuegoTest {

    private fun directo(id: Int) = Directo(id, Partido(Ejemplo.rayo, Ejemplo.toros), MS_POR_MINUTO)

    private fun TestScope.jugarMinutos(minutos: Int) {
        advanceTimeBy(minutos * MS_POR_MINUTO + 1)
        runCurrent()
    }

    @Test
    fun empezarPublicaElPartidoEnJuego() = runTest {
        val enJuego = PartidoEnJuego(backgroundScope)
        assertTrue(enJuego.empezar(directo(1)))
        jugarMinutos(34)
        assertEquals(
            PartidoEnJuego.EnJuego(1, "Rayo FC", "Toros", Marcador(0, 0), minuto = 34, terminado = false),
            enJuego.actual.value,
        )
    }

    @Test
    fun noDejaEmpezarOtroMientrasSeJuega() = runTest {
        val enJuego = PartidoEnJuego(backgroundScope)
        enJuego.empezar(directo(1))
        assertFalse(enJuego.empezar(directo(2)))
        runCurrent()
        assertEquals(1, enJuego.actual.value?.partidoId)
        assertNull(enJuego.directo(2))
    }

    @Test
    fun alLlegarAlMinuto90QuedaTerminado() = runTest {
        val enJuego = PartidoEnJuego(backgroundScope)
        enJuego.empezar(directo(1))
        jugarMinutos(90)
        assertEquals(90, enJuego.actual.value?.minuto)
        assertEquals(true, enJuego.actual.value?.terminado)
        // Terminado, ya deja empezar otro.
        assertTrue(enJuego.empezar(directo(2)))
    }

    @Test
    fun cerrarQuitaElPartidoTerminado() = runTest {
        val enJuego = PartidoEnJuego(backgroundScope)
        enJuego.empezar(directo(1))
        jugarMinutos(90)
        enJuego.cerrar()
        assertNull(enJuego.actual.value)
        assertNull(enJuego.directo(1))
    }

    @Test
    fun cerrarNoHaceNadaMientrasSeJuega() = runTest {
        val enJuego = PartidoEnJuego(backgroundScope)
        enJuego.empezar(directo(1))
        jugarMinutos(10)
        enJuego.cerrar()
        assertEquals(10, enJuego.actual.value?.minuto)
    }

    private companion object {
        const val MS_POR_MINUTO = 250L
    }
}
