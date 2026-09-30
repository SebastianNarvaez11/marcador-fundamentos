package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.Partido

// f32 · PRIMEROS PASOS CON COMPOSE
//
// PARADIGMA DECLARATIVO. Con Views/XML describias un arbol de objetos y luego
// lo MUTABAS a mano (`textView.text = "2-1"`). Con Compose describes como se ve
// la pantalla PARA UN ESTADO DADO, con funciones; cuando el estado cambia, Compose
// vuelve a llamar a esas funciones (recomposicion, f36) y actualiza lo necesario.
// Nadie hace `setText`: la pantalla es una funcion del estado.
//
// COMPOSABLE = funcion con @Composable. No devuelve nada: EMITE elementos al arbol.
// Conviene nombrarla con Mayuscula (como un sustantivo) y darle siempre un
// parametro `modifier: Modifier = Modifier` para que quien la use pueda colocarla.

// Scaffold es el esqueleto de una pantalla Material: rellena los «slots» que le
// pasas (topBar, bottomBar, floatingActionButton...) y te da en `paddingValues` el
// hueco que esos slots ocupan. Un SLOT es un parametro que recibe un @Composable:
// en vez de `titulo: String`, `topBar: @Composable () -> Unit`, y decides TU que
// se dibuja dentro.
//
// CenterAlignedTopAppBar es «experimental» en Material 3: hay que aceptarlo con
// @OptIn, o el compilador se niega (ver diario).
// f41 · LA PANTALLA CON ESTADO («stateful»): la fina capa que conecta con el ViewModel.
// Recoge UN StateFlow y pasa el estado hacia abajo y los eventos hacia arriba.
@Composable
fun PantallaMarcador(
    onVerDemos: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartidoViewModel = viewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Esto SI es estado de la pantalla (cuantas veces paso a segundo plano), y
    // rememberSaveable basta: es un Int.
    var pausas by rememberSaveable { mutableIntStateOf(0) }
    ObservadorDelCiclo(alPararse = { pausas++ })

    PantallaMarcadorContenido(estado, pausas, viewModel::alEvento, onVerDemos, modifier)
}

// SIN ESTADO: recibe el UiState y una lambda de eventos. Se puede previsualizar con
// cualquiera de las tres variantes sin ViewModel (ver las @Preview de abajo).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMarcadorContenido(
    estado: MarcadorUiState,
    pausas: Int,
    onEvento: (MarcadorEvento) -> Unit,
    onVerDemos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Marcador") }) },
        bottomBar = {
            // Otro slot: una barra inferior con un boton para ir a las demos de F3.
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onVerDemos) { Text("Demos de F3") }
            }
        },
    ) { paddingValues ->
        // `when` exhaustivo sobre el sealed: si manana hay una variante mas, no compila
        // hasta que la pantalla diga que hacer con ella.
        when (estado) {
            MarcadorUiState.Cargando -> Box(Modifier.padding(paddingValues).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is MarcadorUiState.Error -> Column(
                Modifier.padding(paddingValues).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = { onEvento(MarcadorEvento.Elegir(1)) }) { Text("Volver al partido 1") }
            }
            is MarcadorUiState.Exito -> {
                TituloDeLaActividad(estado.marcador)
                // SIEMPRE se usa paddingValues (si no, el contenido queda tapado por las barras).
                MarcadorContent(
                    estado = EstadoMarcador(estado.partido, estado.marcador),
                    onGol = { lado -> onEvento(MarcadorEvento.Gol(lado)) },
                    modifier = Modifier.padding(paddingValues),
                    // Un slot mas: lo que sobra de la pantalla (demos de f32 y f33) se inyecta desde fuera.
                    extras = {
                        Text("Partido n.º ${estado.partidoId} (elegido en la pestana Partidos)")
                        PanelDelCronometro(
                            estado.minuto, estado.corriendo, estado.ultimoAviso, pausas,
                            onEmpezar = { onEvento(MarcadorEvento.Empezar) },
                        )
                        // Para VER la variante Error: pide un partido que no existe.
                        Button(onClick = { onEvento(MarcadorEvento.Elegir(99)) }) { Text("Provocar error (partido 99)") }
                        ComparacionDeEstado()
                        OrdenDeLosModifiers()
                        InsigniaSobreEscudo()
                    },
                )
            }
        }
    }
}

// Column apila en vertical, Row en horizontal, Box SUPERPONE (el ultimo hijo va encima).
// Alineacion: en una Row, `verticalAlignment` (eje transversal) y `horizontalArrangement`
// (eje principal); en una Column, al reves.
@Composable
fun TarjetaDePartido(partido: Partido, marcador: Marcador, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(partido.local.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                "${marcador.local} - ${marcador.visitante}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(partido.visitante.nombre, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// EL ORDEN DE LOS MODIFIERS IMPORTA. Cada modifier envuelve a lo que viene DESPUES,
// de fuera hacia dentro, y el que queda mas cerca del contenido se aplica ultimo.
//
//   A) padding(16.dp).background(color)  -> primero un margen VACIO de 16 dp y luego
//      el color solo detras de lo que queda dentro: se ve un recuadro de color con
//      el texto pegado al borde (no hay «aire» dentro del color).
//   B) background(color).padding(16.dp)  -> primero el color en TODO el espacio y luego
//      el margen por dentro: se ve un recuadro de color con aire alrededor del texto.
//
// Lo mismo ocurre con clickable: `clickable().padding()` hace clicable (y con ripple)
// tambien el margen; `padding().clickable()`, solo el contenido.
// En pantalla se ve: la A tiene el texto pegado al borde del color, la B con margen.
@Composable
fun OrdenDeLosModifiers() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("A) padding y luego background: el color NO incluye el margen")
        Text(
            "Rayo FC",
            Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Text("B) background y luego padding: el color SI incluye el margen")
        Text(
            "Rayo FC",
            Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
        )
    }
}

// Box: un escudo (circulo) con una insignia encima, alineada en una esquina.
@Composable
fun InsigniaSobreEscudo(modifier: Modifier = Modifier) {
    Box(modifier.size(64.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text("RF", fontWeight = FontWeight.Bold) }
        Text(
            "2",
            Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = 6.dp),
            color = MaterialTheme.colorScheme.onError,
        )
    }
}

// @Preview: Android Studio dibuja este composable sin ejecutar la app (panel Split /
// Design). Solo vale para composables SIN parametros (o con valores por defecto /
// PreviewParameterProvider). Por eso la envoltura: un composable que pasa datos de
// ejemplo. Las anotaciones de preview viven en ui-tooling-preview; el runtime que
// las pinta, en ui-tooling (debug).
@Preview(showBackground = true)
@Composable
private fun PantallaMarcadorPreview() {
    MaterialTheme {
        PantallaMarcadorContenido(
            MarcadorUiState.Exito(1, Ejemplo.rayoContraToros, Marcador(2, 1), 45, true, "minuto 30 con 1-1"),
            pausas = 0, onEvento = {}, onVerDemos = {},
        )
    }
}
