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
//
// LA PANTALLA OBSERVA, NO ES LA DUEÑA. El partido ya no lo juega un `.task` de la vista (moriría al
// cerrarla): lo empieza `PartidoEnJuego`, el estado de toda la app. Si el partido de esta pantalla ya
// está en juego (o acaba de terminar), el ViewModel REUTILIZA ese directo en vez de crear otro.
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
    // El partido en juego de la app. Es `@Observable`: leerlo en `uiState` basta para que la vista se
    // entere cuando empieza o termina OTRO partido.
    @ObservationIgnored private let enJuego: PartidoEnJuego

    init(
        partidoId: Int,
        repositorio: any PartidosRepositorio,
        partidoEnJuego: PartidoEnJuego,
        duracion: Int = 90,
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
        self.enJuego = partidoEnJuego
        if let vivo = partidoEnJuego.directo(id: partidoId) {
            directo = vivo
        } else {
            cargar(duracion: duracion)
        }
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
            corriendo: directo.corriendo, ultimoAviso: directo.ultimoAviso, duracion: directo.duracion,
            otroEnJuego: otroEnJuego
        ))
    }

    // El id de OTRO partido sin terminar (nil si no hay). Entonces este no se puede empezar.
    private var otroEnJuego: Int? {
        guard let vivo = enJuego.directo, vivo.partidoId != partidoId, !vivo.terminado else { return nil }
        return vivo.partidoId
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

    // «Empezar»: lo empieza el partido en juego, no la vista. Si hay otro sin terminar, no hace nada
    // (la vista ni siquiera enseña el botón: enseña el aviso).
    func empezar() {
        guard let directo else { return }
        _ = enJuego.empezar(directo)
    }

    // Otra duración = un directo NUEVO sin empezar. Mientras este partido se juega, no (la vista
    // desactiva el selector y aquí se ignora).
    func cambiarDuracion(_ nueva: Int) {
        guard let directo, directo.duracion != nueva else { return }
        if let vivo = enJuego.directo(id: partidoId), !vivo.terminado { return }
        cargar(duracion: nueva)
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
