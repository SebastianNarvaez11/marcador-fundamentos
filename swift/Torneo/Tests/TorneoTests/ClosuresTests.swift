import Testing
@testable import Torneo

struct ClosuresTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let luis = Jugador(nombre: "Luis")
    let ivan = Jugador(nombre: "Iván", dorsal: 7)
    let rayo: Equipo
    let toros: Equipo
    let partido: Partido

    init() {
        rayo = Equipo(nombre: "Rayo FC", plantilla: [ana, luis])!
        toros = Equipo(nombre: "Toros", plantilla: [ivan])!
        partido = Partido(local: rayo, visitante: toros)
            .registrando(.gol(minuto: 12, jugador: ana, equipo: rayo))
            .registrando(.tarjeta(minuto: 30, jugador: ivan, color: .amarilla))
            .registrando(.gol(minuto: 55, jugador: ivan, equipo: toros))
            .registrando(.gol(minuto: 80, jugador: ana, equipo: rayo))
            .registrando(.cambio(minuto: 85, sale: ana, entra: luis))
    }

    @Test func filtroConTrailingClosure() {
        let tarjetas = partido.eventos { if case .tarjeta = $0 { true } else { false } }
        #expect(tarjetas.count == 1)
        #expect(partido.eventos(esGol).count == 3)
    }

    @Test func closureQueCapturaElMinuto() {
        #expect(partido.eventos(antesDelMinuto(45)).count == 2)
        #expect(partido.eventos(antesDelMinuto(0)).isEmpty)
        #expect(partido.eventos { esDelMinutoOAntes($0, 30) }.count == 2)
    }

    @Test func goleadoresConCompactMap() {
        #expect(partido.goleadores() == [ana, ivan, ana])
    }

    @Test func minutosDeGolConReturnEnLaClosure() {
        #expect(partido.minutosDeGolDe(ana) == [12, 80])
        #expect(partido.minutosDeGolDe(luis).isEmpty)
    }

    @Test func repetirLlamaConCadaVuelta() {
        var vueltas: [Int] = []
        repetir(3) { vueltas.append($0) }
        #expect(vueltas == [1, 2, 3])
    }

    @Test func cadaContadorTieneSuPropiaCuenta() {
        let uno = contador()
        let otro = contador()
        #expect(uno() == 1)
        #expect(uno() == 2)
        #expect(otro() == 1)   // no comparte `cuenta` con `uno`
    }

    @Test func variasTrailingClosures() {
        var texto = ""
        alResolver(partido) { texto = "gana \($0.nombre)" } enEmpate: { texto = "tablas" }
        #expect(texto == "gana Rayo FC")
        alResolver(Partido(local: rayo, visitante: toros)) { _ in texto = "?" } enEmpate: { texto = "tablas" }
        #expect(texto == "tablas")
    }

    @Test func closureEscapingSeGuardaYSeLlamaDespues() {
        let avisador = Avisador()
        var vistos: [Int] = []
        avisador.alEvento { vistos.append($0.minuto) }
        #expect(vistos.isEmpty)   // guardada, aún no llamada
        avisador.avisar(.gol(minuto: 12, jugador: ana, equipo: rayo))
        avisador.avisar(.tarjeta(minuto: 30, jugador: ivan, color: .roja))
        #expect(vistos == [12, 30])
    }
}
