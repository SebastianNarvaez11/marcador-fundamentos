package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.sebastiannarvaez.marcador.AppContainer
import com.sebastiannarvaez.marcador.MarcadorApplication

// f44 · FABRICAS DE VIEWMODEL
//
// `viewModel()` sin mas solo sabe construir ViewModels con un constructor vacio (o con
// SavedStateHandle). Un ViewModel que pide un repositorio necesita una FABRICA: el
// «como se construye». `viewModelFactory { initializer { ... } }` la escribe sin clases:
// cada `initializer` dice como crear un tipo, y `CreationExtras` trae lo que el sistema
// sabe en ese momento: la Application y, con `createSavedStateHandle()`, el handle de f40.
//
// La fabrica es el UNICO sitio que une el contenedor con los ViewModels. En las pruebas
// (f48) no se usa: el test llama al constructor con un repositorio falso.
private val CreationExtras.contenedor: AppContainer
    get() = (this[APPLICATION_KEY] as MarcadorApplication).contenedor

object Fabricas {
    val partido = viewModelFactory {
        initializer {
            PartidoViewModel(createSavedStateHandle(), contenedor.partidosRepository, contenedor.registrarGol, contenedor.preferenciasRepository)
        }
    }

    val lista = viewModelFactory {
        initializer { ListaViewModel(contenedor.partidosRepository) }
    }
}
