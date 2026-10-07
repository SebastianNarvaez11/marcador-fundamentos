import XCTest

final class MarcadorAppUITestsLaunchTests: XCTestCase {

    // El asistente genera `true` (repite la prueba con cada apariencia: clara, oscura, y con cada
    // orientación: cuatro arranques y minutos de espera). Aquí `false`.
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
        // Como lo genera el asistente: una captura de la pantalla de arranque, adjunta al resultado.
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Launch Screen"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
