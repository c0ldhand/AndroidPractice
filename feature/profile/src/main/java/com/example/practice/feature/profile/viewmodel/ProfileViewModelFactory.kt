package com.example.practice.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.practice.feature.profile.domain.repository.ProfileMediaRepository
import com.example.practice.feature.profile.domain.repository.ProfileRepository
import com.example.practice.feature.profile.domain.repository.ResumeRepository
import com.example.practice.feature.profile.domain.usecase.SaveProfileUseCase

class ProfileViewModelFactory(
    private val repository: ProfileRepository,
    private val media: ProfileMediaRepository,
    private val resumes: ResumeRepository,
    private val saveProfile: SaveProfileUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel = when (modelClass) {
            ProfileViewModel::class.java -> ProfileViewModel(repository, resumes)
            ProfileEditViewModel::class.java -> ProfileEditViewModel(repository, media, saveProfile, extras.createSavedStateHandle())
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
