package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.domain.Arbitro

// Sin estado: recibe el UiState y avisa de «Reintentar» hacia arriba.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaArbitros(estado: ArbitrosUiState, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Árbitros") }) },
    ) { interior ->
        when (estado) {
            ArbitrosUiState.Cargando -> Box(Modifier.padding(interior).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is ArbitrosUiState.Error -> Column(
                Modifier.padding(interior).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = onReintentar) { Text("Reintentar") }
            }
            is ArbitrosUiState.Exito -> LazyColumn(Modifier.padding(interior)) {
                items(estado.arbitros, key = { it.id }) { arbitro ->
                    ListItem(
                        headlineContent = { Text(arbitro.nombre) },
                        supportingContent = { Text(arbitro.ciudad) },
                    )
                    HorizontalDivider()
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
            ArbitrosUiState.Exito(listOf(Arbitro(1, "Leanne Graham", "Gwenborough"), Arbitro(2, "Ervin Howell", "Wisokyburgh"))),
            onReintentar = {},
        )
    }
}
