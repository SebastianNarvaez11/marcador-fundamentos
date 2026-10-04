// swift-tools-version: 6.2
// La primera línea NO es un comentario cualquiera: le dice a SwiftPM con qué
// versión de las herramientas se escribió este manifiesto. Con 6.2 hace falta
// el Swift de Xcode 26 (`xcrun swift`), no el de swiftly.
import PackageDescription

let package = Package(
    name: "Torneo",
    // Sistemas mínimos. Sin esta línea, SwiftPM compila para macOS 10.13 y las
    // APIs de concurrencia (`Task`, `Task.sleep(for:)`) «no existen» todavía.
    platforms: [.macOS(.v15), .iOS(.v18)],
    // Un producto es lo que otros paquetes (o la app de iOS) pueden usar.
    products: [
        .library(name: "Torneo", targets: ["Torneo"]),
        .executable(name: "torneo-cli", targets: ["torneo-cli"]),
    ],
    targets: [
        // La lógica del torneo: el gemelo de kotlin/torneo (sin el main).
        .target(name: "Torneo"),
        // El programa de consola: depende de la librería.
        .executableTarget(name: "torneo-cli", dependencies: ["Torneo"]),
        // Las pruebas, con Swift Testing (`import Testing`).
        // `resources`: ficheros que viajan con las pruebas. `.copy("Fixtures")` copia la
        // carpeta tal cual y se lee con `Bundle.module`, que SwiftPM genera solo cuando
        // hay recursos. Ahí vive el `torneo.json` que escribió la versión Kotlin.
        .testTarget(
            name: "TorneoTests",
            dependencies: ["Torneo"],
            resources: [.copy("Fixtures")]
        ),
    ]
    // Sin `swiftLanguageModes`: con `swift-tools-version: 6.2` el modo de lenguaje por
    // defecto es Swift 6, con la concurrencia estricta. Mientras se aprendía cada
    // tema por separado esta línea decía `swiftLanguageModes: [.v5]`; al llegar a
    // Swift 6 se quitó y se arregló todo lo que avisaba. Un proyecto de Xcode 26
    // añade, además, aislamiento a `MainActor` por defecto (aquí se probaría con
    // `.defaultIsolation(MainActor.self)`).
)
