import Torneo

// `let` es constante (como `val` de Kotlin); `var` se puede reasignar.
// El tipo se infiere: `nombreDelTorneo` es String y `totalDeJornadas` es Int.
let nombreDelTorneo = "Copa Barrio"
let totalDeJornadas = 3
print("Torneo: \(nombreDelTorneo)")

// Argumento con etiqueta: se lee sin adivinar qué es cada número.
for jornada in 1...totalDeJornadas {
    print(encabezadoDeJornada(numero: jornada, de: totalDeJornadas))
}
print(encabezadoDeJornada(numero: 1, de: 3, prefijo: "Fecha"))

// Todos los marcadores de 0 a 2 goles y los puntos del local.
for golesLocal in 0...2 {
    for golesVisitante in 0...2 {
        let puntos = puntosPor(golesAFavor: golesLocal, golesEnContra: golesVisitante)
        print("\(golesLocal)-\(golesVisitante) -> \(puntos) puntos para el local")
    }
}

// `switch` sobre el marcador y `inout`.
for (local, visitante) in [(0, 0), (2, 2), (3, 0), (0, 1), (5, 1), (2, 1), (1, 3)] {
    print("\(local)-\(visitante): \(titular(golesLocal: local, golesVisitante: visitante))")
}
var golesDeAna = 0
sumarGol(a: &golesDeAna)
sumarGol(a: &golesDeAna)
print("Goles de Ana: \(golesDeAna)")

// Opcionales: jugadores con y sin dorsal.
let jugadores = [
    Jugador(nombre: "Ana", dorsal: 9),
    Jugador(nombre: "Luis"),
    Jugador(nombre: "Marta", dorsal: 1),
]
for jugador in jugadores { print(presentar(jugador)) }
print(presentar(nil))

for texto in ["10", " 7 ", "diez", ""] {
    print("«\(texto)» -> \(dorsalDesdeTexto(texto).map(String.init) ?? "no es un dorsal")")
}
