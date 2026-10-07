package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sebastiannarvaez.marcador.domain.PublicarCronica
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Publica la cronica de un partido. La regla («como se escribe una cronica») vive en el
// caso de uso; el ViewModel solo traduce su resultado a estado de pantalla.
// INYECCION ASISTIDA, como en PartidoViewModel: Hilt pone lo que sabe construir; el id
// del partido lo pone la pantalla (sale de la CronicaKey). @Assisted marca el que llega de fuera.
@HiltViewModel(assistedFactory = CronicaViewModel.Fabrica::class)
class CronicaViewModel @AssistedInject constructor(
    private val publicarCronica: PublicarCronica,
    @Assisted private val partidoId: Int,
) : ViewModel() {

    // La fabrica que Hilt ESCRIBE: solo se declara la funcion con lo que falta.
    @AssistedFactory
    interface Fabrica {
        fun crear(partidoId: Int): CronicaViewModel
    }

    private val _uiState = MutableStateFlow(CronicaUiState(partidoId))
    val uiState: StateFlow<CronicaUiState> = _uiState.asStateFlow()

    fun publicar() {
        if (_uiState.value.publicando) return
        _uiState.update { it.copy(publicando = true, error = null) }
        viewModelScope.launch {
            val resultado = publicarCronica(partidoId)
            _uiState.update { estado ->
                resultado.fold(
                    onSuccess = { estado.copy(publicando = false, publicada = it) },
                    onFailure = { estado.copy(publicando = false, error = it.mensajeParaElUsuario()) },
                )
            }
        }
    }
}
