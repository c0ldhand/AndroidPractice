package com.example.practice.feature.profile.domain.repository

import com.example.practice.feature.profile.domain.model.ResumeDocument

interface ResumeRepository {
    suspend fun download(url: String): ResumeDocument
}
