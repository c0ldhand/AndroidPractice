package com.example.practice.data.local

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.practice.domain.repository.ProfileMediaRepository
import com.example.practice.platform.images.decodeProfileImage
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidProfileMediaRepository(private val context: Context) : ProfileMediaRepository {
    override suspend fun galleryImages(): List<String> = withContext(Dispatchers.IO) {
        buildList {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID), null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn)).toString())
                }
            }
        }
    }

    override suspend fun importAvatar(sourceUri: String): String = withContext(Dispatchers.IO) {
        val bitmap = decodeProfileImage(context.contentResolver, Uri.parse(sourceUri), 1024)
        val file = File(directory("avatars"), "${UUID.randomUUID()}.jpg")
        try {
            file.outputStream().use {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) throw IOException("Cannot write avatar")
            }
            uriFor(file)
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally { bitmap.recycle() }
    }

    override suspend fun createCameraUri(): String = withContext(Dispatchers.IO) {
        val captures = directory("camera")
        captures.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24L * 60 * 60 * 1000 }?.forEach { it.delete() }
        uriFor(File.createTempFile("capture_", ".jpg", captures))
    }

    override suspend fun deleteCameraPhoto(uri: String): Unit = withContext(Dispatchers.IO) {
        // Only our temporary captures may be deleted, never gallery originals.
        val parsed = Uri.parse(uri)
        if (parsed.authority == "${context.packageName}.files" && parsed.pathSegments.firstOrNull() == "camera") {
            File(directory("camera"), parsed.lastPathSegment.orEmpty()).delete()
        }
    }

    private fun directory(name: String) = File(context.filesDir, name).apply {
        if (!isDirectory && !mkdirs()) throw IOException("Cannot create $name directory")
    }
    private fun uriFor(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.files", file).toString()
}
