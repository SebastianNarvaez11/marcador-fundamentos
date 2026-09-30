package com.sebastiannarvaez.marcador

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

// Un Worker es UNA tarea que WorkManager ejecuta cuando toca, aunque tu app no
// este abierta e incluso si el movil se reinicia (WorkManager guarda las tareas
// en una base de datos propia). `CoroutineWorker` permite usar `suspend` dentro.
//
// WorkManager es para trabajo DIFERIBLE y GARANTIZADO: «avisame antes del partido»,
// «sube esto cuando haya red». NO es para algo que el usuario ve avanzar ahora
// mismo, ni para hacerlo en un instante exacto (puede retrasarse unos minutos).
//
// FOREGROUND SERVICE, la otra opcion: es para trabajo que el usuario NOTA mientras
// ocurre (musica, navegacion GPS, una descarga larga) y obliga a mostrar una
// notificacion permanente. Marcador no lo necesita: un recordatorio es diferible.
// Regla practica: si puede esperar, WorkManager; si no puede esperar y dura, un
// servicio en primer plano (y con permiso y tipo declarados en el Manifest).
class RecordatorioWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    // Se ejecuta en un hilo de fondo (Dispatchers.Default por defecto): aqui NO se
    // toca la pantalla. Devuelve Result.success(), failure() o retry().
    override suspend fun doWork(): Result {
        val partido = inputData.getString(CLAVE_PARTIDO) ?: return Result.failure()
        Log.d("Trabajo", "RecordatorioWorker ejecutado: $partido")
        publicar(partido)
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun publicar(partido: String) {
        if (!puedeNotificar(applicationContext)) return
        val notificacion = NotificationCompat.Builder(applicationContext, CANAL_RECORDATORIOS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("El partido empieza en una hora")
            .setContentText(partido)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java).notify(ID_NOTIFICACION, notificacion)
    }

    companion object {
        const val CLAVE_PARTIDO = "partido"
        private const val ID_NOTIFICACION = 1000
        private const val NOMBRE_UNICO = "recordatorio-partido"

        // En la app real serian 60 minutos; para verlo en una demo se puede pasar menos.
        fun programar(contexto: Context, partido: String, retrasoSegundos: Long = 60 * 60) {
            val peticion = OneTimeWorkRequestBuilder<RecordatorioWorker>()
                .setInitialDelay(retrasoSegundos, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putString(CLAVE_PARTIDO, partido).build())
                .build()
            // Trabajo UNICO con nombre: si pulsas dos veces, la segunda sustituye a la
            // primera en vez de acumular dos recordatorios.
            WorkManager.getInstance(contexto)
                .enqueueUniqueWork(NOMBRE_UNICO, ExistingWorkPolicy.REPLACE, peticion)
        }
    }
}
