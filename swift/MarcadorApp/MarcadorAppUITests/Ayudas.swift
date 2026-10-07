import XCTest

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
}
