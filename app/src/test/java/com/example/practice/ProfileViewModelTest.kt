package com.example.practice

import androidx.lifecycle.SavedStateHandle
import com.example.practice.domain.model.ResumeDocument
import com.example.practice.domain.model.UserProfile
import com.example.practice.domain.repository.ProfileMediaRepository
import com.example.practice.domain.repository.ProfileRepository
import com.example.practice.domain.repository.ResumeRepository
import com.example.practice.domain.usecase.InvalidResumeUrlException
import com.example.practice.domain.usecase.SaveProfileUseCase
import com.example.practice.domain.usecase.normalizeResumeUrl
import com.example.practice.viewmodel.ProfileEditViewModel
import com.example.practice.viewmodel.ProfileError
import com.example.practice.viewmodel.ProfileViewModel
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun normalizesHttpsLinksAndRejectsInvalidOrNonWebLinks() {
        assertEquals("", normalizeResumeUrl("  "))
        assertEquals("https://example.com/cv.pdf", normalizeResumeUrl(" example.com/cv.pdf "))
        listOf("file:///cv.pdf", "javascript:alert(1)", "http://example.com/a", "https://", "https://user:secret@example.com/a", "not a link").forEach {
            assertThrows(InvalidResumeUrlException::class.java) { normalizeResumeUrl(it) }
        }
    }

    @Test fun draftIsSeparateAndSaveImmediatelyUpdatesObservedProfile() = runTest(main.dispatcher) {
        val repository = MemoryProfile(UserProfile(fullName = "Old name"))
        val profile = ProfileViewModel(repository, MemoryResumes())
        val scheduled = mutableListOf<Pair<Long, String>>()
        val edit = editor(repository, schedule = { time, name -> scheduled.add(time to name) })
        advanceUntilIdle()
        edit.setName("  New name  ")
        edit.setResumeUrl("example.com/resume.pdf")
        edit.setFavoriteClassTime("12:30")
        assertEquals("Old name", profile.uiState.value.profile.fullName)
        edit.save()
        advanceUntilIdle()
        assertTrue(edit.uiState.value.saved)
        assertEquals("New name", profile.uiState.value.profile.fullName)
        assertEquals("https://example.com/resume.pdf", repository.profile.value.resumeUrl)
        assertEquals("12:30", repository.profile.value.favoriteClassTime)
        val trigger = today().apply { set(Calendar.MINUTE, 30) }.timeInMillis
        assertEquals(listOf(trigger to "New name"), scheduled)
    }

    @Test fun rejectsEmptyMalformedAndPastClassTime() = runTest(main.dispatcher) {
        val repository = MemoryProfile()
        val edit = editor(repository, schedule = { _, _ -> fail("Unexpected alarm") })
        advanceUntilIdle()
        for (time in listOf("", "9:05", "12:5", "12-05", "24:00", "12:60", "12:05 ", "text")) {
            edit.setFavoriteClassTime(time)
            assertFalse(edit.uiState.value.canSave)
            edit.save()
        }
        edit.setFavoriteClassTime("11:59")
        assertTrue(edit.uiState.value.hasValidClassTime)
        edit.save()
        advanceUntilIdle()
        assertEquals(ProfileError.CLASS_TIME_UNAVAILABLE, edit.uiState.value.error)
        assertEquals(UserProfile(), repository.profile.value)
    }

    @Test fun failedSaveKeepsDraftAndDoesNotNavigateOrChangeProfile() = runTest(main.dispatcher) {
        val repository = MemoryProfile(UserProfile(fullName = "Saved"))
        val scheduledNames = mutableListOf<String>()
        val edit = editor(repository, schedule = { _, name -> scheduledNames.add(name) })
        advanceUntilIdle()
        edit.setName("Draft")
        edit.setFavoriteClassTime("12:30")
        repository.failSave = true
        edit.save()
        advanceUntilIdle()
        assertEquals(ProfileError.SAVE, edit.uiState.value.error)
        assertFalse(edit.uiState.value.saved)
        assertEquals("Draft", edit.uiState.value.draft.fullName)
        assertEquals("Saved", repository.profile.value.fullName)
        assertTrue(scheduledNames.isEmpty())
        repository.failSave = false
        edit.save()
        advanceUntilIdle()
        assertTrue(edit.uiState.value.saved)
        assertEquals(listOf("Draft"), scheduledNames)
    }

    @Test fun invalidUrlLeavesFormOpenAndDoesNotWrite() = runTest(main.dispatcher) {
        val repository = MemoryProfile()
        val edit = editor(repository)
        advanceUntilIdle()
        edit.setResumeUrl("not a URL")
        edit.setFavoriteClassTime("12:30")
        edit.save()
        advanceUntilIdle()
        assertEquals(ProfileError.INVALID_URL, edit.uiState.value.error)
        assertFalse(edit.uiState.value.saved)
        assertEquals(UserProfile(), repository.profile.value)
    }

    @Test fun restoresDraftAndPendingCameraAfterRecreation() = runTest(main.dispatcher) {
        val repository = MemoryProfile()
        val media = MemoryMedia()
        val handle = SavedStateHandle()
        val edit = editor(repository, media, handle)
        advanceUntilIdle()
        edit.setName("Draft name")
        edit.setFavoriteClassTime("18:05")
        edit.prepareCamera()
        advanceUntilIdle()
        val restored = editor(repository, media, handle)
        advanceUntilIdle()
        restored.cameraResult(true)
        advanceUntilIdle()
        assertEquals("Draft name", restored.uiState.value.draft.fullName)
        assertEquals("18:05", restored.uiState.value.draft.favoriteClassTime)
        assertEquals("content://owned/avatar.jpg", restored.uiState.value.draft.avatarUri)
        assertEquals(listOf("content://owned/camera.jpg"), media.deleted)
        assertEquals(UserProfile(), repository.profile.value)
    }

    @Test fun cancelledCameraAndUnreadableImageKeepPreviousAvatar() = runTest(main.dispatcher) {
        val repository = MemoryProfile(UserProfile(avatarUri = "content://old/avatar"))
        val media = MemoryMedia()
        val edit = editor(repository, media)
        advanceUntilIdle()
        edit.prepareCamera()
        advanceUntilIdle()
        edit.cameraResult(false)
        advanceUntilIdle()
        assertEquals("content://old/avatar", edit.uiState.value.draft.avatarUri)
        media.failImport = true
        edit.selectGalleryPhoto("content://missing")
        advanceUntilIdle()
        assertEquals(ProfileError.PHOTO, edit.uiState.value.error)
        assertEquals("content://old/avatar", edit.uiState.value.draft.avatarUri)
    }

    @Test fun loadFailureCannotOverwriteProfileWithEmptyDraft() = runTest(main.dispatcher) {
        val repository = MemoryProfile(UserProfile(fullName = "Saved"))
        repository.failLoad = true
        val edit = editor(repository)
        advanceUntilIdle()
        edit.save()
        advanceUntilIdle()
        assertEquals(ProfileError.LOAD, edit.uiState.value.error)
        assertEquals("Saved", repository.profile.value.fullName)
    }

    @Test fun downloadPreventsDuplicateRequestsAndReportsFailure() = runTest(main.dispatcher) {
        val resumes = MemoryResumes().apply { gate = CompletableDeferred() }
        val vm = ProfileViewModel(MemoryProfile(UserProfile(resumeUrl = "https://example.com/cv.pdf")), resumes)
        advanceUntilIdle()
        vm.downloadResume()
        vm.downloadResume()
        advanceUntilIdle()
        assertEquals(1, resumes.calls)
        assertTrue(vm.uiState.value.downloading)
        resumes.gate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.downloading)
        resumes.fail = true
        vm.downloadResume()
        advanceUntilIdle()
        assertEquals(ProfileError.DOWNLOAD, vm.uiState.value.error)
    }

    private fun today() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 7, 12, 0, 0)
    }

    private fun editor(
        repo: MemoryProfile,
        media: MemoryMedia = MemoryMedia(),
        handle: SavedStateHandle = SavedStateHandle(),
        schedule: (Long, String) -> Unit = { _, _ -> }
    ) = ProfileEditViewModel(repo, media, SaveProfileUseCase(repo, schedule, ::today), handle)
}

private class MemoryProfile(initial: UserProfile = UserProfile()) : ProfileRepository {
    override val profile = MutableStateFlow(initial)
    var failSave = false
    var failLoad = false
    override suspend fun load(): UserProfile {
        if (failLoad) throw IOException()
        return profile.value
    }
    override suspend fun save(profile: UserProfile) {
        if (failSave) throw IOException()
        this.profile.value = profile
    }
}

private class MemoryMedia : ProfileMediaRepository {
    var failImport = false
    val deleted = mutableListOf<String>()
    override suspend fun galleryImages() = listOf("content://gallery/1")
    override suspend fun importAvatar(sourceUri: String): String {
        if (failImport) throw IOException()
        return "content://owned/avatar.jpg"
    }
    override suspend fun createCameraUri() = "content://owned/camera.jpg"
    override suspend fun deleteCameraPhoto(uri: String) { deleted += uri }
}

private class MemoryResumes : ResumeRepository {
    var calls = 0
    var fail = false
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun download(url: String): ResumeDocument {
        calls++
        gate?.await()
        if (fail) throw IOException()
        return ResumeDocument("content://owned/cv.pdf", "application/pdf")
    }
}
