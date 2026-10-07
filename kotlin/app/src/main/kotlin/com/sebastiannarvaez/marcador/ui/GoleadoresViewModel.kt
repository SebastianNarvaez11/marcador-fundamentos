package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sebastiannarvaez.marcador.domain.Goleador
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// Los goleadores salen de la consulta GROUP BY de Room: la base cuenta, la
// pantalla solo pinta. Cada gol nuevo vuelve a emitir la lista.
@HiltViewModel
class GoleadoresViewModel @Inject constructor(repositorio: PartidosRepository) : ViewModel() {
    val goleadores: StateFlow<List<Goleador>> = repositorio.observarGoleadores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
