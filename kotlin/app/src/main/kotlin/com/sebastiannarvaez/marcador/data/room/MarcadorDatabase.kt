package com.sebastiannarvaez.marcador.data.room

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

// LA BASE DE DATOS: lista las entidades, su version y da acceso a los DAO.
// `exportSchema = true` escribe el esquema en app/schemas/<version>.json (ver build.gradle.kts).
//
// SOLO ANDROID: en un modulo KMP (la Pokedex) hace falta ademas un `expect object ...Constructor`
// con @ConstructedBy; en un modulo `com.android.application` NO (el compilador da
// «'expect' and 'actual' declarations can be used only in multiplatform projects»).
@Database(
    entities = [PartidoEntity::class, GolEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MarcadorDatabase : RoomDatabase() {
    abstract fun partidoDao(): PartidoDao
}

// Room 3 OBLIGA a elegir un DRIVER de SQLite (Room 2 usaba el del sistema sin preguntar).
//   AndroidSQLiteDriver  -> el SQLite que trae Android (el de `android.database.sqlite`).
//                           No engorda la app, pero su version depende del movil.
//   BundledSQLiteDriver  -> SQLite incrustado en la app (androidx.sqlite:sqlite-bundled):
//                           misma version en todos los moviles y en iOS, a cambio de unos MB.
// La Pokedex usa el bundled porque comparte codigo con iOS; aqui, solo Android, basta el del sistema.
fun crearBaseDeDatos(contexto: Context): MarcadorDatabase =
    Room.databaseBuilder<MarcadorDatabase>(
        context = contexto.applicationContext,
        name = contexto.getDatabasePath("marcador.db").absolutePath,
    )
        .setDriver(AndroidSQLiteDriver())
        .build()
