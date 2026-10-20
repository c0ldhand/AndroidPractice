package com.example.practice

import androidx.lifecycle.SavedStateHandle
import com.example.practice.data.LocalFirstTrackRepository
import com.example.practice.domain.model.FilterBadgeCache
import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.FavoritesRepository
import com.example.practice.domain.repository.FiltersRepository
import com.example.practice.domain.repository.TrackRepository
import com.example.practice.domain.usecase.SearchTracksUseCase
import com.example.practice.viewmodel.FiltersViewModel
import com.example.practice.viewmodel.TrackViewModel
import com.example.practice.viewmodel.FavoritesViewModel
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalStorageTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val track = Track(1, "One More Time", "Daft Punk", "Discovery", "Dance", "2000", 320000, null, "USD", "USA")

    @Test fun filtersCombineCaseInsensitiveTextAndStreamability() = runTest {
        val repository = Catalog(listOf(track, track.copy(id = 2, isStreamable = false), track.copy(id = 3, collectionName = "Homework")))
        val result = SearchTracksUseCase(repository)("Daft Punk", filters = TrackFilters(" daNce ", " DISC ", true))
        assertEquals(listOf(track), result)
        assertFalse(TrackFilters("  ", " ").isActive)
        assertTrue(SearchTracksUseCase(repository)("Daft Punk", filters = TrackFilters(genre = "Rock")).isEmpty())
    }

    @Test fun draftsAndResetDoNotChangeStoredFiltersOrBadgeUntilDone() = runTest {
        val repository = MemoryFiltersRepository(TrackFilters(genre = "Dance"))
        val badge = FilterBadgeCache()
        val vm = FiltersViewModel(SavedStateHandle(), repository, badge)
        vm.reset()
        assertTrue(badge.isVisible.value)
        assertEquals("Dance", repository.load().genre)
        vm.save()
        advanceUntilIdle()
        assertEquals(TrackFilters(), repository.load())
        assertFalse(badge.isVisible.value)
        assertTrue(vm.uiState.value.isSaved)
    }

    @Test fun doneWaitsForSuccessfulStorageAndNormalizesValues() = runTest {
        val diskWrite = CompletableDeferred<Unit>()
        val repository = object : FiltersRepository {
            var stored = TrackFilters()
            override fun load() = stored
            override suspend fun save(filters: TrackFilters) { diskWrite.await(); stored = filters }
        }
        val badge = FilterBadgeCache()
        val vm = FiltersViewModel(SavedStateHandle(), repository, badge)
        vm.update(TrackFilters(" Dance ", " Discovery ", true))
        vm.save()
        runCurrent()
        assertTrue(vm.uiState.value.isSaving)
        assertFalse(vm.uiState.value.isSaved)
        assertFalse(badge.isVisible.value)
        diskWrite.complete(Unit)
        advanceUntilIdle()
        assertEquals(TrackFilters("Dance", "Discovery", true), repository.load())
        assertTrue(badge.isVisible.value)
    }

    @Test fun writeFailureKeepsScreenOpenAndBadgeUnchanged() = runTest {
        val repository = object : FiltersRepository {
            override fun load() = TrackFilters()
            override suspend fun save(filters: TrackFilters) { throw IOException("disk full") }
        }
        val badge = FilterBadgeCache()
        val vm = FiltersViewModel(SavedStateHandle(), repository, badge)
        vm.update(TrackFilters(genre = "Dance"))
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.saveFailed)
        assertFalse(vm.uiState.value.isSaved)
        assertFalse(badge.isVisible.value)
    }

    @Test fun listRestoresFiltersAndBadgeAndReloadsSubmittedQueryOnApply() = runTest {
        val repository = Catalog(listOf(track, track.copy(id = 2, genre = "Rock")))
        val filters = MemoryFiltersRepository(TrackFilters(genre = "dance"))
        val badge = FilterBadgeCache()
        val vm = TrackViewModel(SearchTracksUseCase(repository), filters, badge)
        advanceUntilIdle()
        assertEquals(listOf(track), vm.uiState.value.tracks)
        assertTrue(badge.isVisible.value)
        vm.updateQuery("Unsent draft")
        filters.save(TrackFilters())
        vm.onFiltersApplied()
        advanceUntilIdle()
        assertEquals(2, repository.searchCount)
        assertEquals("Daft Punk", repository.lastQuery)
        assertEquals(2, vm.uiState.value.tracks.size)
        assertFalse(badge.isVisible.value)
    }

    @Test fun offlineDetailsUseSavedTrackWithoutCallingNetwork() = runTest {
        val favorites = MemoryFavorites().also { it.add(track) }
        val network = Catalog(emptyList())
        val repository = LocalFirstTrackRepository(network, favorites)
        assertEquals(track, repository.getTrackById(1))
        assertEquals(0, network.detailCount)
        assertNull(repository.getTrackById(2))
        assertEquals(1, network.detailCount)
    }

    @Test fun favoritesFollowDatabaseChangesAndFailedRemovePreservesSavedItem() = runTest {
        val repository = MemoryFavorites()
        val vm = FavoritesViewModel(repository)
        runCurrent()
        vm.toggle(track)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.contains(1))
        repository.failRemove = true
        vm.toggle(track)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.contains(1))
        assertTrue(vm.uiState.value.changeFailed)
        assertTrue(vm.uiState.value.busyIds.isEmpty())
        repository.failRemove = false
        vm.toggle(track)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.contains(1))
    }

    private class Catalog(val tracks: List<Track>) : TrackRepository {
        var searchCount = 0
        var detailCount = 0
        var lastQuery = ""
        override suspend fun searchTracks(query: String, limit: Int): List<Track> {
            searchCount++; lastQuery = query
            return tracks
        }
        override suspend fun getTrackById(id: Long): Track? { detailCount++; return null }
    }

    private class MemoryFavorites : FavoritesRepository {
        val tracks = MutableStateFlow<List<Track>>(emptyList())
        var failRemove = false
        override fun observeTracks() = tracks
        override suspend fun getTrack(id: Long) = tracks.value.find { it.id == id }
        override suspend fun add(track: Track) { tracks.value = tracks.value.filterNot { it.id == track.id } + track }
        override suspend fun remove(id: Long) {
            if (failRemove) throw IOException("disk full")
            tracks.value = tracks.value.filterNot { it.id == id }
        }
    }
}
