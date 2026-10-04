package com.sebastiannarvaez.marcador.ui

import android.util.Log
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.despuesDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import com.sebastiannarvaez.marcador.torneo.Partido

// PRIMEROS PASOS CON COMPOSE
//
// PARADIGMA DECLARATIVO. Con Views/XML describias un arbol de objetos y luego
// lo MUTABAS a mano (`textView.text = "2-1"`). Con Compose describes como se ve
// la pantalla PARA UN ESTADO DADO, con funciones; cuando el estado cambia, Compose
// vuelve a llamar a esas funciones (recomposicion) y actualiza lo necesario.
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
// @OptIn, o el compilador se niega.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMarcador(onVerDemos: () -> Unit, modifier: Modifier = Modifier) {
    // COMPOSABLE CON ESTADO (el «contenedor»): es el unico sitio que POSEE los
    // goles. No dibuja nada por su cuenta: crea el estado, lo pasa hacia abajo y
    // recoge los eventos que suben. Se le llama tambien «stateful».
    val partido = Ejemplo.rayoContraToros

    // EL PARTIDO EN VIVO VIVE EN LA COMPOSICION, A PROPOSITO.
    //
    // `remember { MutableStateFlow(...) }` crea el marcador del partido la primera vez y lo
    // guarda mientras el composable siga en la composicion (igual que el minuto y el resto
    // del reloj, mas abajo). Al ROTAR, Android destruye la Activity, la composicion se
    // descarta y `remember` empieza de cero: NACE OTRO partido, sin empezar, en 0-0. El
    // reloj que lo hacia avanzar es un LaunchedEffect y muere con la composicion.
    //
    // UN SOLO RELOJ. El bucle de `RelojDelPartido` es la unica fuente del tiempo: en cada
    // minuto aplica los goles del guion al marcador y, si toca, calcula el aviso con ESE
    // marcador. (Con un segundo reloj, el de `PartidoEnVivo`, en otro hilo, el aviso podia
    // leer el marcador un gol por detras.)
    //
    // COMO REPRODUCIRLO: pulsa «Empezar partido», espera a que haya algun gol (el primero
    // llega en unos 3 s) y gira el movil (adb shell settings put system accelerometer_rotation 0
    //   adb shell settings put system user_rotation 1). El marcador vuelve a 0-0, el
    // minuto a 0' y el boton vuelve a decir «Empezar partido». No es un fallo de
    // collectAsStateWithLifecycle: el fallo es que el partido no debia vivir aqui.
    // Lo arregla un ViewModel: sobrevive a la rotacion y el partido vive en el.
    val marcadorEnVivo = remember { MutableStateFlow(Marcador(0, 0)) }

    // Goles «a mano» (los botones de sumar), sumados a los del partido en vivo.
    // Tambien en `remember`: es parte del mismo partido y se reinicia con el.
    var golesLocalAMano by remember { mutableIntStateOf(0) }
    var golesVisitanteAMano by remember { mutableIntStateOf(0) }

    // RECOGER UN StateFlow EN COMPOSE. `marcadorEnVivo` es un StateFlow<Marcador>:
    // siempre tiene valor, asi que no hace falta valor inicial. Hay dos formas de
    // convertirlo en un State de Compose:
    //
    //   collectAsState()              recoge mientras el composable este en la composicion.
    //                                 Con la app en segundo plano (Home) SIGUE recogiendo:
    //                                 la Activity esta parada, pero la composicion existe
    //                                 y el flujo sigue trabajando para nadie.
    //   collectAsStateWithLifecycle() recoge solo mientras el ciclo de vida este al menos
    //                                 en STARTED (visible): al pasar a STOPPED CANCELA la
    //                                 recogida, y al volver la REANUDA con el ultimo valor.
    //                                 Es lo mismo que repeatOnLifecycle(STARTED), ya
    //                                 empaquetado. Es la opcion por defecto en Android.
    //
    // El interruptor de abajo deja probar las dos; `onEach` escribe en Logcat cada valor
    // que llega (adb logcat -s Recoleccion): con la app en segundo plano, solo se ven
    // lineas con collectAsState.
    var conCicloDeVida by remember { mutableStateOf(true) }
    val flujo = remember {
        marcadorEnVivo.onEach {
            Log.d("Recoleccion", "llega $it (${if (conCicloDeVida) "con ciclo de vida" else "collectAsState"})")
        }
    }
    val marcadorDelPartido by if (conCicloDeVida) {
        flujo.collectAsStateWithLifecycle(initialValue = marcadorEnVivo.value)
    } else {
        flujo.collectAsState(initial = marcadorEnVivo.value)
    }
    val estado = EstadoMarcador(
        partido,
        Marcador(marcadorDelPartido.local + golesLocalAMano, marcadorDelPartido.visitante + golesVisitanteAMano),
    )

    // El reloj, el observador y el titulo. Tambien en `remember`: el minuto es del partido.
    var minuto by remember { mutableIntStateOf(0) }
    var corriendo by remember { mutableStateOf(false) }
    var ultimoAviso by remember { mutableStateOf("ninguno") }
    var pausas by rememberSaveable { mutableIntStateOf(0) }

    RelojDelPartido(corriendo, minutoActual = minuto, msPorMinuto = MS_POR_MINUTO) { nuevoMinuto ->
        // 1) Los goles del guion que caen en este minuto, al marcador.
        marcadorEnVivo.update { antes ->
            partido.eventos.filter { it.minuto == nuevoMinuto }.fold(antes) { m, evento -> m.despuesDe(evento, partido) }
        }
        minuto = nuevoMinuto
        if (nuevoMinuto % 15 == 0) {
            // 2) El aviso, con el marcador que ACABAMOS de calcular (no con `estado`, que es
            // el que la pantalla recogio en la composicion anterior y puede ir un gol por
            // detras). Sin rememberUpdatedState en el reloj, `golesLocalAMano` seria el de
            // cuando arranco el efecto.
            val vivo = marcadorEnVivo.value
            ultimoAviso = "minuto $nuevoMinuto con ${Marcador(vivo.local + golesLocalAMano, vivo.visitante + golesVisitanteAMano)}"
        }
        if (nuevoMinuto >= MINUTOS_DEL_PARTIDO) corriendo = false
    }
    ObservadorDelCiclo(alPararse = { pausas++ })
    TituloDeLaActividad(estado.marcador)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("Marcador") }) },
        bottomBar = {
            // Otro slot: una barra inferior con un boton para ir a las demos de Android.
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = onVerDemos) { Text("Pruebas de Android") }
            }
        },
    ) { paddingValues ->
        // SIEMPRE se usa paddingValues (si no, el contenido queda tapado por las barras).
        MarcadorContent(
            estado = estado,
            onGol = { lado ->
                when (lado) {
                    Lado.LOCAL -> golesLocalAMano++
                    Lado.VISITANTE -> golesVisitanteAMano++
                }
            },
            modifier = Modifier.padding(paddingValues),
            // Un slot mas: lo que sobra de la pantalla (las demos del principio) se inyecta desde fuera.
            extras = {
                PanelDelCronometro(
                    minuto, corriendo, ultimoAviso, pausas,
                    // Empezar = poner en marcha el unico reloj.
                    onEmpezar = { corriendo = true },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = conCicloDeVida, onCheckedChange = { conCicloDeVida = it })
                    Text(if (conCicloDeVida) "collectAsStateWithLifecycle" else "collectAsState (sin ciclo de vida)")
                }
                ComparacionDeEstado()
                OrdenDeLosModifiers()
                InsigniaSobreEscudo()
            },
        )
    }
}

private const val MS_POR_MINUTO = 250L

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
    MaterialTheme { PantallaMarcador(onVerDemos = {}) }
}
