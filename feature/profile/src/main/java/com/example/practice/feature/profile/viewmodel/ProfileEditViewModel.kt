package com.example.practice.feature.profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice.feature.profile.domain.model.ClassTime
import com.example.practice.feature.profile.domain.model.UserProfile
import com.example.practice.feature.profile.domain.repository.ProfileMediaRepository
import com.example.practice.feature.profile.domain.repository.ProfileRepository
import com.example.practice.feature.profile.domain.usecase.ClassReminderScheduleException
import com.example.practice.feature.profile.domain.usecase.ClassTimeUnavailableException
import com.example.practice.feature.profile.domain.usecase.InvalidResumeUrlException
import com.example.practice.feature.profile.domain.usecase.SaveProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileEditUiState(
    val draft: UserProfile = UserProfile(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val mediaBusy: Boolean = false,
    val gallery: List<String>? = null,
    val error: ProfileError? = null,
    val saved: Boolean = false
) {
    val hasValidClassTime: Boolean get() = ClassTime.parse(draft.favoriteClassTime) != null
    val canSave: Boolean get() = !loading && !saving && !mediaBusy && !saved &&
        error != ProfileError.LOAD && hasValidClassTime
}

class ProfileEditViewModel(
    private val repository: ProfileRepository,
    private val media: ProfileMediaRepository,
    private val saveProfile: SaveProfileUseCase,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val state = MutableStateFlow(ProfileEditUiState())
    val uiState = state.asStateFlow()
    private val cameraRequests = Channel<String>(Channel.BUFFERED)
    val openCamera = cameraRequests.receiveAsFlow()

    init { reload() }

    fun reload() {
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = if (savedState.get<Boolean>("draftLoaded") == true) {
                    UserProfile(
                        savedState["fullName"] ?: "", savedState["avatarUri"],
                        savedState["resumeUrl"] ?: "", savedState["position"] ?: "",
                        savedState["favoriteClassTime"] ?: ""
                    )
                } else repository.load()
                updateDraft(profile)
                state.update { it.copy(loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(loading = false, error = ProfileError.LOAD) } }
        }
    }

    private fun updateDraft(profile: UserProfile) {
        savedState["draftLoaded"] = true
        savedState["fullName"] = profile.fullName
        savedState["avatarUri"] = profile.avatarUri
        savedState["resumeUrl"] = profile.resumeUrl
        savedState["position"] = profile.position
        savedState["favoriteClassTime"] = profile.favoriteClassTime
        state.update { it.copy(draft = profile, error = null) }
    }
    fun setName(value: String) = updateDraft(state.value.draft.copy(fullName = value))
    fun setPosition(value: String) = updateDraft(state.value.draft.copy(position = value))
    fun setResumeUrl(value: String) = updateDraft(state.value.draft.copy(resumeUrl = value))
    fun setFavoriteClassTime(value: String) = updateDraft(state.value.draft.copy(favoriteClassTime = value))
    fun clearError() { state.update { it.copy(error = null) } }
    fun closeGallery() { state.update { it.copy(gallery = null) } }

    fun showGallery() {
        if (state.value.mediaBusy) return
        state.update { it.copy(mediaBusy = true, error = null) }
        viewModelScope.launch {
            try {
                val images = media.galleryImages()
                state.update { it.copy(gallery = images) }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = ProfileError.GALLERY) } }
            finally { state.update { it.copy(mediaBusy = false) } }
        }
    }

    fun selectGalleryPhoto(uri: String) {
        closeGallery()
        importPhoto(uri, camera = false)
    }

    fun prepareCamera() {
        if (state.value.mediaBusy) return
        state.update { it.copy(mediaBusy = true, error = null) }
        viewModelScope.launch {
            try {
                val uri = media.createCameraUri()
                savedState["pendingCameraUri"] = uri
                cameraRequests.send(uri)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = ProfileError.CAMERA) } }
            finally { state.update { it.copy(mediaBusy = false) } }
        }
    }

    fun cameraResult(success: Boolean) {
        val uri = savedState.get<String>("pendingCameraUri") ?: return
        savedState.remove<String>("pendingCameraUri")
        if (success) importPhoto(uri, camera = true)
        else viewModelScope.launch { runCatching { media.deleteCameraPhoto(uri) } }
    }

    private fun importPhoto(uri: String, camera: Boolean) {
        if (state.value.mediaBusy) return
        state.update { it.copy(mediaBusy = true, error = null) }
        viewModelScope.launch {
            try { updateDraft(state.value.draft.copy(avatarUri = media.importAvatar(uri))) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = ProfileError.PHOTO) } }
            finally {
                state.update { it.copy(mediaBusy = false) }
                if (camera) withContext(NonCancellable) { runCatching { media.deleteCameraPhoto(uri) } }
            }
        }
    }

    fun cameraUnavailable() {
        cameraResult(false)
        state.update { it.copy(error = ProfileError.CAMERA) }
    }

    fun save() {
        val snapshot = state.value
        if (!snapshot.canSave) return
        state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                saveProfile(snapshot.draft)
                state.update { it.copy(saved = true) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: InvalidResumeUrlException) { state.update { it.copy(error = ProfileError.INVALID_URL) } }
            catch (_: ClassTimeUnavailableException) { state.update { it.copy(error = ProfileError.CLASS_TIME_UNAVAILABLE) } }
            catch (_: ClassReminderScheduleException) { state.update { it.copy(error = ProfileError.REMINDER_SCHEDULE) } }
            catch (_: Exception) { state.update { it.copy(error = ProfileError.SAVE) } }
            finally { state.update { it.copy(saving = false) } }
        }
    }
}
