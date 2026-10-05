import Foundation
import Torneo

// `torneo-cli cronometro` ejecuta SOLO la demo del cronómetro y termina. Sirve para
// mirar la memoria con `leaks --atExit -- .build/debug/torneo-cli cronometro`.
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

// Un título para separar esta parte de la salida anterior.
print("== un partido suspendido ==")
// Un reloj para medir cuánto dura el partido.
let reloj = ContinuousClock()
// El instante en que empieza.
let inicioDelPartido = reloj.now
// `main.swift` permite `await` al nivel superior, sin una función alrededor.
do {
    // Juega el partido entero: `try` por si falla, `await` porque se pausa.
    let jugado = try await jugarPartido(Ejemplo.rayoContraToros)
    print("Final: \(jugado.golesLocal)-\(jugado.golesVisitante) con \(jugado.eventos.count) eventos")
} catch {
    // Solo llega aquí si `jugarPartido` lanza un error.
    print("El partido no terminó: \(error)")
}
// Cuánto pasó desde el inicio, en milisegundos.
print("Duró \(milisegundos(inicioDelPartido.duration(to: reloj.now))) ms")

print("== cancelar un partido ==")
// Lanza el partido en una `Task` y sigue sin esperarlo.
let tarea = Task { try await jugarPartido(Ejemplo.rayoContraToros) }
// Espera 100 ms (el partido sigue jugándose mientras tanto)…
try? await Task.sleep(for: .milliseconds(100))
// …y entonces lo cancela: solo lo marca como cancelado.
tarea.cancel()
// `result` espera a que la tarea termine y la entrega como `Result`.
switch await tarea.result {
case .success:
    print("Terminó (no debía)")
case let .failure(error):
    // `is` contesta si el error es de ese tipo.
    let nombre = error is CancellationError ? "CancellationError" : "\(error)"
    print("Cancelado: \(nombre)")
}

// Un título para separar esta parte de la salida anterior.
print("== dos partidos a la vez con async let ==")
// El instante de salida, con el `reloj` que ya tienes.
let inicioJornada = reloj.now
do {
    // Juega los dos partidos a la vez y recoge los dos resultados en una tupla.
    let (uno, otro) = try await jugarJornada(Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas)
    // Un «pitido final» por partido: equipos y goles.
    print("Pitido final: \(uno.local.nombre) \(uno.golesLocal)-\(uno.golesVisitante) \(uno.visitante.nombre)")
    print("Pitido final: \(otro.local.nombre) \(otro.golesLocal)-\(otro.golesVisitante) \(otro.visitante.nombre)")
    // Lo que tardó todo.
    print("Los dos partidos duraron \(milisegundos(inicioJornada.duration(to: reloj.now))) ms")
} catch {
    // Solo llega aquí si un partido lanza un error.
    print("Falló la jornada: \(error)")
}

// Un título para separar esta parte.
print("== tres consultas a la vez ==")
do {
    // El instante de salida.
    let inicioEstadisticas = reloj.now
    // Pide las tres consultas a la vez e imprime las estadísticas.
    print(try await estadisticasDe(Ejemplo.rayoContraToros))
    // Tiene que salir cerca de 50 ms, no de 150.
    print("Estadísticas en \(milisegundos(inicioEstadisticas.duration(to: reloj.now))) ms (tres consultas de 50 ms a la vez)")
} catch {
    print("Falló la consulta: \(error)")
}

// Un título para separar esta parte.
print("== una jornada con TaskGroup ==")
do {
    // Una lista de dos partidos, jugados muy rápido (2 ms por minuto).
    let jugados = try await jugarJornada([Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas], msPorMinuto: 2)
    // Un texto «goles local-goles visitante» por cada partido, en su orden original.
    print(jugados.map { "\($0.golesLocal)-\($0.golesVisitante)" })
} catch {
    print("Falló la jornada: \(error)")
}

// Convierte un error en una frase para la consola.
func textoDelFallo(_ error: any Error) -> String {
    // Si es el apagón, dice en qué minuto ocurrió.
    if let apagon = error as? SuspendidoPorApagon {
        return "se fue la luz en el minuto \(apagon.minuto)"
    }
    // Cualquier otro error se imprime tal cual.
    return "\(error)"
}

// Un título para separar esta parte.
print("== un partido que falla no tumba al otro ==")
// La jornada supervisada; el closure final dice cómo se juega cada partido.
let supervisados = try await jugarJornadaSupervisada([Ejemplo.rayoContraToros, Ejemplo.lobosContraAguilas]) { partido in
    if partido.local == Ejemplo.lobos {
        // Los Lobos se quedan sin luz en el minuto 30…
        try await jugarConApagon(partido, minutoDelApagon: 30)
    } else {
        // …y el otro partido se juega entero.
        try await jugarPartido(partido, msPorMinuto: 2)
    }
}
// Cada resultado es un `Result`: o terminó, o falló.
for resultado in supervisados {
    switch resultado {
    case let .success(partido):
        print("Terminó \(partido.local.nombre)-\(partido.visitante.nombre): \(partido.golesLocal)-\(partido.golesVisitante)")
    case let .failure(error):
        print("Falló: \(textoDelFallo(error))")
    }
}

// Un título para separar esta parte de la salida anterior.
print("== Un AsyncStream frío: la narración del partido ==")
// Un partido en vivo muy rápido: 2 ms por minuto de juego.
let enVivoRapido = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 2)
// La narración es una secuencia asíncrona de frases; aún no ha corrido nada.
let narracion = enVivoRapido.narracion
// `for await` recorre la narración y se pausa entre una frase y la siguiente.
for await linea in narracion { print("  \(linea)") }

// Cada recorrido es un partido nuevo: el frío se repite entero.
// Un contador de eventos…
var cuantos = 0
// …que suma uno por cada evento del segundo partido.
for await _ in enVivoRapido.eventos { cuantos += 1 }
print("Segundo recorrido: otro partido completo (\(cuantos) eventos)")

// `primerGol()` devuelve el primer gol y cancela el resto del partido.
if let gol = await enVivoRapido.primerGol() {
    print("Primer gol: \(describir(gol)) (el resto del partido se canceló)")
}

// `terminator: ""` imprime sin salto de línea: los marcadores salen en una línea.
print("Marcadores: ", terminator: "")
// Un marcador por cada gol, empezando en 0-0.
for await marcador in enVivoRapido.marcadores { print(marcador, terminator: " ") }
// Un salto de línea para cerrar la línea de marcadores.
print()

// Un título para separar esta parte de la salida anterior.
print("== dos goles a la vez ya no se pisan ==")
// Un marcador protegido por un actor.
let seguro = MarcadorSeguro()
// Un grupo de tareas, como en la lección de `TaskGroup`.
await withTaskGroup(of: Void.self) { grupo in
    for _ in 0..<1_000 {
        // Una tarea suma un gol local y otra uno visitante, todas a la vez.
        // `await` porque desde fuera hay que esperar el turno del actor.
        grupo.addTask { await seguro.golLocal() }
        grupo.addTask { await seguro.golVisitante() }
    }
}
// Tiene que salir 1000-1000: ningún gol se pierde.
print("Marcador tras 1000 goles de cada equipo a la vez: \(await seguro.marcador)")
// Un partido en vivo rápido (2 ms por minuto de juego).
let enDirecto = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 2)
// `jugar()` lo juega entero, anotando cada evento en el actor, y devuelve el marcador final.
print("Marcador final del partido en vivo: \(await enDirecto.jugar())")

print("== El estado de pantalla vive en el actor principal ==")
let seguimiento = SeguimientoEnPantalla()
seguimiento.seguir(PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 2))
await seguimiento.esperarAlFinal()
print(seguimiento.titular(seguimiento.marcador))
print("Narración: \(seguimiento.narracion.count) líneas; primera: \(seguimiento.narracion.first ?? "-")")
