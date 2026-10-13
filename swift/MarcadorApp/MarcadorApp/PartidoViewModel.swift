import Foundation
import Observation
import Torneo

// MVVM EN SWIFTUI (gemelo del `PartidoViewModel` de Android)
//
//   Vista (SwiftUI)  ──eventos──▶  ViewModel  ──llama──▶  caso de uso / repositorio  ──▶  datos
//        ▲                            │
//        └──── estado (`uiState`) ────┘
//
// La VISTA solo pinta `uiState` y traduce toques en llamadas al ViewModel. El VIEWMODEL sabe de
// pantalla (qué se ve, en qué orden) pero NO de SwiftUI: no importa `SwiftUI`. Por eso se puede
// probar sin dibujar nada.
//
// `@MainActor`: el ViewModel es estado de INTERFAZ y SwiftUI lo lee desde el hilo principal. Con
// esta anotación el compilador impide tocarlo desde otro hilo sin `await`. En un proyecto de
// Xcode 26 el aislamiento a MainActor ya es el valor por defecto de TODO el módulo, así que aquí es
// redundante; se escribe igualmente porque dice la intención y porque en el paquete `Torneo` (que no
// lo tiene por defecto) sí haría falta. Comparado con Android: `Dispatchers.Main` + `viewModelScope`.
//
// EL VIEWMODEL, FINO. El reloj, los goles y el aviso viven en `Directo`; este ViewModel lo crea, lo
// traduce a `uiState` y le pasa los toques. Las vistas siguen sin saber que existe el directo.
@MainActor
@Observable
final class PartidoViewModel {
    let partidoId: Int

    // ---- estado (lo lee la vista; solo este ViewModel lo escribe) ----
    private(set) var directo: Directo?
    private(set) var mensajeDeError: String?

    // ---- dependencias (llegan por el constructor: nadie las crea a escondidas) ----
    @ObservationIgnored private let repositorio: any PartidosRepositorio
    @ObservationIgnored private let registrarGol: RegistrarGol
    @ObservationIgnored private let msPorMinuto: Int
    @ObservationIgnored private let reintroducirElCiclo: Bool
    // Quien avisa de los goles (nil = nadie). Se lo pasa al directo.
    @ObservationIgnored private let notificador: (any Notificador)?

    init(
        partidoId: Int,
        repositorio: any PartidosRepositorio,
        notificador: (any Notificador)? = nil,
        msPorMinuto: Int = Configuracion.msPorMinuto,
        reintroducirElCiclo: Bool = Configuracion.cicloDelCronometro
    ) {
        self.partidoId = partidoId
        self.repositorio = repositorio
        self.registrarGol = RegistrarGol(repositorio: repositorio)
        self.notificador = notificador
        self.msPorMinuto = msPorMinuto
        self.reintroducirElCiclo = reintroducirElCiclo
        cargar(duracion: Ajustes.duracionPorDefecto)
    }

    // `deinit` sale en el registro cuando ARC libera el ViewModel.
    deinit {
        Registro.anotar("PartidoViewModel LIBERADO")
    }

    // ---- lo que ve la pantalla ----
    var uiState: MarcadorUiState {
        if let mensajeDeError { return .error(mensajeDeError) }
        guard let directo else { return .cargando }
        return .exito(.init(
            partidoId: partidoId, partido: directo.partido, marcador: directo.marcador, minuto: directo.minuto,
            corriendo: directo.corriendo, ultimoAviso: directo.ultimoAviso, duracion: directo.duracion
        ))
    }

    // ---- eventos de la pantalla ----
    func alGol(_ lado: Lado) {
        guard let directo else { return }
        // Se ve al instante en el directo Y se guarda con el caso de uso.
        directo.golAMano(lado)
        if case let .failure(fallo) = registrarGol(partidoId: partidoId, lado: lado, minuto: directo.minuto) {
            Registro.anotar("no se pudo registrar el gol: \(fallo)")
        }
    }

    // La vista la llama desde `.task(id: duración)`: se cancela sola al irse la pantalla.
    // `.task` se CANCELA cuando otra pantalla se apila encima (Goleadores) y se RELANZA al volver, pero el
    // ViewModel (y su directo) siguen vivos: si la duración no cambió, `jugar` SIGUE donde iba. Solo hace
    // un directo nuevo si cambia la duración.
    func jugar(duracion nueva: Int) async {
        if directo?.duracion != nueva { cargar(duracion: nueva) }
        await directo?.jugar()
    }

    // Leer el partido del repositorio y preparar un directo sin empezar.
    private func cargar(duracion: Int) {
        guard let partido = repositorio.partido(id: partidoId) else {
            directo = nil
            mensajeDeError = "No existe el partido n.º \(partidoId)"
            return
        }
        mensajeDeError = nil
        directo = Directo(
            partidoId: partidoId, partido: partido, duracion: duracion,
            notificador: notificador, msPorMinuto: msPorMinuto, reintroducirElCiclo: reintroducirElCiclo
        )
    }
}
