package com.example.practice.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") term: String,
        @Query("limit") limit: Int,
        @Query("entity") entity: String = "song",
        @Query("media") media: String = "music",
        @Query("country") country: String = "US"
    ): ItunesResponseDto

    @GET("lookup")
    suspend fun lookupTrack(
        @Query("id") id: Long,
        @Query("entity") entity: String = "song",
        @Query("country") country: String = "US"
    ): ItunesResponseDto
}
