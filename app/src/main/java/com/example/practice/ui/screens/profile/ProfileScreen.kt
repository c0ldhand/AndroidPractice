package com.example.practice.ui.screens.profile

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practice.PracticeApplication
import com.example.practice.R
import com.example.practice.ui.components.ProfileAvatar
import com.example.practice.viewmodel.ProfileError
import com.example.practice.viewmodel.ProfileUiState
import com.example.practice.viewmodel.ProfileViewModel

@Composable
fun ProfileRoute() {
    val context = LocalContext.current
    val factory = (context.applicationContext as PracticeApplication).container.profileViewModelFactory
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.openDocument.collect { document ->
                val uri = Uri.parse(document.uri)
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, document.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = ClipData.newRawUri("resume", uri)
                    })
                } catch (_: ActivityNotFoundException) { viewModel.viewerUnavailable() }
                catch (_: SecurityException) { viewModel.viewerUnavailable() }
            }
        }
    }
    ProfileScreen(state, viewModel::downloadResume, viewModel::reload, viewModel::clearError)
}

@Composable
fun ProfileScreen(state: ProfileUiState, onResume: () -> Unit, onRetry: () -> Unit, onDismissError: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.loading) {
            CircularProgressIndicator()
            return@Column
        }
        ProfileAvatar(state.profile.avatarUri, Modifier.size(128.dp).testTag("profile_avatar"))
        Text(
            state.profile.fullName.ifBlank { stringResource(R.string.profile_empty_name) },
            Modifier.fillMaxWidth().testTag("profile_name"),
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center
        )
        if (state.profile.position.isNotBlank()) Text(state.profile.position, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (state.profile.fullName.isBlank()) Text(stringResource(R.string.profile_empty_hint), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onResume, enabled = state.profile.resumeUrl.isNotBlank() && !state.downloading, modifier = Modifier.testTag("download_resume")) {
            if (state.downloading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(if (state.downloading) R.string.resume_downloading else R.string.resume))
        }
        if (state.error != null) {
            Text(stringResource(profileErrorString(state.error)), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            TextButton(if (state.error == ProfileError.LOAD) onRetry else onDismissError) {
                Text(stringResource(if (state.error == ProfileError.LOAD) R.string.retry else R.string.dismiss))
            }
        }
    }
}

internal fun profileErrorString(error: ProfileError): Int = when (error) {
    ProfileError.LOAD -> R.string.profile_load_failed
    ProfileError.SAVE -> R.string.profile_save_failed
    ProfileError.INVALID_URL -> R.string.profile_invalid_url
    ProfileError.PHOTO -> R.string.profile_photo_failed
    ProfileError.GALLERY -> R.string.profile_gallery_failed
    ProfileError.CAMERA -> R.string.profile_camera_failed
    ProfileError.DOWNLOAD -> R.string.resume_download_failed
    ProfileError.NO_VIEWER -> R.string.resume_no_viewer
}
