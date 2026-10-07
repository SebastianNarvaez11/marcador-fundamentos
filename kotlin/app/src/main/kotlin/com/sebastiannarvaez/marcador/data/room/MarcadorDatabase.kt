package com.sebastiannarvaez.marcador.data.room

import android.content.Context
import androidx.room3.AutoMigration
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
//
// Version 3: la tabla nueva `arbitro`. Una tabla nueva es el caso facil de una migracion
// automatica: Room compara 2.json con 3.json y escribe el CREATE TABLE. Los partidos y los
// goles de la version 2 se quedan como estaban.
@Database(
    entities = [PartidoEntity::class, GolEntity::class, ArbitroEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class MarcadorDatabase : RoomDatabase() {
    abstract fun partidoDao(): PartidoDao
    abstract fun arbitroDao(): ArbitroDao
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
