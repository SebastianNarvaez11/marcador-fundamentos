// Una closure guardada en una constante, con su tipo función explícito.
// Con un solo parámetro, si no se le pone nombre se llama `$0` (el `it` de Kotlin).
// El cuerpo es una sola expresión: no hace falta `return`.
public let esGol: (EventoDePartido) -> Bool = {
    if case .gol = $0 { true } else { false }
}

// Con nombres de parámetro y varios parámetros: `evento, limite in …`.
public let esDelMinutoOAntes: (EventoDePartido, Int) -> Bool = { evento, limite in
    evento.minuto <= limite
}

// Función de orden superior propia: ejecuta `accion` tantas veces como se pida.
// Una closure recibida como parámetro es NO ESCAPING por defecto: solo puede
// usarse mientras la función corre, y no puede guardarse para después.
public func repetir(_ veces: Int, accion: (Int) -> Void) {
    for vuelta in 1...veces { accion(vuelta) }
}

// Una función puede DEVOLVER una closure. Al salir de la función, la closure
// sigue viva («escapa»), y por eso el tipo devuelto no lleva `@escaping`
// (en un valor de retorno es implícito). Recuerda `minuto`: lo CAPTURA.
public func antesDelMinuto(_ minuto: Int) -> (EventoDePartido) -> Bool {
    { $0.minuto < minuto }
}

// Capturas: la closure y la variable `cuenta` viven juntas. Cada llamada a
// `contador()` crea una `cuenta` propia; la closure devuelta la va sumando.
public func contador() -> () -> Int {
    var cuenta = 0
    return {
        cuenta += 1
        return cuenta
    }
}

// Varias closures finales: la primera va sin etiqueta y las siguientes CON su
// etiqueta. Se llama así:
//
//     alResolver(partido) { ganador in print(ganador.nombre) } enEmpate: { print("tablas") }
//
// Kotlin solo permite UNA lambda final; en Swift pueden ser varias.
public func alResolver(
    _ partido: Partido,
    enVictoria: (Equipo) -> Void,
    enEmpate: () -> Void
) {
    switch partido.resultado() {
    case let .victoria(ganador, _): enVictoria(ganador)
    case .empate: enEmpate()
    }
}

// `@escaping`: esta closure se GUARDA y se llamará más tarde, cuando la función
// ya haya vuelto. Swift obliga a decirlo: sin la palabra, no compila.
public final class Avisador {
    private var oyentes: [(EventoDePartido) -> Void] = []

    public init() {}

    public func alEvento(_ oyente: @escaping (EventoDePartido) -> Void) {
        oyentes.append(oyente)
    }

    public func avisar(_ evento: EventoDePartido) {
        for oyente in oyentes { oyente(evento) }
    }
}
