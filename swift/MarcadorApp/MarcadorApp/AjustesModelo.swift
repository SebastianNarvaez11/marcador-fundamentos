import Foundation
import Observation

// Los ajustes del usuario, compartidos por toda la app.
//
// f73 · `@Observable` (iOS 17) sustituye a `ObservableObject` + `@Published`. Es una MACRO: al
// escribirla sobre la clase, el compilador reescribe cada propiedad guardada para que registre
// quién la lee y avise solo a esas vistas cuando cambie. Ya no hay `@Published` ni `import Combine`.
//
// Se inyecta UNA vez en la raíz con `.environment(ajustes)` (antes, `.environmentObject`) y cualquier
// vista descendiente lo lee con `@Environment(AjustesModelo.self)` (antes, `@EnvironmentObject`).
@Observable
final class AjustesModelo {
    static let duraciones = [90, 60]

    // 90 o 60 minutos. En f77 pasará a guardarse en el disco (`@AppStorage`).
    var duracion = 90
}
