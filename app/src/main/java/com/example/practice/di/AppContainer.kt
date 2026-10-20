package com.example.practice.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.example.practice.data.NetworkTrackRepository
import androidx.room.Room
import com.example.practice.data.LocalFirstTrackRepository
import com.example.practice.data.local.RoomFavoritesRepository
import com.example.practice.data.local.SharedPreferencesFiltersRepository
import com.example.practice.data.local.TrackDatabase
import com.example.practice.domain.model.FilterBadgeCache
import com.example.practice.domain.repository.FavoritesRepository
import com.example.practice.domain.repository.FiltersRepository
import com.example.practice.data.remote.ItunesApi
import com.example.practice.domain.repository.TrackRepository
import com.example.practice.domain.usecase.GetTrackDetailsUseCase
import com.example.practice.domain.usecase.SearchTracksUseCase
import com.example.practice.viewmodel.TrackViewModelFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            ChuckerInterceptor.Builder(context)
                .collector(ChuckerCollector(context, showNotification = false))
                .build()
        )
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ItunesApi::class.java)

    private val database = Room.databaseBuilder(
        context.applicationContext, TrackDatabase::class.java, "tracks.db"
    ).build()
    val filtersRepository: FiltersRepository = SharedPreferencesFiltersRepository(
        context.getSharedPreferences("track_filters", Context.MODE_PRIVATE)
    )
    val favoritesRepository: FavoritesRepository = RoomFavoritesRepository(database.favoriteTrackDao())
    val filterBadgeCache = FilterBadgeCache()
    val repository: TrackRepository = LocalFirstTrackRepository(NetworkTrackRepository(api), favoritesRepository)
    val viewModelFactory = TrackViewModelFactory(
        SearchTracksUseCase(repository), GetTrackDetailsUseCase(repository),
        filtersRepository, favoritesRepository, filterBadgeCache
    )
}
