import Testing
import Torneo
@testable import MarcadorApp

// Pruebas del directo: el reloj, los goles del guion, los avisos y la memoria. Antes eran del
// ViewModel; se mudaron con el código. `@MainActor`: el directo lo es.
@MainActor
struct DirectoTests {

    private func directo(
        duracion: Int = 90, notificador: (any Notificador)? = nil, msPorMinuto: Int = 0, ciclo: Bool = false
    ) -> Directo {
        Directo(
            partidoId: 1, partido: Ejemplo.rayoContraToros, duracion: duracion,
            notificador: notificador, msPorMinuto: msPorMinuto, reintroducirElCiclo: ciclo
        )
    }

    @Test func jugarAplicaLosGolesDelGuion() async {
        let directo = directo()
        await directo.jugar()
        #expect(directo.marcador == Marcador(local: 2, visitante: 1))
        #expect(directo.minuto == 90)
        #expect(!directo.corriendo)
        #expect(directo.terminado)
        #expect(directo.ultimoAviso == "minuto 90 con 2-1")
    }

    @Test func unPartidoDeSesentaMinutosIgnoraElGolDelMinutoOchenta() async {
        let directo = directo(duracion: 60)
        await directo.jugar()
        #expect(directo.marcador == Marcador(local: 1, visitante: 1))
    }

    @Test func cancelarLaTareaDetieneElPartido() async {
        let directo = directo(msPorMinuto: 20)
        let tarea = Task { await directo.jugar() }
        try? await Task.sleep(for: .milliseconds(150))
        tarea.cancel()
        await tarea.value
        #expect(directo.minuto < 90)
        #expect(!directo.corriendo)
    }

    // LA PRUEBA DEL CICLO. Con `[weak self]` el directo se libera en cuanto se suelta...
    @Test func elDirectoSeLiberaAlSoltarlo() async {
        weak var referencia: Directo?
        do {
            let directo = directo(duracion: 3)
            referencia = directo
            await directo.jugar()
        }
        #expect(referencia == nil)
    }

    // ...y con el ciclo reintroducido, NO: `referencia` sigue viva aunque nadie tenga el directo.
    // (Este objeto se queda en memoria hasta que termina el proceso de pruebas: es la fuga.)
    @Test func conElCicloReintroducidoElDirectoSeQuedaEnMemoria() async {
        weak var referencia: Directo?
        do {
            let directo = directo(duracion: 3, ciclo: true)
            referencia = directo
            await directo.jugar()
        }
        #expect(referencia != nil)
    }

    // Cada gol del guion avisa: goles de Ana (12), Iván (55) y Ana (80).
    @Test func cadaGolDelGuionAvisaAlNotificador() async {
        let espia = NotificadorEspia()
        let directo = directo(notificador: espia)
        await directo.jugar()
        #expect(espia.avisos.map(\.minuto) == [12, 55, 80])
        #expect(espia.avisos.map(\.jugador) == ["Ana", "Iván", "Ana"])
        #expect(espia.avisos.first?.equipo == "Rayo FC")
    }

    // Los goles «a mano» no avisan, y un partido de 60 minutos no avisa del gol del minuto 80.
    @Test func losGolesAManoNoAvisanYLosPosterioresALaDuracionTampoco() async {
        let espia = NotificadorEspia()
        let directo = directo(duracion: 60, notificador: espia)
        directo.golAMano(.local)
        #expect(espia.avisos.isEmpty)
        await directo.jugar()
        #expect(espia.avisos.map(\.minuto) == [12, 55])
    }
}
