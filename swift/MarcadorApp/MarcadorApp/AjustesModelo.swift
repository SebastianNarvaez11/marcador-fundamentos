import Combine   // `ObservableObject` y `@Published` son de Combine, no de Foundation
import Foundation

// Los ajustes del usuario, compartidos por toda la app. Es un `ObservableObject`: una CLASE que
// avisa a SwiftUI cuando cambia alguna propiedad `@Published`.
//
// Se inyecta UNA vez en la raíz con `.environmentObject(ajustes)` y cualquier vista descendiente
// lo lee con `@EnvironmentObject`, sin pasarlo de padre a hijo por el constructor (es la
// versión de SwiftUI de un `CompositionLocal` de Compose, o de un objeto que Hilt entrega).
final class AjustesModelo: ObservableObject {
    static let duraciones = [90, 60]

    // 90 o 60 minutos. En f77 pasará a guardarse en el disco (`@AppStorage`).
    @Published var duracion = 90
}
