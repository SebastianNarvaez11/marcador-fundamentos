// El torneo es lo único mutable del dominio: va acumulando partidos. Es una
// `class` porque tiene IDENTIDAD y estado compartido: si la pantalla de la
// tabla y la de resultados reciben «el torneo», deben ver EL MISMO.
public final class Torneo {
    public let nombre: String
    public let equipos: [Equipo]

    // `private(set) var` sobre un array: de fuera se lee, y lo que se lee es una
    // COPIA (los arrays son de valor). Kotlin necesitaba una lista privada, un
    // getter con `toList()` y explicar el `as MutableList`: aquí el lenguaje ya
    // lo evita.
    public private(set) var partidos: [Partido] = []

    public init(nombre: String, equipos: [Equipo]) {
        self.nombre = nombre
        self.equipos = equipos
    }

    public func registrar(_ partido: Partido) {
        partidos.append(partido)
    }
}
