package com.sebastiannarvaez.marcador

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

// Intent IMPLICITO: no dices QUE app, dices QUE quieres hacer (ACTION_SEND con un
// texto) y el sistema ofrece las apps que saben hacerlo (mensajes, correo...).
// `createChooser` fuerza que salga siempre el selector, aunque haya una por defecto.
fun Context.compartir(texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    startActivity(Intent.createChooser(envio, "Compartir resultado"))
}

// Otro implicito: ACTION_VIEW con una URL abre el navegador. Si no hubiera ninguna
// app capaz (un emulador sin navegador, por ejemplo) startActivity lanza
// ActivityNotFoundException: hay que atraparla.
fun Context.abrirEnlace(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Log.w("Intents", "Nadie sabe abrir $url", e)
        Toast.makeText(this, "No hay app para abrir el enlace", Toast.LENGTH_SHORT).show()
    }
}
