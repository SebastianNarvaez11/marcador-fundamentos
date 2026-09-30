package com.sebastiannarvaez.marcador.data.room

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// f45 · LA VERSION 1 DEL ESQUEMA, guardada como REFERENCIA.
//
// La base de datos esta en la VERSION 2 (ver MarcadorDatabase). Esta clase es como era la
// tabla `gol` en la version 1: la misma que `GolEntity` pero SIN la columna `aMano`. No se
// registra en `@Database(entities = ...)`, asi que Room no la usa ni genera nada con ella; se
// deja en el repositorio para poder ver (y citar) el «antes» de la migracion, porque el
// cambio de version es un cambio de esquema y el codigo de la v1 ya no esta en ningun otro sitio.
// (`PartidoEntity` no cambio entre versiones.)
//
// COMO FUNCIONA LA MIGRACION 1 -> 2 (sigue funcionando; ninguna de estas dos piezas sobra):
//   - `AutoMigration(from = 1, to = 2)` en MarcadorDatabase le dice a Room que la genere.
//   - Room la calcula COMPARANDO dos ficheros que exporta el compilador (`exportSchema = true`):
//       app/schemas/<paquete>.MarcadorDatabase/1.json   <- la v1: es lo que describe esta clase
//       app/schemas/<paquete>.MarcadorDatabase/2.json   <- la v2, la actual
//     El 1.json TIENE que estar en el repositorio: si se borra, la migracion automatica no se
//     puede generar (error de KSP: «Schema '1.json' required for migration was not found at
//     the schema out folder ... Cannot generate auto migrations.», comprobado). Por eso ambos
//     ficheros se versionan con git.
//   - La diferencia es una columna nueva (`aMano INTEGER NOT NULL DEFAULT 0`, mira 2.json), que
//     la migracion anade con un `ALTER TABLE ... ADD COLUMN`. Por eso `aMano` lleva `@ColumnInfo(defaultValue = "0")`: sin valor por
//     defecto Room no sabria que poner en las filas que ya existian.
//
// COMO REGENERAR 1.json si se perdiera: deja `GolEntity` sin `aMano`, pon `version = 1` sin
// autoMigrations, compila (`./gradlew :app:assembleGratisDebug`) y copia el 1.json que
// aparece en app/schemas; despues deshaz los cambios.
@Deprecated("Version 1 del esquema: solo referencia. La entidad vigente es GolEntity.")
@Entity(
    tableName = "gol",
    foreignKeys = [
        ForeignKey(
            entity = PartidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["partidoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("partidoId")],
)
data class GolEntityV1(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partidoId: Int,
    val minuto: Int,
    val jugador: String,
    val equipo: String,
)
