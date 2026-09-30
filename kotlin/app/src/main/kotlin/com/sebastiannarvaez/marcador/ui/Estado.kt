package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// f33 · ESTADO EN COMPOSE
//
// ESTADO = cualquier valor que puede cambiar y que la pantalla muestra. Compose
// solo se entera de un cambio si el valor vive dentro de un `State<T>`
// (mutableStateOf / mutableIntStateOf): al escribirlo, las funciones que lo LEYERON
// se vuelven a ejecutar (recomposicion). Una `var` normal cambia pero nadie avisa.
//
// Y como una recomposicion vuelve a ejecutar la funcion ENTERA, una variable local
// se reinicia en cada pasada. Por eso hay tres niveles, de menos a mas duradero:
//
//   var normal             ->  no dispara recomposicion (la pantalla no se entera)
//   remember { state }     ->  sobrevive a las RECOMPOSICIONES, pero no a que la
//                              Activity se destruya (rotar, cambiar tema, idioma...)
//   rememberSaveable       ->  ademas se guarda en el Bundle de onSaveInstanceState:
//                              sobrevive a rotar Y a la muerte del proceso (am kill)
//
// `by` con getValue/setValue (los imports de arriba) permite escribir `goles++` en
// vez de `goles.value++`. Con `mutableIntStateOf` no hay boxing de Int.

// Tres contadores, uno por nivel de durabilidad, para probarlos lado a lado.
// (Los botones de gol de verdad, con `remember`, estan en PantallaMarcador.)
@Composable
fun ComparacionDeEstado(modifier: Modifier = Modifier) {
    // 1) var normal: al pulsar sube, pero la pantalla NO se repinta (nadie la avisa).
    //    Y si OTRA cosa fuerza una recomposicion, la funcion se ejecuta de nuevo y la
    //    variable vuelve a 0: el valor sube en memoria pero nunca llega a verse.
    var golesSinEstado = 0

    // 2) remember: sobrevive a recomposiciones, se pierde al rotar y con am kill.
    var golesRemember by remember { mutableIntStateOf(0) }

    // 3) rememberSaveable: sobrevive a rotar y a am kill.
    var golesSaveable by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { golesSinEstado++ }) { Text("var normal (NO se repinta): $golesSinEstado") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { golesRemember++ }) { Text("remember: $golesRemember") }
            Button(onClick = { golesSaveable++ }) { Text("saveable: $golesSaveable") }
        }
        // COMO COMPROBARLO (tras sumar unos cuantos en cada boton):
        //  - Rotar:  adb shell settings put system accelerometer_rotation 0
        //            adb shell settings put system user_rotation 1
        //            -> remember vuelve a 0, saveable conserva su valor.
        //  - Muerte del proceso: pulsa Home y ejecuta
        //            adb shell am kill com.sebastiannarvaez.marcador.debug
        //            luego reabre la app desde recientes -> igual: solo saveable sobrevive.
        Text("Rota el movil o haz am kill: solo «saveable» conserva su valor")
    }
}
