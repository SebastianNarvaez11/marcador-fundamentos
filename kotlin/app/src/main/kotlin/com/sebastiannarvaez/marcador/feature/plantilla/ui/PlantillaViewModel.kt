package com.sebastiannarvaez.marcador.feature.plantilla.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.marcador.feature.partidos.domain.PartidosRepository
import com.sebastiannarvaez.marcador.torneo.Jugador
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// La plantilla de UN equipo, observada desde el repositorio (la tabla `jugador`).
// Inyeccion asistida, como en PartidoViewModel: el nombre del equipo sale de la PlantillaKey.
@HiltViewModel(assistedFactory = PlantillaViewModel.Fabrica::class)
class PlantillaViewModel @AssistedInject constructor(
    repositorio: PartidosRepository,
    @Assisted val equipo: String,
) : ViewModel() {

    @AssistedFactory
    interface Fabrica {
        fun crear(equipo: String): PlantillaViewModel
    }

    val jugadores: StateFlow<List<Jugador>> = repositorio.observarPlantilla(equipo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
