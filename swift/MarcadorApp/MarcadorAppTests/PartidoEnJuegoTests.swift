import Testing
import Torneo
@testable import MarcadorApp

// Pruebas del partido en juego (estado de toda la app). Sin vistas y sin ViewModel.
@MainActor
struct PartidoEnJuegoTests {

    // `msPorMinuto` grande: el partido empieza y se queda en el minuto 0 mientras dura la prueba.
    private func directo(_ id: Int, msPorMinuto: Int = 0, ciclo: Bool = false) -> Directo {
        Directo(partidoId: id, partido: Ejemplo.rayoContraToros, duracion: 90, msPorMinuto: msPorMinuto, reintroducirElCiclo: ciclo)
    }

    @Test func empezarLoPoneEnJuego() async {
        let enJuego = PartidoEnJuego()
        #expect(enJuego.empezar(directo(1)))
        await enJuego.tarea?.value
        #expect(enJuego.directo?.partidoId == 1)
        #expect(enJuego.directo?.minuto == 90)
    }

    @Test func noDejaEmpezarOtroMientrasSeJuega() {
        let enJuego = PartidoEnJuego()
        _ = enJuego.empezar(directo(1, msPorMinuto: 60_000))
        #expect(!enJuego.empezar(directo(2)))
        #expect(enJuego.directo?.partidoId == 1)
        #expect(enJuego.directo(id: 2) == nil)
    }

    @Test func cerrarNoHaceNadaMientrasSeJuega() {
        let enJuego = PartidoEnJuego()
        _ = enJuego.empezar(directo(1, msPorMinuto: 60_000))
        enJuego.cerrar()
        #expect(enJuego.directo?.partidoId == 1)
    }

    @Test func alTerminarSeCierraYDejaEmpezarOtro() async {
        let enJuego = PartidoEnJuego()
        _ = enJuego.empezar(directo(1))
        await enJuego.tarea?.value
        #expect(enJuego.empezar(directo(2)))
        await enJuego.tarea?.value
        enJuego.cerrar()
        #expect(enJuego.directo == nil)
    }

    // Sustituye a la prueba del ciclo del ViewModel: el directo vive en el partido en juego, y al
    // cerrarlo nadie lo retiene (con `[weak self]` en el cronómetro).
    @Test func elDirectoSeLiberaAlCerrar() async {
        let enJuego = PartidoEnJuego()
        weak var referencia: Directo?
        do {
            let directo = directo(1)
            referencia = directo
            _ = enJuego.empezar(directo)
        }
        await enJuego.tarea?.value
        #expect(referencia != nil)   // terminado, sigue ahí («Final»)
        enJuego.cerrar()
        #expect(referencia == nil)
    }
}
