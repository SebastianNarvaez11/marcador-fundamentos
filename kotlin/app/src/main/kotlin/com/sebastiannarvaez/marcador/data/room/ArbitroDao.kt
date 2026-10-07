package com.sebastiannarvaez.marcador.data.room

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

// La tabla `arbitro`: la COPIA LOCAL de lo que dio la red. El id es el del servidor, asi
// que refrescar no duplica filas: la misma persona, la misma fila.
@Entity(tableName = "arbitro")
data class ArbitroEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val ciudad: String,
)

@Dao
interface ArbitroDao {
    // Flow: emite lo guardado y vuelve a emitir cada vez que la tabla cambia.
    @Query("SELECT * FROM arbitro ORDER BY nombre")
    fun observarArbitros(): Flow<List<ArbitroEntity>>

    // UPSERT = «update o insert»: si el id ya existe, actualiza la fila; si no, la inserta.
    @Upsert
    suspend fun guardar(arbitros: List<ArbitroEntity>)
}
