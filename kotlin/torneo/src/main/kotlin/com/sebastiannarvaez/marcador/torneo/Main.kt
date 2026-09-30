package com.sebastiannarvaez.marcador.torneo

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

// Punto de entrada de consola: ./gradlew :torneo:run
// (la demo de F1 sigue disponible: ./gradlew :torneo:run --args=kotlin)
//
// `runBlocking` es el puente entre el mundo normal (`main` no es suspend) y el
// de las corrutinas: bloquea el hilo actual hasta que el bloque termina.
fun main(args: Array<String>) {
    if ("kotlin" in args) return demoKotlin()
    runBlocking {
        println("== f15: corrutina frente a hilo ==")
        demoCorrutinasFrenteAHilos()
        println("== f15: un partido suspendido ==")
        val final = jugarPartido(Ejemplo.rayoContraToros)
        println("Final: ${final.golesLocal}-${final.golesVisitante} con ${final.eventos.size} eventos")

        println("== f16: dos partidos a la vez con launch ==")
        val tiempo = measureTimeMillis {
            // Cada `launch` arranca una corrutina hija y sigue sin esperarla.
            // `coroutineScope` espera a que terminen las dos antes de continuar.
            coroutineScope {
                for (guion in listOf(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)) {
                    launch {
                        val final = jugarPartido(guion)
                        println("Pitido final: ${final.local.nombre} ${final.golesLocal}-${final.golesVisitante} ${final.visitante.nombre}")
                    }
                }
            }
        }
        println("Los dos partidos duraron $tiempo ms (uno solo dura algo más de 1 s)")

        println("== f16: estadísticas con async ==")
        val (uno, otro) = jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)
        val tiempoEstadisticas = measureTimeMillis {
            println(estadisticasDe(uno))
            println(estadisticasDe(otro))
        }
        println("Dos estadísticas en $tiempoEstadisticas ms (cada una son 3 consultas de 50 ms en paralelo)")
    }
}

// 10.000 esperas de 100 ms. Con corrutinas tardan ~100 ms en total, porque
// todas esperan a la vez sobre un puñado de hilos. Con un hilo cada una serían
// 10.000 hilos del sistema operativo.
suspend fun demoCorrutinasFrenteAHilos() {
    val ms = measureTimeMillis {
        // `coroutineScope` no termina hasta que terminan todas sus corrutinas hijas.
        coroutineScope { repeat(10_000) { launch { delay(100) } } }
    }
    println("10.000 corrutinas esperando 100 ms terminan en $ms ms")
}
