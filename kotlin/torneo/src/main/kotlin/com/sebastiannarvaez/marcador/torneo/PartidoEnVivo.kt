package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Un partido que se está jugando. `guion` es todo lo que va a pasar.
//
// `dispatcher` se INYECTA (llega de fuera con un valor por defecto): en la app
// es Default, pero una prueba con `runTest` le pasa su propio dispatcher de
// pruebas. Con `flowOn(Dispatchers.Default)` clavado en el código, el `delay`
// correría en tiempo real y el reloj virtual de `runTest` no podría adelantarlo.
class PartidoEnVivo(
    val guion: Partido,
    val msPorMinuto: Long = 10,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    // Un Flow FRÍO: `flow { ... }` no hace nada hasta que alguien lo recoge con
    // `collect` (o `first`, `toList`, `fold`…). Y cada `collect` ejecuta el bloque
    // ENTERO otra vez, desde el principio: dos espectadores = dos partidos
    // independientes. `emit` entrega un valor y suspende hasta que el que recoge
    // lo ha procesado.
    //
    // `flowOn` cambia el dispatcher de lo que está ANTES (el `flow { }` y sus
    // operadores anteriores); quien recoge sigue en su propio contexto.
    fun eventos(): Flow<EventoDePartido> = flow {
        guion.eventos.filter { it.minuto == 0 }.forEach { emit(it) }
        val ultimoMinuto = maxOf(MINUTOS_DEL_PARTIDO, guion.eventos.maxOfOrNull { it.minuto } ?: 0)
        for (minuto in 1..ultimoMinuto) {
            delay(msPorMinuto)
            guion.eventos.filter { it.minuto == minuto }.forEach { emit(it) }
        }
    }.flowOn(dispatcher)

    // Operadores: cada uno devuelve OTRO Flow, y sigue sin ejecutarse nada hasta
    // que se recoja el resultado.
    fun narracion(): Flow<String> = eventos().map(::describir)

    fun golesEnFrio(): Flow<Gol> = eventos().filterIsInstance<Gol>()

    // `first()` es un operador terminal que se queda con el primer valor y CANCELA
    // el resto del flujo: el partido no se sigue jugando por nada.
    suspend fun primerGol(): Gol = golesEnFrio().first()

    // ---- f22: flujos CALIENTES ----
    //
    // `eventos()` es frío: cada espectador ve SU partido. Pero un partido de
    // verdad es uno solo y todos ven lo mismo. Para eso hay flujos calientes: la
    // fuente existe aunque nadie escuche.
    //
    // StateFlow: siempre TIENE un valor (`value`), el estado actual. Un
    // suscriptor nuevo recibe primero ese valor. Es CONFLATED (si llegan dos
    // valores muy seguidos, el lento solo ve el último) y descarta valores iguales
    // al anterior. Es la elección para ESTADO que se pinta: el marcador.
    private val marcadorActual = MutableStateFlow(Marcador(0, 0))
    val marcador: StateFlow<Marcador> = marcadorActual.asStateFlow()

    // SharedFlow: un canal de EVENTOS. Sin valor actual y sin repetición
    // (`replay = 0`): quien se suscribe tarde NO ve los goles que ya pasaron, y
    // cada gol llega a todos los suscriptores, uno por uno, sin conflar.
    // Es la elección para cosas que ocurren (un gol, un aviso), no para estado.
    // Equivalente de LiveData en corrutinas: StateFlow ~ LiveData con valor
    // inicial; SharedFlow ~ un evento de una sola vez (lo que LiveData hacía mal).
    // (Un Channel también reparte eventos, pero UNO a cada receptor, no a todos.)
    private val golesEmitidos = MutableSharedFlow<Gol>(extraBufferCapacity = 16)
    val goles: SharedFlow<Gol> = golesEmitidos.asSharedFlow()

    private var iniciado = false

    // Arranca el partido: recoge `eventos()` UNA vez y alimenta a los dos flujos
    // calientes. Devuelve el Job, que termina al pitar el final. Se ejecuta en el
    // alcance que le den, así que quien lo da decide cuándo se cancela todo.
    fun iniciar(alcance: CoroutineScope): Job {
        check(!iniciado) { "El partido ya empezó" }
        iniciado = true
        return alcance.launch {
            eventos().collect { evento ->
                if (evento is Gol) {
                    marcadorActual.update { it.despuesDe(evento, guion) }
                    golesEmitidos.emit(evento)
                }
            }
        }
    }
}

// Los dos partidos de la jornada, con sus flujos calientes derivados.
//
// `stateIn` y `shareIn` convierten un flujo en StateFlow / SharedFlow dentro de
// un alcance. `WhileSubscribed(5_000)` significa: el flujo de origen solo se
// recoge mientras haya suscriptores, y sigue 5 s después de que el último se
// vaya (para no reiniciarlo en una rotación de pantalla, por ejemplo).
// Aquí es apropiado porque el origen (`combine` sobre StateFlows) se puede
// reiniciar sin perder nada. NO lo sería para `eventos()`, que empezaría otro
// partido de cero.
class Jornada(val primero: PartidoEnVivo, val segundo: PartidoEnVivo, private val alcance: CoroutineScope) {
    val marcadores: StateFlow<MarcadoresDeJornada> =
        combine(primero.marcador, segundo.marcador, ::MarcadoresDeJornada)
            .stateIn(
                scope = alcance,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = MarcadoresDeJornada(Marcador(0, 0), Marcador(0, 0)),
            )

    val goles: SharedFlow<Gol> =
        merge(primero.goles, segundo.goles)
            .shareIn(alcance, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000))

    // Los dos partidos a la vez. El Job termina cuando terminan los dos.
    fun iniciar(): Job = alcance.launch {
        primero.iniciar(this)
        segundo.iniciar(this)
    }
}
