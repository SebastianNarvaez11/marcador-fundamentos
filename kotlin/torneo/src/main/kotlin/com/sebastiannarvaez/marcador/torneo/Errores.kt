package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope

// Algo que sale mal en mitad de un partido: se va la luz del campo.
class SuspendidoPorApagon(val minuto: Int) : Exception("Partido suspendido en el minuto $minuto: se fue la luz")

// Un partido que se juega hasta el minuto del apagón y ahí falla.
suspend fun jugarConApagon(partido: Partido, msPorMinuto: Long = 10, minutoDelApagon: Int): Partido {
    delay(msPorMinuto * minutoDelApagon)
    throw SuspendidoPorApagon(minutoDelApagon)
}

// En un `coroutineScope` normal, si una hija falla, el alcance cancela a todas
// las hermanas y relanza el error: un partido roto tumba la jornada.
// `supervisorScope` cambia esa regla: cada hija falla POR SU CUENTA y las
// demás siguen. El error de cada una llega al hacer `await()` sobre ella.
//
// El resultado es un Result por partido, en el mismo orden. `intentar` (la que
// escribiste en Alcance, Job y cancelación) convierte el error en valor sin tragarse la CancellationException.
suspend fun jugarJornadaSupervisada(
    partidos: List<Partido>,
    jugar: suspend (Partido) -> Partido = { jugarPartido(it) },
): List<Result<Partido>> = supervisorScope {
    val jugando = partidos.map { partido -> async { jugar(partido) } }
    jugando.map { intentar { it.await() } }
}
