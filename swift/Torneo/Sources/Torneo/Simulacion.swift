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

// ---- Varias cosas a la vez ----

// `async let` lanza una tarea HIJA que corre a la vez que el resto de la función.
// Las dos líneas `async let` arrancan antes del primer `await`, así que los dos
// partidos se juegan a la vez; el `await` de la última línea espera a los dos.
// El total dura lo que el más largo, no la suma. (El `async { }` + `await()` de
// Kotlin.)
//
// «A la vez» aquí es CONCURRENCIA, no necesariamente paralelismo: en Swift las
// tareas se reparten entre los hilos del sistema, así que además pueden correr
// en paralelo de verdad.
public func jugarJornada(
    _ primero: Partido,
    _ segundo: Partido,
    msPorMinuto: Int = 10
) async throws -> (Partido, Partido) {
    async let a = jugarPartido(primero, msPorMinuto: msPorMinuto)
    async let b = jugarPartido(segundo, msPorMinuto: msPorMinuto)
    return try await (a, b)
}

public struct Estadisticas: Equatable, Sendable {
    public let goles: Int
    public let tarjetas: Int
    public let cambios: Int
}

// Tres consultas independientes (aquí simuladas con una espera de `msPorConsulta`,
// como si fueran a una base de datos). Con `async let` corren a la vez y el total
// es ~1 consulta; una detrás de otra serían ~3.
public func estadisticasDe(_ partido: Partido, msPorConsulta: Int = 50) async throws -> Estadisticas {
    async let goles: Int = {
        try await Task.sleep(for: .milliseconds(msPorConsulta))
        return partido.eventos.count { if case .gol = $0 { true } else { false } }
    }()
    async let tarjetas: Int = {
        try await Task.sleep(for: .milliseconds(msPorConsulta))
        return partido.eventos.count { if case .tarjeta = $0 { true } else { false } }
    }()
    async let cambios: Int = {
        try await Task.sleep(for: .milliseconds(msPorConsulta))
        return partido.eventos.count { if case .cambio = $0 { true } else { false } }
    }()
    return try await Estadisticas(goles: goles, tarjetas: tarjetas, cambios: cambios)
}

// `async let` sirve cuando sabes CUÁNTAS tareas hay al escribir el código. Con una
// lista de tamaño variable se usa un TaskGroup: `addTask` añade una tarea hija por
// partido y el `for try await` recoge los resultados A MEDIDA QUE TERMINAN (no
// en el orden en que se lanzaron). Por eso cada tarea devuelve también su
// posición, y al final se coloca cada partido donde estaba.
public func jugarJornada(_ partidos: [Partido], msPorMinuto: Int = 10) async throws -> [Partido] {
    try await withThrowingTaskGroup(of: (Int, Partido).self) { grupo in
        for (posicion, guion) in partidos.enumerated() {
            grupo.addTask {
                (posicion, try await jugarPartido(guion, msPorMinuto: msPorMinuto))
            }
        }
        var resultado = [Partido?](repeating: nil, count: partidos.count)
        for try await (posicion, jugado) in grupo {
            resultado[posicion] = jugado
        }
        return resultado.compactMap { $0 }
    }
}

// ---- Errores en tareas hijas (como en Kotlin) ----

// Algo que sale mal en mitad de un partido: se va la luz del campo.
public struct SuspendidoPorApagon: Error, Equatable {
    public let minuto: Int
}

// Un partido que se juega hasta el minuto del apagón y ahí falla.
public func jugarConApagon(_ partido: Partido, msPorMinuto: Int = 10, minutoDelApagon: Int) async throws -> Partido {
    try await Task.sleep(for: .milliseconds(msPorMinuto * minutoDelApagon))
    throw SuspendidoPorApagon(minuto: minutoDelApagon)
}

// En un `withThrowingTaskGroup` normal, si UNA tarea hija lanza un error, el grupo
// cancela a las demás hermanas y relanza el error: un partido roto tumba la
// jornada (el `coroutineScope` de Kotlin). Para que cada hija falle POR SU
// CUENTA, la hija atrapa su error y lo devuelve como VALOR (un `Result`): al
// grupo solo le llegan resultados, nunca errores (el `supervisorScope` de Kotlin).
//
// La cancelación NO se traga (igual que `intentar` en Kotlin): un `catch`
// genérico atraparía también la `CancellationError`, y una tarea cancelada
// seguiría como si nada. Se atrapa aparte y se vuelve a lanzar; solo los demás
// errores se guardan como resultado.
public func jugarJornadaSupervisada(
    _ partidos: [Partido],
    jugar: @escaping @Sendable (Partido) async throws -> Partido = { try await jugarPartido($0) }
) async throws -> [Result<Partido, any Error>] {
    try await withThrowingTaskGroup(of: (Int, Result<Partido, any Error>).self) { grupo in
        for (posicion, guion) in partidos.enumerated() {
            grupo.addTask {
                do {
                    return (posicion, .success(try await jugar(guion)))
                } catch let cancelacion as CancellationError {
                    throw cancelacion
                } catch {
                    return (posicion, .failure(error))
                }
            }
        }
        var resultado = [Result<Partido, any Error>?](repeating: nil, count: partidos.count)
        for try await (posicion, valor) in grupo {
            resultado[posicion] = valor
        }
        return resultado.compactMap { $0 }
    }
}
