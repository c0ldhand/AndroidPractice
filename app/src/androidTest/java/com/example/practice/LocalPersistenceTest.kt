package com.example.practice

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.practice.data.LocalFirstTrackRepository
import com.example.practice.data.local.RoomFavoritesRepository
import com.example.practice.data.local.SharedPreferencesFiltersRepository
import com.example.practice.data.local.TrackDatabase
import com.example.practice.domain.model.Track
import com.example.practice.domain.model.TrackFilters
import com.example.practice.domain.repository.TrackRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun filterSettingsSurviveNewRepositoryAndCanBeReset() = runBlocking {
        val preferences = context.getSharedPreferences("persistence_test_filters", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val expected = TrackFilters("Dance", "Discovery", true)
            SharedPreferencesFiltersRepository(preferences).save(expected)
            val restored = SharedPreferencesFiltersRepository(
                context.getSharedPreferences("persistence_test_filters", Context.MODE_PRIVATE)
            )
            assertEquals(expected, restored.load())
            restored.save(TrackFilters())
            assertFalse(SharedPreferencesFiltersRepository(preferences).load().isActive)
        } finally { preferences.edit().clear().commit() }
    }

    @Test fun favoritesSurviveDatabaseReopenAndDetailsNeverUseNetwork() = runBlocking {
        val name = "persistence-test-tracks.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, TrackDatabase::class.java, name).build()
        val track = Track(123, "Saved song", "Artist", "Album", "Pop", "2020-01-01", 180000, null, "USD", "USA", isStreamable = false)
        try {
            val firstDatabase = open()
            try {
                val favorites = RoomFavoritesRepository(firstDatabase.favoriteTrackDao())
                favorites.add(track)
                favorites.add(track)
                assertEquals(listOf(track), favorites.observeTracks().first())
            } finally { firstDatabase.close() }
            val reopenedDatabase = open()
            try {
                val favorites = RoomFavoritesRepository(reopenedDatabase.favoriteTrackDao())
                val offlineNetwork = object : TrackRepository {
                    override suspend fun searchTracks(query: String, limit: Int): List<Track> = error("Network unavailable")
                    override suspend fun getTrackById(id: Long): Track? = error("Network must not be called for favorites")
                }
                assertEquals(listOf(track), favorites.observeTracks().first())
                assertEquals(track, LocalFirstTrackRepository(offlineNetwork, favorites).getTrackById(123))
                favorites.remove(123)
                assertTrue(favorites.observeTracks().first().isEmpty())
            } finally { reopenedDatabase.close() }
            val afterDeletionDatabase = open()
            try { assertNull(afterDeletionDatabase.favoriteTrackDao().getTrack(123)) }
            finally { afterDeletionDatabase.close() }
        } finally { context.deleteDatabase(name) }
    }
}
