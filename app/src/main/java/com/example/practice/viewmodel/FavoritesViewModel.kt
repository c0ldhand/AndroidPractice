package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.domain.model.Track
import com.example.practice.domain.repository.FavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val changeFailed: Boolean = false,
    val busyIds: Set<Long> = emptySet()
) {
    fun contains(id: Long): Boolean = tracks.any { it.id == id }
}

class FavoritesViewModel(private val repository: FavoritesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init { retry() }

    fun retry() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        observeJob = viewModelScope.launch {
            try {
                repository.observeTracks().collect { tracks ->
                    _uiState.update { it.copy(tracks = tracks, isLoading = false, loadFailed = false) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun toggle(track: Track) {
        val state = _uiState.value
        if (state.isLoading || state.loadFailed || track.id in state.busyIds) return
        _uiState.update { it.copy(busyIds = it.busyIds + track.id, changeFailed = false) }
        viewModelScope.launch {
            try {
                if (state.contains(track.id)) repository.remove(track.id) else repository.add(track)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(changeFailed = true) }
            } finally {
                _uiState.update { it.copy(busyIds = it.busyIds - track.id) }
            }
        }
    }

    fun dismissError() { _uiState.update { it.copy(changeFailed = false) } }
}
