package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.marcador.data.Repositorios
import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import com.sebastiannarvaez.marcador.domain.armarTorneo
import com.sebastiannarvaez.marcador.torneo.Torneo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// f43 · Los partidos y las tablas, OBSERVADOS desde el repositorio. Cada vez que el
// repositorio cambia (un gol nuevo), la lista y las tablas se recalculan solas.
class ListaViewModel : ViewModel() {
    private val repositorio = Repositorios.partidos

    val partidos: StateFlow<List<PartidoDeLista>> = repositorio.observarPartidos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val torneo: StateFlow<Torneo> = repositorio.observarPartidos()
        .map { armarTorneo(repositorio.equipos, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Torneo("Copa Barrio", repositorio.equipos))
}
