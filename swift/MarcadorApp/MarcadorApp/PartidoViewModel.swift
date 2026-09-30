import Foundation
import Observation
import Torneo

// f75 · MVVM EN SWIFTUI (gemelo del `PartidoViewModel` de Android, f39)
//
//   Vista (SwiftUI)  ──eventos──▶  ViewModel  ──llama──▶  caso de uso / repositorio  ──▶  datos
//        ▲                            │
//        └──── estado (`uiState`) ────┘
//
// La VISTA solo pinta `uiState` y traduce toques en llamadas al ViewModel. El VIEWMODEL sabe de
// pantalla (qué se ve, en qué orden) pero NO de SwiftUI: no importa `SwiftUI`. Por eso se puede
// probar sin dibujar nada (f79).
//
// `@MainActor`: el ViewModel es estado de INTERFAZ y SwiftUI lo lee desde el hilo principal. Con
// esta anotación el compilador impide tocarlo desde otro hilo sin `await` (f65). En un proyecto de
// Xcode 26 el aislamiento a MainActor ya es el valor por defecto de TODO el módulo, así que aquí es
// redundante; se escribe igualmente porque dice la intención y porque en el paquete `Torneo` (que no
// lo tiene por defecto) sí haría falta. Comparado con Android: `Dispatchers.Main` + `viewModelScope`.
@MainActor
@Observable
final class PartidoViewModel {
    let partidoId: Int

    // ---- estado (lo lee la vista; solo este ViewModel lo escribe) ----
    private(set) var partido: Partido?
    private(set) var mensajeDeError: String?
    private(set) var minuto = 0
    private(set) var corriendo = false
    private(set) var ultimoAviso = "ninguno"
    private(set) var duracion = 90
    // Los goles que va marcando el guion y los que se ponen «a mano» con los botones.
    private(set) var golesEnVivo = Marcador(local: 0, visitante: 0)
    private(set) var golesAMano = Marcador(local: 0, visitante: 0)

    // ---- dependencias (llegan por el constructor: nadie las crea a escondidas) ----
    @ObservationIgnored private let repositorio: any PartidosRepositorio
    @ObservationIgnored private let registrarGol: RegistrarGol
    @ObservationIgnored private let msPorMinuto: Int
    @ObservationIgnored private let reintroducirElCiclo: Bool

    // EL DUEÑO del cronómetro es este ViewModel (referencia FUERTE). Ojo a la flecha inversa: el
    // cronómetro guarda una closure (`alTick`) y esa closure llama al ViewModel. Si la closure captura
    // `self` con una referencia fuerte, hay DOS flechas fuertes que se miran: un ciclo (mira `prepararReloj`).
    @ObservationIgnored private var cronometro: Cronometro?
    @ObservationIgnored private var eventosDelGuion: [EventoDePartido] = []

    init(
        partidoId: Int,
        repositorio: any PartidosRepositorio,
        msPorMinuto: Int = Configuracion.msPorMinuto,
        reintroducirElCiclo: Bool = Configuracion.cicloDelCronometro
    ) {
        self.partidoId = partidoId
        self.repositorio = repositorio
        self.registrarGol = RegistrarGol(repositorio: repositorio)
        self.msPorMinuto = msPorMinuto
        self.reintroducirElCiclo = reintroducirElCiclo
        cargar()
    }

    // `deinit` sale en el registro cuando ARC libera el ViewModel. Si NO sale al cerrar la pantalla,
    // algo lo retiene (f66): es la prueba más barata de que no hay una fuga.
    deinit {
        Registro.anotar("PartidoViewModel LIBERADO")
    }

    // ---- lo que ve la pantalla ----
    var marcador: Marcador {
        Marcador(local: golesEnVivo.local + golesAMano.local, visitante: golesEnVivo.visitante + golesAMano.visitante)
    }

    var uiState: MarcadorUiState {
        if let mensajeDeError { return .error(mensajeDeError) }
        guard let partido else { return .cargando }
        return .exito(.init(
            partidoId: partidoId, partido: partido, marcador: marcador, minuto: minuto,
            corriendo: corriendo, ultimoAviso: ultimoAviso, duracion: duracion
        ))
    }

    // ---- eventos de la pantalla ----
    func alGol(_ lado: Lado) {
        switch lado {
        case .local: golesAMano = Marcador(local: golesAMano.local + 1, visitante: golesAMano.visitante)
        case .visitante: golesAMano = Marcador(local: golesAMano.local, visitante: golesAMano.visitante + 1)
        }
        // Se ve al instante en el directo Y se guarda con el caso de uso.
        if case let .failure(fallo) = registrarGol(partidoId: partidoId, lado: lado, minuto: minuto) {
            Registro.anotar("no se pudo registrar el gol: \(fallo)")
        }
    }

    // Cargar el partido del repositorio. Se llama al crear y cada vez que se reinicia el partido.
    private func cargar() {
        if let encontrado = repositorio.partido(id: partidoId) {
            partido = encontrado
            mensajeDeError = nil
        } else {
            partido = nil
            mensajeDeError = "No existe el partido n.º \(partidoId)"
        }
    }

    // La vista la llama desde `.task(id: duración)` (f74): se cancela sola al irse la pantalla.
    // UN SOLO BUCLE marca el tiempo y el cronómetro solo cuenta lo que le mandan.
    //
    // f76: `.task` se CANCELA cuando otra pantalla se apila encima (Goleadores) y se RELANZA al volver,
    // pero el ViewModel sigue vivo (su `@State` sobrevive). Por eso `jugar` no puede empezar siempre de
    // cero: si ya había un partido en marcha con esta duración, RETOMA desde el minuto actual (sin
    // borrar goles ni volver a avisar de los que ya ocurrieron); si ya había terminado, no hace nada.
    // Solo empieza de nuevo cuando es la primera vez o cambia la duración. En Android no hace falta:
    // el ViewModel de la entrada sigue vivo y su corrutina no se cancela al apilar otra pantalla.
    func jugar(duracion nueva: Int) async {
        cargar()
        guard let partido else { return }
        let reloj: Cronometro
        if let vivo = cronometro, duracion == nueva {
            if vivo.minuto >= nueva { return }   // ya terminó: volver a la pantalla no lo relanza
            reloj = vivo                          // en marcha (o pausado por la navegación): sigue donde iba
        } else {
            duracion = nueva
            minuto = 0
            golesEnVivo = Marcador(local: 0, visitante: 0)
            golesAMano = Marcador(local: 0, visitante: 0)
            ultimoAviso = "ninguno"
            eventosDelGuion = partido.eventos.filter { $0.minuto <= nueva }
            reloj = Cronometro()
            cronometro = reloj
            prepararReloj(reloj)
        }
        corriendo = true
        while reloj.minuto < nueva {
            do {
                try await Task.sleep(for: .milliseconds(msPorMinuto))
            } catch {
                Registro.anotar("partido CANCELADO en el minuto \(minuto)")
                corriendo = false
                return
            }
            reloj.avanzar()   // llama a `alTick`, que llama a `alMinuto()`
        }
        corriendo = false
        Registro.anotar("partido TERMINADO en el minuto \(minuto)")
    }

    // EL CICLO DE RETENCIÓN (f66) EN UNA APP DE VERDAD.
    //
    //   ASÍ NO VALE:        reloj.alTick = { self.alMinuto() }
    //   BIEN:               reloj.alTick = { [weak self] in self?.alMinuto() }
    //
    // `alTick` es una propiedad del cronómetro: la closure se GUARDA. Si captura `self` fuerte, el
    // ViewModel retiene al cronómetro (`cronometro`) y el cronómetro retiene al ViewModel (dentro de la
    // closure): ninguno llega a contador cero, `deinit` no sale nunca y el ViewModel de cada partido
    // visitado se queda en memoria para siempre. Ni `Task` ni `Timer` vivos son una fuga: alguien los
    // sigue apuntando y se ven en el Memory Graph, pero `leaks` no los marca. Este sí lo marca.
    //
    // (El parámetro `reintroducirElCiclo` y el argumento de lanzamiento `-cicloDelCronometro YES`
    // existen SOLO para esta lección: vuelven a meter el bug para verlo en el Memory Graph Debugger.
    // En una app real no existiría ese `if`.)
    private func prepararReloj(_ reloj: Cronometro) {
        if reintroducirElCiclo {
            reloj.alTick = { self.alMinuto() }
        } else {
            reloj.alTick = { [weak self] in self?.alMinuto() }
        }
    }

    private func alMinuto() {
        guard let reloj = cronometro, let partido else { return }
        let nuevo = reloj.minuto
        for evento in eventosDelGuion where evento.minuto == nuevo {
            golesEnVivo = golesEnVivo.despuesDe(evento, en: partido)
        }
        minuto = nuevo
        if nuevo % 15 == 0 { ultimoAviso = "minuto \(nuevo) con \(marcador)" }
    }
}
