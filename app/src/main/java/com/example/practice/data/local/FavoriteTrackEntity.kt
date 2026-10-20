package com.example.practice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.practice.domain.model.Track

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val id: Long,
    val trackName: String,
    val artistName: String,
    val collectionName: String,
    val genre: String,
    val releaseDate: String,
    val trackTimeMillis: Long,
    val trackPrice: Double?,
    val currency: String,
    val country: String,
    val artworkUrl: String?,
    val previewUrl: String?,
    val isStreamable: Boolean
) {
    fun toTrack() = Track(
        id, trackName, artistName, collectionName, genre, releaseDate, trackTimeMillis,
        trackPrice, currency, country, artworkUrl, previewUrl, isStreamable
    )

    companion object {
        fun from(track: Track) = with(track) {
            FavoriteTrackEntity(
                id, trackName, artistName, collectionName, genre, releaseDate, trackTimeMillis,
                trackPrice, currency, country, artworkUrl, previewUrl, isStreamable
            )
        }
    }
}
