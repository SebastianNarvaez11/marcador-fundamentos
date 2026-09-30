package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

// Concurrencia estructurada: toda corrutina vive dentro de un alcance
// (CoroutineScope) y el alcance no termina hasta que terminan sus hijas.
// Si el alcance se cancela, se cancelan todas las hijas. Nada queda huérfano.

// Una función de extensión sobre CoroutineScope: `launch` sin nombre de alcance
// dentro es `this.launch`, es decir, hija de quien la llame.
// `isActive` es la forma cooperativa de mirar «¿me han cancelado?».
fun CoroutineScope.cronometro(msPorMinuto: Long, alMinuto: (Int) -> Unit): Job = launch {
    var minuto = 0
    while (isActive) {
        delay(msPorMinuto)
        minuto++
        alMinuto(minuto)
    }
}

// El cronómetro es infinito. `coroutineScope` no termina mientras tenga hijas
// vivas, así que si nadie lo cancelara, esta función no volvería nunca.
// Al pitar el final se cancela el reloj y el alcance puede cerrarse.
suspend fun jugarConCronometro(
    partido: Partido,
    msPorMinuto: Long = 10,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    alMinuto: (Int) -> Unit = {},
): Partido = coroutineScope {
    val reloj = cronometro(msPorMinuto, alMinuto)
    val final = jugarPartido(partido, msPorMinuto, dispatcher)
    reloj.cancel()
    final
}

// La cancelación es COOPERATIVA: cancelar solo marca al Job. Las funciones
// suspendidas de la librería (`delay`, `await`…) comprueban la marca y lanzan
// CancellationException. Un bucle que solo calcula, sin suspender nunca,
// tiene que mirar él mismo con `ensureActive()`; si no, no se entera.
suspend fun probabilidadDeVictoria(partido: Partido, simulaciones: Int = 5_000_000): Double {
    val fuerzaLocal = partido.golesLocal + 1
    val fuerzaVisitante = partido.golesVisitante + 1
    var victoriasLocales = 0
    for (i in 1..simulaciones) {
        // Cada mil vueltas, ¿sigo vivo? Lanza CancellationException si no.
        if (i % 1_000 == 0) currentCoroutineContext().ensureActive()
        if (Random.nextInt(fuerzaLocal + fuerzaVisitante) < fuerzaLocal) victoriasLocales++
    }
    return victoriasLocales.toDouble() / simulaciones
}

// `runCatching` y `catch (e: Exception)` atrapan TAMBIÉN la CancellationException,
// porque es una Exception. Si la tragas, la corrutina cancelada sigue como si
// nada. Regla: atrápala y vuelve a lanzarla.
suspend fun <T> intentar(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (cancelacion: CancellationException) {
    throw cancelacion
} catch (error: Exception) {
    Result.failure(error)
}

// Un alcance PROPIO: la clase decide cuándo empieza y cuándo acaba la vida de
// todo lo que lanza. El Job del alcance es el padre de todas las corrutinas;
// cancelarlo las cancela a todas. Al pitar el final, `pitarElFinal()`.
//
// Con `SupervisorJob` (f19), un partido que falla no cancela al alcance ni a los
// demás partidos. Su error llega al CoroutineExceptionHandler, que aquí avisa
// con `alFallar`. Sin handler, un error en un `launch` de la raíz acaba en la
// consola como «Exception in thread …».
class Transmisor(
    private val alFallar: (Throwable) -> Unit = {},
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val jugar: suspend (Partido, Long) -> Partido = { partido, ms -> jugarConCronometro(partido, ms, dispatcher) },
) {
    private val trabajo = SupervisorJob()
    private val manejador = CoroutineExceptionHandler { _, error -> alFallar(error) }
    private val alcance = CoroutineScope(dispatcher + trabajo + manejador)

    val activo: Boolean get() = trabajo.isActive

    fun transmitir(partido: Partido, msPorMinuto: Long = 10, alFinal: (Partido) -> Unit = {}): Job =
        alcance.launch { alFinal(jugar(partido, msPorMinuto)) }

    fun pitarElFinal() = trabajo.cancel()
}
