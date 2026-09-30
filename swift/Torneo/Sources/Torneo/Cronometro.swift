// Quien quiera enterarse de los minutos sin dar una closure implementa este
// protocolo. `AnyObject` = solo lo pueden adoptar clases, y eso es lo que permite
// guardarlo como `weak` (un struct no tiene contador de referencias).
@MainActor
public protocol DelegadoDelCronometro: AnyObject {
    func cronometro(_ cronometro: Cronometro, llegoAlMinuto minuto: Int)
}

// El cronómetro del partido. Es de la interfaz (marca los minutos que se ven en
// pantalla), por eso `@MainActor` (f65).
//
// Guarda una closure en una propiedad (`alTick`) que llama cada minuto. Ahí está el
// riesgo de memoria de Swift: una closure GUARDADA en un objeto y que CAPTURA a ese
// mismo objeto forma un ciclo de retención: el objeto retiene la closure y la
// closure retiene al objeto. Ninguno llega a contador cero y no se liberan nunca.
// Es el caso que `leaks` sabe ver (un `Task` o un `Timer` vivos, en cambio, no se ven
// como fuga: alguien los sigue apuntando).
@MainActor
public final class Cronometro {
    public private(set) var minuto = 0

    // Se llama cada vez que pasa un minuto. Es una propiedad: la closure se guarda.
    public var alTick: (() -> Void)?

    // Los delegados SIEMPRE `weak`: el delegado suele ser el dueño del objeto que lo
    // llama (una pantalla que tiene su cronómetro), y con una referencia fuerte
    // serían dos dueños mirándose.
    public weak var delegado: (any DelegadoDelCronometro)?

    private var tarea: Task<Void, Never>?

    public init() {}

    public func avanzar() {
        minuto += 1
        alTick?()
        delegado?.cronometro(self, llegoAlMinuto: minuto)
    }

    // ASÍ NO VALE (f66, y el `leaks` de antes lo demostró):
    //
    //     alTick = { aviso(self.minuto) }
    //
    // La closure captura `self` con una referencia FUERTE y `self` guarda la closure
    // en `alTick`: ciclo. Con `[weak self]` la closure mira al cronómetro sin
    // retenerlo; si ya no existe, no hace nada.
    public func avisarCadaMinuto(_ aviso: @escaping (Int) -> Void) {
        alTick = { [weak self] in
            guard let self else { return }
            aviso(self.minuto)
        }
    }

    // Un cronómetro que avanza solo. La `Task` captura `self` con `[weak self]` y
    // vuelve a abrirlo en cada vuelta: si el cronómetro desaparece, el bucle termina.
    // Sin `[weak self]` la tarea mantendría vivo al cronómetro mientras dure (no es
    // un ciclo, así que `leaks` no lo vería, pero no se libera hasta cancelarla).
    public func arrancar(msPorMinuto: Int) {
        tarea?.cancel()
        tarea = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(msPorMinuto))
                guard let self, !Task.isCancelled else { return }
                self.avanzar()
            }
        }
    }

    public func detener() {
        tarea?.cancel()
        tarea = nil
    }
}
