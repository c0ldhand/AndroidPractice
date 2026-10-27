package com.example.practice.domain.usecase

import com.example.practice.domain.model.UserProfile
import com.example.practice.domain.repository.ProfileRepository
import java.net.URI

class InvalidResumeUrlException : IllegalArgumentException()

fun normalizeResumeUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return ""
    val url = if ("://" in trimmed) trimmed else "https://$trimmed"
    val uri = try { URI(url) } catch (_: Exception) { throw InvalidResumeUrlException() }
    if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) {
        throw InvalidResumeUrlException()
    }
    return uri.toASCIIString()
}

class SaveProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(profile: UserProfile) {
        repository.save(profile.copy(
            fullName = profile.fullName.trim(),
            position = profile.position.trim(),
            resumeUrl = normalizeResumeUrl(profile.resumeUrl)
        ))
    }
}
