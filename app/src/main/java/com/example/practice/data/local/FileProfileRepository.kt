package com.example.practice.data.local

import android.util.AtomicFile
import com.example.practice.domain.model.UserProfile
import com.example.practice.domain.repository.ProfileRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A single small record: JSON is sufficient; AtomicFile protects the last saved profile. */
class FileProfileRepository(
    directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProfileRepository {
    private val file = AtomicFile(File(directory, "profile.json"))
    private val mutex = Mutex()
    private val current = MutableStateFlow(UserProfile())
    override val profile = current.asStateFlow()

    override suspend fun load(): UserProfile = withContext(ioDispatcher) {
        mutex.withLock {
            val loaded = if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                val json = file.openRead().bufferedReader().use { JSONObject(it.readText()) }
                UserProfile(
                    fullName = json.optString("fullName"),
                    avatarUri = json.optString("avatarUri").takeIf { it.isNotBlank() },
                    resumeUrl = json.optString("resumeUrl"),
                    position = json.optString("position")
                )
            } else UserProfile()
            current.value = loaded
            loaded
        }
    }

    override suspend fun save(profile: UserProfile): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            val json = JSONObject()
                .put("version", 1)
                .put("fullName", profile.fullName)
                .put("avatarUri", profile.avatarUri.orEmpty())
                .put("resumeUrl", profile.resumeUrl)
                .put("position", profile.position)
            val stream = file.startWrite()
            try {
                stream.write(json.toString().toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (error: Exception) {
                file.failWrite(stream)
                throw error
            }
            current.value = profile
        }
    }
}
