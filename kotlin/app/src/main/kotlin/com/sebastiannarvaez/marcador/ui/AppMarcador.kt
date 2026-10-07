package com.sebastiannarvaez.marcador.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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

// Las pestanas de primer nivel, que nunca se apilan entre si (la barra solo sale en ellas).
private val PESTANAS: List<NavKey> = listOf(ListaKey, TablasKey, RecomposicionKey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMarcador(onVerDemos: () -> Unit, modifier: Modifier = Modifier) {
    val pila = rememberNavBackStack(ListaKey)
    val actual = pila.lastOrNull()

    // Un Scaffold dentro de otro: el de fuera pone la barra inferior y los de dentro
    // sus TopAppBar. Todos con `contentWindowInsets = WindowInsets(0)`, o sale margen doble.
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
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
                    val vm: ListaViewModel = viewModel(factory = Fabricas.lista)
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
                    val vm: ListaViewModel = viewModel(factory = Fabricas.lista)
                    val torneo by vm.torneo.collectAsStateWithLifecycle()
                    Scaffold(
                        topBar = { CenterAlignedTopAppBar(title = { Text("Tablas") }) },
                        contentWindowInsets = WindowInsets(0),
                    ) { interior ->
                        PantallaTablas(torneo.tablaDePosiciones(), torneo.goleadoresConGoles(), Modifier.padding(interior))
                    }
                }
                entry<RecomposicionKey> {
                    val vm: ListaViewModel = viewModel(factory = Fabricas.lista)
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
                    val vm: PartidoViewModel = viewModel(factory = Fabricas.partido(clave.partidoId))
                    PantallaMarcador(
                        onVerGoleadores = { pila.add(GoleadoresKey) },
                        onVerCronica = { pila.add(CronicaKey(clave.partidoId)) },
                        onVolver = { pila.removeAt(pila.lastIndex) },
                        viewModel = vm,
                    )
                }
                entry<GoleadoresKey> {
                    val vm: GoleadoresViewModel = viewModel(factory = Fabricas.goleadores)
                    val goleadores by vm.goleadores.collectAsStateWithLifecycle()
                    PantallaGoleadores(goleadores, alVolver = { pila.removeAt(pila.lastIndex) })
                }
                entry<ArbitrosKey> {
                    val vm: ArbitrosViewModel = viewModel(factory = Fabricas.arbitros)
                    val estado by vm.uiState.collectAsStateWithLifecycle()
                    PantallaArbitros(estado, onActualizar = vm::actualizar)
                }
                entry<CronicaKey> { clave ->
                    val vm: CronicaViewModel = viewModel(factory = Fabricas.cronica(clave.partidoId))
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
