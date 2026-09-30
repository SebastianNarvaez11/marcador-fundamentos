package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sebastiannarvaez.marcador.torneo.Partido
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.despuesDe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
class PartidoViewModel(private val estadoGuardado: SavedStateHandle) : ViewModel() {

    // Un StateFlow LEIDO DEL HANDLE: cada vez que se escribe `estadoGuardado[CLAVE]`, cambia.
    val partidoId: StateFlow<Int> = estadoGuardado.getStateFlow(CLAVE_PARTIDO, 1)

    // EL PARTIDO, que en f38 vivia en `remember` y moria al rotar, vive ahora aqui: el guion
    // elegido y el marcador que va marcando (`golesEnVivo`). Al elegir otro partido se
    // cambia el guion y el marcador vuelve a 0-0.
    private val guion = MutableStateFlow(guionDe(partidoId.value))
    private val golesEnVivo = MutableStateFlow(Marcador(0, 0))

    val partido: StateFlow<Partido> = guion.asStateFlow()

    // Estado PRIVADO y mutable, expuesto como solo lectura: solo el ViewModel escribe.
    private val minutoActual = MutableStateFlow(0)
    val minuto: StateFlow<Int> = minutoActual.asStateFlow()

    private val enMarcha = MutableStateFlow(false)
    val corriendo: StateFlow<Boolean> = enMarcha.asStateFlow()

    private val avisoActual = MutableStateFlow("ninguno")
    val ultimoAviso: StateFlow<String> = avisoActual.asStateFlow()

    private val golesAMano = MutableStateFlow(Marcador(0, 0))

    // El marcador que se ve = el del partido en vivo + los goles de los botones.
    // stateIn convierte el combine en un StateFlow con el alcance del ViewModel.
    val marcador: StateFlow<Marcador> =
        combine(golesEnVivo, golesAMano) { vivo, mano ->
            Marcador(vivo.local + mano.local, vivo.visitante + mano.visitante)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Marcador(0, 0))

    private var trabajos: List<Job> = emptyList()

    // Un evento de la pantalla: «han marcado los de aqui».
    fun golAMano(lado: Lado) = golesAMano.update {
        when (lado) {
            Lado.LOCAL -> it.copy(local = it.local + 1)
            Lado.VISITANTE -> it.copy(visitante = it.visitante + 1)
        }
    }

    // Elegir otro partido: para el directo, lo pone a cero y GUARDA el id en el handle.
    fun seleccionar(id: Int) {
        if (id == partidoId.value) return
        trabajos.forEach { it.cancel() }
        trabajos = emptyList()
        minutoActual.value = 0
        enMarcha.value = false
        avisoActual.value = "ninguno"
        golesAMano.value = Marcador(0, 0)
        golesEnVivo.value = Marcador(0, 0)
        guion.value = guionDe(id)
        estadoGuardado[CLAVE_PARTIDO] = id
    }

    fun empezar() {
        if (enMarcha.value || minutoActual.value > 0) return
        enMarcha.value = true
        // UN SOLO RELOJ, en el scope del ViewModel (sobrevive a rotar). En cada minuto el
        // mismo bucle aplica los goles del guion y, si toca, calcula el aviso con ESE
        // marcador. (Otro reloj para los goles, en otro hilo, dejaba el aviso un gol por
        // detras; y `marcador.value` tampoco vale: el `stateIn` lo actualiza un poco despues.)
        val partidoActual = guion.value
        trabajos = listOf(
            viewModelScope.launch {
                while (minutoActual.value < MINUTOS_DEL_PARTIDO) {
                    delay(MS_POR_MINUTO)
                    val nuevo = minutoActual.value + 1
                    golesEnVivo.update { antes ->
                        partidoActual.eventos.filter { it.minuto == nuevo }.fold(antes) { m, evento -> m.despuesDe(evento, partidoActual) }
                    }
                    minutoActual.value = nuevo
                    if (nuevo % 15 == 0) {
                        val vivo = golesEnVivo.value
                        val mano = golesAMano.value
                        avisoActual.value = "minuto $nuevo con ${Marcador(vivo.local + mano.local, vivo.visitante + mano.visitante)}"
                    }
                }
                enMarcha.value = false
            },
        )
    }

    private fun guionDe(id: Int): Partido = DatosDeEjemplo.partidos.first { it.id == id }.partido

    companion object {
        const val MS_POR_MINUTO = 250L
        const val CLAVE_PARTIDO = "partidoId"
    }
}
