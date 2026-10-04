import Torneo

// LO QUE VE LA PANTALLA, en un solo valor (gemelo de `MarcadorUiState`).
//
// Un `enum` con valores asociados: la pantalla hace un `switch` exhaustivo y, si mañana hay un
// estado más, no compila hasta que diga qué hacer con él. No se pueden dar a la vez «cargando» y «error».
enum MarcadorUiState: Equatable {
    case cargando
    case error(String)
    case exito(Exito)

    struct Exito: Equatable {
        let partidoId: Int
        let partido: Partido
        let marcador: Marcador
        let minuto: Int
        let corriendo: Bool
        let ultimoAviso: String
        let duracion: Int
    }
}
