package com.example.practice.data

import com.example.practice.domain.model.Track
import com.example.practice.domain.repository.FavoritesRepository
import com.example.practice.domain.repository.TrackRepository

/** Saved details never require a network request, even after process recreation. */
class LocalFirstTrackRepository(
    private val network: TrackRepository,
    private val favorites: FavoritesRepository
) : TrackRepository {
    override suspend fun searchTracks(query: String, limit: Int): List<Track> = network.searchTracks(query, limit)
    override suspend fun getTrackById(id: Long): Track? = favorites.getTrack(id) ?: network.getTrackById(id)
}
