import Testing
@testable import Torneo

// LO QUE NO HAY QUE HACER: una clase normal compartida entre tareas. `golLocal()`
// lee el marcador, suma uno y lo escribe: tres pasos, y otra tarea puede meterse
// entre dos de ellos. Vive solo en las pruebas.
//
// En modo Swift 6 esto ni compila: `grupo.addTask { inseguro.golLocal() }` da
// «passing closure as a 'sending' parameter risks causing data races…». Con
// `@unchecked Sendable` le decimos al compilador «cállate, yo me hago responsable»
// y el bug vuelve a existir, sin aviso: eso es justo lo que no hay que hacer.
private final class MarcadorInseguro: @unchecked Sendable {
    var marcador = Marcador(local: 0, visitante: 0)
    func golLocal() {
        marcador = Marcador(local: marcador.local + 1, visitante: marcador.visitante)
    }
}

struct ActoresTests {
    static let goles = 10_000

    // El data race. Con 10 000 goles a la vez, la clase pierde algunos: el resultado
    // suele ser algo como 9800 en vez de 10 000, y CAMBIA en cada ejecución. No se
    // afirma que sea distinto (podría salir bien por casualidad y la prueba sería
    // inestable): se afirma lo único seguro, que nunca sobra ninguno, y se imprime.
    @Test func laClaseCompartidaSePisa() async {
        let inseguro = MarcadorInseguro()
        await withTaskGroup(of: Void.self) { grupo in
            for _ in 0..<Self.goles {
                grupo.addTask { inseguro.golLocal() }
            }
        }
        print("Con una clase: \(inseguro.marcador.local) goles de \(Self.goles)")
        #expect(inseguro.marcador.local <= Self.goles)
    }

    // El actor: cada `await golLocal()` entra de uno en uno, así que no se pierde ninguno.
    @Test func elActorNoPierdeNingunGol() async {
        let seguro = MarcadorSeguro()
        await withTaskGroup(of: Void.self) { grupo in
            for _ in 0..<Self.goles {
                grupo.addTask { await seguro.golLocal() }
            }
        }
        let final = await seguro.marcador
        print("Con un actor: \(final.local) goles de \(Self.goles)")
        #expect(final.local == Self.goles)
        #expect(final.visitante == 0)
    }

    @Test func golesDeLosDosEquiposALaVez() async {
        let seguro = MarcadorSeguro()
        await withTaskGroup(of: Void.self) { grupo in
            for _ in 0..<1_000 {
                grupo.addTask { await seguro.golLocal() }
                grupo.addTask { await seguro.golVisitante() }
            }
        }
        #expect(await seguro.marcador == Marcador(local: 1_000, visitante: 1_000))
    }

    @Test func registrarEventosEnElActor() async {
        let seguro = MarcadorSeguro()
        for evento in Ejemplo.rayoContraToros.eventos {
            await seguro.registrar(evento, en: Ejemplo.rayoContraToros)
        }
        #expect(await seguro.marcador.description == "2-1")
    }

    // Reentrancia: el actor protege entre `await`, no A TRAVÉS de un `await`.
    @Test func reentranciaPierdeUnGolSiSeLeeAntesDeEsperar() async {
        let seguro = MarcadorSeguro()
        async let uno: Void = seguro.golLocalConRevisionIncorrecta(msDeRevision: 50)
        async let dos: Void = seguro.golLocalConRevisionIncorrecta(msDeRevision: 50)
        _ = await (uno, dos)
        let final = await seguro.marcador
        print("Reentrancia, versión incorrecta: \(final.local) goles de 2")
        #expect(final.local == 1)   // dos goles, uno perdido
    }

    @Test func reentranciaSeArreglaLeyendoDespuesDeEsperar() async {
        let seguro = MarcadorSeguro()
        async let uno: Void = seguro.golLocalConRevision(msDeRevision: 50)
        async let dos: Void = seguro.golLocalConRevision(msDeRevision: 50)
        _ = await (uno, dos)
        #expect(await seguro.marcador.local == 2)
    }

    @Test func elPartidoEnVivoAnotaSusEventosEnElActor() async {
        let enVivo = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        let final = await enVivo.jugar()
        #expect(final.description == "2-1")
        #expect(await enVivo.marcadorSeguro.marcador == final)
    }

    @Test func dosPartidosAlaVezConSuPropioActor() async {
        let uno = PartidoEnVivo(guion: Ejemplo.rayoContraToros, msPorMinuto: 1)
        let otro = PartidoEnVivo(guion: Ejemplo.lobosContraAguilas, msPorMinuto: 1)
        async let a = uno.jugar()
        async let b = otro.jugar()
        let (marcadorUno, marcadorOtro) = await (a, b)
        #expect(marcadorUno.description == "2-1")
        #expect(marcadorOtro.description == "1-2")
    }
}
