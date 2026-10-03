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
// Antes `jugar` tenia por defecto `{ jugarPartido(it) }`. Al compilar el target
// JVM (`compileKotlinJvm`) el compilador 2.4.20 se cae con ese valor por defecto
// (una lambda suspend que llama a otra suspend con parametros por defecto:
// «Backend Internal error ... has no continuation»); los targets Android e iOS
// lo compilan. Una sobrecarga que lo pasa a mano es equivalente para quien llama
// y compila en todos los targets.
suspend fun jugarJornadaSupervisada(partidos: List<Partido>): List<Result<Partido>> =
    jugarJornadaSupervisada(partidos) { jugarPartido(it) }

suspend fun jugarJornadaSupervisada(
    partidos: List<Partido>,
    jugar: suspend (Partido) -> Partido,
): List<Result<Partido>> = supervisorScope {
    val jugando = partidos.map { partido -> async { jugar(partido) } }
    jugando.map { intentar { it.await() } }
}
