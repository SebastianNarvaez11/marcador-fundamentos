package com.sebastiannarvaez.marcador.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import com.sebastiannarvaez.marcador.data.DatosDeEjemplo
import com.sebastiannarvaez.marcador.domain.PartidoDeLista
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.FilaDePosicion
import com.sebastiannarvaez.marcador.torneo.Jugador
import com.sebastiannarvaez.marcador.torneo.Torneo
import com.sebastiannarvaez.marcador.torneo.marcador
import kotlinx.coroutines.launch

// LISTAS CON LazyColumn
//
// `Column` compone TODOS sus hijos aunque no se vean. `LazyColumn` solo compone (y
// mide) los que caben en pantalla mas un poco de margen: con 10.000 filas, sigue
// siendo barata. Su contenido no es un lambda @Composable normal sino un DSL
// (`LazyListScope`): `item { }` para uno, `items(lista) { }` para muchos.
//
// KEY: `items(lista, key = { it.id })`. Sin key, Compose identifica cada fila por su
// POSICION. Si se inserta una fila arriba, la fila que estaba en la posicion 0 pasa a
// la 1 y Compose cree que «la fila 1 ha cambiado de contenido»: recompone todas y,
// peor, el estado interno de cada fila (`remember`, scroll, foco) se queda pegado a la
// posicion, no al dato. Con key, el estado sigue al dato aunque se mueva.
//
// Reglas de la key: unica dentro de la lista, estable (el mismo dato, la misma key en
// cada pasada) y, en Android, de un tipo que quepa en un Bundle (Int, Long, String,
// enum...; NO una data class tuya). Ver diario: pasarle un `Jugador` de key falla.
//
// `contentType` opcional: dice que filas son «del mismo tipo» para reutilizar mejor
// su composicion (util cuando la lista mezcla cabeceras y filas).

@Composable
fun PantallaPartidos(
    partidos: List<PartidoDeLista>,
    onElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // LazyListState es el estado del scroll. Se lee `firstVisibleItemIndex`, que
    // cambia con CADA pixel de desplazamiento; si el `if` dependiera de el directamente,
    // esta funcion se recompondria continuamente. derivedStateOf lo evita: calcula
    // «¿ya bajo de la fila 3?» y solo avisa cuando ese BOOLEANO cambia (2 veces en total).
    val estadoDeLista = rememberLazyListState()
    val mostrarBotonSubir by remember { derivedStateOf { estadoDeLista.firstVisibleItemIndex > 3 } }

    // rememberCoroutineScope: un scope atado a esta composicion para lanzar corrutinas
    // desde un callback. `animateScrollToItem` es suspend, y un onClick no lo es;
    // LaunchedEffect aqui no vale porque no se puede llamar dentro de un onClick. El scope
    // se cancela cuando este composable sale de la composicion.
    val alcance = rememberCoroutineScope()

    Box(modifier) {
        LazyColumn(
            state = estadoDeLista,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(partidos, key = { it.id }) { item ->
                FilaDePartido(
                    item.partido.local.nombre,
                    item.partido.visitante.nombre,
                    item.partido.marcador().toString(),
                    Modifier.clickable { onElegir(item.id) },
                )
            }
        }
        if (mostrarBotonSubir) {
            SmallFloatingActionButton(
                onClick = { alcance.launch { estadoDeLista.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) { Text("↑") }
        }
    }
}

@Composable
fun FilaDePartido(local: String, visitante: String, resultado: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(local, Modifier.weight(1f))
            Text(resultado, fontWeight = FontWeight.Bold)
            Text(visitante, Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

// Dos listas en una sola LazyColumn: cabecera (`item`), filas (`items`), otra cabecera
// y otras filas. Una sola lista que se desplaza, en vez de dos LazyColumn anidadas
// (una LazyColumn dentro de otra que se desplaza en la misma direccion no es un patron valido).
@Composable
fun PantallaTablas(
    tabla: List<FilaDePosicion>,
    goleadores: List<Pair<Jugador, Int>>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "cabecera-tabla", contentType = "cabecera") {
            Text("Tabla de posiciones", style = MaterialTheme.typography.titleLarge)
        }
        // itemsIndexed da tambien la posicion: el puesto en la tabla.
        itemsIndexed(tabla, key = { _, fila -> "equipo-${fila.equipo.nombre}" }, contentType = { _, _ -> "fila" }) { indice, fila ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${indice + 1}. ${fila.equipo.nombre}", Modifier.weight(1f))
                Text("${fila.jugados} PJ  ${fila.diferencia} DG  ${fila.puntos} pts")
            }
        }
        item(key = "cabecera-goleadores", contentType = "cabecera") {
            Text("Goleadores", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        }
        // La key es el NOMBRE (String), no el Jugador: un String siempre cabe en un Bundle.
        items(goleadores, key = { (jugador, _) -> "jugador-${jugador.nombre}" }, contentType = { "fila" }) { (jugador, goles) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${jugador.nombre} ${jugador.dorsalOGuion()}", Modifier.weight(1f))
                Text("$goles goles")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaTablasPreview() {
    val torneo = DatosDeEjemplo.torneo
    MaterialTheme { PantallaTablas(torneo.tablaDePosiciones(), torneo.goleadoresConGoles()) }
}

// El Ranking de :torneo sabe cuantos goles lleva cada uno (`puntosDe`); la pantalla
// necesita el par (jugador, goles) ya calculado.
fun Torneo.goleadoresConGoles(): List<Pair<Jugador, Int>> =
    goleadores().let { ranking -> ranking.ordenados.map { it to ranking.puntosDe(it) } }
