package com.sebastiannarvaez.marcador.feature.plantilla.ui

import com.sebastiannarvaez.marcador.core.FakePartidosRepository
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Jugador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// El ViewModel del formulario, con el repositorio falso (la plantilla del Rayo FC: Ana #9,
// Luis sin dorsal y Marta #1). El formulario es un mutableStateOf: se lee DIRECTAMENTE, sin
// Turbine. Lo unico asincrono es la plantilla (llega por Flow) y el guardado.
@OptIn(ExperimentalCoroutinesApi::class)
class AnadirJugadorViewModelTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val rayo = Ejemplo.rayo.nombre

    @Test
    fun alAbrirNoSeVeNingunErrorYNoSePuedeGuardar() = runTest {
        val viewModel = AnadirJugadorViewModel(FakePartidosRepository(), rayo)
        advanceUntilIdle()
        assertNull(viewModel.errorNombre)
        assertNull(viewModel.errorDorsal)
        assertFalse(viewModel.sePuedeGuardar)
    }

    @Test
    fun borrarElNombreEnsenaElErrorPorqueElCampoYaEstaTocado() = runTest {
        val viewModel = AnadirJugadorViewModel(FakePartidosRepository(), rayo)
        viewModel.alEscribirNombre("B")
        viewModel.alEscribirNombre("")
        assertEquals(ErrorNombre.VACIO, viewModel.errorNombre)
    }

    @Test
    fun unDorsalDeLaPlantillaGuardadaEsUnError() = runTest {
        val viewModel = AnadirJugadorViewModel(FakePartidosRepository(), rayo)
        advanceUntilIdle() // llega la plantilla
        viewModel.alEscribirNombre("Bea")
        viewModel.alEscribirDorsal("9")
        assertEquals(ErrorDorsal.REPETIDO, viewModel.errorDorsal)
        assertFalse(viewModel.sePuedeGuardar)
    }

    @Test
    fun guardarUnJugadorValidoLoAnadeALaPlantilla() = runTest {
        val repositorio = FakePartidosRepository()
        val viewModel = AnadirJugadorViewModel(repositorio, rayo)
        advanceUntilIdle()
        viewModel.alEscribirNombre("Bea")
        viewModel.alEscribirDorsal("7")
        assertTrue(viewModel.sePuedeGuardar)

        viewModel.guardar()
        advanceUntilIdle()

        assertTrue(viewModel.guardado)
        assertEquals(Jugador("Bea", 7), repositorio.observarPlantilla(rayo).first().last())
    }
}
