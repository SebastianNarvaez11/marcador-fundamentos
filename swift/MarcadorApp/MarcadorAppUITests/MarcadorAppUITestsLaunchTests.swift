import XCTest

final class MarcadorAppUITestsLaunchTests: XCTestCase {

    // El asistente genera `true` (repite la prueba con cada apariencia: clara, oscura, y con cada
    // orientación; en este Mac eran cuatro arranques y minutos de espera). Aquí `false`.
    override class var runsForEachTargetApplicationUIConfiguration: Bool {
        false
    }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    func testLaunch() throws {
        let app = XCUIApplication()
        app.launch()

        // Aquí se pueden añadir pasos antes de la captura, como iniciar sesión.
        let captura = XCTAttachment(screenshot: app.screenshot())
        captura.name = "Pantalla de arranque"
        captura.lifetime = .keepAlways
        add(captura)
    }
}
