package com.sebastiannarvaez.marcador

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sebastiannarvaez.marcador.torneo.Ejemplo
import com.sebastiannarvaez.marcador.torneo.FilaDePosicion
import com.sebastiannarvaez.marcador.ui.AppMarcador
import com.sebastiannarvaez.marcador.torneo.PartidoEnVivo
import com.sebastiannarvaez.marcador.torneo.probabilidadDeVictoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

// Una Activity es UNA pantalla que el sistema crea, pausa y destruye cuando
// quiere. No la construyes tu con `MainActivity()`: la crea Android, y te avisa
// de cada cambio llamando a estos metodos (los «callbacks» del ciclo de vida).
//
// Aqui cada callback deja una linea en Logcat con el tag `Ciclo`. Para verlas:
//   adb logcat -s Ciclo
// o en Android Studio: Logcat, y filtra por `tag:Ciclo`.
class MainActivity : ComponentActivity() {

    // Dos contadores, dos destinos distintos al girar la pantalla.
    //
    // Al rotar, Android DESTRUYE esta Activity y crea otra nueva: las propiedades
    // de la clase empiezan otra vez desde su valor inicial. Este contador se pierde
    // A PROPOSITO, para que veas el problema.
    //
    // Para reproducirlo: toca el primer boton, gira el movil (o
    //   adb shell settings put system accelerometer_rotation 0
    //   adb shell settings put system user_rotation 1)
    // y el numero vuelve a 0. Para la MUERTE DEL PROCESO: pulsa Home y ejecuta
    //   adb shell am kill com.sebastiannarvaez.marcador
    // y vuelve a la app desde recientes: el segundo contador sigue, el primero no.
    private var golesQueSePierden by mutableIntStateOf(0)

    // Este se guarda en el Bundle de onSaveInstanceState y se restaura en onCreate.
    private var golesQueSobreviven by mutableIntStateOf(0)

    // ---- f29 ----
    // El dialogo del permiso es asincrono: pides y la respuesta llega despues, a este
    // lambda. Hay que registrarlo ANTES de que la Activity llegue a STARTED (por eso es
    // una propiedad, no algo dentro de un onClick).
    private val pedirPermisoDeAvisos =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            avisosActivados = concedido
            if (!concedido) {
                // El usuario dijo que no: se explica y se sigue sin avisos. No se insiste.
                Toast.makeText(this, "Sin permiso no hay avisos de gol", Toast.LENGTH_LONG).show()
            }
        }
    private var avisosActivados by mutableStateOf(false)

    private fun activarAvisos() {
        if (puedeNotificar(this)) {
            avisosActivados = true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermisoDeAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---- f26 ----
    private var resultadoDelCalculo by mutableStateOf("sin calcular")
    private var marcadorEnVivo by mutableStateOf("0-0")
    // El partido solo se puede iniciar una vez (`iniciar` lanza IllegalStateException: «El
    // partido ya empezó»): el boton se deshabilita tras el primer toque.
    private var partidoEmpezado by mutableStateOf(false)
    private val partidoEnVivo = PartidoEnVivo(Ejemplo.rayoContraToros, msPorMinuto = 60)

    // EL BUG (ANR): el hilo principal es el UNICO que atiende los toques y dibuja
    // la pantalla. Si tarda mas de ~5 s en volver de un trabajo, Android muestra
    // «La aplicacion no responde». `runBlocking` NO cambia de hilo: bloquea el
    // actual hasta que termina lo de dentro (y `probabilidadDeVictoria` es suspend,
    // pero suspend no significa «en otro hilo»: corre donde la llamen).
    //
    // Para reproducirlo: pulsa el boton y, MIENTRAS esta bloqueado, toca la pantalla
    // varias veces. A los ~5 s aparece el dialogo del ANR.
    // Intent EXPLICITO: el destino es una clase tuya, sin ambiguedad. Los datos
    // viajan como extras (clave, valor).
    private fun abrirDetalle() {
        val intent = Intent(this, DetalleActivity::class.java)
            .putExtra(DetalleActivity.EXTRA_RESUMEN, Ejemplo.rayoContraToros.resumen())
        startActivity(intent)
    }

    private fun calcularBloqueando() {
        val ms = measureTimeMillis {
            val p = runBlocking { probabilidadDeVictoria(Ejemplo.rayoContraToros, SIMULACIONES) }
            resultadoDelCalculo = "victoria local: ${"%.1f".format(p * 100)}%"
        }
        anotar("calculo bloqueante: $ms ms")
    }

    // EL ARREGLO: `lifecycleScope` es un alcance atado a la Activity: se cancela
    // solo en onDestroy. `launch` arranca en el hilo principal, pero `withContext(
    // Dispatchers.Default)` mueve SOLO el calculo a un hilo de fondo y vuelve al
    // principal con el resultado. Mientras tanto la pantalla sigue viva.
    private fun calcularEnSegundoPlano() {
        resultadoDelCalculo = "calculando..."
        lifecycleScope.launch {
            val p = withContext(Dispatchers.Default) {
                probabilidadDeVictoria(Ejemplo.rayoContraToros, SIMULACIONES)
            }
            resultadoDelCalculo = "victoria local: ${"%.1f".format(p * 100)}%"
        }
    }

    // `super.onXxx()` primero en todos: la clase madre hace su trabajo (por
    // ejemplo, restaurar el estado de las vistas) y `ComponentActivity` avisa a
    // Compose y a los componentes de Jetpack. Sin la llamada a `super`, Android
    // lanza SuperNotCalledException.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        anotar("onCreate (estado guardado: ${if (savedInstanceState == null) "no" else "si"})")

        // repeatOnLifecycle(STARTED): el bloque se ARRANCA cada vez que la Activity
        // llega a STARTED (visible) y se CANCELA cuando baja de STARTED (onStop).
        // Asi no se sigue pintando algo que nadie ve. Prueba: pulsa Home y mira
        // Logcat: «recogida PARADA». Al volver: «recogida ARRANCADA».
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                anotar("recogida ARRANCADA")
                try {
                    partidoEnVivo.marcador.collect { marcadorEnVivo = it.toString() }
                } finally {
                    anotar("recogida PARADA")
                }
            }
        }

        avisosActivados = puedeNotificar(this)

        // Cada gol del partido en vivo se convierte en una notificacion. `goles` es
        // un SharedFlow: solo ve los goles que ocurren MIENTRAS se recoge. Sin
        // repeatOnLifecycle a proposito: con la app en segundo plano tambien avisa
        // (hasta que la Activity se destruye; para avisar sin pantalla haria falta
        // un servicio, ver f30).
        lifecycleScope.launch {
            partidoEnVivo.goles.collect { gol -> notificarGol(applicationContext, gol) }
        }

        // Si `savedInstanceState` no es null, Android nos devuelve lo que guardamos.
        golesQueSobreviven = savedInstanceState?.getInt(CLAVE_GOLES) ?: 0

        // Prueba de que `:app` ve el modulo `:torneo`: la tabla del torneo de ejemplo.
        val tabla = Ejemplo.torneoConPartidos().tablaDePosiciones()
        setContent {
            MaterialTheme {
                // f32: la pantalla de verdad es PantallaMarcador (ui/). Los botones de
                // demostracion de F3 siguen ahi, detras de un boton (en la barra de la lista
                // de partidos, desde f48), para no perderlos.
                if (verDemosDeF3) DemosDeF3(tabla) else AppMarcador(onVerDemos = { verDemosDeF3 = true })
            }
        }
    }

    private var verDemosDeF3 by mutableStateOf(false)

    @Composable
    private fun DemosDeF3(tabla: List<FilaDePosicion>) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                    Button(onClick = { verDemosDeF3 = false }) { Text("Volver a la pantalla del marcador") }
                    Text("Marcador")
                    // f31: datos que solo conoce la compilacion (BuildConfig se genera).
                    Text("Modo: ${BuildConfig.MODO} · plan ${BuildConfig.PLAN} · ${BuildConfig.APPLICATION_ID}")
                    // f28: intents. El primero es EXPLICITO (nombras la clase destino).
                    Button(onClick = { abrirDetalle() }) { Text("Ver detalle (intent explicito)") }
                    Button(onClick = { compartir(Ejemplo.rayoContraToros.resumen()) }) {
                        Text("Compartir resultado (ACTION_SEND)")
                    }
                    Button(onClick = { abrirEnlace("https://kotlinlang.org") }) {
                        Text("Abrir web (ACTION_VIEW)")
                    }
                    // f29: permiso y notificacion.
                    Button(onClick = { activarAvisos() }) {
                        Text(if (avisosActivados) "Avisos de gol: activados" else "Activar avisos de gol")
                    }
                    // f30: WorkManager. Para la demo el «una hora» son 15 segundos.
                    Button(onClick = {
                        RecordatorioWorker.programar(applicationContext, Ejemplo.rayoContraToros.resumen(), retrasoSegundos = 15)
                        Toast.makeText(this@MainActivity, "Recordatorio en 15 s (en la app real, 1 h)", Toast.LENGTH_SHORT).show()
                    }) { Text("Recordarme el partido") }
                    Button(onClick = { calcularBloqueando() }) {
                        Text("Calcular (BLOQUEA: ANR)")
                    }
                    Button(onClick = { calcularEnSegundoPlano() }) {
                        Text("Calcular (en segundo plano)")
                    }
                    Text(resultadoDelCalculo)
                    Button(
                        onClick = {
                            partidoEmpezado = true
                            partidoEnVivo.iniciar(lifecycleScope)
                        },
                        enabled = !partidoEmpezado,
                    ) {
                        Text("Empezar partido")
                    }
                    Text("Marcador en vivo: $marcadorEnVivo")
                    // f27. Para reproducir la fuga: pulsa el primero, gira el movil y
                    // pulsa Home; a los pocos segundos LeakCanary avisa en Logcat (tag
                    // LeakCanary) y con una notificacion. El segundo es el arreglo.
                    Button(onClick = { RegistroDeContexto.guardarSinCuidado(this@MainActivity) }) {
                        Text("Guardar Activity (FUGA)")
                    }
                    Button(onClick = { RegistroDeContexto.guardarConCuidado(this@MainActivity) }) {
                        Text("Guardar applicationContext (bien)")
                    }
                    Button(onClick = { golesQueSePierden++ }) {
                        Text("Gol (se pierde al rotar): $golesQueSePierden")
                    }
                    Button(onClick = { golesQueSobreviven++ }) {
                        Text("Gol (sobrevive): $golesQueSobreviven")
                    }
                    tabla.forEach { fila ->
                        Text("${fila.equipo.nombre}: ${fila.puntos} pts")
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
        // Un Bundle es un mapa pequeno (clave, valor): solo datos sencillos y poco
        // peso (limite practico de ~1 MB para TODO el proceso). Es para el ESTADO DE
        // LA PANTALLA, no para guardar datos de verdad: eso llega con Room y DataStore.
        outState.putInt(CLAVE_GOLES, golesQueSobreviven)
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

    private companion object {
        const val CLAVE_GOLES = "golesQueSobreviven"
        // Con 100 millones de vueltas el calculo tarda decenas de segundos: de sobra para un ANR.
        const val SIMULACIONES = 100_000_000
    }

    private fun anotar(mensaje: String) {
        // Log.d(tag, mensaje): nivel Debug. El tag es el filtro para encontrarlo.
        Log.d("Ciclo", "${javaClass.simpleName}@${System.identityHashCode(this).toString(16)} $mensaje")
    }
}
