import SwiftUI
import UIKit

// f68 · CICLO DE VIDA DE UN UIViewController
//
// Antes de SwiftUI, cada pantalla de iOS era un `UIViewController`. Sigue siendo la base
// (SwiftUI se apoya en UIKit por debajo) y todavía hay código y librerías que lo usan.
//
// Comparado con la Activity de Android (F3):
//
//   Activity                    UIViewController
//   onCreate                    viewDidLoad          (la vista se crea; una sola vez)
//   onStart                     viewWillAppear       (va a hacerse visible)
//   onResume                    viewDidAppear        (ya es visible y recibe toques)
//   onPause                     viewWillDisappear    (va a dejar de verse)
//   onStop                      viewDidDisappear     (ya no se ve)
//   onDestroy                   deinit               (ARC libera el objeto; no es un callback de UIKit)
//
// Diferencias que importan:
//   - Los pares de iOS son «will/did»: avisan ANTES y DESPUÉS del cambio. Android avisa una vez.
//   - Un VC que se tapa con otro a pantalla completa recibe disappear; si se tapa con una hoja
//     (sheet) que deja ver algo por detrás, según el estilo NO lo recibe.
//   - Rotar NO destruye el VC (en Android sí destruye la Activity): solo cambia el tamaño y
//     se vuelve a maquetar (`viewWillLayoutSubviews`).
//   - `deinit` no es de UIKit sino de Swift: corre cuando ARC libera el objeto, no cuando
//     «se cierra» la pantalla. Si nunca sale en el registro, algo lo está reteniendo (f66).
final class CicloDeVidaViewController: UIViewController {

    // El identificador se añade a cada mensaje, como el `identityHashCode` de la Activity.
    private var identificador: String { String(UInt(bitPattern: ObjectIdentifier(self).hashValue), radix: 16) }

    private var maquetaciones = 0

    private func anotar(_ mensaje: String) {
        Registro.anotar("Ciclo CicloDeVidaViewController@\(identificador) \(mensaje)")
    }

    // Siempre `super.xxx()`: la clase madre hace su trabajo. En viewWillAppear y compañía,
    // primero super (igual que en la Activity).
    override func viewDidLoad() {
        super.viewDidLoad()
        anotar("viewDidLoad")
        view.backgroundColor = .systemBackground

        let etiqueta = UILabel()
        etiqueta.text = "Un UIViewController\n(mira el registro)"
        etiqueta.numberOfLines = 0
        etiqueta.textAlignment = .center
        etiqueta.font = .preferredFont(forTextStyle: .title2)
        etiqueta.accessibilityIdentifier = "etiquetaDelControlador"
        etiqueta.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(etiqueta)
        NSLayoutConstraint.activate([
            etiqueta.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            etiqueta.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            etiqueta.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24),
        ])
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        anotar("viewWillAppear(animated: \(animated))")
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        // Se llama muchas veces (al rotar, al cambiar el teclado…): solo se cuentan.
        maquetaciones += 1
        if maquetaciones == 1 { anotar("viewWillLayoutSubviews (primera de varias)") }
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        anotar("viewDidAppear(animated: \(animated))")
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        anotar("viewWillDisappear(animated: \(animated))")
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        anotar("viewDidDisappear(animated: \(animated)), maquetaciones: \(maquetaciones)")
    }

    deinit {
        // `deinit` no es de UIKit sino de Swift: corre cuando ARC libera el objeto.
        Registro.anotar("Ciclo CicloDeVidaViewController deinit")
    }
}

// PUENTE: `UIViewControllerRepresentable` mete un UIViewController dentro de una vista de
// SwiftUI. SwiftUI llama a `makeUIViewController` UNA vez y a `updateUIViewController`
// cada vez que cambian los datos de la vista.
struct CicloDeVidaView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> CicloDeVidaViewController {
        CicloDeVidaViewController()
    }

    func updateUIViewController(_ controlador: CicloDeVidaViewController, context: Context) {}
}
