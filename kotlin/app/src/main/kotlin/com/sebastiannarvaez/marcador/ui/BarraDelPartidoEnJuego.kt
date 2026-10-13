package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.domain.PartidoEnJuego
import com.sebastiannarvaez.marcador.torneo.Marcador

// LA BARRA DEL PARTIDO EN JUEGO: abajo, encima de las pestanas, en todas las pantallas menos
// la del propio partido. Al tocarla se abre el partido; al terminar, una X la cierra.

// El texto, en una funcion aparte: se prueba y se previsualiza sin pintar nada.
//   en juego -> «Rayo FC 2–1 Toros · 34'»
//   al final -> «Final · Rayo FC 3–2 Toros»
fun textoDeLaBarra(enJuego: PartidoEnJuego.EnJuego): String {
    val resultado = "${enJuego.local} ${enJuego.marcador.local}–${enJuego.marcador.visitante} ${enJuego.visitante}"
    return if (enJuego.terminado) "Final · $resultado" else "$resultado · ${enJuego.minuto}'"
}

@Composable
fun BarraDelPartidoEnJuego(
    enJuego: PartidoEnJuego.EnJuego,
    onAbrir: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    // Sin pestanas debajo, la barra es lo ultimo de la pantalla: deja el hueco de la barra del sistema.
    ultimaDeLaPantalla: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth().clickable(onClickLabel = "Abrir el partido", onClick = onAbrir),
    ) {
        Row(
            (if (ultimaDeLaPantalla) Modifier.navigationBarsPadding() else Modifier)
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(textoDeLaBarra(enJuego), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            // Mientras se juega no se puede cerrar: la X solo sale al final.
            if (enJuego.terminado) {
                IconButton(onClick = onCerrar) {
                    // TalkBack no lee «✕»: lee «Cerrar».
                    Text("✕", Modifier.clearAndSetSemantics { contentDescription = "Cerrar" })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BarraEnJuegoPreview() {
    MaterialTheme {
        BarraDelPartidoEnJuego(
            PartidoEnJuego.EnJuego(1, "Rayo FC", "Toros", Marcador(2, 1), minuto = 34, terminado = false),
            onAbrir = {}, onCerrar = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BarraFinalPreview() {
    MaterialTheme {
        BarraDelPartidoEnJuego(
            PartidoEnJuego.EnJuego(1, "Rayo FC", "Toros", Marcador(3, 2), minuto = 90, terminado = true),
            onAbrir = {}, onCerrar = {},
        )
    }
}
