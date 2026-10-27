package com.example.practice.domain.repository

import com.example.practice.domain.model.Track

interface TrackRepository {
    suspend fun searchTracks(query: String, limit: Int = 50): List<Track>
    suspend fun getTrackById(id: Long): Track?
}
