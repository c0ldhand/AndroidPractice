package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.practice.data.TrackRepository

class TrackDetailViewModelFactory(private val repository: TrackRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(TrackDetailViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TrackDetailViewModel(repository, extras.createSavedStateHandle()) as T
    }
}
