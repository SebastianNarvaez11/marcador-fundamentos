import Foundation
import Testing
import Torneo
@testable import MarcadorApp

// El caso de uso con DOS falsos: el repositorio de partidos y el servicio. Sin pantalla y sin red.
@MainActor
struct PublicarCronicaTests {

    @Test func publicaElTituloYUnaLineaPorGolDelPartidoUno() async {
        let servicio = ServicioFalso()
        let publicar = PublicarCronica(partidos: RepositorioFalso(), cronicas: RepositorioDeCronicasEnRed(servicio: servicio))
        #expect(await publicar(partidoId: 1) == .success(101))
        // El partido 1 del falso es Rayo FC 2-1 Toros: tres goles (la tarjeta y el cambio no cuentan).
        #expect(servicio.publicadas.first?.title == "Rayo FC 2-1 Toros")
        #expect(servicio.publicadas.first?.body == "12' Ana (Rayo FC)\n55' Iván (Toros)\n80' Ana (Rayo FC)")
    }

    @Test func unPartidoQueNoExisteNoLlegaAlServidor() async {
        let servicio = ServicioFalso()
        let publicar = PublicarCronica(partidos: RepositorioFalso(), cronicas: RepositorioDeCronicasEnRed(servicio: servicio))
        let resultado = await publicar(partidoId: 7)
        #expect(resultado == .failure(.noExisteElPartido(7)))
        // El texto que verá la pantalla.
        #expect(PublicarCronica.Fallo.noExisteElPartido(7).localizedDescription == "No existe el partido 7")
        #expect(servicio.publicadas.isEmpty)
    }
}
