import Testing
import Torneo
@testable import MarcadorApp

// El texto de la barra es una función normal: se prueba sin pintar nada.
struct BarraDelPartidoEnJuegoTests {
    @Test func enJuegoMuestraElMinuto() {
        let texto = textoDeLaBarra(local: "Rayo FC", visitante: "Toros", marcador: Marcador(local: 2, visitante: 1), minuto: 34, terminado: false)
        #expect(texto == "Rayo FC 2–1 Toros · 34'")
    }

    @Test func alTerminarMuestraFinal() {
        let texto = textoDeLaBarra(local: "Rayo FC", visitante: "Toros", marcador: Marcador(local: 3, visitante: 2), minuto: 90, terminado: true)
        #expect(texto == "Final · Rayo FC 3–2 Toros")
    }
}
