import Foundation

// Ajustes de ejecución de la app. Se leen de `UserDefaults`, y iOS rellena `UserDefaults`
// con los ARGUMENTOS DE LANZAMIENTO que empiezan por guion: `xcrun simctl launch … -msPorMinuto 20`
// (o en el esquema de Xcode, «Arguments Passed On Launch») da `msPorMinuto == 20`.
// Sirve para que las pruebas de UI no esperen 22 segundos por partido.
// `nonisolated`: se lee desde valores por defecto de inicializadores y desde cualquier contexto
// (el proyecto aísla todo a MainActor por defecto; sin esto Xcode avisa de la lectura cruzada).
nonisolated enum Configuracion {
    // Cuánto tarda un minuto de partido, en milisegundos (en Kotlin, `MS_POR_MINUTO = 250`).
    static var msPorMinuto: Int {
        let valor = UserDefaults.standard.integer(forKey: "msPorMinuto")
        return valor > 0 ? valor : 250
    }

    // SOLO PARA LA LECCIÓN f75: `-cicloDelCronometro YES` vuelve a meter el ciclo de retención en
    // `PartidoViewModel` para verlo con `leaks` o en el Memory Graph Debugger. Por defecto, apagado.
    static var cicloDelCronometro: Bool {
        UserDefaults.standard.bool(forKey: "cicloDelCronometro")
    }

    // `-reiniciarDatos YES`: borra el `torneo.json` guardado al arrancar (las pruebas de UI parten de cero).
    static var reiniciarDatos: Bool {
        UserDefaults.standard.bool(forKey: "reiniciarDatos")
    }
}
