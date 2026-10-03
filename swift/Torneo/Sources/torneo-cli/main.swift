import Foundation
import Torneo

// `torneo-cli cronometro` ejecuta SOLO la demo del cronómetro y termina. Sirve para
// mirar la memoria con `leaks --atExit -- .build/debug/torneo-cli cronometro` (f66).
// La demo vive en una función: un objeto guardado en una variable global de
// `main.swift` sigue siendo alcanzable al salir y `leaks` no lo contaría como fuga.
@MainActor
func demoCronometro() {
    let reloj = Cronometro()
    reloj.avisarCadaMinuto { minuto in print("  minuto \(minuto)") }
    for _ in 1...3 { reloj.avanzar() }
    print("Fin de la demo: el cronómetro sale de su alcance")
}

if CommandLine.arguments.dropFirst().contains("cronometro") {
    demoCronometro()
    exit(0)
}

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

// Struct frente a class.
let ana = jugadores[0]
let ivan = Jugador(nombre: "Iván", dorsal: 7)
let rayo = Equipo(nombre: "Rayo FC", plantilla: jugadores)!
rayo.nombrarCapitan(ana)
print("\(rayo), capitán: \(rayo.capitan?.nombre ?? "sin capitán")")
print("Toros con dorsales repetidos: \(Equipo(nombre: "Toros", plantilla: [Jugador(nombre: "A", dorsal: 7), Jugador(nombre: "B", dorsal: 7)]) == nil ? "nil" : "creado")")

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
print("Tarjetas: \(alFinal.eventos { if case .tarjeta = $0 { true } else { false } }.map(describir))")
print("Primera mitad: \(alFinal.eventos(antesDelMinuto(45)).count) eventos")
print("Goleadores: \(alFinal.goleadores().map(\.nombre))")
print("Minutos de gol de Ana: \(alFinal.minutosDeGolDe(ana))")
repetir(2) { vuelta in print("Vuelta \(vuelta)") }
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
let goleadores = [alFinal, partidoDeToros].goleadores()
for jugador in goleadores {
    print("\(goleadores.puesto(de: jugador) ?? 0). \(jugador.etiqueta()): \(goleadores.puntosDe(jugador))")
}
print("Líder: \(lider(goleadores)?.nombre ?? "nadie"); nombres: \(nombres(de: goleadores))")
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

// ARC: el partido se libera cuando nadie lo mira.
do {
    let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, alLiberarse: { print("PartidoEnVivo liberado (deinit)") })
    let narrador = Narrador(nombre: "Pepe")
    enVivo.narrador = narrador
    narrador.partido = enVivo
    print(narrador.frase())
    print("Al salir del bloque, ARC libera el partido...")
}
print("...y ya no hay nada que narrar.")

// async/await: un partido suspendido. `main.swift` permite `await` al nivel superior.
print("== f61: un partido suspendido ==")
let reloj = ContinuousClock()
let inicioDelPartido = reloj.now
do {
    let final = try await jugarPartido(Ejemplo.rayoContraToros)
    print("Final: \(final.golesLocal)-\(final.golesVisitante) con \(final.eventos.count) eventos")
} catch {
    print("El partido no terminó: \(error)")
}
print("Duró \(milisegundos(inicioDelPartido.duration(to: reloj.now))) ms")

print("== f61: cancelar un partido ==")
let tarea = Task { try await jugarPartido(Ejemplo.rayoContraToros) }
try? await Task.sleep(for: .milliseconds(100))
tarea.cancel()
switch await tarea.result {
case .success: print("Terminó (no debía)")
case let .failure(error): print("Cancelado: \(error is CancellationError ? "CancellationError" : "\(error)")")
}

print("== f62: dos partidos a la vez con async let ==")
let inicioJornada = reloj.now
do {
    let (uno, otro) = try await jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)
    print("Pitido final: \(uno.local.nombre) \(uno.golesLocal)-\(uno.golesVisitante) \(uno.visitante.nombre)")
    print("Pitido final: \(otro.local.nombre) \(otro.golesLocal)-\(otro.golesVisitante) \(otro.visitante.nombre)")
    print("Los dos partidos duraron \(milisegundos(inicioJornada.duration(to: reloj.now))) ms (uno solo dura unos 1100 ms)")
    let inicioEstadisticas = reloj.now
    print(try await estadisticasDe(uno))
    print("Estadísticas en \(milisegundos(inicioEstadisticas.duration(to: reloj.now))) ms (tres consultas de 50 ms a la vez)")
} catch {
    print("Falló la jornada: \(error)")
}

print("== f62: una jornada con TaskGroup ==")
do {
    let jugados = try await jugarJornada([Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas], msPorMinuto: 2)
    print(jugados.map { "\($0.golesLocal)-\($0.golesVisitante)" })
} catch {
    print("Falló la jornada: \(error)")
}

print("== f62: un partido que falla no tumba al otro ==")
let supervisados = try await jugarJornadaSupervisada([Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas]) { partido in
    if partido.local == Ejemplo.lobos {
        try await jugarConApagon(partido, minutoDelApagon: 30)
    } else {
        try await jugarPartido(partido, msPorMinuto: 2)
    }
}
for resultado in supervisados {
    switch resultado {
    case let .success(partido): print("Terminó \(partido.local.nombre)-\(partido.visitante.nombre): \(partido.golesLocal)-\(partido.golesVisitante)")
    case let .failure(error): print("Falló: \((error as? SuspendidoPorApagon).map { "se fue la luz en el minuto \($0.minuto)" } ?? "\(error)")")
    }
}

print("== f63: un AsyncStream frío, la narración del partido ==")
let enVivoRapido = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 2)
// Aún no ha pasado nada: pedir el stream no arranca el partido.
let narracion = enVivoRapido.narracion
print("Stream creado; nada ha corrido todavía")
for await linea in narracion { print("  \(linea)") }

// Cada recorrido es un partido nuevo: el frío se repite entero.
var cuantos = 0
for await _ in enVivoRapido.eventos { cuantos += 1 }
print("Segundo recorrido: otro partido completo (\(cuantos) eventos)")

if let gol = await enVivoRapido.primerGol() {
    print("Primer gol: \(describir(gol)) (el resto del partido se canceló)")
}

print("Marcadores: ", terminator: "")
for await marcador in enVivoRapido.marcadores { print(marcador, terminator: " ") }
print()

print("== f64: dos goles a la vez ya no se pisan ==")
let seguro = MarcadorSeguro()
await withTaskGroup(of: Void.self) { grupo in
    for _ in 0..<1_000 {
        grupo.addTask { await seguro.golLocal() }
        grupo.addTask { await seguro.golVisitante() }
    }
}
print("Marcador tras 1000 goles de cada equipo a la vez: \(await seguro.marcador)")
let enDirecto = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 2)
print("Marcador final del partido en vivo: \(await enDirecto.jugar())")

print("== f65: el estado de pantalla vive en el actor principal ==")
// El código de nivel superior de `main.swift` ya corre en el actor principal en
// Swift 6, así que puede usar una clase `@MainActor` sin `await` en cada línea.
let seguimiento = SeguimientoEnPantalla()
seguimiento.seguir(PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 2))
await seguimiento.esperarAlFinal()
print(seguimiento.titular(seguimiento.marcador))
print("Narración: \(seguimiento.narracion.count) líneas; primera: \(seguimiento.narracion.first ?? "-")")
