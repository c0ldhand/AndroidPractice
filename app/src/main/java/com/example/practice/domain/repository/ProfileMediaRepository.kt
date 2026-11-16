package com.example.practice.domain.repository

interface ProfileMediaRepository {
    suspend fun galleryImages(): List<String>
    suspend fun importAvatar(sourceUri: String): String
    suspend fun createCameraUri(): String
    suspend fun deleteCameraPhoto(uri: String)
}
