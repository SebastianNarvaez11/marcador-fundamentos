import Foundation
import Observation
import Torneo

// EL DIRECTO: un partido que se está jugando (gemelo del `Directo` de Android).
//
// Antes todo esto vivía dentro de `PartidoViewModel`: el reloj, los goles del guion, los «a mano» y
// el aviso de cada 15'. Se saca a su propia clase para que el partido no tenga que ser de UNA
// pantalla: un objeto que viva lo que la app puede guardarlo, y varias pantallas mirarlo a la vez.
// El ViewModel se queda fino: traduce este directo a lo que pinta la vista.
//
// No importa `SwiftUI`: se prueba sin dibujar nada. `@Observable`: cuando cambia el minuto o un gol,
// se redibujan las vistas que lo leen (aunque lo lean a través del ViewModel).
@MainActor
@Observable
final class Directo {
    let partidoId: Int
    let partido: Partido
    let duracion: Int

    private(set) var minuto = 0
    private(set) var corriendo = false
    private(set) var ultimoAviso = "ninguno"
    // Los goles que va marcando el guion y los que se ponen «a mano» con los botones.
    private(set) var golesEnVivo = Marcador(local: 0, visitante: 0)
    private(set) var golesAMano = Marcador(local: 0, visitante: 0)

    // Quien avisa de los goles (nil = nadie). Es un protocolo: en las pruebas, un espía.
    @ObservationIgnored private let notificador: (any Notificador)?
    @ObservationIgnored private let msPorMinuto: Int
    @ObservationIgnored private let eventosDelGuion: [EventoDePartido]

    // EL DUEÑO del cronómetro es este directo (referencia FUERTE). Ojo a la flecha inversa: el
    // cronómetro guarda una closure (`alTick`) y esa closure llama al directo. Si la closure captura
    // `self` con una referencia fuerte, hay DOS flechas fuertes que se miran: un ciclo (mira `prepararReloj`).
    @ObservationIgnored private let cronometro = Cronometro()

    init(
        partidoId: Int,
        partido: Partido,
        duracion: Int,
        notificador: (any Notificador)? = nil,
        msPorMinuto: Int = Configuracion.msPorMinuto,
        reintroducirElCiclo: Bool = Configuracion.cicloDelCronometro
    ) {
        self.partidoId = partidoId
        self.partido = partido
        self.duracion = duracion
        self.notificador = notificador
        self.msPorMinuto = msPorMinuto
        // Un partido de 60 minutos ignora los goles del guion posteriores al 60.
        eventosDelGuion = partido.eventos.filter { $0.minuto <= duracion }
        prepararReloj(reintroducirElCiclo: reintroducirElCiclo)
    }

    // `deinit` sale en el registro cuando ARC libera el directo. Si NO sale al cerrarlo, algo lo
    // retiene: es la prueba más barata de que no hay una fuga.
    deinit {
        Registro.anotar("Directo LIBERADO")
    }

    var marcador: Marcador {
        Marcador(local: golesEnVivo.local + golesAMano.local, visitante: golesEnVivo.visitante + golesAMano.visitante)
    }

    var terminado: Bool { minuto >= duracion }

    func golAMano(_ lado: Lado) {
        switch lado {
        case .local: golesAMano = Marcador(local: golesAMano.local + 1, visitante: golesAMano.visitante)
        case .visitante: golesAMano = Marcador(local: golesAMano.local, visitante: golesAMano.visitante + 1)
        }
    }

    // UN SOLO BUCLE marca el tiempo y el cronómetro solo cuenta lo que le mandan. Si lo cancelan,
    // se para donde iba; si se vuelve a llamar, SIGUE desde ese minuto (no borra goles ni repite
    // avisos). Si ya terminó, no hace nada.
    func jugar() async {
        guard !corriendo, !terminado else { return }
        corriendo = true
        while cronometro.minuto < duracion {
            do {
                try await Task.sleep(for: .milliseconds(msPorMinuto))
            } catch {
                Registro.anotar("partido CANCELADO en el minuto \(minuto)")
                corriendo = false
                return
            }
            cronometro.avanzar()   // llama a `alTick`, que llama a `alMinuto()`
        }
        corriendo = false
        Registro.anotar("partido TERMINADO en el minuto \(minuto)")
    }

    // EL CICLO DE RETENCIÓN EN UNA APP DE VERDAD.
    //
    //   ASÍ NO VALE:        reloj.alTick = { self.alMinuto() }
    //   BIEN:               reloj.alTick = { [weak self] in self?.alMinuto() }
    //
    // `alTick` es una propiedad del cronómetro: la closure se GUARDA. Si captura `self` fuerte, el
    // directo retiene al cronómetro y el cronómetro retiene al directo (dentro de la closure): ninguno
    // llega a contador cero, `deinit` no sale nunca y el directo de cada partido visitado se queda en
    // memoria para siempre. Se prepara en el `init`: basta con abrir un partido para que el ciclo exista.
    //
    // (El parámetro `reintroducirElCiclo` y el argumento de lanzamiento `-cicloDelCronometro YES`
    // existen SOLO para la lección del Memory Graph: vuelven a meter el bug para verlo. En una app real
    // no existiría ese `if`.)
    private func prepararReloj(reintroducirElCiclo: Bool) {
        if reintroducirElCiclo {
            cronometro.alTick = { self.alMinuto() }
        } else {
            cronometro.alTick = { [weak self] in self?.alMinuto() }
        }
    }

    private func alMinuto() {
        let nuevo = cronometro.minuto
        for evento in eventosDelGuion where evento.minuto == nuevo {
            golesEnVivo = golesEnVivo.despuesDe(evento, en: partido)
            // Cada gol del guion avisa (los «a mano» no: los puso el usuario, que ya lo sabe).
            if case let .gol(minutoDelGol, jugador, equipo) = evento {
                notificador?.notificarGol(minuto: minutoDelGol, jugador: jugador.nombre, equipo: equipo.nombre)
            }
        }
        minuto = nuevo
        if nuevo % 15 == 0 { ultimoAviso = "minuto \(nuevo) con \(marcador)" }
    }
}
