import Testing
@testable import Torneo

// Un tipo de prueba que adopta el protocolo y sobrescribe lo que quiere.
private struct ReglamentoConDerrotaCastigada: Reglamento {
    let puntosPorVictoria = 3
    let puntosPorEmpate = 1
    let puntosPorDerrota = -1   // requisito con valor por defecto: aquí se cambia
    // NO es requisito del protocolo: `resumen()` sale de la extensión aunque se escriba otro.
    func resumen() -> String { "el mío" }
}

struct ReglamentoTests {
    @Test func laLigaDaTresUnoCero() {
        let liga = ReglamentoLiga()
        #expect(liga.puntosPor(golesAFavor: 1, golesEnContra: 0) == 3)
        #expect(liga.puntosPor(golesAFavor: 1, golesEnContra: 1) == 1)
        #expect(liga.puntosPor(golesAFavor: 0, golesEnContra: 1) == 0)
        #expect(liga.puntosPorDerrota == 0)   // valor por defecto de la extensión
    }

    @Test func unReglamentoPersonalizadoUsaLaImplementacionPorDefecto() {
        let antiguo = ReglamentoPersonalizado(puntosPorVictoria: 2, puntosPorEmpate: 1)
        #expect(antiguo.puntosPor(golesAFavor: 3, golesEnContra: 0) == 2)
        #expect(antiguo.puntosPor(golesAFavor: 0, golesEnContra: 0) == 1)
        #expect(antiguo.puntosPor(golesAFavor: 0, golesEnContra: 5) == 0)
    }

    @Test func unRequisitoSobrescritoSeRespetaAunqueSeUseComoAnyReglamento() {
        let castigado: any Reglamento = ReglamentoConDerrotaCastigada()
        #expect(castigado.puntosPorDerrota == -1)
        #expect(castigado.puntosPor(golesAFavor: 0, golesEnContra: 2) == -1)
    }

    @Test func unMetodoQueNoEsRequisitoSeDespachaEstaticamente() {
        let concreto = ReglamentoConDerrotaCastigada()
        let comoProtocolo: any Reglamento = concreto
        #expect(concreto.resumen() == "el mío")
        #expect(comoProtocolo.resumen() == "3/1/-1")   // sale de la extensión
    }

    @Test func descripcionDeLaLiga() {
        #expect(ReglamentoLiga().description == "Reglamento Liga (3/1/0)")
    }

    @Test func puntosPorUsaElReglamentoDeLaLiga() {
        #expect(puntosPor(golesAFavor: 5, golesEnContra: 0) == 3)
        #expect(puntos(para: 0, contra: 0) == 1)
    }

    @Test func elTorneoTieneUnReglamentoPorDefectoYSePuedeCambiar() {
        let porDefecto = Torneo(nombre: "Copa", equipos: [])
        #expect(porDefecto.reglamento.puntosPorVictoria == 3)
        let antiguo = Torneo(nombre: "Copa", equipos: [], reglamento: ReglamentoPersonalizado(puntosPorVictoria: 2, puntosPorEmpate: 1))
        #expect(antiguo.reglamento.puntosPorVictoria == 2)
    }

    @Test func extensionesDeFormato() {
        let ana = Jugador(nombre: "Ana", dorsal: 9)
        let rayo = Equipo(nombre: "Rayo", plantilla: [ana])!
        #expect(ana.etiqueta() == "Ana (#9)")
        #expect(rayo.capitanONinguno == "sin capitán")
        rayo.nombrarCapitan(ana)
        #expect(rayo.capitanONinguno == "Ana")
    }

    @Test func hashablePermiteJugadoresEnUnSet() {
        let ana = Jugador(nombre: "Ana", dorsal: 9)
        let conjunto: Set<Jugador> = [ana, Jugador(nombre: "Ana", dorsal: 9), Jugador(nombre: "Luis")]
        #expect(conjunto.count == 2)
        #expect(conjunto.contains(Jugador(nombre: "Luis")))
    }
}
