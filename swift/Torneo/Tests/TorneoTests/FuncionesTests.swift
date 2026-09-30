import Testing
@testable import Torneo

struct FuncionesTests {
    @Test func puntosParaMarcadores() {
        #expect(puntos(para: 2, contra: 1) == 3)
        #expect(puntos(para: 1, contra: 1) == 1)
        #expect(puntos(para: 0, contra: 4) == 0)
    }

    @Test func puntosPorSigueDandoLoMismo() {
        for aFavor in 0...3 {
            for enContra in 0...3 {
                #expect(puntosPor(golesAFavor: aFavor, golesEnContra: enContra) == puntos(para: aFavor, contra: enContra))
            }
        }
    }

    @Test func encabezadoConValorPorDefecto() {
        #expect(encabezadoDeJornada(numero: 2, de: 3) == "Jornada 2 de 3")
        #expect(encabezadoDeJornada(numero: 1, de: 3, prefijo: "Fecha") == "Fecha 1 de 3")
    }

    @Test(arguments: [
        (0, 0, "Sin goles"),
        (2, 2, "Empate a 2"),
        (3, 0, "El local gana sin recibir goles"),
        (0, 1, "El visitante gana sin recibir goles"),
        (5, 1, "Goleada"),
        (1, 5, "Goleada"),
        (2, 1, "Gana el local"),
        (1, 3, "Gana el visitante"),
    ])
    func titularSegunElMarcador(local: Int, visitante: Int, esperado: String) {
        #expect(titular(golesLocal: local, golesVisitante: visitante) == esperado)
    }

    @Test func inoutCambiaLaVariableDeQuienLlama() {
        var goles = 0
        sumarGol(a: &goles)
        sumarGol(a: &goles)
        #expect(goles == 2)
    }
}
