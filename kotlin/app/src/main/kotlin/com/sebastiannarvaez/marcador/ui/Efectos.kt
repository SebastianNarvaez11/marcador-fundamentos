package com.sebastiannarvaez.marcador.ui

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sebastiannarvaez.marcador.torneo.Marcador
import kotlinx.coroutines.delay

// f37 · EFECTOS
//
// Un composable debe ser una funcion pura del estado: se puede ejecutar muchas veces,
// en cualquier orden y hasta descartarse a medias. Todo lo que NO es «dibujar»
// (arrancar una corrutina, registrar un listener, cambiar algo de fuera de Compose)
// no puede ir suelto en el cuerpo: se ejecutaria en CADA recomposicion. Para eso hay
// los «efectos», que atan ese trabajo al ciclo de vida del composable en la
// composicion (entra, se recompone, sale).
//
//   LaunchedEffect(keys)   corrutina atada a la composicion. Arranca al entrar; si
//                          cambia una key, CANCELA la corrutina y arranca otra; se
//                          cancela al salir. -> el cronometro.
//   DisposableEffect(keys) algo que hay que DESHACER: registras y en `onDispose { }`
//                          des-registras. -> el observador del ciclo de vida.
//   SideEffect             corre tras CADA composicion correcta; para publicar el
//                          estado de Compose hacia fuera. -> el titulo de la Activity.
//   rememberUpdatedState   una referencia que siempre apunta al ULTIMO valor, para
//                          que un efecto longevo no use una lambda vieja.
//   derivedStateOf         un estado calculado desde otros, que solo avisa cuando
//                          cambia SU resultado. -> ver PantallaPartidos (Listas.kt).
//   rememberCoroutineScope una corrutina lanzada desde un CALLBACK (un onClick), donde
//                          no puedes usar LaunchedEffect. -> ver PantallaPartidos.
//
// Una key = «vuelve a empezar cuando esto cambie». `LaunchedEffect(Unit)` = solo una
// vez. Una key que sobra reinicia el trabajo de mas; una que falta lo deja con datos
// viejos.

private const val ETIQUETA = "Efectos"
const val MINUTOS_DEL_PARTIDO = 90

// LaunchedEffect + rememberUpdatedState. Es un composable que NO dibuja nada: solo
// hace un trabajo mientras esta en la composicion.
@Composable
fun RelojDelPartido(
    corriendo: Boolean,
    minutoActual: Int,
    msPorMinuto: Long,
    alMinuto: (Int) -> Unit,
) {
    // Si la key fuera `alMinuto`, cada recomposicion del padre (que crea una lambda
    // nueva) cancelaria y reiniciaria el reloj. Si no fuera key ni se envolviera, el
    // efecto se quedaria con la PRIMERA lambda y avisaria con los datos de entonces.
    // rememberUpdatedState resuelve las dos cosas: la key no cambia y `avisar` siempre
    // apunta a la ultima lambda. (Ver diario: se probo quitandolo.)
    val avisar by rememberUpdatedState(alMinuto)

    // key = corriendo: al pulsar Pausa la corrutina se CANCELA (delay lanza
    // CancellationException) y al pulsar Reanudar arranca una nueva desde el minuto en que
    // estabamos. Leer `minutoActual` aqui dentro solo lo lee al arrancar: es lo que queremos.
    LaunchedEffect(corriendo) {
        if (!corriendo) return@LaunchedEffect
        var minuto = minutoActual
        while (minuto < MINUTOS_DEL_PARTIDO) {
            delay(msPorMinuto)
            minuto++
            avisar(minuto)
        }
    }
}

// DisposableEffect: registrar un observador y quitarlo al salir. Sin `onDispose`, el
// observador se quedaria enganchado al ciclo de vida de la Activity aunque el
// composable ya no exista (una fuga; compilar sin `onDispose` ni siquiera se deja).
@Composable
fun ObservadorDelCiclo(alPararse: () -> Unit) {
    val propietario = LocalLifecycleOwner.current
    val alPararseActual by rememberUpdatedState(alPararse)

    // key = propietario: si cambiara el LifecycleOwner, se des-registra del viejo y se
    // registra en el nuevo.
    DisposableEffect(propietario) {
        val observador = LifecycleEventObserver { _, evento ->
            Log.d(ETIQUETA, "el composable ve $evento")
            if (evento == Lifecycle.Event.ON_STOP) alPararseActual()
        }
        propietario.lifecycle.addObserver(observador)
        Log.d(ETIQUETA, "observador REGISTRADO")
        onDispose {
            propietario.lifecycle.removeObserver(observador)
            Log.d(ETIQUETA, "observador QUITADO")
        }
    }
}

// SideEffect: tras CADA composicion correcta de esta funcion se copia el marcador a
// algo que Compose no conoce: el titulo de la Activity (el que sale en «recientes»).
// Ojo: se ejecuta en cada recomposicion del composable, cambie o no el marcador (con el
// reloj en marcha, una vez por minuto: mira Logcat). Es barato aqui; para algo caro
// habria que comparar antes con el valor anterior.
@Composable
fun TituloDeLaActividad(marcador: Marcador) {
    val actividad = LocalActivity.current
    SideEffect {
        actividad?.title = "Marcador $marcador"
        Log.d(ETIQUETA, "titulo publicado: Marcador $marcador")
    }
}

// La parte visible del cronometro (sin estado: recibe todo y avisa con lambdas).
// En f38 desaparecen Pausa y Reiniciar: el partido en vivo solo corre hacia delante
// (no se pausa ni se reinicia) y el boton se deshabilita al empezar.
@Composable
fun PanelDelCronometro(
    minuto: Int,
    corriendo: Boolean,
    ultimoAviso: String,
    pausas: Int,
    onEmpezar: () -> Unit,
    duracion: Int = MINUTOS_DEL_PARTIDO,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Minuto $minuto'", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onEmpezar, enabled = !corriendo && minuto == 0) {
            Text(if (minuto >= duracion) "Partido terminado" else if (corriendo) "En juego" else "Empezar partido")
        }
        Text("Ultimo aviso (cada 15'): $ultimoAviso")
        Text("Veces que la pantalla paso a segundo plano: $pausas")
    }
}
