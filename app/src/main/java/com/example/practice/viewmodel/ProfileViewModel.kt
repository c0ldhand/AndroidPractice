package com.example.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.domain.model.ResumeDocument
import com.example.practice.domain.model.UserProfile
import com.example.practice.domain.repository.ProfileRepository
import com.example.practice.domain.repository.ResumeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileError { LOAD, SAVE, INVALID_URL, PHOTO, GALLERY, CAMERA, DOWNLOAD, NO_VIEWER }

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val loading: Boolean = true,
    val downloading: Boolean = false,
    val error: ProfileError? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val resumes: ResumeRepository
) : ViewModel() {
    private val state = MutableStateFlow(ProfileUiState())
    val uiState = state.asStateFlow()
    private val documents = Channel<ResumeDocument>(Channel.BUFFERED)
    val openDocument = documents.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.profile.collect { profile -> state.update { it.copy(profile = profile) } }
        }
        reload()
    }

    fun reload() {
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                repository.load()
                state.update { it.copy(loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(loading = false, error = ProfileError.LOAD) } }
        }
    }

    fun downloadResume() {
        val snapshot = state.value
        if (snapshot.loading || snapshot.downloading || snapshot.profile.resumeUrl.isBlank()) return
        state.update { it.copy(downloading = true, error = null) }
        viewModelScope.launch {
            try {
                documents.send(resumes.download(snapshot.profile.resumeUrl))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = ProfileError.DOWNLOAD) } }
            finally { state.update { it.copy(downloading = false) } }
        }
    }

    fun viewerUnavailable() { state.update { it.copy(error = ProfileError.NO_VIEWER) } }
    fun clearError() { state.update { it.copy(error = null) } }
}
