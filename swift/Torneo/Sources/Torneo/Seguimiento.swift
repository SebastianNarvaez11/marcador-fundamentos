// `@MainActor`: esta clase entera vive en el ACTOR PRINCIPAL, que es el hilo de la
// interfaz. Todo lo que toca (propiedades y métodos) solo se puede usar desde
// allí, y el compilador lo comprueba. Es la forma de decir «esto es estado de
// pantalla»: en la app de iOS será lo que lee SwiftUI. En Android el
// papel lo hacía `Dispatchers.Main` + `viewModelScope`.
//
// Aquí no hay pantalla: la clase solo guarda lo que una pantalla mostraría
// (marcador y narración) y lo va actualizando a medida que el partido avanza.
@MainActor
public final class SeguimientoEnPantalla {
    public private(set) var marcador = Marcador(local: 0, visitante: 0)
    public private(set) var narracion: [String] = []
    private var tarea: Task<Void, Never>?

    public init() {}

    // Una `Task { }` creada dentro de un método `@MainActor` HEREDA el aislamiento:
    // su cuerpo también corre en el actor principal, así que puede tocar
    // `marcador` sin `await`. (`Task.detached` NO hereda nada.)
    //
    // Cada `for await` se pausa entre valores y suelta el hilo principal: la
    // interfaz no se bloquea mientras espera el siguiente gol.
    public func seguir(_ partido: PartidoEnVivo) {
        tarea?.cancel()
        marcador = Marcador(local: 0, visitante: 0)
        narracion = []
        let guion = partido.guion
        tarea = Task {
            for await evento in partido.eventos {
                self.narracion.append(describir(evento))
                self.marcador = self.marcador.despuesDe(evento, en: guion)
            }
        }
    }

    // Detener el seguimiento cancela la tarea, y con ella los dos streams.
    public func detener() {
        tarea?.cancel()
        tarea = nil
    }

    public func esperarAlFinal() async {
        await tarea?.value
    }

    // `nonisolated`: esta función no toca estado de la clase, así que se puede
    // llamar desde cualquier sitio sin `await`.
    public nonisolated func titular(_ marcador: Marcador) -> String {
        "Marcador: \(marcador)"
    }
}
