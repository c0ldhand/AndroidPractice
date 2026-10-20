package com.example.practice.ui.screens.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.practice.R
import com.example.practice.domain.model.TrackFilters
import com.example.practice.viewmodel.FiltersUiState
import com.example.practice.viewmodel.FiltersViewModel

@Composable
fun FiltersRoute(viewModel: FiltersViewModel, onBack: () -> Unit, onApplied: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) { if (state.isSaved) onApplied() }
    FiltersScreen(state, viewModel::update, viewModel::reset, viewModel::save, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    state: FiltersUiState,
    onChange: (TrackFilters) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(enabled = state.isSaving) { /* Finish the write before leaving. */ }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.filters)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = onReset, enabled = !state.isSaving) {
                        Text(stringResource(R.string.reset_filters))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(24.dp).testTag("filters_screen"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(stringResource(R.string.filters_description), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = state.filters.genre,
                onValueChange = { onChange(state.filters.copy(genre = it)) },
                label = { Text(stringResource(R.string.genre)) },
                placeholder = { Text(stringResource(R.string.genre_example)) },
                modifier = Modifier.fillMaxWidth().testTag("filter_genre"),
                singleLine = true,
                enabled = !state.isSaving
            )
            OutlinedTextField(
                value = state.filters.album,
                onValueChange = { onChange(state.filters.copy(album = it)) },
                label = { Text(stringResource(R.string.album_name_filter)) },
                placeholder = { Text(stringResource(R.string.album_example)) },
                modifier = Modifier.fillMaxWidth().testTag("filter_album"),
                singleLine = true,
                enabled = !state.isSaving
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.streamable_only), modifier = Modifier.weight(1f))
                Switch(
                    checked = state.filters.streamableOnly,
                    onCheckedChange = { onChange(state.filters.copy(streamableOnly = it)) },
                    enabled = !state.isSaving,
                    modifier = Modifier.testTag("filter_streamable")
                )
            }
            if (state.saveFailed) {
                Text(stringResource(R.string.filters_save_failed), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { keyboard?.hide(); onSave() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag("save_filters")
            ) {
                Text(stringResource(if (state.isSaving) R.string.saving else R.string.done))
            }
        }
    }
}
