package com.sebastiannarvaez.marcador.data.room

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

// DAO (Data Access Object): la interfaz con las operaciones sobre la base. Room
// GENERA la implementacion (PartidoDao_Impl) al compilar leyendo estas anotaciones.
//
// En Room 3 TODO es corrutina:
//   - las lecturas que OBSERVAN devuelven Flow: emiten la lista actual y VUELVEN A EMITIR
//     cada vez que cambia una tabla de la consulta. Nadie recarga a mano;
//   - las escrituras y las lecturas puntuales son `suspend`.
// Room ejecuta todo fuera del hilo principal por su cuenta: no hay withContext.
@Dao
interface PartidoDao {

    @Query("SELECT * FROM partido ORDER BY id")
    fun observarPartidos(): Flow<List<PartidoEntity>>

    @Query("SELECT * FROM gol ORDER BY minuto, id")
    fun observarGoles(): Flow<List<GolEntity>>

    // GROUP BY: una fila por jugador; COUNT(*) cuenta sus goles. El alias `goles` es el
    // nombre de la propiedad de GoleadorFila.
    @Query("SELECT jugador, COUNT(*) AS goles FROM gol GROUP BY jugador ORDER BY goles DESC, jugador")
    fun observarGoleadores(): Flow<List<GoleadorFila>>

    @Query("SELECT COUNT(*) FROM partido")
    suspend fun contarPartidos(): Int

    @Query("SELECT * FROM partido WHERE id = :id")
    suspend fun partido(id: Int): PartidoEntity?

    @Query("SELECT * FROM gol WHERE partidoId = :partidoId ORDER BY minuto, id")
    suspend fun golesDe(partidoId: Int): List<GolEntity>

    @Insert
    suspend fun insertarPartidos(partidos: List<PartidoEntity>)

    @Insert
    suspend fun insertarGoles(goles: List<GolEntity>)

    @Insert
    suspend fun insertarGol(gol: GolEntity)
}
