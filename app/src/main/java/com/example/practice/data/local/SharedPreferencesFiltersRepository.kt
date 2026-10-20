package com.example.practice.data.local

import android.content.SharedPreferences
import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.FiltersRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesFiltersRepository(
    private val preferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FiltersRepository {
    override fun load() = TrackFilters(
        genre = preferences.getString("genre", "").orEmpty(),
        album = preferences.getString("album", "").orEmpty(),
        streamableOnly = preferences.getBoolean("streamable_only", false)
    )

    override suspend fun save(filters: TrackFilters): Unit = withContext(ioDispatcher) {
        val normalized = filters.normalized()
        // Wait for the disk write before closing the settings screen (including force-stop).
        val saved = preferences.edit()
            .putString("genre", normalized.genre)
            .putString("album", normalized.album)
            .putBoolean("streamable_only", normalized.streamableOnly)
            .commit()
        if (!saved) throw IOException("Unable to save track filters")
    }
}
