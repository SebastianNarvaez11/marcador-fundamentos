import Foundation
import Observation

// EL CONTENEDOR: la RAÍZ DE COMPOSICIÓN de la app (gemelo de `AppContainer` en Android).
//
// Es el ÚNICO sitio que crea las piezas y las conecta entre sí. Cada pieza RECIBE lo que necesita
// por su `init` (inyección de dependencias) en vez de BUSCARLO o crearlo a escondidas. Así se crea
// cada pieza UNA sola vez, y en las pruebas basta con pasar otras (falsas).
//
// Está ordenado por capas, de abajo arriba:
//   red         el servicio (con su única `URLSession`)
//   datos       los repositorios, cada uno con su almacén en el disco
//   dominio     los casos de uso
//   interfaz    una fábrica por pantalla: cada pantalla recibe un ViewModel NUEVO
//
// Alcances: el contenedor y lo que guarda viven lo que la app (un `@State` de `MarcadorApp`); cada
// ViewModel vive lo que su pantalla (un `@State` de su vista).
//
// `@Observable`: para poder repartirlo por el entorno con `.environment(contenedor)`.
@MainActor
@Observable
final class ContenedorDeLaApp {
    // ---- red ----
    let servicio: any ServicioDelTorneo

    // ---- datos ----
    let partidos: RepositorioDePartidos
    let arbitros: RepositorioDeArbitros
    let cronicas: any CronicasRepositorio

    // ---- dominio ----
    let publicarCronica: PublicarCronica

    // ---- sistema ----
    let notificador: NotificadorDeSistema

    // Sin valores por defecto: quien crea el contenedor decide el servidor y dónde se guarda
    // (la app pasa los almacenes de `Documents`; las vistas previas, `nil`, para no tocar el disco).
    init(servicio: any ServicioDelTorneo, almacenDelTorneo: AlmacenDelTorneo?, almacenDeArbitros: AlmacenDeArbitros?) {
        self.servicio = servicio
        partidos = RepositorioDePartidos(almacen: almacenDelTorneo)
        arbitros = RepositorioDeArbitros(servicio: servicio, almacen: almacenDeArbitros)
        cronicas = RepositorioDeCronicasEnRed(servicio: servicio)
        publicarCronica = PublicarCronica(partidos: partidos, cronicas: cronicas)
        notificador = NotificadorDeSistema()
    }

    // ---- interfaz: un ViewModel por pantalla ----
    func partidoViewModel(id: Int) -> PartidoViewModel {
        PartidoViewModel(partidoId: id, repositorio: partidos, notificador: notificador)
    }

    func arbitrosViewModel() -> ArbitrosViewModel {
        ArbitrosViewModel(repositorio: arbitros)
    }

    func cronicaViewModel(id: Int) -> CronicaViewModel {
        CronicaViewModel(partidoId: id, publicarCronica: publicarCronica)
    }
}
