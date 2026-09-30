import Testing
@testable import Torneo

// Las mismas dos clases, pero con el error: las DOS referencias son fuertes. Viven
// solo en las pruebas, para demostrar el ciclo de retención sin ensuciar la librería.
private final class PartidoConCiclo {
    var narrador: NarradorConCiclo?
}

private final class NarradorConCiclo {
    var partido: PartidoConCiclo?   // fuerte: debería ser `weak`
}

struct ArcTests {
    @Test func elPartidoSeLiberaCuandoNadieLoUsa() {
        weak var referenciaDebil: PartidoEnVivo?
        var liberado = false
        do {
            let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros, alLiberarse: { liberado = true })
            referenciaDebil = partido
            #expect(referenciaDebil != nil)
            #expect(!liberado)
        }   // aquí `partido` sale de su alcance: contador a cero
        #expect(referenciaDebil == nil)
        #expect(liberado)   // deinit se ejecutó
    }

    @Test func unaSegundaReferenciaFuerteLoMantieneVivo() {
        weak var referenciaDebil: PartidoEnVivo?
        var segunda: PartidoEnVivo?
        do {
            let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros)
            referenciaDebil = partido
            segunda = partido   // contador: 2
        }   // contador: 1
        #expect(referenciaDebil != nil)
        #expect(segunda != nil)
        segunda = nil   // contador: 0
        #expect(referenciaDebil == nil)
    }

    @Test func conWeakElPartidoYElNarradorSeLiberanJuntos() {
        weak var partidoDebil: PartidoEnVivo?
        weak var narradorDebil: Narrador?
        do {
            let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros)
            let narrador = Narrador(nombre: "Pepe")
            partido.narrador = narrador        // el partido mantiene al narrador
            narrador.partido = partido         // el narrador lo mira `weak`
            partidoDebil = partido
            narradorDebil = narrador
            #expect(narrador.frase() == "Pepe: Rayo FC contra Toros")
        }
        #expect(partidoDebil == nil)
        #expect(narradorDebil == nil)
    }

    @Test func unaReferenciaWeakSePoneANilAlLiberarse() {
        let narrador = Narrador(nombre: "Pepe")
        do {
            let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros)
            narrador.partido = partido
            #expect(narrador.partido != nil)
        }
        #expect(narrador.partido == nil)
        #expect(narrador.frase() == "Pepe: ya no hay partido")
    }

    // El bug: con dos referencias fuertes, NINGUNO de los dos se libera.
    @Test func asiNoVale_dosReferenciasFuertesFormanUnCiclo() {
        weak var partidoDebil: PartidoConCiclo?
        weak var narradorDebil: NarradorConCiclo?
        do {
            let partido = PartidoConCiclo()
            let narrador = NarradorConCiclo()
            partido.narrador = narrador
            narrador.partido = partido
            partidoDebil = partido
            narradorDebil = narrador
        }   // las dos variables locales salen del alcance...
        // ...pero cada objeto sigue vivo porque el otro lo retiene: FUGA de memoria.
        #expect(partidoDebil != nil)
        #expect(narradorDebil != nil)
        // Se rompe el ciclo a mano para que la prueba no deje basura.
        partidoDebil?.narrador = nil
        #expect(partidoDebil == nil)
        #expect(narradorDebil == nil)
    }

    @Test func unownedNoEsOpcional() {
        let partido = PartidoEnVivo(guion: Ejemplo.rayoContraToros)
        let estadillo = Estadillo(partido: partido)
        #expect(estadillo.golesLocal == 2)
    }
}
