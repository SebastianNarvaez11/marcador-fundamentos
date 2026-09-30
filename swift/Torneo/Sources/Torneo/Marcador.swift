// El marcador en un instante. Inmutable: cada gol produce uno nuevo.
// (El gemelo de `data class Marcador` de Kotlin; en f64 aparece el actor que lo
// protege cuando lo tocan varias tareas a la vez, `MarcadorSeguro`.)
public struct Marcador: Equatable, Sendable, CustomStringConvertible {
    public let local: Int
    public let visitante: Int

    public init(local: Int, visitante: Int) {
        self.local = local
        self.visitante = visitante
    }

    public var goles: Int { local + visitante }

    public var description: String { "\(local)-\(visitante)" }

    // El marcador tras un evento: solo un gol lo cambia.
    public func despuesDe(_ evento: EventoDePartido, en partido: Partido) -> Marcador {
        guard case let .gol(_, _, equipo) = evento else { return self }
        return equipo == partido.local
            ? Marcador(local: local + 1, visitante: visitante)
            : Marcador(local: local, visitante: visitante + 1)
    }
}

extension Partido {
    public func marcador() -> Marcador { Marcador(local: golesLocal, visitante: golesVisitante) }
}
