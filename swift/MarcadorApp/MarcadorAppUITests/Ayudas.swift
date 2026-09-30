import XCTest

// Argumentos de lanzamiento que TODAS las pruebas de UI pasan salvo la de notificaciones: la app no
// enseña banners con ella delante. Sin esto, el permiso que concede `testUnGolSacaUnaNotificacion`
// queda en el simulador y en cada pasada siguiente cada gol saca un banner que XCUITest espera.
let sinAvisos = ["-sinAvisosEnPrimerPlano", "YES"]

extension XCTestCase {
    // Guarda una captura de la pantalla. Siempre la adjunta al resultado de la prueba (se ve en
    // el informe de Xcode). Si al lanzar las pruebas se define `TEST_RUNNER_CAPTURAS=/ruta`,
    // además escribe el PNG en esa carpeta: así se sacan las capturas desde la línea de comandos.
    @MainActor
    func capturar(_ app: XCUIApplication, _ nombre: String) {
        let captura = app.screenshot()
        let adjunto = XCTAttachment(screenshot: captura)
        adjunto.name = nombre
        adjunto.lifetime = .keepAlways
        add(adjunto)
        if let carpeta = ProcessInfo.processInfo.environment["CAPTURAS"] {
            try? captura.pngRepresentation.write(to: URL(fileURLWithPath: carpeta).appendingPathComponent("\(nombre).png"))
        }
    }

    // Igual que `capturar`, pero de la PANTALLA ENTERA del dispositivo (con los banners del sistema,
    // que dibuja SpringBoard y no la app; `app.screenshot()` no los ve).
    @MainActor
    func capturarPantalla(_ nombre: String) {
        let captura = XCUIScreen.main.screenshot()
        let adjunto = XCTAttachment(screenshot: captura)
        adjunto.name = nombre
        adjunto.lifetime = .keepAlways
        add(adjunto)
        if let carpeta = ProcessInfo.processInfo.environment["CAPTURAS"] {
            try? captura.pngRepresentation.write(to: URL(fileURLWithPath: carpeta).appendingPathComponent("\(nombre).png"))
        }
    }
}
