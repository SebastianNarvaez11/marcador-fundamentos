public let minutosDelPartido = 90

// `async` NO significa «corre en otro hilo». Significa «esta función puede
// PAUSARSE»: dentro puede llamar a otras `async` con `await` (como
// `Task.sleep`) y, mientras espera, el hilo queda libre para hacer otra cosa.
// Es exactamente el `suspend` de Kotlin. `await` marca cada punto donde puede
// pausarse (en Kotlin la marca no se ve en el código, solo en el IDE).
//
// `throws` porque `Task.sleep` lanza `CancellationError` si alguien cancela la
// tarea: la cancelación es COOPERATIVA (igual que en Kotlin), y el error que
// avisa es un error normal que sube por la función.
//
// `guion` es todo lo que va a pasar. La función lo «juega» minuto a minuto y
// devuelve el partido tal como quedó. `msPorMinuto` es cuánto dura un minuto de
// juego en tiempo real (10 ms hace un partido de algo más de un segundo).
public func jugarPartido(_ guion: Partido, msPorMinuto: Int = 10) async throws -> Partido {
    var jugado = Partido(local: guion.local, visitante: guion.visitante)
    // Lo que pasa en el minuto 0 (antes de empezar a contar).
    for evento in guion.eventos where evento.minuto == 0 {
        jugado.registrar(evento)
    }
    let ultimoMinuto = max(minutosDelPartido, guion.eventos.map(\.minuto).max() ?? 0)
    for minuto in 1...ultimoMinuto {
        // Aquí se pausa: el hilo queda libre y, cuando pase el tiempo, la
        // función sigue por la línea siguiente (a lo mejor en otro hilo).
        try await Task.sleep(for: .milliseconds(msPorMinuto))
        for evento in guion.eventos where evento.minuto == minuto {
            jugado.registrar(evento)
        }
    }
    return jugado
}

// La cancelación es COOPERATIVA: cancelar solo marca la tarea. `Task.sleep`
// mira la marca y lanza `CancellationError`. Un bucle que solo calcula, sin
// esperar nunca, tiene que mirar él mismo con `Task.checkCancellation()`; si no,
// no se entera (el `ensureActive()` de Kotlin).
public func probabilidadDeVictoria(_ partido: Partido, simulaciones: Int = 5_000_000) async throws -> Double {
    let fuerzaLocal = partido.golesLocal + 1
    let fuerzaVisitante = partido.golesVisitante + 1
    var victoriasLocales = 0
    for vuelta in 1...simulaciones {
        // Cada mil vueltas, ¿sigo vivo? Lanza `CancellationError` si no.
        if vuelta % 1_000 == 0 { try Task.checkCancellation() }
        if Int.random(in: 0..<(fuerzaLocal + fuerzaVisitante)) < fuerzaLocal { victoriasLocales += 1 }
    }
    return Double(victoriasLocales) / Double(simulaciones)
}
