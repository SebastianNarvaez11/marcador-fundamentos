package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.domain.armarTorneo
import com.sebastiannarvaez.marcador.torneo.Torneo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Los partidos y las tablas, OBSERVADOS desde el repositorio. Cada vez que el
// repositorio cambia (un gol nuevo), la lista y las tablas se recalculan solas.
//
// @HiltViewModel + @Inject constructor: Hilt construye este ViewModel y le pasa el
// repositorio. La pantalla lo pide con hiltViewModel().
@HiltViewModel
class ListaViewModel @Inject constructor(private val repositorio: PartidosRepository) : ViewModel() {

    val partidos: StateFlow<List<PartidoDeLista>> = repositorio.observarPartidos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val torneo: StateFlow<Torneo> = repositorio.observarPartidos()
        .map { armarTorneo(repositorio.equipos, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Torneo("Copa Barrio", repositorio.equipos))
}
