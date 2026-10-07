package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.marcador.domain.ArbitrosRepository
import com.sebastiannarvaez.marcador.torneo.intentar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Pide los arbitros al abrirse la pantalla (init) y otra vez con «Reintentar».
// `intentar` (de :torneo) atrapa el fallo como Result y RELANZA la cancelacion: si la
// pantalla se cierra a mitad de la peticion, la corrutina se cancela de verdad.
class ArbitrosViewModel(private val repositorio: ArbitrosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ArbitrosUiState>(ArbitrosUiState.Cargando)
    val uiState: StateFlow<ArbitrosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        _uiState.value = ArbitrosUiState.Cargando
        viewModelScope.launch {
            _uiState.value = intentar { repositorio.arbitros() }.fold(
                onSuccess = { ArbitrosUiState.Exito(it) },
                // El repositorio ya tradujo el fallo a FalloDeRed; aqui solo se pone en palabras.
                onFailure = { ArbitrosUiState.Error(it.mensajeParaElUsuario()) },
            )
        }
    }
}
