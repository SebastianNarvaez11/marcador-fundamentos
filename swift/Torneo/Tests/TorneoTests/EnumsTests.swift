import Testing
@testable import Torneo

struct EnumsTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let luis = Jugador(nombre: "Luis")
    let ivan = Jugador(nombre: "Iván", dorsal: 7)
    let rayo = Equipo(nombre: "Rayo FC", plantilla: [Jugador(nombre: "Ana", dorsal: 9), Jugador(nombre: "Luis")])!
    let toros = Equipo(nombre: "Toros", plantilla: [Jugador(nombre: "Iván", dorsal: 7)])!

    @Test func valoresBrutosDeLaTarjeta() {
        #expect(ColorDeTarjeta.amarilla.rawValue == "AMARILLA")
        #expect(ColorDeTarjeta(rawValue: "ROJA") == .roja)
        #expect(ColorDeTarjeta(rawValue: "AZUL") == nil)   // init?(rawValue:) devuelve opcional
        #expect(ColorDeTarjeta.allCases == [.amarilla, .roja])
        #expect(ColorDeTarjeta.roja.etiqueta == "Roja")
    }

    @Test func minutoDeCadaCaso() {
        #expect(EventoDePartido.gol(minuto: 12, jugador: ana, equipo: rayo).minuto == 12)
        #expect(EventoDePartido.tarjeta(minuto: 30, jugador: ivan, color: .amarilla).minuto == 30)
        #expect(EventoDePartido.cambio(minuto: 85, sale: ana, entra: luis).minuto == 85)
    }

    @Test func describirCadaEvento() {
        #expect(describir(.gol(minuto: 12, jugador: ana, equipo: rayo)) == "12' Gol de Ana (Rayo FC)")
        #expect(describir(.tarjeta(minuto: 30, jugador: ivan, color: .amarilla)) == "30' Tarjeta amarilla para Iván")
        #expect(describir(.cambio(minuto: 85, sale: ana, entra: luis)) == "85' Cambio: sale Ana, entra Luis")
    }

    @Test func elMarcadorSeDeduceDeLosEventos() {
        let partido = Partido(local: rayo, visitante: toros)
            .registrando(.gol(minuto: 12, jugador: ana, equipo: rayo))
            .registrando(.tarjeta(minuto: 30, jugador: ivan, color: .amarilla))
            .registrando(.gol(minuto: 55, jugador: ivan, equipo: toros))
            .registrando(.gol(minuto: 80, jugador: ana, equipo: rayo))
        #expect(partido.eventos.count == 4)
        #expect(partido.golesLocal == 2)
        #expect(partido.golesVisitante == 1)
    }

    @Test func resultadoVictoriaEmpate() {
        let base = Partido(local: rayo, visitante: toros)
        #expect(base.resultado() == .empate)
        #expect(base.resultado().titular == "Empate")

        let gana = base.registrando(.gol(minuto: 1, jugador: ana, equipo: rayo))
        #expect(gana.resultado() == .victoria(ganador: rayo, perdedor: toros))
        #expect(gana.resultado().titular == "Gana Rayo FC")

        let pierde = base.registrando(.gol(minuto: 1, jugador: ivan, equipo: toros))
        #expect(pierde.resultado().titular == "Gana Toros")
    }

    @Test func ifCaseYaSabeSiEsUnGol() {
        let evento = EventoDePartido.gol(minuto: 5, jugador: ana, equipo: rayo)
        if case let .gol(minuto, jugador, _) = evento {
            #expect(minuto == 5)
            #expect(jugador == ana)
        } else {
            Issue.record("Debía ser un gol")
        }
    }
}
