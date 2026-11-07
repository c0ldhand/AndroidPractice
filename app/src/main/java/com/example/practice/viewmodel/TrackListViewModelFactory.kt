package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.practice.data.TrackRepository

class TrackListViewModelFactory(private val repository: TrackRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TrackListViewModel::class.java)) {
            "Unknown ViewModel: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TrackListViewModel(repository) as T
    }
}
