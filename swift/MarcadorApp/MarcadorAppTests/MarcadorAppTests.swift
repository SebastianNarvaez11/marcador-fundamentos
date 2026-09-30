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
}
