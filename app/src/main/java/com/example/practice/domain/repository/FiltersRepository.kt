package com.example.practice.domain.repository

import com.example.practice.domain.model.TrackFilters

interface FiltersRepository {
    fun load(): TrackFilters
    suspend fun save(filters: TrackFilters)
}
