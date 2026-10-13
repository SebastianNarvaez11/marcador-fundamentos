package com.sebastiannarvaez.marcador.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sebastiannarvaez.marcador.feature.arbitros.ui.ArbitrosViewModel
import com.sebastiannarvaez.marcador.feature.arbitros.ui.PantallaArbitros
import com.sebastiannarvaez.marcador.feature.cronica.ui.CronicaViewModel
import com.sebastiannarvaez.marcador.feature.cronica.ui.PantallaCronica
import com.sebastiannarvaez.marcador.feature.goleadores.ui.GoleadoresViewModel
import com.sebastiannarvaez.marcador.feature.goleadores.ui.PantallaGoleadores
import com.sebastiannarvaez.marcador.feature.marcador.ui.PantallaMarcador
import com.sebastiannarvaez.marcador.feature.marcador.ui.PartidoViewModel
import com.sebastiannarvaez.marcador.feature.partidos.ui.ListaViewModel
import com.sebastiannarvaez.marcador.feature.partidos.ui.PantallaPartidos
import com.sebastiannarvaez.marcador.feature.partidos.ui.PantallaTablas
import com.sebastiannarvaez.marcador.feature.partidos.ui.goleadoresConGoles
import com.sebastiannarvaez.marcador.feature.plantilla.ui.AnadirJugadorViewModel
import com.sebastiannarvaez.marcador.feature.plantilla.ui.PantallaAnadirJugador
import com.sebastiannarvaez.marcador.feature.plantilla.ui.PantallaPlantilla
import com.sebastiannarvaez.marcador.feature.plantilla.ui.PlantillaViewModel
import com.sebastiannarvaez.marcador.laboratorio.PantallaRecomposicion
import kotlinx.serialization.Serializable

// NAVIGATION 3
//
// En Navigation 3 la pila de pantallas ES UNA LISTA que tu posees: `pila.add(clave)` navega,
// `pila.removeAt(pila.lastIndex)` vuelve. Nada de grafo XML, ni `navController`, ni rutas de
// texto (eso es Navigation Compose 2, la que preguntan en las entrevistas: un
// `NavHost` con `composable("partido/{id}")` y un `navController.navigate("partido/3")`).
//
// Cada destino es una CLAVE: un objeto `NavKey` pequeno y @Serializable con los argumentos.
// `NavDisplay` mira el ULTIMO elemento de la pila y `entryProvider` dice que pantalla
// dibuja cada clave. La pila se guarda con `rememberNavBackStack`, y por ser serializable
// SOBREVIVE a rotar y a am kill.
@Serializable
data object ListaKey : NavKey

@Serializable
data object TablasKey : NavKey

@Serializable
data object RecomposicionKey : NavKey

@Serializable
data class PartidoKey(val partidoId: Int) : NavKey

@Serializable
data object GoleadoresKey : NavKey

@Serializable
data object ArbitrosKey : NavKey

@Serializable
data class CronicaKey(val partidoId: Int) : NavKey

// La plantilla de un equipo: el argumento es su nombre (el mismo que guarda la base).
@Serializable
data class PlantillaKey(val equipo: String) : NavKey

@Serializable
data class AnadirJugadorKey(val equipo: String) : NavKey

// Las pestanas de primer nivel, que nunca se apilan entre si (la barra solo sale en ellas).
private val PESTANAS: List<NavKey> = listOf(ListaKey, TablasKey, RecomposicionKey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMarcador(onVerDemos: () -> Unit, modifier: Modifier = Modifier) {
    val pila = rememberNavBackStack(ListaKey)
    val actual = pila.lastOrNull()

    // ESTADO DE TODA LA APP, LEIDO EN LA RAIZ. Este ViewModel se pide aqui, FUERA de NavDisplay:
    // es el de la Activity, no el de una pantalla (mira AppViewModel).
    val appViewModel: AppViewModel = hiltViewModel()
    val enJuego by appViewModel.enJuego.collectAsStateWithLifecycle()

    // Un Scaffold dentro de otro: el de fuera pone la barra inferior y los de dentro
    // sus TopAppBar. Todos con `contentWindowInsets = WindowInsets(0)`, o sale margen doble.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // La barra del partido en juego va ENCIMA de las pestanas. No sale en la pantalla
            // del propio partido (alli ya se ve todo).
            Column {
                AnimatedVisibility(
                    visible = enJuego != null && actual != enJuego?.let { PartidoKey(it.partidoId) },
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    enJuego?.let { partido ->
                        BarraDelPartidoEnJuego(
                            partido,
                            onAbrir = { irAlPartido(pila, partido.partidoId) },
                            onCerrar = appViewModel::cerrar,
                            ultimaDeLaPantalla = actual !in PESTANAS,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = actual in PESTANAS,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    NavigationBar {
                        listOf(ListaKey to "Partidos", TablasKey to "Tablas", RecomposicionKey to "Recomp.")
                            .forEach { (clave, titulo) ->
                                NavigationBarItem(
                                    selected = actual == clave,
                                    onClick = { irAPestana(pila, clave) },
                                    icon = {},
                                    label = { Text(titulo) },
                                )
                            }
                    }
                }
            }
        },
    ) { paddingValues ->
        NavDisplay(
            modifier = Modifier.padding(paddingValues),
            backStack = pila,
            onBack = { if (pila.size > 1) pila.removeAt(pila.lastIndex) },
            // Dos decoradores, y al poner `entryDecorators` hay que dar los dos:
            //  - SaveableStateHolder: un almacen de `rememberSaveable` POR ENTRADA. Sin el, al
            //    volver de un partido la lista pierde su scroll (y lo que guarde con
            //    rememberSaveable).
            //  - ViewModelStore: un almacen de ViewModel POR ENTRADA: al abrir el partido 3, su
            //    ViewModel nace con esa entrada y MUERE al volver atras. Sin el todas las
            //    entradas compartirian el ViewModel de la Activity.
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<ListaKey> {
                    val vm: ListaViewModel = hiltViewModel()
                    val partidos by vm.partidos.collectAsStateWithLifecycle()
                    Scaffold(
                        // Las demos de Android (ciclo de vida, intents, permisos...) ya no cuelgan de la
                        // pantalla del partido: se abren desde la lista.
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Partidos") },
                                actions = {
                                    TextButton(onClick = { pila.add(ArbitrosKey) }) { Text("Árbitros") }
                                    TextButton(onClick = onVerDemos) { Text("Pruebas de Android") }
                                },
                            )
                        },
                        contentWindowInsets = WindowInsets(0),
                    ) { interior ->
                        PantallaPartidos(partidos, onElegir = { id -> pila.add(PartidoKey(id)) }, modifier = Modifier.padding(interior))
                    }
                }
                entry<TablasKey> {
                    val vm: ListaViewModel = hiltViewModel()
                    val torneo by vm.torneo.collectAsStateWithLifecycle()
                    Scaffold(
                        topBar = { CenterAlignedTopAppBar(title = { Text("Tablas") }) },
                        contentWindowInsets = WindowInsets(0),
                    ) { interior ->
                        PantallaTablas(
                            torneo.tablaDePosiciones(),
                            torneo.goleadoresConGoles(),
                            onElegirEquipo = { equipo -> pila.add(PlantillaKey(equipo)) },
                            modifier = Modifier.padding(interior),
                        )
                    }
                }
                entry<RecomposicionKey> {
                    val vm: ListaViewModel = hiltViewModel()
                    val torneo by vm.torneo.collectAsStateWithLifecycle()
                    Scaffold(
                        topBar = { CenterAlignedTopAppBar(title = { Text("Recomposición") }) },
                        contentWindowInsets = WindowInsets(0),
                    ) { interior -> PantallaRecomposicion(torneo, Modifier.padding(interior)) }
                }
                // La clave lleva el argumento (`clave.partidoId`): tipado, sin parsear texto.
                entry<PartidoKey> { clave ->
                    // Sin `key`: cada entrada tiene su propio almacen de ViewModel (el decorador de
                    // arriba), asi que hay un ViewModel por partido abierto sin mas.
                    // Inyeccion asistida: Hilt pone las dependencias y esta lambda (creationCallback)
                    // pone el id del partido, que sale de la clave.
                    val vm = hiltViewModel<PartidoViewModel, PartidoViewModel.Fabrica> { fabrica ->
                        fabrica.crear(clave.partidoId)
                    }
                    PantallaMarcador(
                        onVerGoleadores = { pila.add(GoleadoresKey) },
                        onVerCronica = { pila.add(CronicaKey(clave.partidoId)) },
                        onVolver = { pila.removeAt(pila.lastIndex) },
                        onIrAlPartido = { id -> irAlPartido(pila, id) },
                        viewModel = vm,
                    )
                }
                entry<GoleadoresKey> {
                    val vm: GoleadoresViewModel = hiltViewModel()
                    val goleadores by vm.goleadores.collectAsStateWithLifecycle()
                    PantallaGoleadores(goleadores, alVolver = { pila.removeAt(pila.lastIndex) })
                }
                entry<ArbitrosKey> {
                    val vm: ArbitrosViewModel = hiltViewModel()
                    val estado by vm.uiState.collectAsStateWithLifecycle()
                    PantallaArbitros(estado, onActualizar = vm::actualizar)
                }
                entry<PlantillaKey> { clave ->
                    val vm = hiltViewModel<PlantillaViewModel, PlantillaViewModel.Fabrica> { fabrica ->
                        fabrica.crear(clave.equipo)
                    }
                    val jugadores by vm.jugadores.collectAsStateWithLifecycle()
                    PantallaPlantilla(clave.equipo, jugadores, onAnadir = { pila.add(AnadirJugadorKey(clave.equipo)) })
                }
                entry<AnadirJugadorKey> { clave ->
                    val vm = hiltViewModel<AnadirJugadorViewModel, AnadirJugadorViewModel.Fabrica> { fabrica ->
                        fabrica.crear(clave.equipo)
                    }
                    // Guardado: se vuelve a la plantilla, que ya muestra al jugador nuevo (la observa).
                    LaunchedEffect(vm.guardado) {
                        if (vm.guardado) pila.removeAt(pila.lastIndex)
                    }
                    PantallaAnadirJugador(
                        equipo = clave.equipo,
                        formulario = vm.formulario,
                        errorNombre = vm.errorNombre,
                        errorDorsal = vm.errorDorsal,
                        sePuedeGuardar = vm.sePuedeGuardar,
                        onNombre = vm::alEscribirNombre,
                        onDorsal = vm::alEscribirDorsal,
                        onGuardar = vm::guardar,
                    )
                }
                entry<CronicaKey> { clave ->
                    // La segunda inyeccion asistida: el id de la cronica sale de la clave.
                    val vm = hiltViewModel<CronicaViewModel, CronicaViewModel.Fabrica> { fabrica ->
                        fabrica.crear(clave.partidoId)
                    }
                    val estado by vm.uiState.collectAsStateWithLifecycle()
                    PantallaCronica(estado, onPublicar = vm::publicar)
                }
            },
        )
    }
}

// Cambiar de pestana vacia la pila y deja la pestana sola (con la lista debajo, para que
// «atras» desde otra pestana vuelva a la lista y no cierre la app). Igual que la Pokedex.
private fun irAPestana(pila: MutableList<NavKey>, destino: NavKey) {
    if (pila.lastOrNull() == destino) return
    pila.clear()
    if (destino != ListaKey) pila.add(ListaKey)
    pila.add(destino)
}

// Ir a un partido: si ya esta en la pila, se vuelve hasta el (quitando lo de encima); si no, se abre.
internal fun irAlPartido(pila: MutableList<NavKey>, partidoId: Int) {
    val clave = PartidoKey(partidoId)
    val donde = pila.lastIndexOf(clave)
    if (donde == -1) {
        pila.add(clave)
    } else {
        while (pila.lastIndex > donde) pila.removeAt(pila.lastIndex)
    }
}
