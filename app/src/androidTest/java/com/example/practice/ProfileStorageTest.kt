package com.example.practice

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.practice.data.DownloadedResumeRepository
import com.example.practice.data.local.AndroidProfileMediaRepository
import com.example.practice.data.local.FileProfileRepository
import com.example.practice.domain.model.UserProfile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileStorageTest {
    private val context = ApplicationProvider.getApplicationContext<PracticeApplication>()

    @Test fun jsonSurvivesRepositoryRecreationAndPreservesUnicode() = runBlocking {
        val directory = File(context.cacheDir, "profile-test-${System.nanoTime()}").apply { mkdirs() }
        try {
            val profile = UserProfile("Иванов Иван Иванович", "content://owned/avatar.jpg", "https://example.com/cv.pdf", "Разработчик")
            val first = FileProfileRepository(directory)
            assertEquals(UserProfile(), first.load())
            first.save(profile)
            assertEquals(profile, first.profile.value)
            assertEquals(profile, FileProfileRepository(directory).load())
            first.save(UserProfile(fullName = "Новое имя"))
            assertEquals("Новое имя", FileProfileRepository(directory).load().fullName)
        } finally { directory.deleteRecursively() }
    }

    @Test fun corruptJsonIsReportedInsteadOfSilentlyReplaced() = runBlocking {
        val directory = File(context.cacheDir, "profile-corrupt-${System.nanoTime()}").apply { mkdirs() }
        try {
            File(directory, "profile.json").writeText("broken JSON")
            var failed = false
            try { FileProfileRepository(directory).load() } catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertEquals("broken JSON", File(directory, "profile.json").readText())
        } finally { directory.deleteRecursively() }
    }

    @Test fun importedAvatarIsOwnedAndSurvivesOriginalRemoval() = runBlocking {
        val source = File(context.cacheDir, "original-avatar.png")
        val bitmap = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val media = AndroidProfileMediaRepository(context)
        val uri = Uri.parse(media.importAvatar(Uri.fromFile(source).toString()))
        try {
            source.delete()
            assertEquals("${context.packageName}.files", uri.authority)
            assertTrue(context.contentResolver.openInputStream(uri)!!.use { it.readBytes().isNotEmpty() })
        } finally { File(context.filesDir, "avatars/${uri.lastPathSegment}").delete() }
    }

    @Test fun cancelledCameraDeletesOnlyTemporaryCapture() = runBlocking {
        val media = AndroidProfileMediaRepository(context)
        val uri = Uri.parse(media.createCameraUri())
        val file = File(context.filesDir, "camera/${uri.lastPathSegment}")
        assertTrue(file.exists())
        media.deleteCameraPhoto(uri.toString())
        assertFalse(file.exists())
        val original = File(context.cacheDir, "do-not-delete.jpg").apply { writeText("original") }
        try {
            media.deleteCameraPhoto(Uri.fromFile(original).toString())
            assertTrue(original.exists())
        } finally { original.delete() }
    }

    @Test fun downloadsPdfAndFileProviderCanReadIt() = runBlocking {
        withHttpsServer { server, client ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf").setBody("%PDF-1.4\nTest resume\n%%EOF"))
            val repository = DownloadedResumeRepository(context, client)
            val document = repository.download(server.url("/cv.pdf").toString())
            val uri = Uri.parse(document.uri)
            try {
                assertEquals("application/pdf", document.mimeType)
                assertTrue(context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText().startsWith("%PDF") })
                assertEquals("/cv.pdf", server.takeRequest().path)
            } finally { File(context.filesDir, "resumes/${uri.lastPathSegment}").delete() }
        }
    }

    @Test fun failedDownloadsLeaveNoPartialFiles() = runBlocking {
        withHttpsServer { server, client ->
            val directory = File(context.filesDir, "resumes")
            val before = directory.list()?.toSet().orEmpty()
            val repository = DownloadedResumeRepository(context, client)
            listOf(
                MockResponse().setResponseCode(404),
                MockResponse().setHeader("Content-Type", "text/html").setBody("<html>Not a document</html>"),
                MockResponse().setHeader("Content-Type", "application/pdf").setBody(""),
                MockResponse().setHeader("Content-Length", 26L * 1024 * 1024)
            ).forEach { response ->
                server.enqueue(response)
                var failed = false
                try { repository.download(server.url("/bad.pdf").toString()) } catch (_: IOException) { failed = true }
                assertTrue(failed)
                assertEquals(before, directory.list()?.toSet().orEmpty())
            }
        }
    }

    private suspend fun withHttpsServer(block: suspend (MockWebServer, OkHttpClient) -> Unit) {
        val certificate = HeldCertificate.Builder().commonName("localhost").addSubjectAlternativeName("localhost").build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        val client = OkHttpClient.Builder().sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager).build()
        try { block(server, client) } finally { server.shutdown() }
    }
}
