package com.sebastiannarvaez.marcador.feature.plantilla.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Jugador

// La plantilla de un equipo: una fila por jugador, con su dorsal o un guion. Sin estado propio.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPlantilla(equipo: String, jugadores: List<Jugador>, onAnadir: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Plantilla de $equipo") }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onAnadir) { Text("Añadir jugador") }
            }
        },
    ) { interior ->
        LazyColumn(Modifier.padding(interior), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // En un equipo no hay dos jugadores con el mismo nombre: sirve de key.
            items(jugadores, key = { it.nombre }) { jugador ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(jugador.nombre)
                    Text(jugador.dorsalOGuion())
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaPlantillaPreview() {
    MaterialTheme { PantallaPlantilla(Ejemplo.rayo.nombre, Ejemplo.rayo.plantilla, onAnadir = {}) }
}
