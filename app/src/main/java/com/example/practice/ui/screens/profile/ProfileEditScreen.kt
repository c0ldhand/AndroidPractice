package com.example.practice.ui.screens.profile

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.practice.domain.model.ClassTime
import com.example.practice.ui.components.ProfileAvatar
import com.example.practice.viewmodel.ProfileEditUiState
import com.example.practice.viewmodel.ProfileEditViewModel
import com.example.practice.viewmodel.ProfileError
import java.util.Calendar

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
    val container = (context.applicationContext as PracticeApplication).container
    val viewModel: ProfileEditViewModel = viewModel(factory = container.profileViewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reminders = container.classReminderScheduler
    val packageUri = Uri.fromParts("package", context.packageName, null)
    var showNotificationSettings by rememberSaveable { mutableStateOf(false) }
    val exactAlarmSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when {
            !reminders.canScheduleExactAlarms() ->
                Toast.makeText(context, R.string.class_reminder_exact_alarms_disabled, Toast.LENGTH_LONG).show()
            !reminders.notificationsEnabled() -> showNotificationSettings = true
            else -> viewModel.save()
        }
    }
    fun saveWithPermissions() {
        when {
            !reminders.notificationsEnabled() -> showNotificationSettings = true
            reminders.canScheduleExactAlarms() -> viewModel.save()
            else -> try {
                exactAlarmSettings.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, R.string.class_reminder_settings_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        saveWithPermissions()
    }
    val notificationSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (reminders.notificationsEnabled()) saveWithPermissions()
    }
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
        onSave = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else saveWithPermissions()
        },
        onNameChange = viewModel::setName,
        onPositionChange = viewModel::setPosition,
        onResumeChange = viewModel::setResumeUrl,
        onClassTimeChange = viewModel::setFavoriteClassTime,
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
    if (showNotificationSettings) AlertDialog(
        onDismissRequest = { showNotificationSettings = false },
        text = { Text(stringResource(R.string.class_reminder_notifications_disabled)) },
        dismissButton = {
            TextButton({ showNotificationSettings = false }) { Text(stringResource(R.string.profile_cancel)) }
        },
        confirmButton = {
            TextButton({
                showNotificationSettings = false
                val settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                try {
                    notificationSettings.launch(settings)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.class_reminder_settings_unavailable, Toast.LENGTH_LONG).show()
                }
            }) { Text(stringResource(R.string.class_reminder_open_settings)) }
        }
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
    onClassTimeChange: (String) -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onSelectImage: (String) -> Unit,
    onCloseGallery: () -> Unit,
    onReselect: (() -> Unit)?,
    onRetry: () -> Unit,
    onDismissError: () -> Unit
) {
    var chooseSource by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val busy = state.saving || state.mediaBusy
    BackHandler(enabled = state.saving) { /* Finish the atomic save before leaving. */ }
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("profile_edit"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_edit)) },
                navigationIcon = { IconButton(onBack, enabled = !state.saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onSave()
                        },
                        enabled = state.canSave,
                        modifier = Modifier.testTag("save_profile")
                    ) {
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
                ClassTimeField(
                    value = state.draft.favoriteClassTime,
                    enabled = !state.saving,
                    error = when {
                        !state.hasValidClassTime -> R.string.profile_class_time_invalid
                        state.error == ProfileError.CLASS_TIME_UNAVAILABLE -> R.string.profile_class_time_unavailable
                        else -> null
                    },
                    onChange = onClassTimeChange
                )
                if (state.error != null && state.error != ProfileError.CLASS_TIME_UNAVAILABLE) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassTimeField(value: String, enabled: Boolean, error: Int?, onChange: (String) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().testTag("profile_class_time_input"),
        enabled = enabled,
        label = { Text(stringResource(R.string.profile_class_time)) },
        placeholder = { Text("HH:mm") },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = {
                    focusManager.clearFocus()
                    showPicker = true
                },
                enabled = enabled
            ) {
                Icon(painterResource(R.drawable.ic_class_time), stringResource(R.string.profile_pick_class_time))
            }
        }
    )
    if (showPicker) {
        val initialTime = remember {
            ClassTime.parse(value) ?: Calendar.getInstance().let {
                ClassTime(it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE))
            }
        }
        val time = rememberTimePickerState(initialTime.hour, initialTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            text = { TimePicker(state = time) },
            dismissButton = { TextButton({ showPicker = false }) { Text(stringResource(R.string.profile_cancel)) } },
            confirmButton = {
                TextButton({
                    onChange(ClassTime(time.hour, time.minute).format())
                    showPicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }
}
