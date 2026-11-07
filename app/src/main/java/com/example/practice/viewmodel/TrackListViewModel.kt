package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import com.example.practice.data.TrackRepository
import com.example.practice.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackListUiState(val tracks: List<Track> = emptyList())

class TrackListViewModel(repository: TrackRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TrackListUiState(repository.getTracks()))
    val uiState: StateFlow<TrackListUiState> = _uiState.asStateFlow()
}
