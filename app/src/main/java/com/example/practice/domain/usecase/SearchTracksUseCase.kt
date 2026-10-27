package com.example.practice.domain.usecase

import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.TrackRepository

class SearchTracksUseCase(private val repository: TrackRepository) {
    suspend operator fun invoke(query: String, limit: Int = 50, filters: TrackFilters = TrackFilters()): List<Track> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        return repository.searchTracks(term, limit.coerceIn(1, 200)).filter(filters::matches)
    }
}
