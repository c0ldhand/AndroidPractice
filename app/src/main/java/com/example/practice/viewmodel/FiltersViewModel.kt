package com.example.practice.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.domain.model.FilterBadgeCache
import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.FiltersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FiltersUiState(
    val filters: TrackFilters,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false
)

class FiltersViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: FiltersRepository,
    private val badgeCache: FilterBadgeCache
) : ViewModel() {
    private val stored = repository.load()
    private val _uiState = MutableStateFlow(
        FiltersUiState(
            TrackFilters(
                savedStateHandle["genre"] ?: stored.genre,
                savedStateHandle["album"] ?: stored.album,
                savedStateHandle["streamableOnly"] ?: stored.streamableOnly
            )
        )
    )
    val uiState = _uiState.asStateFlow()

    init { badgeCache.update(stored) }

    fun update(filters: TrackFilters) {
        if (_uiState.value.isSaving) return
        savedStateHandle["genre"] = filters.genre
        savedStateHandle["album"] = filters.album
        savedStateHandle["streamableOnly"] = filters.streamableOnly
        _uiState.update { it.copy(filters = filters, saveFailed = false) }
    }

    fun reset() = update(TrackFilters())

    fun save() {
        if (_uiState.value.isSaving || _uiState.value.isSaved) return
        val filters = _uiState.value.filters.normalized()
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.save(filters)
                badgeCache.update(filters)
                _uiState.update { it.copy(filters = filters, isSaving = false, isSaved = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
