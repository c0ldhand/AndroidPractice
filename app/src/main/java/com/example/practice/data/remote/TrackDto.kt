package com.example.practice.data.remote

import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackError
import com.example.practice.domain.model.TrackException

// Nullable transport fields also handle absent fields in Gson responses.
data class ItunesResponseDto(val resultCount: Int?, val results: List<TrackDto>?) {
    fun toTracks(): List<Track> {
        val items = results ?: throw TrackException(TrackError.INVALID_RESPONSE)
        return items.filter { it.kind == "song" }.map { it.toDomain() }.distinctBy { it.id }
    }
}

data class TrackDto(
    val trackId: Long?,
    val kind: String?,
    val trackName: String?,
    val artistName: String?,
    val collectionName: String?,
    val primaryGenreName: String?,
    val releaseDate: String?,
    val trackTimeMillis: Long?,
    val trackPrice: Double?,
    val currency: String?,
    val country: String?,
    val artworkUrl100: String?,
    val previewUrl: String?,
    val isStreamable: Boolean?
) {
    fun toDomain(): Track {
        val id = trackId?.takeIf { it > 0 }
            ?: throw TrackException(TrackError.INVALID_RESPONSE)
        val name = trackName?.takeIf { it.isNotBlank() }
            ?: throw TrackException(TrackError.INVALID_RESPONSE)
        val artist = artistName?.takeIf { it.isNotBlank() }
            ?: throw TrackException(TrackError.INVALID_RESPONSE)
        return Track(
            id = id,
            trackName = name,
            artistName = artist,
            collectionName = collectionName ?: "—",
            genre = primaryGenreName ?: "—",
            releaseDate = releaseDate ?: "—",
            trackTimeMillis = trackTimeMillis?.coerceAtLeast(0) ?: 0,
            trackPrice = trackPrice?.takeIf { it >= 0 },
            currency = currency ?: "—",
            country = country ?: "—",
            artworkUrl = artworkUrl100,
            previewUrl = previewUrl,
            isStreamable = isStreamable ?: false
        )
    }
}
