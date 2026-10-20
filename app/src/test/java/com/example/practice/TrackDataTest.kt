package com.example.practice

import androidx.lifecycle.SavedStateHandle
import com.example.practice.domain.repository.TrackRepository
import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackError
import com.example.practice.domain.model.TrackException
import com.example.practice.domain.model.FilterBadgeCache
import com.example.practice.domain.usecase.SearchTracksUseCase
import com.example.practice.domain.usecase.GetTrackDetailsUseCase
import com.example.practice.viewmodel.TrackViewModel
import com.example.practice.viewmodel.TrackDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class TrackDataTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val sample = Track(42, "Get Lucky", "Daft Punk", "Album", "Pop", "2013-04-19", 369000, 1.29, "USD", "USA")

    @Test
    fun useCasesTrimQueryClampLimitAndSkipInvalidInput() = runTest {
        val repository = FakeRepository()
        val search = SearchTracksUseCase(repository)
        assertTrue(search("   ").isEmpty())
        assertNull(repository.lastQuery)
        assertEquals(listOf(sample), search("  Daft Punk  ", 500))
        assertEquals("Daft Punk", repository.lastQuery)
        assertEquals(200, repository.lastLimit)
        assertNull(GetTrackDetailsUseCase(repository)(-1))
        assertNull(repository.lastId)
    }

    @Test
    fun loadingIsVisibleUntilSearchCompletes() = runTest {
        val response = CompletableDeferred<List<Track>>()
        val repository = FakeRepository().also { it.search = { response.await() } }
        val vm = TrackViewModel(SearchTracksUseCase(repository), MemoryFiltersRepository(), FilterBadgeCache())
        assertTrue(vm.uiState.value.isLoading)
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        response.complete(listOf(sample))
        advanceUntilIdle()
        assertEquals(listOf(sample), vm.uiState.value.tracks)
        assertTrue(!vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun retryUsesFailedQueryEvenAfterEditingInput() = runTest {
        val repository = FakeRepository().also { it.search = { throw TrackException(TrackError.CONNECTION) } }
        val vm = TrackViewModel(SearchTracksUseCase(repository), MemoryFiltersRepository(), FilterBadgeCache())
        advanceUntilIdle()
        assertEquals(TrackError.CONNECTION, vm.uiState.value.error)
        vm.updateQuery("Queen")
        repository.search = { listOf(sample) }
        vm.retry()
        advanceUntilIdle()
        assertEquals("Daft Punk", repository.lastQuery)
        assertEquals("Queen", vm.uiState.value.query)
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(sample), vm.uiState.value.tracks)
    }

    @Test
    fun newSearchCancelsPreviousRequestAndKeepsLatestResult() = runTest {
        val oldResponse = CompletableDeferred<List<Track>>()
        val repository = FakeRepository().also {
            it.search = { query -> if (query == "Daft Punk") oldResponse.await() else listOf(sample.copy(trackName = "Queen")) }
        }
        val vm = TrackViewModel(SearchTracksUseCase(repository), MemoryFiltersRepository(), FilterBadgeCache())
        runCurrent()
        vm.updateQuery("Queen")
        vm.search()
        advanceUntilIdle()
        oldResponse.complete(listOf(sample))
        advanceUntilIdle()
        assertEquals("Queen", vm.uiState.value.tracks.single().trackName)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun blankQueryDoesNotSendRequestAndEmptyResultIsNotAnError() = runTest {
        val repository = FakeRepository().also { it.search = { emptyList() } }
        val vm = TrackViewModel(SearchTracksUseCase(repository), MemoryFiltersRepository(), FilterBadgeCache())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.tracks.isEmpty())
        assertNull(vm.uiState.value.error)
        vm.updateQuery("   ")
        vm.search()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isQueryEmpty)
        assertEquals("Daft Punk", repository.lastQuery)
    }

    @Test
    fun detailLoadsFromSavedIdAndRecoversAfterError() = runTest {
        val repository = FakeRepository().also { it.detail = { throw TrackException(TrackError.SERVER) } }
        val vm = TrackDetailViewModel(SavedStateHandle(mapOf("trackId" to 42L)), GetTrackDetailsUseCase(repository))
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals(TrackError.SERVER, vm.uiState.value.error)
        repository.detail = { sample }
        vm.retry()
        advanceUntilIdle()
        assertEquals(42L, repository.lastId)
        assertEquals(sample, vm.uiState.value.track)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun invalidDetailIdShowsNotFoundWithoutRequest() = runTest {
        val repository = FakeRepository()
        val vm = TrackDetailViewModel(SavedStateHandle(mapOf("trackId" to -1L)), GetTrackDetailsUseCase(repository))
        advanceUntilIdle()
        assertTrue(!vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.track)
        assertNull(vm.uiState.value.error)
        assertNull(repository.lastId)
    }

    private inner class FakeRepository : TrackRepository {
        var lastQuery: String? = null
        var lastLimit: Int? = null
        var lastId: Long? = null
        var search: suspend (String) -> List<Track> = { listOf(sample) }
        var detail: suspend (Long) -> Track? = { id -> sample.takeIf { it.id == id } }
        override suspend fun searchTracks(query: String, limit: Int): List<Track> {
            lastQuery = query
            lastLimit = limit
            return search(query)
        }
        override suspend fun getTrackById(id: Long): Track? {
            lastId = id
            return detail(id)
        }
    }
}
