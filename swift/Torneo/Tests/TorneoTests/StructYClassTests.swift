import Testing
@testable import Torneo

struct StructYClassTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let luis = Jugador(nombre: "Luis")
    let ivan = Jugador(nombre: "Iván", dorsal: 7)

    private func equipos() -> (Equipo, Equipo) {
        (Equipo(nombre: "Rayo", plantilla: [ana, luis])!, Equipo(nombre: "Toros", plantilla: [ivan])!)
    }

    // ---- Equipo (class) ----

    @Test func equipoRechazaDorsalesRepetidos() {
        let repetidos = [Jugador(nombre: "A", dorsal: 7), Jugador(nombre: "B", dorsal: 7)]
        #expect(Equipo(nombre: "Toros", plantilla: repetidos) == nil)
    }

    @Test func dosJugadoresSinDorsalNoSeConsideranRepetidos() {
        #expect(Equipo(nombre: "X", plantilla: [Jugador(nombre: "A"), Jugador(nombre: "B")]) != nil)
    }

    @Test func equipoRechazaNombreVacio() {
        #expect(Equipo(nombre: "", plantilla: []) == nil)
    }

    @Test func capitanSoloDeLaPlantilla() {
        let (rayo, _) = equipos()
        #expect(rayo.capitan == nil)
        #expect(rayo.nombrarCapitan(ivan) == false)
        #expect(rayo.capitan == nil)
        #expect(rayo.nombrarCapitan(ana))
        #expect(rayo.capitan == ana)
    }

    @Test func dosEquiposConLosMismosDatosNoSonElMismoEquipo() {
        let uno = Equipo(nombre: "Rayo", plantilla: [ana])!
        let otro = Equipo(nombre: "Rayo", plantilla: [ana])!
        #expect(uno != otro)   // identidad, no valor
        #expect(uno == uno)
    }

    // ---- Referencia frente a valor ----

    @Test func unaClaseSeComparteAlAsignar() {
        let (rayo, _) = equipos()
        let mismo = rayo
        mismo.nombrarCapitan(ana)
        #expect(rayo.capitan == ana)   // lo ve la otra variable
        #expect(rayo === mismo)
    }

    @Test func unStructSeCopiaAlAsignar() {
        let (rayo, toros) = equipos()
        let inicio = Partido(local: rayo, visitante: toros)
        var copia = inicio
        copia.registrarGolLocal()
        #expect(inicio.golesLocal == 0)   // el original no cambia
        #expect(copia.golesLocal == 1)
    }

    @Test func laCopiaDeUnStructComparteLasClasesQueContiene() {
        let (rayo, toros) = equipos()
        let inicio = Partido(local: rayo, visitante: toros)
        let copia = inicio
        #expect(copia.local === inicio.local)
    }

    @Test func conGolDevuelveOtroPartido() {
        let (rayo, toros) = equipos()
        let inicio = Partido(local: rayo, visitante: toros)
        let despues = inicio.conGolLocal().conGolVisitante().conGolLocal()
        #expect(inicio.golesLocal == 0)
        #expect(despues.golesLocal == 2)
        #expect(despues.golesVisitante == 1)
    }

    @Test func copyOnWriteEnLosArrays() {
        var original = [1, 2, 3]
        let copia = original
        original.append(4)
        #expect(copia == [1, 2, 3])
        #expect(original == [1, 2, 3, 4])
    }

    // ---- Torneo (class) ----

    @Test func torneoAcumulaPartidosYLoQueSaleEsUnaCopia() {
        let (rayo, toros) = equipos()
        let torneo = Torneo(nombre: "Copa", equipos: [rayo, toros])
        var visto = torneo.partidos
        visto.append(Partido(local: rayo, visitante: toros))   // cambia la copia local
        #expect(torneo.partidos.isEmpty)
        torneo.registrar(Partido(local: rayo, visitante: toros))
        #expect(torneo.partidos.count == 1)
    }

    @Test func dosVariablesDelMismoTorneoVenLoMismo() {
        let (rayo, toros) = equipos()
        let torneo = Torneo(nombre: "Copa", equipos: [rayo, toros])
        let mismo = torneo
        mismo.registrar(Partido(local: rayo, visitante: toros))
        #expect(torneo.partidos.count == 1)
    }
}
