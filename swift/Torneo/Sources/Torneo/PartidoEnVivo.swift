// Un partido que se está jugando. `guion` es todo lo que va a pasar.
//
// Es una CLASE, y no un struct como `Partido`, porque tiene IDENTIDAD y vida:
// alguien lo empieza, varios lo miran y, cuando ya nadie lo necesita, se libera.
// ARC (Automatic Reference Counting) cuenta cuántas referencias FUERTES apuntan
// a cada objeto y lo libera cuando llegan a cero. Los structs y enums no lo
// necesitan: se copian.
public final class PartidoEnVivo {
    public let guion: Partido

    // Se llama en `deinit`, es decir, justo cuando ARC libera el objeto. Sirve
    // para que las pruebas (y la consola) vean que de verdad se liberó.
    private let alLiberarse: (() -> Void)?

    // `strong` es lo normal: esta referencia mantiene vivo al narrador.
    public var narrador: Narrador?

    public init(guion: Partido, alLiberarse: (() -> Void)? = nil) {
        self.guion = guion
        self.alLiberarse = alLiberarse
    }

    // `deinit` corre UNA vez, cuando el contador de referencias fuertes llega a
    // cero. No se llama a mano. (Kotlin no tiene nada igual: el recolector de
    // basura libera «cuando puede», y no hay un momento fijo.)
    deinit {
        alLiberarse?()
    }
}

// El narrador cuenta el partido. Necesita saber de qué partido habla, y el
// partido sabe quién lo narra: dos objetos que se apuntan entre sí.
//
// Si las DOS referencias fueran fuertes, ninguno de los dos llegaría nunca a
// contador cero (cada uno mantiene vivo al otro) y no se liberarían jamás:
// un ciclo de retención (retain cycle). La solución es que UNA de las dos sea
// `weak`: no suma al contador y se pone a `nil` cuando el objeto se libera.
// La pregunta es «¿quién es el dueño?». El partido es el dueño del narrador,
// no al revés, y por eso el narrador lo mira con `weak`.
public final class Narrador {
    public let nombre: String
    public weak var partido: PartidoEnVivo?

    public init(nombre: String) {
        self.nombre = nombre
    }

    public func frase() -> String {
        // Como `partido` es `weak`, es opcional: hay que abrirlo.
        guard let partido else { return "\(nombre): ya no hay partido" }
        return "\(nombre): \(partido.guion.local.nombre) contra \(partido.guion.visitante.nombre)"
    }
}

// `unowned`: como `weak` pero NO opcional. Se usa cuando el otro objeto siempre
// vive al menos tanto como éste: un estadillo no existe sin su partido. Si te
// equivocas y el partido ya no está, el programa se detiene (a diferencia de
// `weak`, que te da `nil`). Es más cómodo y más peligroso.
public final class Estadillo {
    private unowned let partido: PartidoEnVivo

    public init(partido: PartidoEnVivo) {
        self.partido = partido
    }

    public var golesLocal: Int { partido.guion.golesLocal }
}
