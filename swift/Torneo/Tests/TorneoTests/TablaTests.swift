import Testing
@testable import Torneo

struct TablaTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let ivan = Jugador(nombre: "Iván", dorsal: 7)
    let pedro = Jugador(nombre: "Pedro", dorsal: 4)
    let rayo: Equipo
    let toros: Equipo
    let lobos: Equipo

    init() {
        rayo = Equipo(nombre: "Rayo FC", plantilla: [ana, Jugador(nombre: "Luis"), Jugador(nombre: "Marta", dorsal: 1)])!
        toros = Equipo(nombre: "Toros", plantilla: [ivan, Jugador(nombre: "Sofía", dorsal: 10)])!
        lobos = Equipo(nombre: "Lobos", plantilla: [pedro, Jugador(nombre: "Carla", dorsal: 5)])!
    }

    // Los mismos tres partidos que la demo de Kotlin (f08).
    private func copa() throws -> Torneo {
        let copa = Torneo(nombre: "Copa Barrio", equipos: [rayo, toros, lobos])
        try copa.registrar(local: rayo, visitante: toros, eventos: [
            .gol(minuto: 12, jugador: ana, equipo: rayo),
            .tarjeta(minuto: 30, jugador: ivan, color: .amarilla),
            .gol(minuto: 55, jugador: ivan, equipo: toros),
            .gol(minuto: 80, jugador: ana, equipo: rayo),
        ])
        try copa.registrar(local: toros, visitante: lobos, eventos: [.gol(minuto: 20, jugador: ivan, equipo: toros)])
        try copa.registrar(local: lobos, visitante: rayo, eventos: [.gol(minuto: 40, jugador: ana, equipo: rayo)])
        return copa
    }

    @Test func laTablaDeKotlin() throws {
        let tabla = try copa().tablaDePosiciones()
        #expect(tabla.map(\.equipo.nombre) == ["Rayo FC", "Toros", "Lobos"])
        #expect(tabla.map(\.puntos) == [6, 3, 0])
        #expect(tabla.map(\.diferencia) == [2, 0, -2])
        #expect(tabla[0].golesAFavor == 3)
        #expect(tabla[0].golesEnContra == 1)
        #expect(tabla[2].perdidos == 2)
    }

    @Test func laTablaComoTextoEsIgualQueLaDeKotlin() throws {
        let esperado = """
        Pos Equipo    PJ  G  E  P  GF GC  DG Pts
         1  Rayo FC   2  2  0  0   3  1   2   6
         2  Toros     2  1  0  1   2  2   0   3
         3  Lobos     2  0  0  2   0  2  -2   0
        """
        #expect(try copa().tablaComoTexto() == esperado)
    }

    @Test func losEquiposSinPartidosSalenConCeros() {
        let tabla = [Partido]().tablaDePosiciones(equipos: [rayo, toros])
        #expect(tabla.count == 2)
        #expect(tabla.allSatisfy { $0.jugados == 0 && $0.puntos == 0 })
        #expect(tabla.map(\.equipo.nombre) == ["Rayo FC", "Toros"])   // desempate por nombre
    }

    @Test func desempatePorDiferenciaDeGoles() {
        let ganaPoco = Partido(local: rayo, visitante: toros).registrando(.gol(minuto: 1, jugador: ana, equipo: rayo))
        let ganaMucho = Partido(local: lobos, visitante: toros)
            .registrando(.gol(minuto: 1, jugador: pedro, equipo: lobos))
            .registrando(.gol(minuto: 2, jugador: pedro, equipo: lobos))
        let tabla = [ganaPoco, ganaMucho].tablaDePosiciones()
        #expect(tabla.map(\.equipo.nombre) == ["Lobos", "Rayo FC", "Toros"])   // 3 puntos cada uno; +2 antes que +1
    }

    @Test func elReglamentoCambiaLosPuntos() throws {
        let partidos = try copa().partidos
        let antiguo = ReglamentoPersonalizado(puntosPorVictoria: 2, puntosPorEmpate: 1)
        #expect(partidos.tablaDePosiciones(reglamento: antiguo).first?.puntos == 4)
    }

    @Test func didSetInvalidaLaTablaGuardada() throws {
        let copa = try copa()
        let primera = copa.tablaDePosiciones()
        #expect(copa.tablaDePosiciones() == primera)
        try copa.registrar(local: toros, visitante: rayo, eventos: [.gol(minuto: 1, jugador: ivan, equipo: toros)])
        #expect(copa.tablaDePosiciones() != primera)
        #expect(copa.cantidadDePartidos == 4)
    }

    @Test func lazyCalculaLaTablaUnaSolaVez() throws {
        let foto = try copa().instantanea()
        #expect(foto.calculosDeLaTabla == 0)
        #expect(foto.tabla.first?.equipo.nombre == "Rayo FC")
        #expect(foto.tabla.count == 3)
        #expect(foto.calculosDeLaTabla == 1)
    }

    @Test func laFotoNoCambiaConLosPartidosNuevos() throws {
        let copa = try copa()
        let foto = copa.instantanea()
        try copa.registrar(local: lobos, visitante: toros)
        #expect(foto.partidos.count == 3)
        #expect(copa.partidos.count == 4)
    }

    @Test func mapFilterReduceCompactMapYSet() {
        let numeros = [1, 2, 3, 4, 5, 6]
        let dobles = numeros.map { $0 * 2 }
        let grandes = dobles.filter { $0 > 4 }
        #expect(grandes.reduce(0, +) == 36)
        let enteros = ["1", "x", "3"].compactMap { Int($0) }
        #expect(enteros == [1, 3])
        #expect(Set([1, 2, 2, 3, 3, 3]).count == 3)
        let porParidad = Dictionary(grouping: numeros, by: { $0 % 2 == 0 })
        #expect(porParidad[true]?.count == 3)
        #expect(porParidad[false]?.count == 3)
        #expect(numeros.sorted(by: >).first == 6)
    }
}
