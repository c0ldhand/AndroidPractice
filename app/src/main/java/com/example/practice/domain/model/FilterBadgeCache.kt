package com.example.practice.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One in-memory instance is injected into both list and settings ViewModels. */
class FilterBadgeCache {
    private val _isVisible = MutableStateFlow(false)
    val isVisible = _isVisible.asStateFlow()

    fun update(filters: TrackFilters) {
        _isVisible.value = filters.isActive
    }
}
