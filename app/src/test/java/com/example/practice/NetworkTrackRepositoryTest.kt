package com.example.practice

import com.example.practice.data.NetworkTrackRepository
import com.example.practice.data.remote.ItunesApi
import com.example.practice.domain.model.TrackError
import com.example.practice.domain.model.TrackException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkTrackRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NetworkTrackRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ItunesApi::class.java)
        repository = NetworkTrackRepository(api)
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun searchEncodesParametersAndMapsSongsOnly() = runTest {
        server.enqueue(MockResponse().setBody("""{"resultCount":3,"results":[
            {"kind":"song","trackId":42,"trackName":"Get Lucky","artistName":"Daft Punk",
             "collectionName":"Random Access Memories","primaryGenreName":"Pop",
             "releaseDate":"2013-04-19T00:00:00Z","trackTimeMillis":369000,"trackPrice":1.29,
             "currency":"USD","country":"USA","isStreamable":true},
            {"kind":"song","trackId":42,"trackName":"Get Lucky","artistName":"Daft Punk"},
            {"wrapperType":"artist","artistId":7,"artistName":"Daft Punk"}]}"""))
        val track = repository.searchTracks("Daft Punk & friends", 25).single()
        assertEquals(42L, track.id)
        assertEquals("Pop", track.genre)
        assertEquals(369000L, track.trackTimeMillis)
        assertEquals(1.29, track.trackPrice!!, 0.001)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/search", url.encodedPath)
        assertEquals("Daft Punk & friends", url.queryParameter("term"))
        assertEquals("25", url.queryParameter("limit"))
        assertEquals("song", url.queryParameter("entity"))
        assertEquals("music", url.queryParameter("media"))
        assertEquals("US", url.queryParameter("country"))
    }

    @Test
    fun lookupUsesIdAndAllowsOptionalFields() = runTest {
        server.enqueue(MockResponse().setBody("""{"resultCount":1,"results":[
            {"kind":"song","trackId":99,"trackName":"Song","artistName":"Artist","trackPrice":-1}]}"""))
        val track = repository.getTrackById(99)!!
        assertEquals(99L, track.id)
        assertNull(track.trackPrice)
        assertEquals("—", track.collectionName)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/lookup", url.encodedPath)
        assertEquals("99", url.queryParameter("id"))
    }

    @Test
    fun noResultsAreSuccessfulAndMissingDetailIsNull() = runTest {
        repeat(2) { server.enqueue(MockResponse().setBody("""{"resultCount":0,"results":[]}""")) }
        assertTrue(repository.searchTracks("missing", 50).isEmpty())
        assertNull(repository.getTrackById(99))
    }

    @Test
    fun httpErrorsAreTranslated() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals(TrackError.SERVER, searchError())
        server.enqueue(MockResponse().setResponseCode(429))
        assertEquals(TrackError.RATE_LIMIT, searchError())
    }

    @Test
    fun malformedAndIncompleteResponsesAreRejected() = runTest {
        for (body in listOf("not json", "{}", """{"results":[{"kind":"song","trackName":"Missing ID"}]}""")) {
            server.enqueue(MockResponse().setBody(body))
            assertEquals(TrackError.INVALID_RESPONSE, searchError())
        }
    }

    @Test
    fun slowServerProducesTimeout() = runTest {
        server.enqueue(MockResponse().setBody("""{"results":[]}""").setBodyDelay(1, TimeUnit.SECONDS))
        assertEquals(TrackError.TIMEOUT, searchError())
    }

    @Test
    fun unreachableServerProducesConnectionError() = runTest {
        server.shutdown()
        assertEquals(TrackError.CONNECTION, searchError())
    }

    private suspend fun searchError(): TrackError {
        try {
            repository.searchTracks("test", 50)
            fail("Expected a domain error")
        } catch (error: TrackException) {
            return error.error
        }
        error("Expected a domain error")
    }
}
