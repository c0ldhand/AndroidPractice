package com.example.practice.data

import com.example.practice.data.remote.ItunesApi
import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackError
import com.example.practice.domain.model.TrackException
import com.example.practice.domain.repository.TrackRepository
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class NetworkTrackRepository(
    private val api: ItunesApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TrackRepository {
    override suspend fun searchTracks(query: String, limit: Int): List<Track> = request {
        api.searchTracks(term = query, limit = limit).toTracks()
    }

    override suspend fun getTrackById(id: Long): Track? = request {
        api.lookupTrack(id).toTracks().firstOrNull { it.id == id }
    }

    private suspend fun <T> request(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: TrackException) {
            throw error
        } catch (error: HttpException) {
            throw TrackException(
                if (error.code() == 429) TrackError.RATE_LIMIT else TrackError.SERVER, error
            )
        } catch (error: SocketTimeoutException) {
            throw TrackException(TrackError.TIMEOUT, error)
        } catch (error: JsonParseException) {
            throw TrackException(TrackError.INVALID_RESPONSE, error)
        } catch (error: MalformedJsonException) {
            throw TrackException(TrackError.INVALID_RESPONSE, error)
        } catch (error: IOException) {
            throw TrackException(TrackError.CONNECTION, error)
        }
    }
}
