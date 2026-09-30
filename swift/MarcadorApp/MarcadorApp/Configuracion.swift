import Foundation

// Ajustes de ejecución de la app. Se leen de `UserDefaults`, y iOS rellena `UserDefaults`
// con los ARGUMENTOS DE LANZAMIENTO que empiezan por guion: `xcrun simctl launch … -msPorMinuto 20`
// (o en el esquema de Xcode, «Arguments Passed On Launch») da `msPorMinuto == 20`.
// Sirve para que las pruebas de UI no esperen 22 segundos por partido.
enum Configuracion {
    // Cuánto tarda un minuto de partido, en milisegundos (en Kotlin, `MS_POR_MINUTO = 250`).
    static var msPorMinuto: Int {
        let valor = UserDefaults.standard.integer(forKey: "msPorMinuto")
        return valor > 0 ? valor : 250
    }
}
