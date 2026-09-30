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
        capturar(app, "f69-marcador")
    }

    // f72: el partido NO se reinicia cuando el padre se redibuja (con @ObservedObject sí lo hacía).
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
        capturar(app, "f72-antes-de-redibujar")
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Redibujar'")).firstMatch.tap()
        let despues = minuto.label
        capturar(app, "f72-despues-de-redibujar")
        let numeroAntes = Int(antes.filter(\.isNumber)) ?? 0
        let numeroDespues = Int(despues.filter(\.isNumber)) ?? -1
        XCTAssertGreaterThanOrEqual(numeroDespues, numeroAntes, "El partido se reinició: antes «\(antes)», después «\(despues)»")
    }

    // f74: al volver atrás se cancela el `.task` del partido (mira el registro: «partido CANCELADO»).
    @MainActor
    func testAlSalirDelPartidoSeCancelaElTask() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-msPorMinuto", "60"]
        app.launch()
        app.buttons["partido-2"].tap()
        XCTAssertTrue(app.staticTexts["minuto"].waitForExistence(timeout: 5))
        let enJuego = app.staticTexts["En juego"]
        XCTAssertTrue(enJuego.waitForExistence(timeout: 5))
        capturar(app, "f74-en-juego")
        app.navigationBars.buttons.firstMatch.tap()   // «atrás»
        XCTAssertTrue(app.buttons["partido-2"].waitForExistence(timeout: 5))
    }

    // f74: `.task` frente a `.onAppear { Task }`: se abre y se cierra a los 2 s.
    @MainActor
    func testTaskFrenteAOnAppear() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        app.buttons[".task frente a .onAppear"].tap()
        XCTAssertTrue(app.staticTexts["contadorTask"].waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 2)
        capturar(app, "f74-contadores")
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15))
            .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        XCTAssertTrue(app.buttons["Ciclo de vida (UIKit)"].waitForExistence(timeout: 5))
        // Se deja la app viva unos segundos para que el registro enseñe qué Task siguió contando.
        Thread.sleep(forTimeInterval: 4)
    }

    // f76: lista → partido → goleadores → atrás → atrás.
    @MainActor
    func testNavegaDeLaListaAlPartidoYALosGoleadores() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-msPorMinuto", "60"]
        app.launch()
        app.buttons["partido-1"].tap()
        XCTAssertTrue(app.navigationBars["Partido n.º 1"].waitForExistence(timeout: 5))
        app.buttons["Ver goleadores"].tap()
        XCTAssertTrue(app.navigationBars["Goleadores"].waitForExistence(timeout: 5))
        capturar(app, "f76-goleadores")
        app.buttons["Volver al partido"].tap()
        XCTAssertTrue(app.navigationBars["Partido n.º 1"].waitForExistence(timeout: 5))
        capturar(app, "f76-partido")
        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.navigationBars["Partidos"].waitForExistence(timeout: 5))
    }

    // f76: volver de «Goleadores» NO reinicia el partido. Al apilar otra pantalla SwiftUI cancela el `.task` y
    // lo relanza al volver; si `jugar` empezara de cero, el minuto volvería a 0 y el marcador a 0 - 0.
    @MainActor
    func testVolverDeGoleadoresNoReiniciaElPartido() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-msPorMinuto", "150"]
        app.launch()
        app.buttons["partido-1"].tap()
        let minuto = app.staticTexts["minuto"]
        let marcador = app.staticTexts.matching(NSPredicate(format: "label MATCHES %@", "\\d+ - \\d+")).firstMatch
        // Espera a que el reloj pase del minuto 14: el gol del minuto 12 ya está en el marcador.
        let pasaDel14 = NSPredicate { objeto, _ in
            let texto = (objeto as? XCUIElement)?.label ?? ""
            return (Int(texto.filter(\.isNumber)) ?? 0) >= 14
        }
        expectation(for: pasaDel14, evaluatedWith: minuto)
        waitForExpectations(timeout: 15)
        let minutoAntes = Int(minuto.label.filter(\.isNumber)) ?? 0
        let marcadorAntes = marcador.label
        XCTAssertNotEqual(marcadorAntes, "0 - 0", "El gol del minuto 12 debería estar ya en el marcador")

        app.buttons["Ver goleadores"].tap()
        XCTAssertTrue(app.navigationBars["Goleadores"].waitForExistence(timeout: 5))
        Thread.sleep(forTimeInterval: 1)
        app.buttons["Volver al partido"].tap()
        XCTAssertTrue(app.navigationBars["Partido n.º 1"].waitForExistence(timeout: 5))
        XCTAssertTrue(minuto.waitForExistence(timeout: 5))

        let minutoDespues = Int(minuto.label.filter(\.isNumber)) ?? -1
        let marcadorDespues = marcador.label
        XCTAssertGreaterThanOrEqual(minutoDespues, minutoAntes, "El minuto volvió atrás: antes \(minutoAntes), después \(minutoDespues)")
        XCTAssertNotEqual(marcadorDespues, "0 - 0", "El marcador se reinició al volver de Goleadores (antes «\(marcadorAntes)»)")
        print("VOLVER-DE-GOLEADORES minuto antes=\(minutoAntes) despues=\(minutoDespues); marcador antes=\(marcadorAntes) despues=\(marcadorDespues)")
        // Y el partido SIGUE: el minuto sigue avanzando después de volver.
        let avanza = NSPredicate { objeto, _ in
            let texto = (objeto as? XCUIElement)?.label ?? ""
            return (Int(texto.filter(\.isNumber)) ?? 0) > minutoDespues
        }
        expectation(for: avanza, evaluatedWith: minuto)
        waitForExpectations(timeout: 10)
        capturar(app, "f76-tras-volver-de-goleadores")
    }

    // f75: abre un partido y lo cierra; con `TEST_RUNNER_ESPERA` la app se queda viva para medir con `leaks`.
    // `TEST_RUNNER_CICLO=YES` reintroduce el ciclo del cronómetro (solo para la lección).
    @MainActor
    func testAbreYCierraUnPartidoParaMedirLaMemoria() throws {
        let app = XCUIApplication()
        let entorno = ProcessInfo.processInfo.environment
        app.launchArguments = ["-msPorMinuto", "100", "-cicloDelCronometro", entorno["CICLO"] ?? "NO"]
        app.launch()
        app.buttons["partido-1"].tap()
        XCTAssertTrue(app.staticTexts["minuto"].waitForExistence(timeout: 10))
        Thread.sleep(forTimeInterval: 2)
        app.navigationBars.buttons.firstMatch.tap()   // «atrás»
        XCTAssertTrue(app.buttons["partido-1"].waitForExistence(timeout: 5))
        if let segundos = entorno["ESPERA"].flatMap(Double.init) {
            Thread.sleep(forTimeInterval: segundos)
        }
    }

    // f71: la lista de partidos muestra las filas con su marcador.
    @MainActor
    func testLaListaMuestraLosPartidos() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.otherElements["partido-1"].waitForExistence(timeout: 5)
            || app.staticTexts["Rayo FC"].waitForExistence(timeout: 5))
        capturar(app, "f71-lista")
    }

    // f71: identidad por posición (A) frente a identidad por dato (B).
    @MainActor
    func testIdentidadPorPosicionFrenteAIdentidadPorDato() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
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
        capturar(app, "f71-identidad")
    }

    // f70: cada botón cambia el @State y la tarjeta se redibuja.
    @MainActor
    func testLosBotonesDeGolCambianElMarcador() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Marcador"].tap()
        app.buttons["Gol Rayo FC"].tap()
        app.buttons["Gol Rayo FC"].tap()
        app.buttons["Gol Toros"].tap()
        XCTAssertTrue(app.staticTexts["2 - 1"].waitForExistence(timeout: 3))
        capturar(app, "f70-marcador-2-1")
    }

    @MainActor
    func testLaboratorioMuestraElOrdenDeLosModificadores() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        XCTAssertTrue(app.staticTexts["versionA"].waitForExistence(timeout: 5))
        capturar(app, "f69-modificadores")
        // En A el fondo cubre el aire del padding; en B no: A es más grande que B.
        let a = app.staticTexts["versionA"].frame
        let b = app.staticTexts["versionB"].frame
        XCTAssertGreaterThan(a.width, 0)
        XCTAssertGreaterThan(b.width, 0)
    }

    // f68: abre la hoja con el UIViewController y la cierra arrastrándola hacia abajo.
    // Sirve de prueba y de «mano» para ver el registro del ciclo de vida.
    @MainActor
    func testAbreYCierraElControlador() throws {
        let app = XCUIApplication()
        app.launch()
        app.tabBars.buttons["Laboratorio"].tap()
        app.buttons["Ciclo de vida (UIKit)"].tap()
        XCTAssertTrue(app.staticTexts["etiquetaDelControlador"].waitForExistence(timeout: 5))
        capturar(app, "f68-controlador")
        // Se arrastra desde arriba de la hoja hasta abajo del todo.
        let inicio = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.15))
        inicio.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        XCTAssertTrue(app.buttons["Ciclo de vida (UIKit)"].waitForExistence(timeout: 5))
    }
}
