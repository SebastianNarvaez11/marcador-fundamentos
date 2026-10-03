package com.sebastiannarvaez.marcador.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.Jugador
import com.sebastiannarvaez.marcador.torneo.Torneo

// RECOMPOSICION
//
// QUE LA DISPARA: escribir en un State que alguna funcion LEYO durante la composicion.
// Compose apunta «esta funcion leyo ese State» y, al cambiar, vuelve a ejecutar solo
// esas funciones (las «scopes»). Despues, cada composable hijo decide si SALTA (skip)
// o se vuelve a ejecutar comparando sus parametros con los de la pasada anterior.
//
// SKIPPING Y ESTABILIDAD: un composable se salta si TODOS sus parametros «no han
// cambiado». Como se compara depende de si el tipo es ESTABLE (el compilador sabe que
// si equals() dice «igual» nada visible ha cambiado: Int, String, lambdas, data
// classes con campos estables, clases con @Immutable/@Stable):
//   - parametro estable   -> se compara con equals()
//   - parametro inestable -> se compara con === (la MISMA instancia). Es lo que hace
//     el «strong skipping», activado por defecto en el plugin de Kotlin 2.x.
//     Antes (sin strong skipping) un parametro inestable impedia saltar SIEMPRE.
//
// Son inestables: List / Set / Map (son interfaces: podria haber una MutableList
// detras) y las clases de otro modulo que no usa el compilador de Compose, como
// Jugador o Partido de :torneo. Por eso `Pair<Jugador, Int>` cuenta como inestable.
//
// EL BUG QUE SE VE EN ESTA PANTALLA: el padre CREA una lista nueva en cada pasada
// (`torneo.goleadoresConGoles()` en el cuerpo del composable). El contenido es igual,
// la INSTANCIA no: === dice «cambio», y todas las filas se recomponen cada vez que
// el padre lo hace por cualquier otro motivo (aqui: subir el minuto).
//
// COMO VERLO:
//  a) En pantalla: cada fila enseña «recomposiciones: N». Pulsa «Avanzar minuto» con
//     el interruptor apagado: N sube en TODAS las filas. Enciendelo: no sube ninguna.
//  b) En Logcat: `adb logcat -s Recomp` escribe una linea por fila y pasada.
//  c) En Android Studio, Layout Inspector -> «Show Recomposition Counts» (dos
//     columnas por composable: cuantas veces se compuso y cuantas se SALTO). Con el
//     bug, la columna de saltos se queda a 0 en las filas.
//
// EL ARREGLO: `remember(torneo) { ... }` calcula la lista UNA vez y devuelve la misma
// instancia mientras `torneo` no cambie. Si la lista viniese de fuera (un ViewModel,
// por ejemplo) se le pasaria ya estable: un StateFlow<List<...>> emite otra lista solo
// cuando hay datos nuevos. Otras salidas: `@Immutable` sobre un envoltorio de la
// lista, `kotlinx.collections.immutable`, o un fichero de configuracion de
// estabilidad que declare `com.sebastiannarvaez.marcador.torneo.*` como estable.
//
// KEY y lambdas: la key hace que el contador de cada fila siga a SU jugador
// (sin key, el `remember` iria pegado a la posicion). Y con strong skipping las
// lambdas se «recuerdan» solas mientras lo que capturan sea la misma instancia; un
// `onClick = { ... }` que capture un valor que cambia en cada pasada rompe el skipping.
@Composable
fun PantallaRecomposicion(torneo: Torneo, modifier: Modifier = Modifier) {
    var minuto by remember { mutableIntStateOf(0) }
    var conArreglo by rememberSaveable { mutableStateOf(false) }

    // BUG: lista nueva en cada recomposicion de esta funcion.
    // ARREGLO: la misma lista mientras `torneo` sea el mismo.
    val goleadores = if (conArreglo) {
        remember(torneo) { torneo.goleadoresConGoles() }
    } else {
        torneo.goleadoresConGoles()
    }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Esta lectura de `minuto` es lo que recompone a ESTA funcion cuando cambia.
        Text("Minuto $minuto", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { minuto++ }) { Text("Avanzar minuto") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = conArreglo, onCheckedChange = { conArreglo = it })
            Text(if (conArreglo) "Con arreglo: remember(torneo)" else "Con el bug: lista nueva cada vez")
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(goleadores, key = { (jugador, _) -> "jugador-${jugador.nombre}" }) { par ->
                FilaContada(par)
            }
        }
    }
}

// Una fila que cuenta cuantas veces se ha recompuesto. El contador es un objeto normal
// dentro de `remember`, NO un State: si lo fuera, incrementarlo provocaria mas
// recomposiciones. Se incrementa en el CUERPO del composable, a proposito y solo para
// medir: un composable deberia ser una funcion pura y esto no lo es (puede ejecutarse
// varias veces antes de dibujarse). El sitio correcto para «hacer algo tras cada
// composicion» es SideEffect, que se explica con los efectos.
@Composable
private fun FilaContada(par: Pair<Jugador, Int>) {
    val veces = remember { intArrayOf(0) }
    veces[0]++
    // La primera composicion cuenta 1; recompuesta = cuantas veces se ha vuelto a ejecutar.
    val recomposiciones = veces[0] - 1
    Log.d("Recomp", "${par.first.nombre}: composicion n.º ${veces[0]}")
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${par.first.nombre} · ${par.second} goles", Modifier.weight(1f))
        Text("recomposiciones: $recomposiciones")
    }
}
