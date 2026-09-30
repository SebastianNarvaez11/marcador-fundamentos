package com.sebastiannarvaez.marcador.ui

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.domain.Goleador

// f47 · Tercera pantalla de la pila: lista -> partido -> goleadores. Sin estado propio.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaGoleadores(goleadores: List<Goleador>, alVolver: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Goleadores") }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = alVolver) { Text("Volver al partido") }
            }
        },
    ) { interior ->
        LazyColumn(Modifier.padding(interior), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(goleadores, key = { it.jugador }) { goleador ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(goleador.jugador, Modifier.weight(1f))
                    Text("${goleador.goles} goles")
                }
            }
        }
    }
}
