package com.example.practice.domain.model

data class UserProfile(
    val fullName: String = "",
    val avatarUri: String? = null,
    val resumeUrl: String = "",
    val position: String = ""
)

data class ResumeDocument(val uri: String, val mimeType: String)
