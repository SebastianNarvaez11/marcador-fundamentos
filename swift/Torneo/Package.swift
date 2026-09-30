// swift-tools-version: 6.2
// La primera línea NO es un comentario cualquiera: le dice a SwiftPM con qué
// versión de las herramientas se escribió este manifiesto. Con 6.2 hace falta
// el Swift de Xcode 26 (`xcrun swift`), no el de swiftly.
import PackageDescription

let package = Package(
    name: "Torneo",
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
    ],
    // Con `swift-tools-version: 6.2` el modo de lenguaje por defecto YA es Swift 6,
    // con la concurrencia estricta activada. Hasta f64 se trabaja en modo Swift 5
    // para poder aprender cada tema por separado; en f65 se quita esta línea.
    swiftLanguageModes: [.v5]
)
