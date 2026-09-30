// Un partido que se está jugando. `guion` es todo lo que va a pasar.
//
// Es una CLASE, y no un struct como `Partido`, porque tiene IDENTIDAD y vida:
// alguien lo empieza, varios lo miran y, cuando ya nadie lo necesita, se libera.
// ARC (Automatic Reference Counting) cuenta cuántas referencias FUERTES apuntan
// a cada objeto y lo libera cuando llegan a cero. Los structs y enums no lo
// necesitan: se copian.
public final class PartidoEnVivo {
    public let guion: Partido
    public let msPorMinuto: Int

    // Se llama cuando termina la emisión de `eventos` (por final o por cancelación).
    private let alTerminarLaEmision: (@Sendable () -> Void)?

    // Se llama en `deinit`, es decir, justo cuando ARC libera el objeto. Sirve
    // para que las pruebas (y la consola) vean que de verdad se liberó.
    private let alLiberarse: (() -> Void)?

    // El marcador del partido, protegido por un actor (f64): quien lo mira y quien
    // lo actualiza pueden ser tareas distintas.
    public let marcadorSeguro = MarcadorSeguro()

    // `strong` es lo normal: esta referencia mantiene vivo al narrador.
    public var narrador: Narrador?

    public init(
        guion: Partido,
        msPorMinuto: Int = 10,
        alTerminarLaEmision: (@Sendable () -> Void)? = nil,
        alLiberarse: (() -> Void)? = nil
    ) {
        self.guion = guion
        self.msPorMinuto = msPorMinuto
        self.alTerminarLaEmision = alTerminarLaEmision
        self.alLiberarse = alLiberarse
    }

    // Un `AsyncStream` FRÍO: crear el stream no arranca nada. Cada vez que alguien
    // lo recorre con `for await`, se ejecuta el cierre de `AsyncStream { }` y
    // empieza OTRO partido desde el minuto 0: dos espectadores = dos partidos
    // independientes. Es el `flow { }` de Kotlin (f20).
    //
    // `continuation.yield` entrega un valor (el `emit` de Kotlin) y
    // `continuation.finish()` cierra el stream. Como el productor es una `Task` que
    // hemos lanzado nosotros, hay que cancelarla cuando el consumidor se va:
    // para eso está `onTermination`, que corre si el stream termina O si quien lo
    // recorre se cancela o hace `break`. Sin esa línea, el partido seguiría
    // jugándose para nadie.
    //
    // OJO: el cierre NO captura `self` (usa copias de `guion` y `ms`). Si capturara
    // `self`, la tarea mantendría vivo el partido mientras dure (ver f66).
    public var eventos: AsyncStream<EventoDePartido> {
        let guion = self.guion
        let ms = self.msPorMinuto
        let alTerminar = self.alTerminarLaEmision
        return AsyncStream { continuacion in
            let tarea = Task {
                for evento in guion.eventos where evento.minuto == 0 {
                    continuacion.yield(evento)
                }
                let ultimoMinuto = max(minutosDelPartido, guion.eventos.map(\.minuto).max() ?? 0)
                for minuto in 1...ultimoMinuto {
                    // El productor no puede lanzar errores por el stream: si nos cancelan, salimos.
                    do { try await Task.sleep(for: .milliseconds(ms)) } catch { break }
                    for evento in guion.eventos where evento.minuto == minuto {
                        continuacion.yield(evento)
                    }
                }
                continuacion.finish()
            }
            continuacion.onTermination = { _ in
                tarea.cancel()
                alTerminar?()
            }
        }
    }

    // Operadores: `AsyncSequence` trae `map`, `filter`, `compactMap`, `first(where:)`…
    // y cada uno devuelve OTRA secuencia asíncrona: nada corre hasta el `for await`.
    public var narracion: some AsyncSequence<String, Never> {
        eventos.map(describir)
    }

    // `compactMap` deja pasar solo los goles y descarta lo demás.
    public var golesEnFrio: some AsyncSequence<EventoDePartido, Never> {
        eventos.compactMap { evento -> EventoDePartido? in
            if case .gol = evento { evento } else { nil }
        }
    }

    // `first(where:)` se queda con el primero y CANCELA el resto (al salir del
    // `for await` interno, `onTermination` cancela la tarea): el partido no se sigue
    // jugando por nada. Es el `first()` de Kotlin.
    public func primerGol() async -> EventoDePartido? {
        await eventos.first { evento in
            if case .gol = evento { true } else { false }
        }
    }

    // Juega el partido entero y va anotando cada evento en el actor. Devuelve el
    // marcador final. Cada `await` sobre el actor es un posible punto de espera.
    @discardableResult
    public func jugar() async -> Marcador {
        for await evento in eventos {
            await marcadorSeguro.registrar(evento, en: guion)
        }
        return await marcadorSeguro.marcador
    }

    // El marcador tras cada gol, empezando en 0-0: el `scan` + `distinctUntilChanged`
    // de Kotlin. Swift no trae `scan` en la biblioteca estándar (está en el paquete
    // swift-async-algorithms), así que se escribe a mano con un stream propio.
    public var marcadores: AsyncStream<Marcador> {
        let guion = self.guion
        let origen = eventos
        return AsyncStream { continuacion in
            let tarea = Task {
                var marcador = Marcador(local: 0, visitante: 0)
                continuacion.yield(marcador)
                for await evento in origen {
                    let nuevo = marcador.despuesDe(evento, en: guion)
                    if nuevo != marcador {
                        marcador = nuevo
                        continuacion.yield(marcador)
                    }
                }
                continuacion.finish()
            }
            continuacion.onTermination = { _ in tarea.cancel() }
        }
    }

    // `deinit` corre UNA vez, cuando el contador de referencias fuertes llega a
    // cero. No se llama a mano. (Kotlin no tiene nada igual: el recolector de
    // basura libera «cuando puede», y no hay un momento fijo.)
    deinit {
        alLiberarse?()
    }
}

// El narrador cuenta el partido. Necesita saber de qué partido habla, y el
// partido sabe quién lo narra: dos objetos que se apuntan entre sí.
//
// Si las DOS referencias fueran fuertes, ninguno de los dos llegaría nunca a
// contador cero (cada uno mantiene vivo al otro) y no se liberarían jamás:
// un ciclo de retención (retain cycle). La solución es que UNA de las dos sea
// `weak`: no suma al contador y se pone a `nil` cuando el objeto se libera.
// La pregunta es «¿quién es el dueño?». El partido es el dueño del narrador,
// no al revés, y por eso el narrador lo mira con `weak`.
public final class Narrador {
    public let nombre: String
    public weak var partido: PartidoEnVivo?

    public init(nombre: String) {
        self.nombre = nombre
    }

    public func frase() -> String {
        // Como `partido` es `weak`, es opcional: hay que abrirlo.
        guard let partido else { return "\(nombre): ya no hay partido" }
        return "\(nombre): \(partido.guion.local.nombre) contra \(partido.guion.visitante.nombre)"
    }
}

// `unowned`: como `weak` pero NO opcional. Se usa cuando el otro objeto siempre
// vive al menos tanto como éste: un estadillo no existe sin su partido. Si te
// equivocas y el partido ya no está, el programa se detiene (a diferencia de
// `weak`, que te da `nil`). Es más cómodo y más peligroso.
public final class Estadillo {
    private unowned let partido: PartidoEnVivo

    public init(partido: PartidoEnVivo) {
        self.partido = partido
    }

    public var golesLocal: Int { partido.guion.golesLocal }
}
