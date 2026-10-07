package com.sebastiannarvaez.marcador.ui

import com.sebastiannarvaez.marcador.domain.Directo
import com.sebastiannarvaez.marcador.domain.PartidosRepository
import com.sebastiannarvaez.marcador.domain.RegistrarGol
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// f39 · VIEWMODEL
//
// Un ViewModel es un objeto que Android guarda EN UN ALMACEN (ViewModelStore) que
// pertenece a la Activity, no a la composicion. Al rotar, la Activity se destruye y se
// vuelve a crear, pero ese almacen se conserva: `viewModel()` devuelve la MISMA instancia.
//
//   sobrevive a  -> recomposiciones, rotacion, cambios de tema/idioma/tamano de ventana
//   NO sobrevive -> a que el usuario cierre la pantalla (back, finish()) ni a la MUERTE
//                   DEL PROCESO (am kill, Android necesita memoria). Eso es f40.
//
// `onCleared()` es el ultimo aviso: el almacen se vacia porque la Activity se va para
// siempre. Es donde se liberan recursos que NO son corrutinas (un listener, un fichero
// abierto). Aqui no hay ninguno, asi que no se sobrescribe. NO se escribe en Logcat desde
// el ViewModel: un `Log.d` en un ViewModel rompe sus tests en la JVM (f48) o obliga a
// parchearlos; lo que necesita verse (que sobrevive a rotar) se ve en pantalla.
//
// viewModelScope es un CoroutineScope (SupervisorJob + Dispatchers.Main.immediate) que
// el propio ViewModel cancela justo antes de onCleared. Es el scope de F2 (f17) con la
// vida del ViewModel: lo que se lanza aqui sigue vivo al rotar y muere al irse la pantalla.
// Por eso NO se usa GlobalScope: no lo cancela nadie, y una corrutina que sostiene
// `this` en un proceso que dura horas es una fuga (f27).
//
// f40 · SAVEDSTATEHANDLE. El ViewModel sobrevive a rotar pero NO a la muerte del
// proceso: si Android mata la app en segundo plano para ganar memoria, al volver el
// ViewModel es nuevo y su estado, cero. Para lo pequeno que DEBE sobrevivir, Android da
// un `SavedStateHandle`: un mapa clave-valor que se guarda en el mismo Bundle de
// onSaveInstanceState (f25) y que el sistema conserva FUERA del proceso. Se pide como
// parametro del constructor y la fabrica por defecto de `viewModel()` lo entiende.
//
// Regla: en el handle va lo POCO y BARATO que hace falta para reconstruir la pantalla
// (un identificador), no el objeto entero (un partido entero no cabe en un Bundle).
// Aqui: el id del partido seleccionado. El minuto, el marcador en vivo y los goles de
// los botones se pierden con `am kill`, y esta bien: se reconstruye el partido, no el directo.
//
// HILT. @HiltViewModel: Hilt sabe crear este ViewModel. @Inject constructor: le pasa todo
// lo que hay entre parentesis. El SavedStateHandle tambien lo da Hilt, sin receta: es una
// pieza que trae de serie para cada ViewModel.
@HiltViewModel
class PartidoViewModel @Inject constructor(
    private val estadoGuardado: SavedStateHandle,
    private val repositorio: PartidosRepository,
    private val registrarGol: RegistrarGol,
) : ViewModel() {

    // Las dependencias LLEGAN por el constructor (antes salian de un singleton
    // oculto). Este ViewModel no sabe si el repositorio es de memoria, de Room o de mentira.
    // Un StateFlow LEIDO DEL HANDLE: cada vez que se escribe `estadoGuardado[CLAVE]`, cambia.
    val partidoId: StateFlow<Int> = estadoGuardado.getStateFlow(CLAVE_PARTIDO, 1)

    // Los pasos de la carga. PRIVADO: la pantalla solo ve MarcadorUiState.
    private sealed interface Carga {
        data object EnCurso : Carga
        data class Lista(val directo: Directo) : Carga
        data class Fallo(val motivo: String) : Carga
    }

    private val carga = MutableStateFlow<Carga>(Carga.EnCurso)
    private var trabajos: List<Job> = emptyList()

    // f41 · EL ESTADO QUE VE LA PANTALLA, en un solo StateFlow.
    // f42 · Ya no calcula nada: traduce el `Directo` (dominio) a un MarcadorUiState.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MarcadorUiState> = carga.flatMapLatest { paso ->
        when (paso) {
            Carga.EnCurso -> flowOf(MarcadorUiState.Cargando)
            is Carga.Fallo -> flowOf(MarcadorUiState.Error(paso.motivo))
            is Carga.Lista -> combine(paso.directo.marcador, paso.directo.instante) { marcador, instante ->
                MarcadorUiState.Exito(
                    partidoId = paso.directo.partidoId,
                    partido = paso.directo.partido,
                    marcador = marcador,
                    minuto = instante.minuto,
                    corriendo = instante.corriendo,
                    ultimoAviso = instante.ultimoAviso,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MarcadorUiState.Cargando)

    init {
        cargar(partidoId.value)
    }

    // UNICA puerta de entrada de los eventos de la pantalla.
    fun alEvento(evento: MarcadorEvento) = when (evento) {
        MarcadorEvento.Empezar -> (carga.value as? Carga.Lista)?.let { trabajos = it.directo.iniciar(viewModelScope) } ?: Unit
        is MarcadorEvento.Gol -> (carga.value as? Carga.Lista)?.directo?.let { directo ->
            // Se ve al instante en el directo Y se guarda con el caso de uso.
            directo.golAMano(evento.lado)
            // El `Result` con el fallo (no existe el partido, plantilla vacia) se descarta:
            // por ahora no hay donde mostrarlo, y el gol ya se ve en el directo.
            viewModelScope.launch {
                registrarGol(directo.partidoId, evento.lado, directo.instante.value.minuto)
            }
            Unit
        } ?: Unit
        is MarcadorEvento.Elegir -> cargar(evento.partidoId)
    }

    // Elegir un partido: para el directo, y pasa por `Cargando`.
    // La lectura va al repositorio: es `suspend`, asi que ya puede tardar (con Room, f45).
    private fun cargar(id: Int) {
        trabajos.forEach { it.cancel() }
        trabajos = emptyList()
        carga.value = Carga.EnCurso
        viewModelScope.launch {
            val elegido = repositorio.partido(id)
            if (elegido == null) {
                carga.value = Carga.Fallo("No existe el partido n.º $id")
            } else {
                // Solo se guarda el id si es valido: un id malo en el handle
                // rompería la app tambien tras un am kill.
                estadoGuardado[CLAVE_PARTIDO] = id
                carga.value = Carga.Lista(Directo(id, elegido, MS_POR_MINUTO))
            }
        }
    }

    companion object {
        const val MS_POR_MINUTO = 250L
        const val CLAVE_PARTIDO = "partidoId"
    }
}
