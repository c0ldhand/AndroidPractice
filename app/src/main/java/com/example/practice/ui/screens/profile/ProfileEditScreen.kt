package com.example.practice.ui.screens.profile

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practice.PracticeApplication
import com.example.practice.R
import com.example.practice.ui.components.ProfileAvatar
import com.example.practice.viewmodel.ProfileEditUiState
import com.example.practice.viewmodel.ProfileEditViewModel
import com.example.practice.viewmodel.ProfileError

internal fun imagePermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

internal fun hasImageAccess(context: Context): Boolean = imagePermissions().any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun ProfileEditRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = (context.applicationContext as PracticeApplication).container.profileViewModelFactory
    val viewModel: ProfileEditViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var openGalleryAfterPermission by rememberSaveable { mutableStateOf(false) }
    val mediaPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it } || hasImageAccess(context)) {
            if (openGalleryAfterPermission) viewModel.showGallery()
        } else {
            Toast.makeText(context, R.string.profile_permission_denied, Toast.LENGTH_LONG).show()
            onBack()
        }
        openGalleryAfterPermission = false
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::cameraResult)
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.prepareCamera()
        else Toast.makeText(context, R.string.profile_camera_permission_denied, Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) {
        if (!permissionRequested) {
            permissionRequested = true
            if (!hasImageAccess(context)) {
                mediaPermission.launch(imagePermissions())
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.openCamera.collect { uri ->
                try { camera.launch(Uri.parse(uri)) }
                catch (_: ActivityNotFoundException) { viewModel.cameraUnavailable() }
                catch (_: SecurityException) { viewModel.cameraUnavailable() }
            }
        }
    }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    ProfileEditScreen(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onNameChange = viewModel::setName,
        onPositionChange = viewModel::setPosition,
        onResumeChange = viewModel::setResumeUrl,
        onGallery = {
            if (hasImageAccess(context)) viewModel.showGallery()
            else {
                openGalleryAfterPermission = true
                mediaPermission.launch(imagePermissions())
            }
        },
        onCamera = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) viewModel.prepareCamera()
            else cameraPermission.launch(Manifest.permission.CAMERA)
        },
        onSelectImage = viewModel::selectGalleryPhoto,
        onCloseGallery = viewModel::closeGallery,
        onReselect = if (Build.VERSION.SDK_INT >= 34) ({
            viewModel.closeGallery()
            openGalleryAfterPermission = true
            mediaPermission.launch(imagePermissions())
        }) else null,
        onRetry = viewModel::reload,
        onDismissError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    state: ProfileEditUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onResumeChange: (String) -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onSelectImage: (String) -> Unit,
    onCloseGallery: () -> Unit,
    onReselect: (() -> Unit)?,
    onRetry: () -> Unit,
    onDismissError: () -> Unit
) {
    var chooseSource by remember { mutableStateOf(false) }
    val busy = state.saving || state.mediaBusy
    BackHandler(enabled = state.saving) { /* Finish the atomic save before leaving. */ }
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("profile_edit"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_edit)) },
                navigationIcon = { IconButton(onBack, enabled = !state.saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onSave, enabled = !state.loading && !busy && state.error != ProfileError.LOAD, modifier = Modifier.testTag("save_profile")) {
                        Icon(Icons.Default.Check, stringResource(R.string.done))
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.error == ProfileError.LOAD) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.profile_load_failed))
                TextButton(onRetry) { Text(stringResource(R.string.retry)) }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileAvatar(state.draft.avatarUri, Modifier.size(128.dp).testTag("edit_avatar").clickable(enabled = !busy) { chooseSource = true }, description = stringResource(R.string.profile_change_photo))
                if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                TextField(
                    state.draft.fullName, onNameChange, Modifier.fillMaxWidth().testTag("profile_name_input"),
                    enabled = !state.saving, label = { Text(stringResource(R.string.profile_full_name)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), singleLine = true
                )
                TextField(
                    state.draft.resumeUrl, onResumeChange, Modifier.fillMaxWidth().testTag("profile_resume_input"),
                    enabled = !state.saving, label = { Text(stringResource(R.string.profile_resume_link)) },
                    placeholder = { Text("https://example.com/resume.pdf") },
                    isError = state.error == ProfileError.INVALID_URL,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true
                )
                TextField(
                    state.draft.position, onPositionChange, Modifier.fillMaxWidth().testTag("profile_position_input"),
                    enabled = !state.saving, label = { Text(stringResource(R.string.profile_position)) }, singleLine = true
                )
                if (state.error != null) {
                    Text(stringResource(profileErrorString(state.error)), color = MaterialTheme.colorScheme.error)
                    TextButton(onDismissError) { Text(stringResource(R.string.dismiss)) }
                }
            }
        }
    }
    if (chooseSource) AlertDialog(
        onDismissRequest = { chooseSource = false },
        title = { Text(stringResource(R.string.profile_photo_source)) },
        text = { Column {
            TextButton({ chooseSource = false; onGallery() }, Modifier.fillMaxWidth().testTag("choose_gallery")) { Text(stringResource(R.string.profile_gallery)) }
            TextButton({ chooseSource = false; onCamera() }, Modifier.fillMaxWidth().testTag("choose_camera")) { Text(stringResource(R.string.profile_camera)) }
        } },
        confirmButton = { TextButton({ chooseSource = false }) { Text(stringResource(R.string.profile_cancel)) } }
    )
    val gallery = state.gallery
    if (gallery != null) AlertDialog(
        onDismissRequest = onCloseGallery,
        title = { Text(stringResource(R.string.profile_gallery)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onReselect != null) TextButton(onReselect) { Text(stringResource(R.string.profile_reselect_access)) }
                if (gallery.isEmpty()) Text(stringResource(R.string.profile_gallery_empty))
                else LazyVerticalGrid(
                    GridCells.Fixed(3), Modifier.fillMaxWidth().height(360.dp).testTag("profile_gallery"),
                    verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(gallery, key = { _, uri -> uri }) { index, uri ->
                        ProfileAvatar(uri, Modifier.size(88.dp).testTag("gallery_photo_$index").clickable { onSelectImage(uri) }, circular = false, description = stringResource(R.string.profile_gallery_photo, index + 1))
                    }
                }
            }
        },
        confirmButton = { TextButton(onCloseGallery) { Text(stringResource(R.string.profile_cancel)) } }
    )
}
