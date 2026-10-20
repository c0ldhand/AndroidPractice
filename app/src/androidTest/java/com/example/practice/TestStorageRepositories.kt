package com.example.practice

import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.FavoritesRepository
import com.example.practice.domain.repository.FiltersRepository
import kotlinx.coroutines.flow.MutableStateFlow

class TestFiltersRepository : FiltersRepository {
    private var filters = TrackFilters()
    override fun load() = filters
    override suspend fun save(filters: TrackFilters) { this.filters = filters }
}

class TestFavoritesRepository : FavoritesRepository {
    private val tracks = MutableStateFlow<List<Track>>(emptyList())
    override fun observeTracks() = tracks
    override suspend fun getTrack(id: Long) = tracks.value.find { it.id == id }
    override suspend fun add(track: Track) { tracks.value = tracks.value.filterNot { it.id == track.id } + track }
    override suspend fun remove(id: Long) { tracks.value = tracks.value.filterNot { it.id == id } }
}
