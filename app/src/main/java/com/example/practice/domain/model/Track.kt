package com.example.practice.domain.model

/** Domain model: id corresponds to trackId, genre to primaryGenreName in iTunes. */
data class Track(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val collectionName: String,
    val genre: String,
    val releaseDate: String,
    val trackTimeMillis: Long,
    val trackPrice: Double?,
    val currency: String,
    val country: String,
    val artworkUrl: String? = null,
    val previewUrl: String? = null,
    val isStreamable: Boolean = true
)
