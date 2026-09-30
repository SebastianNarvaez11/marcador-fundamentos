package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.Partido

// f34 · STATE HOISTING («elevar el estado»)
//
// Regla: un composable que necesita estado NO lo crea; lo RECIBE como parametro y
// avisa de lo que pasa con lambdas. El estado «sube» al ancestro comun mas bajo
// que lo necesita. El patron son dos parametros:
//
//        estado: T          ->  el valor actual  (baja)
//        onCambio: (T) -> Unit  ->  «quiero que cambie» (sube)
//
// Esto es FLUJO DE DATOS UNIDIRECCIONAL: el estado baja, los eventos suben.
//
// Ventajas de MarcadorContent frente a tener el `remember` dentro:
//   - se puede probar y previsualizar con cualquier estado (mira las @Preview de
//     abajo: dos marcadores distintos sin ejecutar nada);
//   - es reutilizable: hoy el estado viene de rememberSaveable, en f38 vendra de un
//     StateFlow y en F5 de un ViewModel, SIN tocar este archivo;
//   - hay una sola fuente de la verdad: nadie puede dejar el marcador desincronizado.

// Que se pinta. Es un dato inmutable: para cambiar el marcador se crea otro.
data class EstadoMarcador(val partido: Partido, val marcador: Marcador)

// Un evento de la pantalla, sin logica: «han marcado los de aqui».
enum class Lado { LOCAL, VISITANTE }

// SIN ESTADO («stateless»): mismos parametros, misma pantalla. No hay `remember`
// aqui dentro (salvo el scroll, que es estado de presentacion, no de dominio).
@Composable
fun MarcadorContent(
    estado: EstadoMarcador,
    onGol: (Lado) -> Unit,
    modifier: Modifier = Modifier,
    extras: @Composable () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        TarjetaDePartido(estado.partido, estado.marcador)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onGol(Lado.LOCAL) }) { Text("Gol ${estado.partido.local.nombre}") }
            Button(onClick = { onGol(Lado.VISITANTE) }) { Text("Gol ${estado.partido.visitante.nombre}") }
        }
        extras()
    }
}

@Preview(showBackground = true)
@Composable
private fun MarcadorContentPreview() {
    MaterialTheme {
        MarcadorContent(EstadoMarcador(Ejemplo.rayoContraToros, Marcador(2, 1)), onGol = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MarcadorContentGoleadaPreview() {
    MaterialTheme {
        MarcadorContent(EstadoMarcador(Ejemplo.lobosContraAguilas, Marcador(7, 0)), onGol = {})
    }
}
