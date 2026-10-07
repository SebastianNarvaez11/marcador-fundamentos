package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.marcador.domain.CronicaPublicada
import com.sebastiannarvaez.marcador.domain.CronicasRepository
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.resumen
import com.sebastiannarvaez.marcador.torneo.Gol
import com.sebastiannarvaez.marcador.torneo.intentar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Publica la cronica de un partido. OJO: este ViewModel hace DEMASIADO a proposito:
// pide el partido a un repositorio, decide como se escribe la cronica y la publica con
// otro. «Como se escribe una cronica» es una regla del negocio, no de la pantalla.
class CronicaViewModel(
    private val partidos: PartidosRepository,
    private val cronicas: CronicasRepository,
    private val partidoId: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CronicaUiState(partidoId))
    val uiState: StateFlow<CronicaUiState> = _uiState.asStateFlow()

    fun publicar() {
        if (_uiState.value.publicando) return
        _uiState.update { it.copy(publicando = true, error = null) }
        viewModelScope.launch {
            val resultado = intentar {
                val partido = partidos.partido(partidoId) ?: error("No existe el partido $partidoId")
                val titulo = partido.resumen()
                val goles = partido.eventos.filterIsInstance<Gol>().sortedBy { it.minuto }
                val texto = if (goles.isEmpty()) "Sin goles" else goles.joinToString("\n") { "${it.minuto}' ${it.jugador.nombre} (${it.equipo.nombre})" }
                CronicaPublicada(cronicas.publicar(titulo, texto).getOrThrow(), titulo, texto)
            }
            _uiState.update { estado ->
                resultado.fold(
                    onSuccess = { estado.copy(publicando = false, publicada = it) },
                    onFailure = { estado.copy(publicando = false, error = it.mensajeParaElUsuario()) },
                )
            }
        }
    }
}
