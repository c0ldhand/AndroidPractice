package com.example.practice.data

import android.content.Context
import androidx.core.content.FileProvider
import com.example.practice.domain.model.ResumeDocument
import com.example.practice.domain.repository.ResumeRepository
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class DownloadedResumeRepository(
    private val context: Context,
    private val client: OkHttpClient
) : ResumeRepository {
    override suspend fun download(url: String): ResumeDocument = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "resumes")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create downloads directory")
        val temporary = File.createTempFile("resume_", ".part", directory)
        var destination: File? = null
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty document")
                if (body.contentLength() > MAX_BYTES) throw IOException("Document is too large")
                val type = body.contentType()?.let { "${it.type}/${it.subtype}" }
                if (type == "text/html") throw IOException("The URL points to a web page, not a document")
                var total = 0L
                body.byteStream().use { source -> temporary.outputStream().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_BYTES) throw IOException("Document is too large")
                        target.write(buffer, 0, count)
                    }
                } }
                if (total == 0L) throw IOException("Empty document")
                val extension = when {
                    type == "application/pdf" || url.substringBefore('?').endsWith(".pdf", true) -> "pdf"
                    type == "application/msword" -> "doc"
                    type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                    else -> "bin"
                }
                val mime = if (extension == "pdf") "application/pdf" else type ?: "application/octet-stream"
                val file = File(directory, "resume_${UUID.randomUUID()}.$extension")
                if (!temporary.renameTo(file)) throw IOException("Cannot finish download")
                destination = file
                ResumeDocument(FileProvider.getUriForFile(context, "${context.packageName}.files", file).toString(), mime)
            }
        } catch (error: Exception) {
            destination?.delete()
            throw error
        } finally { temporary.delete() }
    }

    private companion object { const val MAX_BYTES = 25L * 1024 * 1024 }
}
