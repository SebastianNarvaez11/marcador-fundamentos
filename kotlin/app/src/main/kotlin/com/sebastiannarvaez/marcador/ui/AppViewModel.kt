package com.sebastiannarvaez.marcador.ui

import androidx.lifecycle.ViewModel
import com.sebastiannarvaez.marcador.domain.PartidoEnJuego
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// EL VIEWMODEL DE LA RAIZ (la misma pauta que `MainActivityViewModel` de Now in Android).
//
// La barra del partido en juego no es de ninguna pantalla: se ve encima de las pestanas, en
// todas. Quien la pinta es `AppMarcador`, la raiz, y lo que lee es estado de toda la app.
// Este ViewModel lo pide FUERA de `NavDisplay`, asi que su almacen es el de la Activity:
// vive mientras la app esta abierta y sobrevive a girar la pantalla.
//
// No guarda nada suyo: reenvia el `StateFlow` del partido en juego (ya es de solo lectura,
// no hace falta `stateIn`) y la unica accion que tiene la barra, cerrarlo.
@HiltViewModel
class AppViewModel @Inject constructor(private val partidoEnJuego: PartidoEnJuego) : ViewModel() {
    val enJuego: StateFlow<PartidoEnJuego.EnJuego?> = partidoEnJuego.actual

    fun cerrar() = partidoEnJuego.cerrar()
}
