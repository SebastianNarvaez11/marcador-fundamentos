import Foundation
import Testing
@testable import Torneo

struct CodableTests {
    // El JSON que escribió la versión Kotlin (`./gradlew :torneo:jvmRun --args=kotlin`).
    private func jsonDeKotlin() throws -> Data {
        let url = try #require(Bundle.module.url(forResource: "torneo", withExtension: "json", subdirectory: "Fixtures"))
        return try Data(contentsOf: url)
    }

    @Test func leeElTorneoQueEscribioKotlin() throws {
        let torneo = try torneoDesdeJson(jsonDeKotlin())
        #expect(torneo.nombre == "Copa Barrio")
        #expect(torneo.equipos.map(\.nombre) == ["Rayo FC", "Toros", "Lobos"])
        #expect(torneo.partidos.count == 4)
        #expect(torneo.tablaDePosiciones().first?.equipo.nombre == "Rayo FC")
    }

    @Test func elDorsalAusenteSeLeeComoNil() throws {
        let torneo = try torneoDesdeJson(jsonDeKotlin())
        let luis = try #require(torneo.equipos[0].plantilla.first { $0.nombre == "Luis" })
        #expect(luis.dorsal == nil)
        #expect(torneo.equipos[0].plantilla.first { $0.nombre == "Ana" }?.dorsal == 9)
    }

    @Test func elCapitanSeResuelvePorNombre() throws {
        let torneo = try torneoDesdeJson(jsonDeKotlin())
        #expect(torneo.equipos[0].capitan?.nombre == "Ana")
        #expect(torneo.equipos[1].capitan == nil)   // «capitan» no está en el JSON
    }

    @Test func losEventosSeDistinguenPorElCampoTipo() throws {
        let torneo = try torneoDesdeJson(jsonDeKotlin())
        let eventos = torneo.partidos[0].eventos
        #expect(eventos.count == 5)
        if case let .tarjeta(minuto, jugador, color) = eventos[1] {
            #expect(minuto == 30)
            #expect(jugador.nombre == "Iván")
            #expect(color == .amarilla)   // «AMARILLA» del JSON, gracias al rawValue
        } else {
            Issue.record("El segundo evento debía ser una tarjeta")
        }
        if case let .cambio(_, sale, entra) = eventos[4] {
            #expect(sale.nombre == "Ana")
            #expect(entra.nombre == "Luis")
        } else {
            Issue.record("El quinto evento debía ser un cambio")
        }
    }

    @Test func calculaLosMismosPuntosQueKotlin() throws {
        let tabla = try torneoDesdeJson(jsonDeKotlin()).tablaDePosiciones()
        #expect(tabla.map(\.equipo.nombre) == ["Rayo FC", "Toros", "Lobos"])
        #expect(tabla.map(\.puntos) == [6, 3, 3])
    }

    @Test func ida_y_vuelta() throws {
        let original = try torneoDesdeJson(jsonDeKotlin())
        let copia = try torneoDesdeJson(original.aJson())
        #expect(copia.nombre == original.nombre)
        #expect(copia.partidos.count == original.partidos.count)
        #expect(copia.tablaDePosiciones().map(\.puntos) == original.tablaDePosiciones().map(\.puntos))
        #expect(copia.equipos[0].capitan?.nombre == "Ana")
    }

    @Test func losNilNoSeEscriben() throws {
        let luis = Jugador(nombre: "Luis")
        let rayo = try #require(Equipo(nombre: "Rayo", plantilla: [luis, Jugador(nombre: "Ana", dorsal: 9)]))
        let texto = try #require(String(data: Torneo(nombre: "Copa", equipos: [rayo]).aJson(), encoding: .utf8))
        #expect(!texto.contains("capitan"))
        // Un solo «dorsal» en el fichero: el de Ana. Luis no lo lleva.
        #expect(texto.components(separatedBy: "\"dorsal\"").count - 1 == 1)
    }

    @Test func elEventoSeEscribeConSuDiscriminador() throws {
        let ana = Jugador(nombre: "Ana", dorsal: 9)
        let rayo = try #require(Equipo(nombre: "Rayo", plantilla: [ana]))
        let toros = try #require(Equipo(nombre: "Toros", plantilla: []))
        let torneo = Torneo(nombre: "Copa", equipos: [rayo, toros])
        try torneo.registrar(local: rayo, visitante: toros, eventos: [.gol(minuto: 3, jugador: ana, equipo: rayo)])
        let texto = try #require(String(data: torneo.aJson(), encoding: .utf8))
        #expect(texto.contains("\"tipo\" : \"gol\""))
    }

    @Test func unTipoDeEventoDesconocidoEsUnErrorDeDecodificacion() {
        let json = """
        {"nombre":"X","equipos":[],"partidos":[{"local":"A","visitante":"B","eventos":[{"tipo":"penalti","minuto":3}]}]}
        """
        #expect(throws: DecodingError.self) { try torneoDesdeJson(Data(json.utf8)) }
    }

    @Test func unEquipoQueNoExisteEsUnErrorDeCarga() {
        let json = """
        {"nombre":"X","equipos":[],"partidos":[{"local":"A","visitante":"B","eventos":[]}]}
        """
        #expect(throws: ErrorDeCarga.equipoDesconocido("A")) { try torneoDesdeJson(Data(json.utf8)) }
    }

    @Test func unJsonRotoLanzaError() {
        #expect(throws: (any Error).self) { try torneoDesdeJson(Data("{no es json".utf8)) }
    }
}
