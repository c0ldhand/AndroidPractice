package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import com.example.practice.domain.model.Track
import androidx.lifecycle.viewModelScope
import com.example.practice.domain.model.TrackError
import com.example.practice.domain.model.TrackException
import com.example.practice.domain.usecase.SearchTracksUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackListUiState(
    val query: String = "Daft Punk",
    val submittedQuery: String = "",
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: TrackError? = null,
    val isQueryEmpty: Boolean = false
)

class TrackViewModel(private val searchTracks: SearchTracksUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackListUiState())
    val uiState: StateFlow<TrackListUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init { search() }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, isQueryEmpty = false) }
    }

    fun search() = load(_uiState.value.query.trim())

    fun retry() = load(_uiState.value.submittedQuery)

    private fun load(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(isQueryEmpty = true) }
            return
        }
        searchJob?.cancel()
        _uiState.update {
            it.copy(submittedQuery = query, tracks = emptyList(), isLoading = true, error = null, isQueryEmpty = false)
        }
        searchJob = viewModelScope.launch {
            try {
                val tracks = searchTracks(query)
                _uiState.update { it.copy(tracks = tracks, isLoading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = (error as? TrackException)?.error ?: TrackError.UNKNOWN)
                }
            }
        }
    }
}
