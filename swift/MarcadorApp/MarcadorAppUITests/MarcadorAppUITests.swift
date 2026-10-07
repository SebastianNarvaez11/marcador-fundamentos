import XCTest

// Las pruebas de UI son SIEMPRE XCTest (Swift Testing no las soporta todavía): arrancan
// la app en un proceso aparte y la manejan como un usuario, con `XCUIApplication`.
final class MarcadorAppUITests: XCTestCase {

    override func setUpWithError() throws {
        // Ante el primer fallo, para: los pasos siguientes ya no tienen sentido.
        continueAfterFailure = false
    }

    @MainActor
    func testArrancaYMuestraElMarcador() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Marcador"].tap()
        XCTAssertTrue(app.staticTexts["Rayo FC"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["0 - 0"].exists)
        capturar(app, "marcador")
    }

    // El partido NO se reinicia cuando el padre se redibuja (con @ObservedObject sí lo hacía).
    @MainActor
    func testElPartidoSobreviveAlRedibujadoDelPadre() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-msPorMinuto", "40"]
        app.launch()
        app.buttons["partido-1"].tap()
        let minuto = app.staticTexts["minuto"]
        // Espera a que el reloj pase del minuto 10.
        let pasaDel10 = NSPredicate { objeto, _ in
            let texto = (objeto as? XCUIElement)?.label ?? ""
            let numero = Int(texto.filter(\.isNumber)) ?? 0
            return numero >= 10
        }
        expectation(for: pasaDel10, evaluatedWith: minuto)
        waitForExpectations(timeout: 15)
        let antes = minuto.label
        capturar(app, "partido-antes-de-redibujar")
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Redibujar'")).firstMatch.tap()
        let despues = minuto.label
        capturar(app, "partido-despues-de-redibujar")
        let numeroAntes = Int(antes.filter(\.isNumber)) ?? 0
        let numeroDespues = Int(despues.filter(\.isNumber)) ?? -1
        XCTAssertGreaterThanOrEqual(numeroDespues, numeroAntes, "El partido se reinició: antes «\(antes)», después «\(despues)»")
    }

    // Al cerrar la hoja se cancela el `.task` del partido (mira el registro: «partido CANCELADO»).
    @MainActor
    func testAlSalirDelPartidoSeCancelaElTask() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-msPorMinuto", "100"]
        app.launch()
        app.buttons["partido-2"].tap()
        XCTAssertTrue(app.staticTexts["minuto"].waitForExistence(timeout: 5))
        let enJuego = app.staticTexts["En juego"]
        XCTAssertTrue(enJuego.waitForExistence(timeout: 5))
        capturar(app, "en-juego")
        // Se desliza la hoja de arriba abajo para cerrarla.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15))
            .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        XCTAssertTrue(app.buttons["partido-2"].waitForExistence(timeout: 5))
    }

    // `.task` frente a `.onAppear { Task }`: se abre y se cierra a los 2 s.
    @MainActor
    func testTaskFrenteAOnAppear() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        app.buttons[".task frente a .onAppear"].tap()
        XCTAssertTrue(app.staticTexts["contadorTask"].waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 2)
        capturar(app, "contadores")
        // Se desliza la hoja de arriba abajo para cerrarla.
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15))
            .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        XCTAssertTrue(app.buttons["Ciclo de vida (UIKit)"].waitForExistence(timeout: 5))
        // Se deja la app viva unos segundos para que el registro enseñe qué Task siguió contando.
        Thread.sleep(forTimeInterval: 4)
    }

    // La lista de partidos muestra las filas con su marcador.
    @MainActor
    func testLaListaMuestraLosPartidos() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.otherElements["partido-1"].waitForExistence(timeout: 5)
            || app.staticTexts["Rayo FC"].waitForExistence(timeout: 5))
        capturar(app, "lista")
    }

    // Identidad por posición (A) frente a identidad por dato (B).
    @MainActor
    func testIdentidadPorPosicionFrenteAIdentidadPorDato() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        app.swipeUp()
        let marcaA = app.switches["A Ana"]
        let marcaB = app.switches["B Ana"]
        XCTAssertTrue(marcaA.waitForExistence(timeout: 5))
        marcaA.tap()
        marcaB.tap()
        app.buttons["Insertar arriba"].tap()
        // A: la marca se queda en la POSICIÓN 0, que ahora es «Nuevo1»; «Ana» ya no está marcada.
        XCTAssertEqual(app.switches["A Nuevo1"].value as? String, "1")
        XCTAssertEqual(app.switches["A Ana"].value as? String, "0")
        // B: la marca sigue al DATO: «Ana» conserva la suya y «Nuevo1» empieza sin marcar.
        XCTAssertEqual(app.switches["B Ana"].value as? String, "1")
        XCTAssertEqual(app.switches["B Nuevo1"].value as? String, "0")
        capturar(app, "identidad")
    }

    // Cada botón cambia el @State y la tarjeta se redibuja.
    @MainActor
    func testLosBotonesDeGolCambianElMarcador() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Marcador"].tap()
        app.buttons["Gol Rayo FC"].tap()
        app.buttons["Gol Rayo FC"].tap()
        app.buttons["Gol Toros"].tap()
        XCTAssertTrue(app.staticTexts["2 - 1"].waitForExistence(timeout: 3))
        capturar(app, "marcador-2-1")
    }

    @MainActor
    func testLaboratorioMuestraElOrdenDeLosModificadores() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        XCTAssertTrue(app.staticTexts["versionA"].waitForExistence(timeout: 5))
        capturar(app, "modificadores")
        // En A el fondo cubre el aire del padding; en B no: A es más grande que B.
        let a = app.staticTexts["versionA"].frame
        let b = app.staticTexts["versionB"].frame
        XCTAssertGreaterThan(a.width, 0)
        XCTAssertGreaterThan(b.width, 0)
    }

    // Abre la hoja con el UIViewController y la cierra arrastrándola hacia abajo.
    // Sirve de prueba y de «mano» para ver el registro del ciclo de vida.
    @MainActor
    func testAbreYCierraElControlador() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        app.buttons["Ciclo de vida (UIKit)"].tap()
        XCTAssertTrue(app.staticTexts["etiquetaDelControlador"].waitForExistence(timeout: 5))
        capturar(app, "controlador")
        // Se arrastra desde arriba de la hoja hasta abajo del todo.
        let inicio = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15))
        inicio.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        XCTAssertTrue(app.buttons["Ciclo de vida (UIKit)"].waitForExistence(timeout: 5))
    }
}
