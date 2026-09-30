package com.sebastiannarvaez.marcador.domain

import com.sebastiannarvaez.marcador.torneo.Marcador
import com.sebastiannarvaez.marcador.torneo.Partido
import com.sebastiannarvaez.marcador.torneo.despuesDe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// f42 · CAPA DE DOMINIO
//
// Tres capas, y una regla: cada una solo conoce a la de DEBAJO.
//
//   ui/      pantallas (Compose) y ViewModels. Saben de Android. Convierten el dominio
//            en un UiState y los eventos del usuario en llamadas al dominio.
//   domain/  las reglas del negocio: que es un directo, cuando avisa, que es un gol.
//            KOTLIN PURO: ni un `import android.*`, ni `androidx.*`. Se prueba en la JVM.
//   data/    de donde salen y donde se guardan los datos (memoria, Room, DataStore...).
//
// FLUJO UNIDIRECCIONAL: el estado baja (data -> domain -> ui -> pantalla) y los eventos
// suben (pantalla -> ui -> domain -> data). Nadie escribe en el estado de otra capa.
// UNICA FUENTE DE LA VERDAD: cada dato tiene un solo dueño que lo modifica; los demas
// lo observan. El marcador lo posee `Directo`; la pantalla lo pinta, no lo cuenta.
// Y el TIEMPO tambien tiene un solo dueño: un unico bucle cuenta el minuto, aplica los
// goles del guion y calcula el aviso con ese mismo marcador. (Con dos relojes, uno para el
// minuto y otro para los goles, el aviso podia salir un gol por detras del marcador.)
//
// Antes de f42 el ViewModel hacia todo esto (reloj, avisos, goles de los botones). Ahora
// el ViewModel solo traduce: esto no depende de Android y por eso se puede probar en la JVM.
//
// f46: la DURACION viene de las preferencias (90 o 60). Un partido de 60 minutos ignora los
// eventos del guion posteriores al 60 y su reloj termina ahi; :torneo no se toca.
class Directo(
    val partidoId: Int,
    guion: Partido,
    private val msPorMinuto: Long,
    val minutosDelPartido: Int = 90,
) {
    val partido: Partido = guion.copy(eventos = guion.eventos.filter { it.minuto <= minutosDelPartido })
    // Los goles que va marcando el guion (los aplica el bucle de `iniciar`).
    private val golesEnVivo = MutableStateFlow(Marcador(0, 0))
    private val golesAMano = MutableStateFlow(Marcador(0, 0))

    data class Instante(val minuto: Int = 0, val corriendo: Boolean = false, val ultimoAviso: String = "ninguno")

    private val instanteActual = MutableStateFlow(Instante())
    val instante: StateFlow<Instante> = instanteActual.asStateFlow()

    // El marcador que se ve = el del partido en vivo + los goles «a mano».
    val marcador: Flow<Marcador> = combine(golesEnVivo, golesAMano) { vivo, mano ->
        Marcador(vivo.local + mano.local, vivo.visitante + mano.visitante)
    }

    fun golAMano(lado: Lado) = golesAMano.update {
        when (lado) {
            Lado.LOCAL -> it.copy(local = it.local + 1)
            Lado.VISITANTE -> it.copy(visitante = it.visitante + 1)
        }
    }

    // Arranca el partido en el alcance que le den (el del ViewModel). Un directo solo se
    // inicia una vez. UN SOLO BUCLE: en cada minuto aplica los goles del guion y, si toca,
    // calcula el aviso con el marcador que acaba de quedar (no con el `Flow` combinado, que
    // podria no haberse actualizado todavia).
    fun iniciar(alcance: CoroutineScope): List<Job> {
        if (instanteActual.value.corriendo || instanteActual.value.minuto > 0) return emptyList()
        instanteActual.update { it.copy(corriendo = true) }
        return listOf(alcance.launch {
            while (instanteActual.value.minuto < minutosDelPartido) {
                delay(msPorMinuto)
                val nuevo = instanteActual.value.minuto + 1
                golesEnVivo.update { antes ->
                    partido.eventos.filter { it.minuto == nuevo }.fold(antes) { m, evento -> m.despuesDe(evento, partido) }
                }
                instanteActual.update { it.copy(minuto = nuevo) }
                if (nuevo % 15 == 0) {
                    val vivo = golesEnVivo.value
                    val mano = golesAMano.value
                    val ahora = Marcador(vivo.local + mano.local, vivo.visitante + mano.visitante)
                    instanteActual.update { it.copy(ultimoAviso = "minuto $nuevo con $ahora") }
                }
            }
            instanteActual.update { it.copy(corriendo = false) }
        })
    }
}
