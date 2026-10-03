package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

// RAIZ DE LA APP: tres pestañas. No es navegacion de verdad (Navigation 3 llega
// mas adelante): es un `Int` con rememberSaveable y un `when`. Vale para llegar a las listas.
//
// Un Scaffold dentro de otro: el de fuera pone la barra inferior (NavigationBar ya
// respeta la barra de gestos por su cuenta) y los de dentro pintan su TopAppBar, que
// respeta la barra de estado. Todos llevan `contentWindowInsets = WindowInsets(0)`:
// el hueco de las barras del sistema lo reserva cada barra, no el contenido; si no,
// sale un margen doble.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMarcador(onVerDemos: () -> Unit, modifier: Modifier = Modifier) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val titulos = listOf("Marcador", "Partidos", "Tablas", "Recomp.")
    val torneo = DatosDeEjemplo.torneo

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                titulos.forEachIndexed { indice, titulo ->
                    NavigationBarItem(
                        selected = pestana == indice,
                        onClick = { pestana = indice },
                        icon = {},
                        label = { Text(titulo) },
                    )
                }
            }
        },
    ) { paddingValues ->
        when (pestana) {
            0 -> PantallaMarcador(onVerDemos, Modifier.padding(paddingValues))
            1 -> Scaffold(
                Modifier.padding(paddingValues),
                topBar = { CenterAlignedTopAppBar(title = { Text("Partidos") }) },
                contentWindowInsets = WindowInsets(0),
            ) { interior -> PantallaPartidos(DatosDeEjemplo.partidos, Modifier.padding(interior)) }
            3 -> Scaffold(
                Modifier.padding(paddingValues),
                topBar = { CenterAlignedTopAppBar(title = { Text("Recomposicion") }) },
                contentWindowInsets = WindowInsets(0),
            ) { interior -> PantallaRecomposicion(torneo, Modifier.padding(interior)) }
            else -> Scaffold(
                Modifier.padding(paddingValues),
                topBar = { CenterAlignedTopAppBar(title = { Text("Tablas") }) },
                contentWindowInsets = WindowInsets(0),
            ) { interior ->
                PantallaTablas(torneo.tablaDePosiciones(), torneo.goleadoresConGoles(), Modifier.padding(interior))
            }
        }
    }
}
