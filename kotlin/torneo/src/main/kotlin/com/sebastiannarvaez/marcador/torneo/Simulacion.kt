package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.fold

const val MINUTOS_DEL_PARTIDO = 90

// `suspend` NO significa «corre en otro hilo». Significa «esta función puede
// pausarse»: dentro puede llamar a otras `suspend` (como `delay`) y, mientras
// espera, el hilo queda libre para hacer otra cosa. Solo se puede llamar desde
// otra `suspend` o desde una corrutina.
//
// `partido` es el GUION: todo lo que va a pasar. La función lo «juega» minuto a
// minuto y devuelve el partido tal como quedó. `msPorMinuto` es cuánto dura un
// minuto de juego en tiempo real (10 ms hace un partido de un segundo).
//
// Desde f20 el minuto a minuto vive en `PartidoEnVivo.eventos()` (un Flow) y
// aquí solo se acumula lo que emite: `fold` parte de un partido vacío y va
// registrando cada evento.
suspend fun jugarPartido(partido: Partido, msPorMinuto: Long = 10): Partido =
    PartidoEnVivo(partido, msPorMinuto).eventos()
        .fold(Partido(partido.local, partido.visitante)) { jugado, evento -> jugado.registrar(evento) }

// `async` lanza una corrutina que DEVUELVE un valor: un `Deferred<T>` (una
// promesa). `await()` espera ese valor. `launch` (en Main.kt) es «lanza y
// olvida»: devuelve un `Job` sin resultado. Regla: si necesitas el resultado,
// `async`; si solo quieres que ocurra, `launch`.
//
// Dos partidos a la vez: los dos `async` arrancan antes del primer `await`, así
// que juegan en paralelo. El total dura lo que el más largo, no la suma.
suspend fun jugarJornada(primero: Partido, segundo: Partido, msPorMinuto: Long = 10): Pair<Partido, Partido> =
    coroutineScope {
        val a = async { jugarPartido(primero, msPorMinuto) }
        val b = async { jugarPartido(segundo, msPorMinuto) }
        a.await() to b.await()
    }

data class Estadisticas(val goles: Int, val tarjetas: Int, val cambios: Int)

// Tres consultas independientes (aquí simuladas con una espera de `msPorConsulta`,
// como si fueran a una base de datos). Con `async` corren a la vez y el total es
// ~1 consulta; una detrás de otra serían ~3.
suspend fun estadisticasDe(partido: Partido, msPorConsulta: Long = 50): Estadisticas = coroutineScope {
    val goles = async { delay(msPorConsulta); partido.eventos.count { it is Gol } }
    val tarjetas = async { delay(msPorConsulta); partido.eventos.count { it is Tarjeta } }
    val cambios = async { delay(msPorConsulta); partido.eventos.count { it is Cambio } }
    Estadisticas(goles.await(), tarjetas.await(), cambios.await())
}
