package com.example.practice

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.practice.platform.reminders.ClassReminderReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The library's Android components and permissions must reach the host APK. */
@RunWith(AndroidJUnit4::class)
class ProfileModuleIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<PracticeApplication>()

    @Test
    fun profileFileProviderIsMergedWithTheHostApplicationId() {
        val provider = context.packageManager.resolveContentProvider(
            "${context.packageName}.files", PackageManager.GET_META_DATA
        )
        assertNotNull(provider)
        requireNotNull(provider)
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
        val paths = provider.metaData.getInt("android.support.FILE_PROVIDER_PATHS")
        context.resources.getXml(paths).close()
    }

    @Test
    fun reminderReceiverAndProfilePermissionsAreMergedIntoTheHost() {
        val receiver = context.packageManager.getReceiverInfo(
            ComponentName(context, ClassReminderReceiver::class.java), 0
        )
        assertFalse(receiver.exported)
        assertEquals("com.example.practice.platform.reminders.ClassReminderReceiver", receiver.name)
        val permissions = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_PERMISSIONS
        ).requestedPermissions.orEmpty().toSet()
        assertTrue(permissions.containsAll(listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        )))
        val launcher = context.packageManager.getLaunchIntentForPackage(context.packageName)
        assertEquals(ComponentName(context, MainActivity::class.java), launcher?.component)
    }
}
