package com.sebastiannarvaez.marcador.feature.marcador.domain

import com.sebastiannarvaez.marcador.torneo.Marcador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// ESTADO DE TODA LA APP: el partido que se esta jugando.
//
// Lo leen varias pantallas a la vez: la del propio partido, la barra de abajo (en todas las
// pestanas) y la de otro partido (para decir «Ya hay un partido en juego»). Por eso no vive en
// ningun ViewModel: vive aqui, en UNA sola instancia que Hilt guarda en el contenedor de la app
// (@Singleton en DatosModule), y cada pantalla lo OBSERVA.
//
// Es Kotlin puro (capa de dominio): recibe el alcance por el constructor. En la app es el
// alcance de la app (@AlcanceDeApp), que dura lo que el proceso; por eso el partido sigue
// aunque se cierre su pantalla. En las pruebas, el `backgroundScope` de `runTest`.
//
// Regla: UNO A LA VEZ. Mientras un partido no ha terminado, no se puede empezar otro. Al
// terminar, se queda aqui («Final») hasta que alguien lo cierra.
class PartidoEnJuego(private val alcance: CoroutineScope) {

    // Lo que necesitan las pantallas para pintarlo, sin conocer el `Directo`.
    data class EnJuego(
        val partidoId: Int,
        val local: String,
        val visitante: String,
        val marcador: Marcador,
        val minuto: Int,
        val terminado: Boolean,
    )

    // PRIVADO y mutable dentro; hacia fuera, solo lectura (la misma pauta que en `Directo`).
    private val estado = MutableStateFlow<EnJuego?>(null)
    val actual: StateFlow<EnJuego?> = estado.asStateFlow()

    private var vivo: Directo? = null
    private var observacion: Job? = null

    // El directo de ESE partido, si es el que esta en juego (o el que acaba de terminar).
    // La pantalla del partido lo reutiliza en vez de crear otro.
    fun directo(partidoId: Int): Directo? = vivo?.takeIf { it.partidoId == partidoId }

    // Empieza `directo` en el alcance de la app. Devuelve `false`, sin hacer nada, si hay otro
    // partido sin terminar.
    // Se mira el directo, no `actual`: `actual` se rellena un instante despues (en el alcance)
    // y dos toques seguidos colarian dos partidos.
    fun empezar(directo: Directo): Boolean {
        val anterior = vivo
        if (anterior != null && !anterior.terminado) return false
        observacion?.cancel()
        vivo = directo
        directo.iniciar(alcance)
        // Cada minuto y cada gol del directo se copian a `actual`.
        observacion = alcance.launch {
            combine(directo.marcador, directo.instante) { marcador, instante ->
                EnJuego(
                    partidoId = directo.partidoId,
                    local = directo.partido.local.nombre,
                    visitante = directo.partido.visitante.nombre,
                    marcador = marcador,
                    minuto = instante.minuto,
                    terminado = instante.minuto >= directo.minutosDelPartido,
                )
            }.collect { estado.value = it }
        }
        return true
    }

    // Quitar el partido (la X de la barra). Solo cuando ya termino: en juego no se cierra.
    fun cerrar() {
        val anterior = vivo ?: return
        if (!anterior.terminado) return
        observacion?.cancel()
        observacion = null
        vivo = null
        estado.value = null
    }
}

// Un directo ha terminado cuando su reloj llega a la duracion del partido.
private val Directo.terminado: Boolean get() = instante.value.minuto >= minutosDelPartido
