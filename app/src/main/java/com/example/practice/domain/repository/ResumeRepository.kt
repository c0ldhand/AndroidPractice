package com.example.practice.domain.repository

import com.example.practice.domain.model.ResumeDocument

interface ResumeRepository {
    suspend fun download(url: String): ResumeDocument
}
