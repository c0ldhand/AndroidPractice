package com.example.practice.domain.repository

import com.example.practice.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeTracks(): Flow<List<Track>>
    suspend fun getTrack(id: Long): Track?
    suspend fun add(track: Track)
    suspend fun remove(id: Long)
}
