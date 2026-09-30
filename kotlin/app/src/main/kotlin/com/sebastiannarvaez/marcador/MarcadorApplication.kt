package com.sebastiannarvaez.marcador

import android.app.Application
import android.util.Log

// La clase Application: la crea Android UNA vez, antes que cualquier Activity, y
// vive tanto como el proceso. Se registra en el Manifest con android:name.
//
// Es un Context (Application extiende ContextWrapper) que NO depende de ninguna
// pantalla: por eso es seguro guardarlo en un singleton. Una Activity tambien es
// un Context, pero muere al rotar; guardarla es una FUGA DE MEMORIA.
class MarcadorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("Ciclo", "MarcadorApplication onCreate (pid ${android.os.Process.myPid()})")
    }
}
