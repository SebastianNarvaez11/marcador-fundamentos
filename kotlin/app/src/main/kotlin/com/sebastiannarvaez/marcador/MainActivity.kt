package com.sebastiannarvaez.marcador

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebastiannarvaez.marcador.torneo.Ejemplo

// Una Activity es UNA pantalla que el sistema crea, pausa y destruye cuando
// quiere. No la construyes tu con `MainActivity()`: la crea Android, y te avisa
// de cada cambio llamando a estos metodos (los «callbacks» del ciclo de vida).
//
// Aqui cada callback deja una linea en Logcat con el tag `Ciclo`. Para verlas:
//   adb logcat -s Ciclo
// o en Android Studio: Logcat, y filtra por `tag:Ciclo`.
class MainActivity : ComponentActivity() {

    // `super.onXxx()` primero en todos: la clase madre hace su trabajo (por
    // ejemplo, restaurar el estado de las vistas) y `ComponentActivity` avisa a
    // Compose y a los componentes de Jetpack. Sin la llamada a `super`, Android
    // lanza SuperNotCalledException.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        anotar("onCreate (estado guardado: ${if (savedInstanceState == null) "no" else "si"})")

        // Prueba de que `:app` ve el modulo `:torneo`: la tabla del torneo de ejemplo.
        val tabla = Ejemplo.torneoConPartidos().tablaDePosiciones()
        setContent {
            MaterialTheme {
                Column(Modifier.padding(24.dp)) {
                    Text("Marcador")
                    tabla.forEach { fila ->
                        Text("${fila.equipo.nombre}: ${fila.puntos} pts")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        anotar("onStart")
    }

    // Solo se llama al VOLVER a una Activity que estaba parada (no la primera vez).
    override fun onRestart() {
        super.onRestart()
        anotar("onRestart")
    }

    override fun onResume() {
        super.onResume()
        anotar("onResume")
    }

    override fun onPause() {
        super.onPause()
        anotar("onPause")
    }

    override fun onStop() {
        super.onStop()
        anotar("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        // isFinishing: true si el usuario cierra la pantalla (atras); false si
        // el sistema la destruye (rotacion, falta de memoria).
        anotar("onDestroy (isFinishing=$isFinishing, isChangingConfigurations=$isChangingConfigurations)")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        anotar("onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        anotar("onRestoreInstanceState")
    }

    // Solo llega si la Activity ya existe y le mandan un Intent nuevo.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        anotar("onNewIntent")
    }

    // Solo se llama si el Manifest declara android:configChanges para ese cambio.
    // Sin esa declaracion, Android destruye y recrea la Activity y este callback
    // no llega nunca.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        anotar("onConfigurationChanged")
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        anotar("onTopResumedActivityChanged($isTopResumedActivity)")
    }

    private fun anotar(mensaje: String) {
        // Log.d(tag, mensaje): nivel Debug. El tag es el filtro para encontrarlo.
        Log.d("Ciclo", "${javaClass.simpleName}@${System.identityHashCode(this).toString(16)} $mensaje")
    }
}
