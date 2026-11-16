package com.example.practice.feature.profile.domain.usecase

import com.example.practice.feature.profile.domain.model.ClassTime
import com.example.practice.feature.profile.domain.model.UserProfile
import com.example.practice.feature.profile.domain.repository.ProfileRepository
import java.net.URI
import java.util.Calendar
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class InvalidResumeUrlException : IllegalArgumentException()
class ClassTimeUnavailableException : IllegalArgumentException()
class ClassReminderScheduleException(cause: RuntimeException) : IllegalStateException(cause)

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

class SaveProfileUseCase(
    private val repository: ProfileRepository,
    private val scheduleClassReminder: (triggerAtMillis: Long, name: String) -> Unit,
    private val now: () -> Calendar = Calendar::getInstance
) {
    private val saveMutex = Mutex()

    suspend operator fun invoke(profile: UserProfile) {
        val normalized = profile.copy(
            fullName = profile.fullName.trim(),
            position = profile.position.trim(),
            resumeUrl = normalizeResumeUrl(profile.resumeUrl)
        )
        saveMutex.withLock {
            val currentTime = now()
            val trigger = ClassTime.parse(profile.favoriteClassTime)?.todayAt(currentTime)
            if (trigger == null || trigger <= currentTime.timeInMillis) throw ClassTimeUnavailableException()

            withContext(NonCancellable) {
                repository.save(normalized)
                try {
                    scheduleClassReminder(trigger, normalized.fullName)
                } catch (failure: RuntimeException) {
                    throw ClassReminderScheduleException(failure)
                }
            }
        }
    }
}
