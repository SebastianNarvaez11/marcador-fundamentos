// Función de nivel superior, como en Kotlin: no vive dentro de ningún tipo.
// `public` porque la usan otro módulo (torneo-cli y los tests); sin él, solo se
// vería dentro de la librería Torneo.
//
// Las etiquetas de argumento (`golesAFavor:`, `golesEnContra:`) son parte del
// nombre de la función: `puntosPor(golesAFavor:golesEnContra:)`.
// La regla vive en el reglamento de la liga; esta función se queda como atajo
// y no repite los números.
public func puntosPor(golesAFavor: Int, golesEnContra: Int) -> Int {
    ReglamentoLiga().puntosPor(golesAFavor: golesAFavor, golesEnContra: golesEnContra)
}

// Cada parámetro tiene DOS nombres: el de fuera (la etiqueta, que se escribe al
// llamar) y el de dentro (el que se usa en el cuerpo). `para golesAFavor: Int`
// se llama `puntos(para: 2, contra: 1)` y se lee como una frase.
// (El `switch` con `where` que había aquí antes se mudó al protocolo Reglamento.)
public func puntos(para golesAFavor: Int, contra golesEnContra: Int) -> Int {
    puntosPor(golesAFavor: golesAFavor, golesEnContra: golesEnContra)
}

// Pattern matching con rangos, comodines (`_`) y `where`.
// Devuelve una frase para el marcador de un partido.
public func titular(golesLocal: Int, golesVisitante: Int) -> String {
    switch (golesLocal, golesVisitante) {
    case (0, 0):
        "Sin goles"
    case (let local, let visitante) where local == visitante:
        "Empate a \(local)"
    case (_, 0):
        "El local gana sin recibir goles"
    case (0, _):
        "El visitante gana sin recibir goles"
    case let (local, visitante) where abs(local - visitante) >= 3:
        "Goleada"
    case let (local, visitante) where local > visitante:
        "Gana el local"
    default:
        "Gana el visitante"
    }
}

// Parámetro con valor por defecto, como en Kotlin (`prefijo: String = "Jornada"`).
// `de total` hace que la llamada lea «numero: 1, de: 3».
public func encabezadoDeJornada(numero: Int, de total: Int, prefijo: String = "Jornada") -> String {
    "\(prefijo) \(numero) de \(total)"
}

// `inout`: el parámetro se pasa POR REFERENCIA temporal. La función lo puede
// cambiar y el cambio sale. Al llamar se escribe `&` delante para que se vea.
// Kotlin no tiene nada equivalente: los parámetros son siempre `val`.
public func sumarGol(a marcador: inout Int) {
    marcador += 1
}
