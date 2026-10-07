package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.domain.Arbitro

// Sin estado: recibe el UiState y avisa de «Actualizar» hacia arriba.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaArbitros(estado: ArbitrosUiState, onActualizar: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Árbitros") },
                actions = { TextButton(onClick = onActualizar, enabled = !estado.refrescando) { Text("Actualizar") } },
            )
        },
    ) { interior ->
        Column(Modifier.padding(interior).fillMaxSize()) {
            // Refrescando CON datos a la vista: una barra fina arriba, la lista no se toca.
            if (estado.refrescando && estado.arbitros.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            // El aviso va ENCIMA de la lista: lo guardado sigue visible.
            if (estado.error != null && estado.arbitros.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${estado.error}. Se muestran los árbitros guardados.",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            when {
                estado.arbitros.isNotEmpty() -> LazyColumn {
                    items(estado.arbitros, key = { it.id }) { arbitro ->
                        ListItem(
                            headlineContent = { Text(arbitro.nombre) },
                            supportingContent = { Text(arbitro.ciudad) },
                        )
                        HorizontalDivider()
                    }
                }
                // Nada guardado todavia (primera vez): o esperando a la red, o sin ella.
                estado.refrescando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                estado.error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(estado.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onActualizar) { Text("Reintentar") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaArbitrosPreview() {
    MaterialTheme {
        PantallaArbitros(
            ArbitrosUiState(
                arbitros = listOf(Arbitro(1, "Leanne Graham", "Gwenborough"), Arbitro(2, "Ervin Howell", "Wisokyburgh")),
                error = "No hay conexión",
            ),
            onActualizar = {},
        )
    }
}
