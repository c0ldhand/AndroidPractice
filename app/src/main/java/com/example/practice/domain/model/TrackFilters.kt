package com.example.practice.domain.model

data class TrackFilters(
    val genre: String = "",
    val album: String = "",
    val streamableOnly: Boolean = false
) {
    val isActive: Boolean get() = genre.isNotBlank() || album.isNotBlank() || streamableOnly

    fun normalized() = copy(genre = genre.trim(), album = album.trim())

    fun matches(track: Track): Boolean =
        track.genre.contains(genre.trim(), ignoreCase = true) &&
            track.collectionName.contains(album.trim(), ignoreCase = true) &&
            (!streamableOnly || track.isStreamable)
}
