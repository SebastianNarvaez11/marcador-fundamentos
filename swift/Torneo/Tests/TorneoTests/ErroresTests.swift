import Foundation
import Testing
@testable import Torneo

struct ErroresTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let luis = Jugador(nombre: "Luis", dorsal: 7)
    let pedro = Jugador(nombre: "Pedro", dorsal: 4)
    let rayo: Equipo
    let toros: Equipo
    let lobos: Equipo
    let torneo: Torneo

    init() {
        rayo = Equipo(nombre: "Rayo", plantilla: [ana])!
        toros = Equipo(nombre: "Toros", plantilla: [luis])!
        lobos = Equipo(nombre: "Lobos", plantilla: [pedro])!
        torneo = Torneo(nombre: "Copa", equipos: [rayo, toros])
    }

    @Test func registrarUnPartidoValido() throws {
        let partido = try torneo.registrar(local: rayo, visitante: toros, eventos: [.gol(minuto: 10, jugador: ana, equipo: rayo)])
        #expect(partido.golesLocal == 1)
        #expect(torneo.partidos.count == 1)
    }

    @Test func equipoNoInscritoLanzaError() {
        #expect(throws: ErrorDeTorneo.equiposNoInscritos(torneo: "Copa")) {
            try torneo.registrar(local: rayo, visitante: lobos)
        }
        #expect(torneo.partidos.isEmpty)
    }

    @Test func unEquipoNoJuegaContraSiMismo() {
        #expect(throws: ErrorDeTorneo.equipoContraSiMismo) {
            try torneo.registrar(local: rayo, visitante: rayo)
        }
    }

    @Test func golDeAlguienAjenoALaPlantilla() {
        #expect(throws: ErrorDeTorneo.jugadorNoJuegaEnElEquipo(jugador: "Luis", equipo: "Rayo")) {
            try torneo.registrar(local: rayo, visitante: toros, eventos: [.gol(minuto: 10, jugador: luis, equipo: rayo)])
        }
        #expect(torneo.partidos.isEmpty)
    }

    @Test func golDeUnEquipoQueNoJuegaElPartido() throws {
        let torneoDeTres = Torneo(nombre: "Copa", equipos: [rayo, toros, lobos])
        #expect(throws: ErrorDeTorneo.golDeEquipoAjeno(equipo: "Lobos")) {
            try torneoDeTres.registrar(local: rayo, visitante: toros, eventos: [.gol(minuto: 10, jugador: pedro, equipo: lobos)])
        }
    }

    @Test func minutoFueraDelPartido() {
        #expect(throws: ErrorDeTorneo.minutoFueraDelPartido(200)) {
            try torneo.registrar(local: rayo, visitante: toros, eventos: [.gol(minuto: 200, jugador: ana, equipo: rayo)])
        }
    }

    @Test func tarjetaYCambioConAjenos() {
        #expect(throws: ErrorDeTorneo.jugadorNoJuegaElPartido(jugador: "Pedro")) {
            try torneo.registrar(local: rayo, visitante: toros, eventos: [.tarjeta(minuto: 5, jugador: pedro, color: .roja)])
        }
        #expect(throws: ErrorDeTorneo.cambioConAjenos) {
            try torneo.registrar(local: rayo, visitante: toros, eventos: [.cambio(minuto: 60, sale: ana, entra: pedro)])
        }
    }

    // Los mensajes son los de la versión Kotlin.
    @Test func losMensajesCoincidenConKotlin() {
        #expect(ErrorDeTorneo.equiposNoInscritos(torneo: "Copa").localizedDescription == "Los dos equipos deben estar inscritos en Copa")
        #expect(ErrorDeTorneo.minutoFueraDelPartido(200).localizedDescription == "Minuto fuera del partido: 200")
        #expect(ErrorDeTorneo.jugadorNoJuegaEnElEquipo(jugador: "Luis", equipo: "Rayo").localizedDescription == "Luis no juega en Rayo")
        #expect(ErrorDeTorneo.equipoContraSiMismo.localizedDescription == "Un equipo no puede jugar contra sí mismo")
    }

    @Test func resultadoConservaElTipoDelError() {
        let bueno = Partido(local: rayo, visitante: toros)
        let malo = Partido(local: rayo, visitante: rayo)
        guard case let .success(registrado) = torneo.intentarRegistrar(bueno) else {
            Issue.record("Debía registrarse")
            return
        }
        #expect(registrado == bueno)
        #expect(torneo.intentarRegistrar(malo) == .failure(.equipoContraSiMismo))
        #expect(torneo.partidos.count == 1)
    }

    @Test func tryInterrogacionConvierteElErrorEnNil() {
        #expect((try? torneo.registrar(local: rayo, visitante: rayo)) == nil)
        #expect((try? torneo.registrar(local: rayo, visitante: toros)) != nil)
    }

    @Test func dorsalValidoDiceSuMotivo() throws {
        #expect(try dorsalValido("10") == 10)
        #expect(throws: ErrorDeDorsal.noEsUnNumero("diez")) { try dorsalValido("diez") }
        #expect(throws: ErrorDeDorsal.fueraDeRango(150)) { try dorsalValido("150") }
    }
}
