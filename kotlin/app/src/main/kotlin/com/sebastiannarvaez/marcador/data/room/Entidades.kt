package com.sebastiannarvaez.marcador.data.room

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// ENTIDADES: una clase = una TABLA; cada propiedad = una COLUMNA.
//
// Son clases APARTE del dominio a proposito: una fila tiene tipos de columna, clave primaria
// y anotaciones, y `Partido` (con Equipo, Jugador, listas de eventos) no cabe en una fila.
// El repositorio traduce entre las dos (ver PartidosRepositoryRoom).
//
// Un partido guarda los NOMBRES de sus equipos (los equipos del torneo son un catalogo
// fijo). Sus goles van en otra tabla: una lista dentro de una columna no existe en SQL.
@Entity(tableName = "partido")
data class PartidoEntity(
    @PrimaryKey val id: Int,
    val local: String,
    val visitante: String,
)

// Relacion 1 partido : N goles. `ForeignKey` hace que la BASE DE DATOS impida un gol de un
// partido que no existe, y CASCADE borra los goles al borrar el partido. El indice sobre
// `partidoId` acelera «los goles de este partido» y Room avisa si falta.
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
data class GolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partidoId: Int,
    val minuto: Int,
    val jugador: String,
    val equipo: String,
)

// El resultado de una consulta con GROUP BY no es una tabla: es una clase con las columnas
// que devuelve el SELECT (los nombres deben coincidir con los alias).
data class GoleadorFila(val jugador: String, val goles: Int)
