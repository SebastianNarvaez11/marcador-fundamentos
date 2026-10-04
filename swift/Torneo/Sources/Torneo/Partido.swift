// Sigue siendo un valor. El marcador no se guarda: se DEDUCE de los eventos,
// así que no puede contradecirlos (como en la versión Kotlin).
public struct Partido: Equatable {
    public let local: Equipo
    public let visitante: Equipo
    public private(set) var eventos: [EventoDePartido]

    public init(local: Equipo, visitante: Equipo, eventos: [EventoDePartido] = []) {
        self.local = local
        self.visitante = visitante
        self.eventos = eventos
    }

    public func golesDe(_ equipo: Equipo) -> Int {
        eventos.count { evento in
            // `if case` es un `switch` de un solo caso: mira si el evento es un
            // gol y, si lo es, liga el equipo.
            if case let .gol(_, _, equipoDelGol) = evento { equipoDelGol == equipo } else { false }
        }
    }

    public var golesLocal: Int { golesDe(local) }
    public var golesVisitante: Int { golesDe(visitante) }

    // Convención de nombres de Swift: el que CAMBIA `self` es un verbo
    // (`registrar`) y el que devuelve una copia usa un participio o gerundio
    // (`registrando`). Kotlin solo tenía el segundo, con el nombre `registrar`.
    public mutating func registrar(_ evento: EventoDePartido) {
        eventos.append(evento)
    }

    public func registrando(_ evento: EventoDePartido) -> Partido {
        var copia = self
        copia.registrar(evento)
        return copia
    }

    public func resultado() -> Resultado {
        if golesLocal > golesVisitante {
            .victoria(ganador: local, perdedor: visitante)
        } else if golesLocal < golesVisitante {
            .victoria(ganador: visitante, perdedor: local)
        } else {
            .empate
        }
    }

    // Función de orden superior: recibe una función como parámetro.
    // `(EventoDePartido) -> Bool` es un tipo función: toma un evento y devuelve
    // si cumple. Al llamarla, la closure va fuera de los paréntesis (trailing
    // closure): `partido.eventos { … }` y, sin nombre, se usa `$0`.
    // (Como en Kotlin, una propiedad `eventos` y una función `eventos(_:)` se llevan bien.)
    public func eventos(_ filtro: (EventoDePartido) -> Bool) -> [EventoDePartido] {
        eventos.filter(filtro)
    }

    // `compactMap` transforma y descarta los nil: cada gol se convierte en su
    // jugador y lo demás (tarjetas, cambios) desaparece. Es el
    // `filterIsInstance<Gol>().map(Gol::jugador)` de Kotlin en un solo paso.
    public func goleadores() -> [Jugador] {
        eventos.compactMap { evento in
            if case let .gol(_, jugador, _) = evento { jugador } else { nil }
        }
    }

    // `return` dentro de una closure sale SOLO de la closure (el `return@forEach`
    // de Kotlin): aquí funciona como un `continue`. No hace falta etiqueta.
    public func minutosDeGolDe(_ jugador: Jugador) -> [Int] {
        var minutos: [Int] = []
        eventos.forEach { evento in
            guard case let .gol(minuto, autor, _) = evento else { return }
            if autor == jugador { minutos.append(minuto) }
        }
        return minutos
    }
}
