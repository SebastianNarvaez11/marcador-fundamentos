package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.domain.CronicaPublicada

// Una data class y no un sealed: «publicando», la cronica ya publicada y el error
// CONVIVEN (si falla la segunda vez, la primera sigue a la vista).
data class CronicaUiState(
    val partidoId: Int,
    val publicando: Boolean = false,
    val publicada: CronicaPublicada? = null,
    val error: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCronica(estado: CronicaUiState, onPublicar: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Crónica del partido") }) },
    ) { interior ->
        Column(
            Modifier.padding(interior).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Partido n.º ${estado.partidoId}: se publicarán el resultado y los goles.")
            if (estado.publicando) {
                CircularProgressIndicator()
            } else {
                Button(onClick = onPublicar) { Text("Publicar") }
            }
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            estado.publicada?.let { cronica ->
                Text("Crónica publicada con el n.º ${cronica.numero}", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(cronica.titulo, style = MaterialTheme.typography.titleLarge)
                        Text(cronica.texto)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaCronicaPreview() {
    MaterialTheme {
        PantallaCronica(
            CronicaUiState(1, publicada = CronicaPublicada(101, "Rayo FC 2-1 Toros", "12' Ana (Rayo FC)\n55' Iván (Toros)\n80' Ana (Rayo FC)")),
            onPublicar = {},
        )
    }
}
