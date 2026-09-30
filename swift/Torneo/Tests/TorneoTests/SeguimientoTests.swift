import Testing
@testable import Torneo

// `@MainActor` en la suite: todas sus pruebas corren en el actor principal, igual
// que el código de pantalla que usa `SeguimientoEnPantalla`.
@MainActor
struct SeguimientoTests {
    @Test func elSeguimientoActualizaElMarcadorYLaNarracion() async {
        let seguimiento = SeguimientoEnPantalla()
        let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        seguimiento.seguir(partido)
        await seguimiento.esperarAlFinal()
        #expect(seguimiento.marcador.description == "2-1")
        #expect(seguimiento.narracion.count == 5)
        #expect(seguimiento.narracion.first == "12' Gol de Ana (Rayo FC)")
    }

    @Test func detenerCancelaElSeguimiento() async {
        let seguimiento = SeguimientoEnPantalla()
        let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 10)
        seguimiento.seguir(partido)
        try? await Task.sleep(for: .milliseconds(200))   // ~minuto 20: solo el gol del 12
        seguimiento.detener()
        try? await Task.sleep(for: .milliseconds(100))
        #expect(seguimiento.marcador.description == "1-0")
        #expect(seguimiento.narracion.count == 1)
    }

    @Test func nonisolatedSePuedeLlamarSinAwait() {
        let seguimiento = SeguimientoEnPantalla()
        #expect(seguimiento.titular(Marcador(local: 2, visitante: 1)) == "Marcador: 2-1")
    }

    @Test func empezarOtroPartidoReiniciaElEstado() async {
        let seguimiento = SeguimientoEnPantalla()
        seguimiento.seguir(PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1))
        await seguimiento.esperarAlFinal()
        seguimiento.seguir(PartidoEnVivo(guion: Ejemplo.lobosContraAguilas, msPorMinuto: 1))
        await seguimiento.esperarAlFinal()
        #expect(seguimiento.marcador.description == "1-2")
        #expect(seguimiento.narracion.count == 4)
    }
}
