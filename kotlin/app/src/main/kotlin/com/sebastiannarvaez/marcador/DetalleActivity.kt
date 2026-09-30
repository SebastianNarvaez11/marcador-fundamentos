package com.sebastiannarvaez.marcador

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Segunda pantalla. Se puede abrir de DOS formas:
//  1. Con un intent EXPLICITO desde MainActivity (`EXTRA_RESUMEN` en el Intent).
//  2. Con un intent IMPLICITO que casa con el intent-filter del Manifest: un
//     enlace `marcador://partido/2`. Prueba desde el ordenador:
//       adb shell am start -a android.intent.action.VIEW -d "marcador://partido/2"
class DetalleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val texto = textoRecibido(intent)
        Log.d("Intents", "DetalleActivity abierta con action=${intent.action} data=${intent.data} -> $texto")
        setContent {
            MaterialTheme {
                Column(Modifier.padding(24.dp)) {
                    Text("Detalle del partido")
                    Text(texto)
                    Button(onClick = { compartir(texto) }) { Text("Compartir") }
                    // finish() saca esta pantalla de la pila: vuelves a la anterior.
                    Button(onClick = { finish() }) { Text("Volver") }
                }
            }
        }
    }

    // Un Intent trae DOS tipos de informacion: lo que se quiere hacer (action,
    // data) y datos sueltos («extras»). Aqui se lee de donde toque.
    private fun textoRecibido(intent: Intent): String {
        // Si viene de un enlace, `intent.data` es la URI marcador://partido/2.
        val numero = intent.data?.lastPathSegment?.toIntOrNull()
        if (numero != null) {
            return partidosDeEjemplo[numero]?.resumen() ?: "No conozco el partido $numero"
        }
        // Si viene del intent explicito, el resumen va en un extra.
        return intent.getStringExtra(EXTRA_RESUMEN) ?: "Sin partido"
    }

    companion object {
        // Convencion: prefijar la clave con el paquete, para no chocar con otras.
        const val EXTRA_RESUMEN = "com.sebastiannarvaez.marcador.RESUMEN"
    }
}
