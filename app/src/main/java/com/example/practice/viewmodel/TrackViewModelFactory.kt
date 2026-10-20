package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.practice.domain.usecase.GetTrackDetailsUseCase
import com.example.practice.domain.usecase.SearchTracksUseCase
import com.example.practice.domain.model.FilterBadgeCache
import com.example.practice.domain.repository.FiltersRepository
import com.example.practice.domain.repository.FavoritesRepository

class TrackViewModelFactory(
    private val searchTracks: SearchTracksUseCase,
    private val getTrackDetails: GetTrackDetailsUseCase,
    private val filtersRepository: FiltersRepository,
    private val favoritesRepository: FavoritesRepository,
    private val badgeCache: FilterBadgeCache
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel = when (modelClass) {
            TrackViewModel::class.java -> TrackViewModel(searchTracks, filtersRepository, badgeCache)
            TrackDetailViewModel::class.java -> TrackDetailViewModel(extras.createSavedStateHandle(), getTrackDetails)
            FiltersViewModel::class.java -> FiltersViewModel(extras.createSavedStateHandle(), filtersRepository, badgeCache)
            FavoritesViewModel::class.java -> FavoritesViewModel(favoritesRepository)
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
