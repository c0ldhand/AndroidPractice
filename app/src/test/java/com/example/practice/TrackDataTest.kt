package com.example.practice

import com.example.practice.data.MockTrackRepository
import com.example.practice.data.TrackRepository
import com.example.practice.model.Track
import androidx.lifecycle.SavedStateHandle
import com.example.practice.navigation.Screen
import com.example.practice.viewmodel.TrackListViewModel
import com.example.practice.viewmodel.TrackDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDataTest {
    @Test
    fun catalogHasUniqueIdsAndValidLocalData() {
        val repository = MockTrackRepository()
        val tracks = repository.getTracks()
        assertEquals(18, tracks.size)
        assertEquals(tracks.size, tracks.map { it.id }.toSet().size)
        assertTrue(tracks.all { it.trackTimeMillis > 0 && it.trackName.isNotBlank() })
        assertTrue(tracks.all { it.artworkUrl == null && it.previewUrl == null })
        assertTrue(tracks.any { it.trackPrice == null })
        assertTrue(tracks.any { !it.isStreamable })
        tracks.forEach { assertEquals(it, repository.getTrackById(it.id)) }
        assertNull(repository.getTrackById(-1))
    }

    @Test
    fun viewModelWorksWithAnotherRepositoryImplementation() {
        val sample = MockTrackRepository().getTracks().first().copy(id = 42, trackName = "Local sample")
        val repository = object : TrackRepository {
            override fun getTracks(): List<Track> = listOf(sample)
            override fun getTrackById(id: Long): Track? = sample.takeIf { it.id == id }
        }
        val viewModel = TrackListViewModel(repository)
        assertEquals(listOf(sample), viewModel.uiState.value.tracks)
        val detail = TrackDetailViewModel(repository, SavedStateHandle(mapOf(Screen.TrackDetail.TRACK_ID to 42L)))
        assertEquals(sample, detail.uiState.value.track)
        assertEquals("5:20", detail.uiState.value.duration)
        val missing = TrackDetailViewModel(repository, SavedStateHandle(mapOf(Screen.TrackDetail.TRACK_ID to 1L)))
        assertNull(missing.uiState.value.track)
        assertNull(TrackDetailViewModel(repository, SavedStateHandle()).uiState.value.track)
    }
}
