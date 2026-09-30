import Torneo

// `let` es constante (como `val` de Kotlin); `var` se puede reasignar.
// El tipo se infiere: `nombreDelTorneo` es String y `totalDeJornadas` es Int.
let nombreDelTorneo = "Copa Barrio"
let totalDeJornadas = 3
print("Torneo: \(nombreDelTorneo)")

for jornada in 1...totalDeJornadas {
    print("Jornada \(jornada) de \(totalDeJornadas)")
}

// Todos los marcadores de 0 a 2 goles y los puntos del local.
for golesLocal in 0...2 {
    for golesVisitante in 0...2 {
        let puntos = puntosPor(golesAFavor: golesLocal, golesEnContra: golesVisitante)
        print("\(golesLocal)-\(golesVisitante) -> \(puntos) puntos para el local")
    }
}
