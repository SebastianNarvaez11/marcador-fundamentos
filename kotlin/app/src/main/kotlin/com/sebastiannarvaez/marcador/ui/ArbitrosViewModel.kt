package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sebastiannarvaez.marcador.domain.ArbitrosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// La lista sale de lo guardado (observarArbitros) y se refresca al abrir y con
// «Actualizar». Tres flujos -> un UiState con `combine`.
@HiltViewModel
class ArbitrosViewModel @Inject constructor(private val repositorio: ArbitrosRepository) : ViewModel() {

    private val refrescando = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ArbitrosUiState> =
        combine(repositorio.observarArbitros(), refrescando, error) { arbitros, cargando, fallo ->
            ArbitrosUiState(arbitros, cargando, fallo)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArbitrosUiState(refrescando = true))

    init {
        actualizar()
    }

    fun actualizar() {
        if (refrescando.value) return
        refrescando.value = true
        error.value = null
        viewModelScope.launch {
            // `refrescar` ya devuelve Result (no lanza): solo hay que mirar si fallo.
            repositorio.refrescar().onFailure { error.value = it.mensajeParaElUsuario() }
            refrescando.value = false
        }
    }
}
