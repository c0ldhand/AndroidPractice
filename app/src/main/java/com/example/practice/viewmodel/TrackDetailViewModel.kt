package com.example.practice.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackError
import com.example.practice.domain.model.TrackException
import com.example.practice.domain.usecase.GetTrackDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrackDetailUiState(
    val track: Track? = null,
    val isLoading: Boolean = true,
    val error: TrackError? = null
)

class TrackDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getTrackDetails: GetTrackDetailsUseCase
) : ViewModel() {
    private val trackId: Long = savedStateHandle["trackId"] ?: -1L
    private val _uiState = MutableStateFlow(TrackDetailUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { retry() }

    fun retry() {
        loadJob?.cancel()
        _uiState.value = TrackDetailUiState()
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = TrackDetailUiState(track = getTrackDetails(trackId), isLoading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = TrackDetailUiState(
                    isLoading = false, error = (error as? TrackException)?.error ?: TrackError.UNKNOWN
                )
            }
        }
    }
}
