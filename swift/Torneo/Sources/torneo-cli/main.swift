import Foundation
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

// Cuatro textos: dos son números (uno con espacios) y dos no.
for texto in ["10", " 7 ", "diez", ""] {
    // `.map { String($0) }`: si hay dorsal, lo convierte en texto; si no, nil.
    // `?? "no es un dorsal"` da el texto de reserva.
    let dorsal = dorsalDesdeTexto(texto).map { String($0) } ?? "no es un dorsal"
    print("«\(texto)» -> \(dorsal)")
}

// Struct frente a class.
let ana = jugadores[0]
let ivan = Jugador(nombre: "Iván", dorsal: 7)
// `Equipo(...)` devuelve un opcional (es un `init?`); el `!` lo abre a la fuerza.
let rayo = Equipo(nombre: "Rayo FC", plantilla: jugadores)!
rayo.nombrarCapitan(ana)                        // sin usar el Bool que devuelve
let nombreDelCapitan = rayo.capitan?.nombre ?? "sin capitán"
print("\(rayo), capitán: \(nombreDelCapitan)")

// Dos jugadores con el mismo dorsal: el `init?` devuelve nil.
let conRepetidos = Equipo(nombre: "Toros", plantilla: [Jugador(nombre: "A", dorsal: 7), Jugador(nombre: "B", dorsal: 7)])
if conRepetidos == nil {
    print("Toros con dorsales repetidos: nil")
} else {
    print("Toros con dorsales repetidos: creado")
}

let toros = Equipo(nombre: "Toros", plantilla: [ivan])!
let inicio = Partido(local: rayo, visitante: toros)
let alFinal = inicio
    .registrando(.gol(minuto: 12, jugador: ana, equipo: rayo))
    .registrando(.tarjeta(minuto: 30, jugador: ivan, color: .amarilla))
    .registrando(.gol(minuto: 55, jugador: ivan, equipo: toros))
    .registrando(.gol(minuto: 80, jugador: ana, equipo: rayo))
    .registrando(.cambio(minuto: 85, sale: ana, entra: jugadores[1]))
print("Antes: \(inicio.eventos.count) eventos, después: \(alFinal.eventos.count)")
for evento in alFinal.eventos { print(describir(evento)) }
print("Marcador: \(alFinal.golesLocal)-\(alFinal.golesVisitante) -> \(alFinal.resultado().titular)")
print("Colores de tarjeta: \(ColorDeTarjeta.allCases.map(\.rawValue))")

// Closures: filtrar eventos y sacar goleadores.
// La closure final decide qué eventos pasan; `.map(describir)` pasa cada uno a texto.
let lasTarjetas = alFinal.eventos { if case .tarjeta = $0 { true } else { false } }
print("Tarjetas: \(lasTarjetas.map(describir))")
// `antesDelMinuto(45)` devuelve la closure que hace de filtro.
let primeraMitad = alFinal.eventos(antesDelMinuto(45))
print("Primera mitad: \(primeraMitad.count) eventos")
// `goleadores()` da un jugador por gol; `\.nombre` se queda con el nombre de cada uno.
let quienesMarcaron = alFinal.goleadores()
print("Goleadores: \(quienesMarcaron.map(\.nombre))")
let minutosDeAna = alFinal.minutosDeGolDe(ana)
print("Minutos de gol de Ana: \(minutosDeAna)")
// `repetir` llama a la closure con 1 y con 2.
repetir(2) { vuelta in print("Vuelta \(vuelta)") }
// `contador()` devuelve una closure que recuerda la cuenta entre llamada y llamada.
let siguiente = contador()
print("Contador: \(siguiente()), \(siguiente()), \(siguiente())")
alResolver(alFinal) { ganador in
    print("Ganó \(ganador.nombre)")
} enEmpate: {
    print("Tablas")
}

// Protocolos: el reglamento se puede cambiar por otro.
print(ReglamentoLiga())
let reglamentoAntiguo = ReglamentoPersonalizado(puntosPorVictoria: 2, puntosPorEmpate: 1)
let torneoAntiguo = Torneo(nombre: "Copa Antigua", equipos: [rayo, toros], reglamento: reglamentoAntiguo)
print("Con reglas antiguas, una victoria vale \(torneoAntiguo.reglamento.puntosPor(golesAFavor: 2, golesEnContra: 0))")
print("Etiqueta: \(ana.etiqueta()); capitán de Toros: \(toros.capitanONinguno)")

// Hashable: un jugador puede ser clave de un diccionario o entrar en un Set.
let golesPorJugador = Dictionary(grouping: alFinal.goleadores(), by: { $0 }).mapValues(\.count)
print("Goles de Ana: \(golesPorJugador[ana] ?? 0)")

// Genéricos: el mismo Ranking sirve para jugadores y para cualquier otra cosa.
let lobos = Equipo(nombre: "Lobos", plantilla: [Jugador(nombre: "Pedro", dorsal: 4)])!
let partidoDeToros = Partido(local: toros, visitante: lobos).registrando(.gol(minuto: 20, jugador: ivan, equipo: toros))
// Los goleadores de dos partidos, ya ordenados: Ana e Iván con 2 goles cada uno.
let tablaDeGoleadores = [alFinal, partidoDeToros].goleadores()
// Un Ranking se recorre con `for` porque es una Sequence.
for jugador in tablaDeGoleadores {
    print("\(tablaDeGoleadores.puesto(de: jugador) ?? 0). \(jugador.etiqueta()): \(tablaDeGoleadores.puntosDe(jugador))")
}
print("Líder: \(lider(tablaDeGoleadores)?.nombre ?? "nadie"); nombres: \(nombres(de: tablaDeGoleadores))")
// El mismo Ranking con textos: se puntúa por la longitud (`\.count` es un key path).
let porLongitud = Ranking(elementos: ["Rayo FC", "Toros", "Lobos"], puntos: \.count)
print("Ranking de textos: \(porLongitud.ordenados)")

// Colecciones: la tabla de posiciones de una jornada completa.
let copa = Torneo(nombre: nombreDelTorneo, equipos: [rayo, toros, lobos])
do {
    try copa.registrar(alFinal)
    try copa.registrar(partidoDeToros)
    try copa.registrar(Partido(local: lobos, visitante: rayo).registrando(.gol(minuto: 40, jugador: ana, equipo: rayo)))
} catch {
    print("No se pudo registrar: \(error.localizedDescription)")
}
print(copa.tablaComoTexto())
let foto = copa.instantanea()
print("Tabla calculada \(foto.calculosDeLaTabla) veces antes de leerla")
print("Líder: \(foto.tabla.first?.equipo.nombre ?? "nadie")")
print("Líder otra vez: \(foto.tabla.first?.equipo.nombre ?? "nadie") (calculada \(foto.calculosDeLaTabla) vez)")

let torneo = Torneo(nombre: nombreDelTorneo, equipos: [rayo, toros, lobos])
let mismoTorneo = torneo        // MISMA referencia: class
do {
    try mismoTorneo.registrar(alFinal)
} catch {
    print("No se registró: \(error.localizedDescription)")
}
print("Partidos vistos desde `torneo`: \(torneo.partidos.count)")

// Errores: un registro inválido no tumba el programa.
let intruso = Jugador(nombre: "Intruso", dorsal: 99)
do {
    try torneo.registrar(local: lobos, visitante: toros, eventos: [.gol(minuto: 10, jugador: intruso, equipo: lobos)])
    print("Registrado")
} catch {
    // Con `throws(ErrorDeTorneo)`, `error` es un ErrorDeTorneo, no un `any Error`.
    print("No se registró: \(error.localizedDescription)")
}

// `Result`: el error como valor, como el `Result` de Kotlin.
let partidoValido = Partido(local: lobos, visitante: toros)
    .registrando(.gol(minuto: 10, jugador: lobos.plantilla[0], equipo: lobos))
switch torneo.intentarRegistrar(partidoValido) {
case let .success(partido): print("Marcador: \(partido.golesLocal)-\(partido.golesVisitante)")
case let .failure(error): print("Error: \(error)")
}
print("Eventos (o -1 si falla): \((try? torneo.registrar(local: lobos, visitante: lobos).eventos.count) ?? -1)")

// `throws` de verdad: `try`, `try?` y `do/catch` con patrones.
for texto in ["10", "diez", "150"] {
    do {
        print("«\(texto)» -> dorsal \(try dorsalValido(texto))")
    } catch ErrorDeDorsal.noEsUnNumero(let escrito) {
        print("«\(escrito)» no es un número")
    } catch {
        print("«\(texto)» -> \(error)")
    }
}
print("Con try?: \(String(describing: try? dorsalValido("diez")))")

// Codable: el torneo entero, a JSON y de vuelta.
do {
    let datos = try torneo.aJson()
    let ficheroGuardado = URL(fileURLWithPath: "torneo-swift.json")
    try datos.write(to: ficheroGuardado)
    let cargado = try torneoDesdeJson(Data(contentsOf: ficheroGuardado))
    print("Cargado: \(cargado.nombre), \(cargado.partidos.count) partidos, líder \(cargado.tablaDePosiciones().first?.equipo.nombre ?? "nadie")")
    try? FileManager.default.removeItem(at: ficheroGuardado)
} catch {
    print("No se pudo guardar o cargar: \(error)")
}
