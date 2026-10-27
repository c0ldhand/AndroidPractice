package com.example.practice.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.practice.data.TrackRepository
import com.example.practice.model.Track
import com.example.practice.navigation.Screen
import com.example.practice.ui.formatting.formatDuration
import com.example.practice.ui.formatting.formatPrice
import com.example.practice.ui.formatting.formatReleaseDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackDetailUiState(
    val track: Track? = null,
    val releaseDate: String = "",
    val duration: String = "",
    val price: String? = null
)

class TrackDetailViewModel(repository: TrackRepository, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val track = savedStateHandle.get<Long>(Screen.TrackDetail.TRACK_ID)?.let(repository::getTrackById)
    val uiState = MutableStateFlow(
        TrackDetailUiState(
            track = track,
            releaseDate = track?.let { formatReleaseDate(it.releaseDate) }.orEmpty(),
            duration = track?.let { formatDuration(it.trackTimeMillis) }.orEmpty(),
            price = track?.let { formatPrice(it.trackPrice, it.currency) }
        )
    ).asStateFlow()
}
