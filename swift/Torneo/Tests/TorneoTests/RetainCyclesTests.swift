import Testing
@testable import Torneo

// ASÍ NO VALE: el cronómetro con el error. La closure guardada en `alTick` captura
// `self` con una referencia fuerte y `self` retiene la closure: ciclo. Es el mismo
// código que tuvo `Cronometro.avisarCadaMinuto` antes de f66, y el que `leaks`
// detectó como `ROOT CYCLE: <Cronometro>` (ver el diario).
private final class CronometroConCiclo {
    var minuto = 0
    var alTick: (() -> Void)?

    func avisarCadaMinuto(_ aviso: @escaping (Int) -> Void) {
        alTick = { aviso(self.minuto) }   // captura fuerte de self
    }
}

// Una tarea que captura `self` sin `[weak self]`. No es un ciclo (la tarea no la
// guarda nadie salvo el propio objeto, y la tarea termina al cancelarla), pero mientras
// el bucle esté vivo el objeto NO se libera, y `leaks` no lo vería como fuga.
@MainActor
private final class CronometroConTareaFuerte {
    var minuto = 0
    var tarea: Task<Void, Never>?

    func arrancar() {
        tarea = Task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(5))
                self.minuto += 1   // `self` fuerte durante toda la vida de la tarea
            }
        }
    }
}

@MainActor
private final class DelegadoQueCuenta: DelegadoDelCronometro {
    var minutosVistos: [Int] = []
    func cronometro(_ cronometro: Cronometro, llegoAlMinuto minuto: Int) {
        minutosVistos.append(minuto)
    }
}

@MainActor
struct RetainCyclesTests {
    // ---- El bug ----

    @Test func asiNoVale_laClosureQueCapturaSelfFormaUnCiclo() {
        weak var cronometroDebil: CronometroConCiclo?
        do {
            let cronometro = CronometroConCiclo()
            cronometro.avisarCadaMinuto { _ in }
            cronometroDebil = cronometro
        }   // sale del alcance...
        // ...pero sigue vivo: se retiene a sí mismo a través de `alTick`. FUGA.
        #expect(cronometroDebil != nil)
        cronometroDebil?.alTick = nil   // se rompe el ciclo a mano para no dejar basura
        #expect(cronometroDebil == nil)
    }

    // ---- El arreglo ----

    @Test func conWeakSelfElCronometroSeLibera() {
        weak var cronometroDebil: Cronometro?
        do {
            let cronometro = Cronometro()
            cronometro.avisarCadaMinuto { _ in }
            cronometroDebil = cronometro
            #expect(cronometroDebil != nil)
        }
        #expect(cronometroDebil == nil)
    }

    @Test func elAvisoSeLlamaConCadaMinuto() {
        let cronometro = Cronometro()
        var minutos: [Int] = []
        cronometro.avisarCadaMinuto { minutos.append($0) }
        for _ in 1...3 { cronometro.avanzar() }
        #expect(minutos == [1, 2, 3])
        #expect(cronometro.minuto == 3)
    }

    @Test func siElCronometroYaNoExisteElAvisoNoHaceNada() {
        var minutos: [Int] = []
        var cronometro: Cronometro? = Cronometro()
        cronometro?.avisarCadaMinuto { minutos.append($0) }
        let alTick = cronometro?.alTick   // una copia de la closure, como la guardaría otro
        cronometro = nil
        alTick?()   // `[weak self]` -> `guard let self else { return }`: sin cronómetro, no avisa
        #expect(minutos.isEmpty)
    }

    // ---- Delegados weak ----

    @Test func elDelegadoRecibeLosMinutos() {
        let cronometro = Cronometro()
        let delegado = DelegadoQueCuenta()
        cronometro.delegado = delegado
        cronometro.avanzar()
        cronometro.avanzar()
        #expect(delegado.minutosVistos == [1, 2])
    }

    @Test func elDelegadoEsWeakYNoLoMantieneVivo() {
        let cronometro = Cronometro()
        weak var delegadoDebil: DelegadoQueCuenta?
        do {
            let delegado = DelegadoQueCuenta()
            cronometro.delegado = delegado
            delegadoDebil = delegado
            #expect(cronometro.delegado != nil)
        }
        #expect(delegadoDebil == nil)         // el cronómetro no lo retuvo
        #expect(cronometro.delegado == nil)   // y su referencia se puso a nil sola
        cronometro.avanzar()                  // no pasa nada: sin delegado, no avisa
        #expect(cronometro.minuto == 1)
    }

    // ---- Task y ARC ----

    @Test func elCronometroQueAvanzaSoloSeParaConDetener() async {
        let cronometro = Cronometro()
        cronometro.arrancar(msPorMinuto: 5)
        try? await Task.sleep(for: .milliseconds(80))
        cronometro.detener()
        let alParar = cronometro.minuto
        #expect(alParar >= 5)
        try? await Task.sleep(for: .milliseconds(40))
        #expect(cronometro.minuto == alParar)   // ya no avanza
    }

    @Test func conWeakSelfLaTareaNoMantieneVivoAlCronometro() async {
        weak var cronometroDebil: Cronometro?
        do {
            let cronometro = Cronometro()
            cronometro.arrancar(msPorMinuto: 5)
            cronometroDebil = cronometro
        }   // se suelta SIN llamar a detener()
        try? await Task.sleep(for: .milliseconds(30))
        #expect(cronometroDebil == nil)   // la tarea usa `[weak self]` y no lo retenía
    }

    @Test func asiNoVale_laTareaConSelfFuerteMantieneVivoAlObjetoHastaCancelarla() async {
        weak var objetoDebil: CronometroConTareaFuerte?
        var tarea: Task<Void, Never>?
        do {
            let objeto = CronometroConTareaFuerte()
            objeto.arrancar()
            tarea = objeto.tarea
            objetoDebil = objeto
        }
        try? await Task.sleep(for: .milliseconds(30))
        #expect(objetoDebil != nil)   // sigue vivo: la tarea lo retiene
        tarea?.cancel()
        await tarea?.value            // al terminar la tarea, suelta `self`
        #expect(objetoDebil == nil)
    }
}
