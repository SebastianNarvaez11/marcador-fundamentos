// Sigue siendo un valor. Desde f53 el marcador ya no se guarda: se DEDUCE de los
// eventos, así que no puede contradecirlos (como en Kotlin f06).
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
}
