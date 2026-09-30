// Extensiones sobre tipos propios: se leen como métodos, pero viven aparte.
// (Kotlin: `fun Jugador.etiqueta()`.) Una extensión puede añadir métodos,
// propiedades CALCULADAS y adopciones de protocolo, pero no propiedades
// guardadas. Y no ve lo `private` del tipo original.
extension Jugador {
    public func etiqueta() -> String { "\(nombre) (\(dorsalOGuion()))" }
}

extension Equipo {
    public var capitanONinguno: String { capitan?.nombre ?? "sin capitán" }
}
