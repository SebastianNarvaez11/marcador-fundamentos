package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// LA PANTALLA DEL FORMULARIO. No decide nada: pinta el formulario y los errores que le dan, y
// avisa de cada tecla. Lo unico suyo es lo que solo tiene sentido en pantalla: el foco y el
// teclado, y el TEXTO de cada error (las reglas devuelven un enum; aqui se elige la frase).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAnadirJugador(
    equipo: String,
    formulario: FormularioJugador,
    errorNombre: ErrorNombre?,
    errorDorsal: ErrorDorsal?,
    sePuedeGuardar: Boolean,
    onNombre: (String) -> Unit,
    onDorsal: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Añadir jugador") }) },
    ) { interior ->
        Column(
            Modifier.padding(interior).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Nuevo jugador de $equipo", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = formulario.nombre,
                onValueChange = onNombre,
                label = { Text("Nombre") },
                singleLine = true,
                isError = errorNombre != null,
                // Debajo del campo: el error si lo hay; si no, cuantas letras van de las 25.
                supportingText = {
                    Text(errorNombre?.let(::textoDe) ?: "${formulario.nombre.trim().length}/${FormularioJugador.MAXIMO_NOMBRE}")
                },
                // Mayuscula al empezar cada palabra, y la tecla «Siguiente» en el teclado: pasa
                // el foco al campo de abajo (es lo que hace ImeAction.Next por su cuenta).
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = formulario.dorsal,
                onValueChange = onDorsal,
                label = { Text("Dorsal (opcional)") },
                singleLine = true,
                isError = errorDorsal != null,
                supportingText = { errorDorsal?.let { Text(textoDe(it)) } },
                // Teclado de numeros y la tecla «Hecho», que guarda como el boton.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onGuardar() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onGuardar, enabled = sePuedeGuardar, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar")
            }
        }
    }
}

// El enum se traduce a texto AQUI, en la pantalla: las reglas no saben de idiomas.
private fun textoDe(error: ErrorNombre): String = when (error) {
    ErrorNombre.VACIO -> "Escribe un nombre"
    ErrorNombre.LARGO -> "Como mucho ${FormularioJugador.MAXIMO_NOMBRE} letras"
    ErrorNombre.REPETIDO -> "Ya hay un jugador con ese nombre"
}

private fun textoDe(error: ErrorDorsal): String = when (error) {
    ErrorDorsal.NO_ES_NUMERO -> "Escribe solo números"
    ErrorDorsal.FUERA_DE_RANGO -> "Del 1 al 99"
    ErrorDorsal.REPETIDO -> "Ese dorsal ya lo lleva otro jugador"
}

@Preview(showBackground = true)
@Composable
private fun PantallaAnadirJugadorPreview() {
    MaterialTheme {
        PantallaAnadirJugador(
            equipo = "Rayo FC",
            formulario = FormularioJugador(nombre = "Bea", dorsal = "9", nombreTocado = true, dorsalTocado = true),
            errorNombre = null,
            errorDorsal = ErrorDorsal.REPETIDO,
            sePuedeGuardar = false,
            onNombre = {},
            onDorsal = {},
            onGuardar = {},
        )
    }
}
