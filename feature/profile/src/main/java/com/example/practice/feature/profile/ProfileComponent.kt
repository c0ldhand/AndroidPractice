package com.example.practice.feature.profile

import android.content.Context
import com.example.practice.feature.profile.data.DownloadedResumeRepository
import com.example.practice.feature.profile.data.local.AndroidProfileMediaRepository
import com.example.practice.feature.profile.data.local.FileProfileRepository
import com.example.practice.feature.profile.domain.usecase.SaveProfileUseCase
import com.example.practice.feature.profile.platform.reminders.ClassReminderScheduler
import com.example.practice.feature.profile.viewmodel.ProfileViewModelFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/** Application-scoped dependencies shared by the profile and its editor. */
class ProfileComponent(context: Context) {
    private val applicationContext = context.applicationContext
    private val repository = FileProfileRepository(applicationContext.filesDir)
    internal val reminders = ClassReminderScheduler(applicationContext)
    internal val viewModelFactory = ProfileViewModelFactory(
        repository,
        AndroidProfileMediaRepository(applicationContext),
        DownloadedResumeRepository(
            applicationContext,
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()
        ),
        SaveProfileUseCase(repository, reminders::schedule)
    )
}
