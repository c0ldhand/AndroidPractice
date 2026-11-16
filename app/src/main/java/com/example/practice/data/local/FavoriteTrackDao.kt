package com.example.practice.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTrackDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY artistName COLLATE NOCASE, trackName COLLATE NOCASE, id")
    fun observeTracks(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT * FROM favorite_tracks WHERE id = :id")
    suspend fun getTrack(id: Long): FavoriteTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun delete(id: Long)
}
