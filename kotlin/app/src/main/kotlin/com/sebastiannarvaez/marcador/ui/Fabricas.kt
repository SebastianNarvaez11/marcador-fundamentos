package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.sebastiannarvaez.marcador.AppContainer
import com.sebastiannarvaez.marcador.MarcadorApplication

// FABRICAS DE VIEWMODEL
//
// `viewModel()` sin mas solo sabe construir ViewModels con un constructor vacio (o con
// SavedStateHandle). Un ViewModel que pide un repositorio necesita una FABRICA: el
// «como se construye». `viewModelFactory { initializer { ... } }` la escribe sin clases:
// cada `initializer` dice como crear un tipo, y `CreationExtras` trae lo que el sistema
// sabe en ese momento: la Application y, con `createSavedStateHandle()`, el handle.
//
// La fabrica es el UNICO sitio que une el contenedor con los ViewModels. En las pruebas
// no se usa: el test llama al constructor con un repositorio falso.
private val CreationExtras.contenedor: AppContainer
    get() = (this[APPLICATION_KEY] as MarcadorApplication).contenedor

object Fabricas {
    // Una fabrica POR PARTIDO: el id de la pantalla (el de la NavKey) es el partido inicial.
    // Si el sistema restauro un handle (am kill), manda el id guardado.
    fun partido(partidoId: Int) = viewModelFactory {
        initializer {
            PartidoViewModel(
                createSavedStateHandle(), contenedor.partidosRepository, contenedor.registrarGol,
                contenedor.preferenciasRepository, partidoId,
            )
        }
    }

    val lista = viewModelFactory {
        initializer { ListaViewModel(contenedor.partidosRepository) }
    }

    val goleadores = viewModelFactory {
        initializer { GoleadoresViewModel(contenedor.partidosRepository) }
    }
}
