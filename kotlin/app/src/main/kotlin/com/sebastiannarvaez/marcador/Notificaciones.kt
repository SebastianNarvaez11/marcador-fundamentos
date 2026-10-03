package com.sebastiannarvaez.marcador

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sebastiannarvaez.marcador.torneo.Gol

const val CANAL_GOLES = "goles"
const val CANAL_RECORDATORIOS = "recordatorios"

// Un CANAL agrupa notificaciones del mismo tipo. Desde Android 8 (API 26) toda
// notificacion pertenece a uno, y es el USUARIO quien decide (en Ajustes) su
// sonido e importancia por canal. Se crea una vez, al arrancar: crearlo de nuevo
// con el mismo id no hace nada. En API 24-25 no existen los canales, de ahi el `if`.
fun crearCanalDeGoles(contexto: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val canal = NotificationChannel(CANAL_GOLES, "Goles", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Avisa cuando se marca un gol"
    }
    val recordatorios = NotificationChannel(CANAL_RECORDATORIOS, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
    val gestor = contexto.getSystemService(NotificationManager::class.java)
    gestor.createNotificationChannel(canal)
    gestor.createNotificationChannel(recordatorios)
}

// Desde Android 13 (API 33) publicar notificaciones es un permiso PELIGROSO: hay
// que pedirlo en tiempo de ejecucion. Antes de la 33 se concede solo.
fun puedeNotificar(contexto: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

// `notify` lanza SecurityException si falta el permiso y el revisor (lint) no lo sabe
// seguir a traves de `puedeNotificar`; por eso se comprueba aqui y se silencia el aviso.
@SuppressLint("MissingPermission")
fun notificarGol(contexto: Context, gol: Gol) {
    if (!puedeNotificar(contexto)) return

    // Un PendingIntent es un Intent que se entrega a OTRA app (el sistema) para
    // que lo lance en tu nombre cuando el usuario toque la notificacion.
    // FLAG_IMMUTABLE es obligatorio desde la API 31 salvo que necesites mutarlo.
    // El requestCode (aqui, el minuto) distingue unos PendingIntent de otros: con el
    // mismo codigo y FLAG_UPDATE_CURRENT, todos los goles compartirian el mismo y
    // al tocar CUALQUIER notificacion se veria el texto del ultimo gol.
    val alTocar = PendingIntent.getActivity(
        contexto,
        gol.minuto,
        Intent(contexto, DetalleActivity::class.java)
            .putExtra(DetalleActivity.EXTRA_RESUMEN, "${gol.minuto}' ${gol.jugador.nombre} (${gol.equipo.nombre})"),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val notificacion = NotificationCompat.Builder(contexto, CANAL_GOLES)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("¡Gol!")
        .setContentText("${gol.jugador.nombre} (${gol.equipo.nombre}), minuto ${gol.minuto}")
        .setContentIntent(alTocar)
        .setAutoCancel(true)
        .build()

    // El id distingue notificaciones: con el mismo id, la nueva sustituye a la anterior.
    contexto.getSystemService(NotificationManager::class.java).notify(gol.minuto, notificacion)
}
