package com.example.practice.feature.profile.domain.repository

import com.example.practice.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val profile: StateFlow<UserProfile>
    suspend fun load(): UserProfile
    suspend fun save(profile: UserProfile)
}
