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
    // f44: el contenedor de dependencias. Las Activities y los ViewModels lo alcanzan
    // desde la Application; nadie mas construye repositorios.
    val contenedor by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        crearCanalDeGoles(this)
        Log.d("Ciclo", "MarcadorApplication onCreate (pid ${android.os.Process.myPid()})")
    }
}
