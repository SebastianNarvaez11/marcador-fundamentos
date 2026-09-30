// Función de nivel superior, como en Kotlin: no vive dentro de ningún tipo.
// `public` porque la usa otro módulo (torneo-cli y los tests); sin él, solo se
// vería dentro de la librería Torneo.
//
// Swift NO tiene «función de expresión» con `when`: aquí el cuerpo es un `if`
// como expresión (Swift 5.9+), cuyo valor es el de la rama que se ejecute.
// Las etiquetas de argumento (`golesAFavor:`, `golesEnContra:`) son parte del
// nombre de la función: `puntosPor(golesAFavor:golesEnContra:)`.
public func puntosPor(golesAFavor: Int, golesEnContra: Int) -> Int {
    if golesAFavor > golesEnContra {
        3
    } else if golesAFavor == golesEnContra {
        1
    } else {
        0
    }
}
