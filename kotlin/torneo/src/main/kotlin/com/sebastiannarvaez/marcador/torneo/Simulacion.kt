package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.delay

const val MINUTOS_DEL_PARTIDO = 90

// `suspend` NO significa «corre en otro hilo». Significa «esta función puede
// pausarse»: dentro puede llamar a otras `suspend` (como `delay`) y, mientras
// espera, el hilo queda libre para hacer otra cosa. Solo se puede llamar desde
// otra `suspend` o desde una corrutina.
//
// `partido` es el GUION: todo lo que va a pasar. La función lo «juega» minuto a
// minuto y devuelve el partido tal como quedó. `msPorMinuto` es cuánto dura un
// minuto de juego en tiempo real (10 ms hace un partido de menos de un segundo).
suspend fun jugarPartido(partido: Partido, msPorMinuto: Long = 10): Partido {
    var jugado = Partido(partido.local, partido.visitante)
    // Lo que pasa «antes de empezar» (minuto 0) no necesita esperar.
    partido.eventos.filter { it.minuto == 0 }.forEach { jugado = jugado.registrar(it) }
    val ultimoMinuto = maxOf(MINUTOS_DEL_PARTIDO, partido.eventos.maxOfOrNull { it.minuto } ?: 0)
    for (minuto in 1..ultimoMinuto) {
        // `delay` suspende SIN bloquear el hilo. `Thread.sleep` lo bloquearía.
        delay(msPorMinuto)
        partido.eventos.filter { it.minuto == minuto }.forEach { jugado = jugado.registrar(it) }
    }
    return jugado
}
