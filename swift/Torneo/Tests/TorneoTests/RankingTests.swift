import Testing
@testable import Torneo

struct RankingTests {
    let ana = Jugador(nombre: "Ana", dorsal: 9)
    let ivan = Jugador(nombre: "Iván", dorsal: 7)
    let pedro = Jugador(nombre: "Pedro", dorsal: 4)

    @Test func ordenaDeMasAMenosPuntos() {
        let ranking = Ranking(elementos: ["a", "bbb", "cc"], puntos: \.count)
        #expect(ranking.ordenados == ["bbb", "cc", "a"])
        #expect(ranking.top(2) == ["bbb", "cc"])
        #expect(ranking.puntosDe("cc") == 2)
    }

    @Test func elDesempateOrdenaLosIgualesEnPuntos() {
        let ranking = Ranking(elementos: ["pb", "pa", "z"], puntos: \.count, desempate: <)
        #expect(ranking.ordenados == ["pa", "pb", "z"])
    }

    @Test func puestoConEmpates() {
        let ranking = Ranking(elementos: ["a", "b", "cc"], puntos: \.count, desempate: <)
        #expect(ranking.puesto(de: "cc") == 1)
        #expect(ranking.puesto(de: "a") == 2)
        #expect(ranking.puesto(de: "b") == 2)   // empatado con «a»: mismo puesto
        #expect(ranking.puesto(de: "zzz") == nil)
    }

    @Test func rankingEsUnaSecuencia() {
        let ranking = Ranking(elementos: [1, 3, 2], puntos: { $0 })
        var vistos: [Int] = []
        for numero in ranking { vistos.append(numero) }
        #expect(vistos == [3, 2, 1])
        #expect(ranking.map { $0 * 10 } == [30, 20, 10])   // los métodos de Sequence, gratis
    }

    @Test func liderYFuncionesGenericas() {
        let ranking = Ranking(elementos: [ana, ivan], puntos: { $0.dorsal ?? 0 })
        #expect(lider(ranking) == ana)
        #expect(lider(Ranking<Int>(elementos: [], puntos: { $0 })) == nil)
        #expect(nombres(de: ranking) == ["Ana", "Iván"])
        #expect(nombres(de: [pedro]) == ["Pedro"])   // sirve con cualquier secuencia
        #expect(cuantosPuestosHay([ranking, Ranking(elementos: [1, 2, 3], puntos: { $0 })]) == 5)
    }

    @Test func goleadoresDeLosPartidos() {
        let rayo = Equipo(nombre: "Rayo", plantilla: [ana])!
        let toros = Equipo(nombre: "Toros", plantilla: [ivan])!
        let lobos = Equipo(nombre: "Lobos", plantilla: [pedro])!
        let uno = Partido(local: rayo, visitante: toros)
            .registrando(.gol(minuto: 12, jugador: ana, equipo: rayo))
            .registrando(.gol(minuto: 55, jugador: ivan, equipo: toros))
            .registrando(.gol(minuto: 80, jugador: ana, equipo: rayo))
        let dos = Partido(local: toros, visitante: lobos)
            .registrando(.gol(minuto: 20, jugador: ivan, equipo: toros))
            .registrando(.gol(minuto: 30, jugador: pedro, equipo: lobos))
        let ranking = [uno, dos].goleadores()
        #expect(ranking.puntosDe(ana) == 2)
        #expect(ranking.puntosDe(ivan) == 2)
        #expect(ranking.puntosDe(pedro) == 1)
        #expect(ranking.ordenados == [ana, ivan, pedro])   // Ana e Iván empatan: gana el nombre (A < I)
        #expect(ranking.puesto(de: ivan) == 1)              // empatado con Ana
        #expect(ranking.puesto(de: pedro) == 3)
    }

    @Test func torneoSinPartidosNoTieneGoleadores() {
        #expect(Torneo(nombre: "Copa", equipos: []).goleadores().ordenados.isEmpty)
    }
}
