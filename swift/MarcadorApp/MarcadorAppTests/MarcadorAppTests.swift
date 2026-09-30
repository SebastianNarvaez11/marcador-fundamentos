import Observation
import Synchronization
import Testing
import Torneo
@testable import MarcadorApp

struct MarcadorAppTests {

    // El asistente de Xcode genera un `example()` vacío. Éste comprueba lo mismo que
    // enseña f67: que el paquete `Torneo` llega hasta el target de pruebas de la app.
    @Test func elPaqueteTorneoLlegaALaApp() {
        let partido = Ejemplo.rayoContraToros
        #expect(partido.marcador() == Marcador(local: 2, visitante: 1))
    }

    // f70: el `Lado` y el `Marcador` (del paquete) son valores: sumar un gol crea otro.
    @Test func sumarUnGolCreaOtroMarcador() {
        let antes = Marcador(local: 0, visitante: 0)
        let despues = Marcador(local: antes.local + 1, visitante: antes.visitante)
        #expect(antes == Marcador(local: 0, visitante: 0))
        #expect(despues.goles == 1)
    }

    // f71: los datos de ejemplo son los mismos que en Android: 18 partidos con id único.
    @Test func losDatosDeEjemploTienenDieciochoPartidosConIdUnico() {
        let partidos = DatosDeEjemplo.partidos
        #expect(partidos.count == 18)
        #expect(Set(partidos.map(\.id)).count == 18)
        #expect(partidos.first?.id == 1)
    }

    // f73: `@Observable` avisa cuando cambia lo que se LEYÓ dentro de `withObservationTracking`.
    @Test func observableAvisaSoloDeLoQueSeLee() {
        let ajustes = AjustesModelo()
        // `Mutex`: el `onChange` es @Sendable y no puede mutar una `var` capturada.
        let avisos = Mutex(0)
        withObservationTracking {
            _ = ajustes.duracion
        } onChange: {
            avisos.withLock { $0 += 1 }
        }
        ajustes.duracion = 60
        #expect(avisos.withLock { $0 } == 1)
        #expect(ajustes.duracion == 60)
    }

    @Test func unModeloSinArrancarSeLibera() {
        weak var referencia: PartidoEnVivoModelo?
        do {
            let modelo = PartidoEnVivoModelo(partido: Ejemplo.rayoContraToros, msPorMinuto: 1)
            referencia = modelo
            #expect(referencia != nil)
        }
        #expect(referencia == nil)
    }
}
