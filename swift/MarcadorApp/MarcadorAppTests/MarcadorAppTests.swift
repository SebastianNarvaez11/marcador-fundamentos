import Observation
import Synchronization
import Testing
import Torneo
@testable import MarcadorApp

// El modelo es de la app y la app aísla todo a MainActor por defecto; el target de pruebas NO:
// hay que decirlo a mano.
@MainActor
struct MarcadorAppTests {

    // El asistente de Xcode genera un `example()` vacío. Éste comprueba lo mismo que
    // importa aquí: que el paquete `Torneo` llega hasta el target de pruebas de la app.
    @Test func elPaqueteTorneoLlegaALaApp() {
        let partido = Ejemplo.rayoContraToros
        #expect(partido.marcador() == Marcador(local: 2, visitante: 1))
    }

    // Un gol no modifica el marcador de antes: devuelve otro, y el de antes sigue igual.
    @Test func sumarUnGolCreaOtroMarcador() {
        let partido = Ejemplo.rayoContraToros
        let antes = Marcador(local: 0, visitante: 0)
        // Un gol del Rayo FC (el local) en el minuto 12.
        let gol = EventoDePartido.gol(minuto: 12, jugador: Ejemplo.ana, equipo: partido.local)
        let despues = antes.despuesDe(gol, en: partido)
        #expect(antes == Marcador(local: 0, visitante: 0))     // el de antes no cambió
        #expect(despues == Marcador(local: 1, visitante: 0))   // el nuevo lleva el gol
        #expect(despues != antes)                              // y son dos marcadores distintos
    }

    // Los datos de ejemplo son los mismos que en Android: 18 partidos con id único.
    @Test func losDatosDeEjemploTienenDieciochoPartidosConIdUnico() {
        let partidos = DatosDeEjemplo.partidos
        #expect(partidos.count == 18)
        #expect(Set(partidos.map(\.id)).count == 18)
        #expect(partidos.first?.id == 1)
    }

    // `@Observable` avisa cuando cambia lo que se LEYÓ dentro de `withObservationTracking`.
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

    // El partido ENTERO con un minuto de 0 ms: los goles del guion suben el marcador.
    @Test func jugarAplicaLosGolesDelGuion() async {
        let modelo = PartidoEnVivoModelo(partido: Ejemplo.rayoContraToros, msPorMinuto: 0)
        await modelo.jugar(duracion: 90)
        #expect(modelo.minuto == 90)
        #expect(modelo.marcador == Marcador(local: 2, visitante: 1))
        #expect(!modelo.corriendo)
        #expect(modelo.ultimoAviso == "minuto 90 con 2-1")
    }

    // Un partido de 60 minutos ignora el gol del minuto 80.
    @Test func unPartidoDeSesentaMinutosIgnoraLosGolesPosteriores() async {
        let modelo = PartidoEnVivoModelo(partido: Ejemplo.rayoContraToros, msPorMinuto: 0)
        await modelo.jugar(duracion: 60)
        #expect(modelo.marcador == Marcador(local: 1, visitante: 1))
    }

    @Test func losGolesAManoSeSumanAlDelGuion() async {
        let modelo = PartidoEnVivoModelo(partido: Ejemplo.rayoContraToros, msPorMinuto: 0)
        modelo.golAMano(.local)
        modelo.golAMano(.visitante)
        modelo.golAMano(.visitante)
        #expect(modelo.marcador == Marcador(local: 1, visitante: 2))
    }

    // Cancelar la tarea detiene el bucle: es lo que hace SwiftUI con `.task` al irse la vista.
    @Test func cancelarLaTareaDetieneElPartido() async {
        let modelo = PartidoEnVivoModelo(partido: Ejemplo.rayoContraToros, msPorMinuto: 20)
        let tarea = Task { await modelo.jugar(duracion: 90) }
        try? await Task.sleep(for: .milliseconds(150))
        tarea.cancel()
        await tarea.value
        #expect(modelo.minuto < 90)
        #expect(!modelo.corriendo)
    }
}
