package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.practice.domain.usecase.GetTrackDetailsUseCase
import com.example.practice.domain.usecase.SearchTracksUseCase

class TrackViewModelFactory(
    private val searchTracks: SearchTracksUseCase,
    private val getTrackDetails: GetTrackDetailsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel = when (modelClass) {
            TrackViewModel::class.java -> TrackViewModel(searchTracks)
            TrackDetailViewModel::class.java -> TrackDetailViewModel(extras.createSavedStateHandle(), getTrackDetails)
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
