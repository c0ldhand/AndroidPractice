package com.example.practice

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.practice.data.local.FileProfileRepository
import java.io.File
import java.util.regex.Pattern
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/** Run on a clean app install to exercise real permission dialogs and the system camera. */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ProfileFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    private val demo get() = InstrumentationRegistry.getArguments().getString("recordDemo") == "true"

    @Test fun a_deniedStoragePermissionClosesEditor() {
        assumeFalse(com.example.practice.ui.screens.profile.hasImageAccess(context))
        openProfile()
        compose.onNodeWithTag("edit_profile").performClick()
        compose.waitForIdle()
        clickSystem(resource("permission_deny_button"))
        compose.onNodeWithTag("profile_screen").assertIsDisplayed()
        compose.onNodeWithTag("profile_edit").assertDoesNotExist()
        pause()
    }

    @Test fun b_galleryCameraSaveRecreateAndOpenResume() {
        val photo = createGalleryPhoto()
        try {
            openProfile()
            pause()
            snapshot("profile-empty")
            compose.onNodeWithTag("edit_profile").performClick()
            compose.waitForIdle()
            if (!com.example.practice.ui.screens.profile.hasImageAccess(context)) {
                clickSystem(resource("permission_allow_all_button"), resource("permission_allow_button"), resource("permission_allow_foreground_only_button"))
            }
            compose.onNodeWithTag("edit_avatar").assertIsDisplayed().performClick()
            pause()
            compose.onNodeWithTag("choose_gallery").performClick()
            compose.waitUntil(15000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag("gallery_photo_0")).fetchSemanticsNodes().isNotEmpty() }
            pause()
            compose.onNodeWithTag("gallery_photo_0").performClick()
            compose.waitUntil(15000) { avatarFiles().isNotEmpty() }
            compose.onNodeWithTag("edit_avatar").assertIsDisplayed()
            pause()
            snapshot("edit-gallery")
            compose.onNodeWithTag("edit_avatar").performClick()
            compose.onNodeWithTag("choose_camera").performClick()
            compose.waitForIdle()
            if (device.wait(Until.hasObject(resource("permission_allow_foreground_only_button")), 1500) || device.hasObject(resource("permission_allow_button"))) {
                clickSystem(resource("permission_allow_foreground_only_button"), resource("permission_allow_button"))
            }
            // The first launch of the emulator camera can ask for its own permissions.
            val shutter = waitForCameraShutter()
            pause()
            shutter.click()
            clickSystem(By.res(Pattern.compile(".*:id/(done_button|review_done|btn_done|confirm_button)")), By.desc("Done"), By.desc("Готово"))
            compose.waitUntil(15000) { avatarFiles().size >= 2 }
            pause()
            snapshot("edit-camera")
            compose.onNodeWithTag("profile_name_input").performTextReplacement("Иванов Иван Иванович")
            compose.onNodeWithTag("profile_resume_input").performTextReplacement("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
            compose.onNodeWithTag("profile_position_input").performTextReplacement("Android-разработчик")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            pause()
            compose.onNodeWithTag("save_profile").assertIsEnabled().performClick()
            compose.waitUntil(15000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag("profile_name")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("profile_name").assertTextEquals("Иванов Иван Иванович")
            snapshot("profile-filled")
            val saved = runBlocking { FileProfileRepository(context.filesDir).load() }
            assertEquals("Android-разработчик", saved.position)
            assertNotNull(saved.avatarUri)
            context.contentResolver.delete(photo, null, null)
            compose.activityRule.scenario.recreate()
            openProfile()
            compose.onNodeWithTag("profile_name").assertTextEquals(saved.fullName)
            pause()
            if (demo) {
                compose.onNodeWithTag("download_resume").performClick()
                assertTrue("No external document viewer opened", device.wait(Until.gone(By.pkg(context.packageName).depth(0)), 45000))
                pause()
                snapshot("resume-viewer")
            }
        } finally {
            runCatching { context.contentResolver.delete(photo, null, null) }
        }
    }

    private fun openProfile() {
        compose.onNode(hasText("Профиль") and hasClickAction()).performClick()
        compose.waitUntil(10000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag("profile_name")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("profile_screen").assertIsDisplayed()
    }

    private fun avatarFiles() = File(context.filesDir, "avatars").listFiles()?.toList().orEmpty()
    private fun resource(id: String) = By.res(Pattern.compile(".*:id/$id"))

    private fun clickSystem(vararg selectors: BySelector) {
        val deadline = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < deadline) {
            for (selector in selectors) {
                val node = device.findObject(selector)
                if (node != null) { pause(); node.click(); return }
            }
            Thread.sleep(150)
        }
        val output = context.getExternalFilesDir(null)!!
        device.dumpWindowHierarchy(File(output, "profile-test-failure.xml"))
        device.takeScreenshot(File(output, "profile-test-failure.png"))
        error("System dialog control not found in ${device.currentPackageName}: ${selectors.toList()}")
    }

    private fun waitForCameraShutter(): androidx.test.uiautomator.UiObject2 {
        val selector = By.res(Pattern.compile(".*:id/(shutter_button|shutter|photo_capture)"))
        val deadline = System.currentTimeMillis() + 30000
        while (System.currentTimeMillis() < deadline) {
            device.findObject(selector)?.let { return it }
            listOf("permission_allow_foreground_only_button", "permission_allow_button").forEach { id -> device.findObject(resource(id))?.click() }
            // AOSP Camera's optional location-tagging introduction.
            val locationOption = device.findObject(By.res("com.android.camera2", "check_box"))
            if (locationOption?.isChecked == true) locationOption.click()
            device.findObject(By.text(Pattern.compile("(?i)next|got it")))?.click()
            Thread.sleep(200)
        }
        error("System camera did not show its shutter button")
    }

    private fun createGalleryPhoto(): android.net.Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Practice6-gallery.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Practice6")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        val bitmap = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(225, 235, 255))
            drawCircle(240f, 240f, 180f, Paint().apply { color = Color.rgb(75, 99, 185); isAntiAlias = true })
            drawText("A", 155f, 325f, Paint().apply { color = Color.WHITE; textSize = 240f; isAntiAlias = true })
        }
        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return uri
    }

    private fun pause() { if (demo) Thread.sleep(1200) }
    private fun snapshot(name: String) {
        if (demo) device.takeScreenshot(File(context.getExternalFilesDir(null), "$name.png"))
    }
}
