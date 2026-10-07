import Foundation

// EL ERROR DE DOMINIO de la red: lo que puede salir mal al hablar con el servidor, dicho con
// palabras de la app y no de `URLSession` (gemelo de `ErrorDeRed` en Android).
//
// Hay DOS fallos muy distintos:
//   - no hubo respuesta (sin internet, el servidor no existe…): `URLSession` lanza un `URLError`;
//   - hubo respuesta, pero con un código de error (404, 500…): `URLSession` NO lanza nada,
//     y es el servicio quien mira el código y lanza `.servidor`.
//
// `LocalizedError`: da el texto que verá el usuario (`error.localizedDescription`).
enum ErrorDeRed: Error, Equatable, LocalizedError {
    case sinConexion
    case servidor(codigo: Int)

    var errorDescription: String? {
        switch self {
        case .sinConexion: "No hay conexión"
        case let .servidor(codigo): "El servidor respondió con el código \(codigo)"
        }
    }
}
