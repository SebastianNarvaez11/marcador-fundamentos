// El marcador de un partido, compartido por varias tareas a la vez (la pantalla
// que lo lee, el árbitro que anota goles, el narrador…).
//
// Un `actor` es como una clase, pero Swift garantiza que SOLO UNA tarea a la vez
// ejecuta su código: el estado interno queda protegido. Desde fuera, cada
// llamada al actor es `async` y se escribe con `await`, porque puede tener que
// esperar su turno. (En Kotlin se consigue con un `Mutex` de corrutinas, un
// `StateFlow.update { }` o un dispatcher de un solo hilo: el lenguaje no lo impone.)
//
// Se llama `MarcadorSeguro` y no `Marcador` porque `Marcador` ya es la estructura
// de f63 (el valor: 2-1). El actor es la caja que protege UN valor `Marcador`.
public actor MarcadorSeguro {
    public private(set) var marcador = Marcador(local: 0, visitante: 0)

    public init() {}

    // Sin `async` ni `await` dentro: mientras corre, nadie más entra al actor.
    // `marcador` se lee y se escribe sin que otra tarea pueda meterse en medio.
    public func golLocal() {
        marcador = Marcador(local: marcador.local + 1, visitante: marcador.visitante)
    }

    public func golVisitante() {
        marcador = Marcador(local: marcador.local, visitante: marcador.visitante + 1)
    }

    public func registrar(_ evento: EventoDePartido, en partido: Partido) {
        marcador = marcador.despuesDe(evento, en: partido)
    }

    // ---- REENTRANCIA (la trampa de los actores) ----
    //
    // Un actor protege su estado MIENTRAS ejecuta código sin pausas. Pero cada
    // `await` dentro de un método del actor es un punto donde el método se PAUSA
    // y OTRA tarea puede entrar al actor y cambiar `marcador`. Al volver, lo que
    // se leyó antes de pausar puede estar viejo.

    // MAL: lee el marcador ANTES de esperar la revisión del VAR y lo escribe DESPUÉS.
    // Con dos goles a la vez, los dos leen 0 y los dos escriben 1: se pierde un gol.
    public func golLocalConRevisionIncorrecta(msDeRevision: Int) async {
        let golesAntes = marcador.local
        try? await Task.sleep(for: .milliseconds(msDeRevision))
        marcador = Marcador(local: golesAntes + 1, visitante: marcador.visitante)
    }

    // BIEN: después de esperar se vuelve a leer el estado actual.
    public func golLocalConRevision(msDeRevision: Int) async {
        try? await Task.sleep(for: .milliseconds(msDeRevision))
        marcador = Marcador(local: marcador.local + 1, visitante: marcador.visitante)
    }
}
