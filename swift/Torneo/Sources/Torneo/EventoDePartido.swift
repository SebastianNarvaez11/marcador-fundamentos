// `enum` con VALORES BRUTOS (raw values): cada caso vale un texto fijo. Aquí
// coinciden con lo que escribe la versión Kotlin en el JSON («AMARILLA»), y eso
// hará que en f59 se lea sin código extra. `CaseIterable` da `allCases`.
public enum ColorDeTarjeta: String, CaseIterable, Codable {
    case amarilla = "AMARILLA"
    case roja = "ROJA"

    // Un enum puede tener propiedades calculadas y métodos.
    public var etiqueta: String {
        switch self {
        case .amarilla: "Amarilla"
        case .roja: "Roja"
        }
    }
}

// `enum` con VALORES ASOCIADOS: cada caso lleva sus propios datos, distintos de
// los de los demás. Es el gemelo del `sealed interface` de Kotlin, con
// `Gol`, `Tarjeta` y `Cambio` como casos en vez de clases aparte.
//
// El compilador sabe cuáles son TODOS los casos: un `switch` sin `default` que
// olvide uno no compila (en Kotlin, el `when` sin `else`).
public enum EventoDePartido: Equatable {
    case gol(minuto: Int, jugador: Jugador, equipo: Equipo)
    case tarjeta(minuto: Int, jugador: Jugador, color: ColorDeTarjeta)
    case cambio(minuto: Int, sale: Jugador, entra: Jugador)

    // En Kotlin, `val minuto` estaba en la interfaz y cada clase lo sobrescribía.
    // Aquí se saca con un `switch` que liga solo lo que interesa: `_` ignora el resto.
    public var minuto: Int {
        switch self {
        case .gol(let minuto, _, _), .tarjeta(let minuto, _, _), .cambio(let minuto, _, _):
            minuto
        }
    }
}

// `switch` sobre un enum: sin `default`. Si mañana se añade un caso, esta función
// deja de compilar y avisa de dónde falta.
public func describir(_ evento: EventoDePartido) -> String {
    switch evento {
    case let .gol(minuto, jugador, equipo):
        "\(minuto)' Gol de \(jugador.nombre) (\(equipo.nombre))"
    case let .tarjeta(minuto, jugador, color):
        "\(minuto)' Tarjeta \(color.etiqueta.lowercased()) para \(jugador.nombre)"
    case let .cambio(minuto, sale, entra):
        "\(minuto)' Cambio: sale \(sale.nombre), entra \(entra.nombre)"
    }
}

// Un partido acaba en victoria (de alguien) o en empate. `.victoria` lleva
// datos; `.empate` no necesita ninguno (el `data object` de Kotlin).
public enum Resultado: Equatable {
    case victoria(ganador: Equipo, perdedor: Equipo)
    case empate

    public var titular: String {
        switch self {
        case let .victoria(ganador, _): "Gana \(ganador.nombre)"
        case .empate: "Empate"
        }
    }
}
