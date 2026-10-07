package com.sebastiannarvaez.marcador

import app.cash.turbine.test
import com.sebastiannarvaez.marcador.domain.Arbitro
import com.sebastiannarvaez.marcador.ui.ArbitrosUiState
import com.sebastiannarvaez.marcador.ui.ArbitrosViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Las mismas tres piezas que PartidoViewModelTest: setMain, runTest y Turbine. La red no
// se toca: el ViewModel recibe un repositorio falso.
@OptIn(ExperimentalCoroutinesApi::class)
class ArbitrosViewModelTest {

    private val despachador = StandardTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(despachador)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    @Test
    fun pasaDeCargandoAExito() = runTest {
        val viewModel = ArbitrosViewModel(FakeArbitrosRepository())
        viewModel.uiState.test {
            assertEquals(ArbitrosUiState.Cargando, awaitItem())
            assertEquals(ArbitrosUiState.Exito(listOf(Arbitro(1, "Leanne Graham", "Gwenborough"))), awaitItem())
        }
    }

    @Test
    fun sinRedSaleElErrorYReintentarVuelveACargar() = runTest {
        val repositorio = FakeArbitrosRepository(fallo = IOException("sin red"))
        val viewModel = ArbitrosViewModel(repositorio)
        viewModel.uiState.test {
            assertEquals(ArbitrosUiState.Cargando, awaitItem())
            assertEquals(ArbitrosUiState.Error("No hay conexión"), awaitItem())

            repositorio.fallo = null // vuelve la red
            viewModel.reintentar()
            assertEquals(ArbitrosUiState.Cargando, awaitItem())
            assertEquals(ArbitrosUiState.Exito(listOf(Arbitro(1, "Leanne Graham", "Gwenborough"))), awaitItem())
        }
    }
}
