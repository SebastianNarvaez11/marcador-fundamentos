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

// Rellena con espacios hasta `ancho`, a la izquierda o a la derecha. (Kotlin usa
// `"%-8s".format(...)`; `String(format:)` de Swift no funciona bien con `String`
// con `%s`, y con `%@` obliga a importar Foundation.)
func relleno(_ texto: String, ancho: Int, alDerecha: Bool = false) -> String {
    let faltan = max(0, ancho - texto.count)
    let espacios = String(repeating: " ", count: faltan)
    return alDerecha ? espacios + texto : texto + espacios
}

extension FilaDePosicion {
    // Mismo formato que el `"%2d  %-8s %2d %2d %2d %2d %3d %2d %3d %3d"` de Kotlin.
    public func aTexto(puesto: Int) -> String {
        let numeros = [
            (jugados, 2), (ganados, 2), (empatados, 2), (perdidos, 2),
            (golesAFavor, 3), (golesEnContra, 2), (diferencia, 3), (puntos, 3),
        ].map { relleno(String($0.0), ancho: $0.1, alDerecha: true) }
        return relleno(String(puesto), ancho: 2, alDerecha: true) + "  "
            + relleno(equipo.nombre, ancho: 8) + " "
            + numeros.joined(separator: " ")
    }
}

extension Torneo {
    public func tablaComoTexto() -> String {
        var lineas = ["Pos Equipo    PJ  G  E  P  GF GC  DG Pts"]
        for (indice, fila) in tablaDePosiciones().enumerated() {
            lineas.append(fila.aTexto(puesto: indice + 1))
        }
        return lineas.joined(separator: "\n")
    }
}

// Convierte una `Duration` en milisegundos enteros, para escribir «1099 ms».
public func milisegundos(_ duracion: Duration) -> Int {
    let (segundos, fraccion) = duracion.components
    return Int(segundos) * 1_000 + Int(fraccion / 1_000_000_000_000_000)
}
