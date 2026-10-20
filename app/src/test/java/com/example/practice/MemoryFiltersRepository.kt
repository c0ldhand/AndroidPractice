package com.example.practice

import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.FiltersRepository

class MemoryFiltersRepository(var filters: TrackFilters = TrackFilters()) : FiltersRepository {
    override fun load() = filters
    override suspend fun save(filters: TrackFilters) { this.filters = filters }
}
