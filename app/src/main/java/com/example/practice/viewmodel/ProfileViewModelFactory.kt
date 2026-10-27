package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.practice.domain.repository.ProfileMediaRepository
import com.example.practice.domain.repository.ProfileRepository
import com.example.practice.domain.repository.ResumeRepository
import com.example.practice.domain.usecase.SaveProfileUseCase

class ProfileViewModelFactory(
    private val repository: ProfileRepository,
    private val media: ProfileMediaRepository,
    private val resumes: ResumeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel = when (modelClass) {
            ProfileViewModel::class.java -> ProfileViewModel(repository, resumes)
            ProfileEditViewModel::class.java -> ProfileEditViewModel(repository, media, SaveProfileUseCase(repository), extras.createSavedStateHandle())
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
