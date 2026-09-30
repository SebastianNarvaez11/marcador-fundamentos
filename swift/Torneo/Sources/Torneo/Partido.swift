// `struct`: tipo de VALOR. Cada variable tiene SU copia; asignar copia. Es el
// gemelo de `data class` en Kotlin, con una diferencia: aquí la mutabilidad la
// decide QUIEN LO GUARDA (`let` o `var`), no el tipo.
public struct Partido: Equatable {
    // `local` y `visitante` son referencias a clases: al copiar el Partido se
    // copian las REFERENCIAS, no los equipos. Los dos partidos comparten Equipo.
    public let local: Equipo
    public let visitante: Equipo
    public private(set) var golesLocal: Int
    public private(set) var golesVisitante: Int

    public init(local: Equipo, visitante: Equipo, golesLocal: Int = 0, golesVisitante: Int = 0) {
        self.local = local
        self.visitante = visitante
        self.golesLocal = golesLocal
        self.golesVisitante = golesVisitante
    }

    // Como en Kotlin: no modifica, DEVUELVE otro partido (el `copy(...)`).
    public func conGolLocal() -> Partido {
        Partido(local: local, visitante: visitante, golesLocal: golesLocal + 1, golesVisitante: golesVisitante)
    }

    public func conGolVisitante() -> Partido {
        Partido(local: local, visitante: visitante, golesLocal: golesLocal, golesVisitante: golesVisitante + 1)
    }

    // `mutating`: un método de un struct que CAMBIA el propio valor. Solo se
    // puede llamar sobre un `var`; sobre un `let` el compilador lo rechaza.
    // En una clase no existe la palabra: cualquier método puede cambiar `self`.
    public mutating func registrarGolLocal() {
        golesLocal += 1
    }

    public mutating func registrarGolVisitante() {
        golesVisitante += 1
    }
}
