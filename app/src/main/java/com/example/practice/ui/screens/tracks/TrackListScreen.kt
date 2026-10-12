package com.example.practice.ui.screens.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.practice.R
import com.example.practice.ui.components.TrackCard
import com.example.practice.ui.components.NetworkError
import com.example.practice.ui.components.NetworkLoading
import com.example.practice.viewmodel.TrackListUiState
import com.example.practice.viewmodel.TrackViewModel

@Composable
fun TrackListRoute(viewModel: TrackViewModel, onTrackClick: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackListScreen(state, viewModel::updateQuery, viewModel::search, viewModel::retry, onTrackClick)
}

@Composable
fun TrackListScreen(
    state: TrackListUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onTrackClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = { keyboard?.hide(); onSearch() }
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().testTag("search_query"),
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                isError = state.isQueryEmpty,
                supportingText = if (state.isQueryEmpty) {
                    { Text(stringResource(R.string.empty_query)) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() })
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = submit, modifier = Modifier.testTag("search_button")) {
                    Text(stringResource(R.string.search))
                }
                TextButton(onClick = onRetry, enabled = !state.isLoading && state.submittedQuery.isNotBlank()) {
                    Text(stringResource(R.string.refresh))
                }
            }
        }
        when {
            state.isLoading -> NetworkLoading()
            state.error != null -> NetworkError(state.error, onRetry)
            state.tracks.isEmpty() -> Box(Modifier.fillMaxSize().testTag("empty_results"), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_tracks), modifier = Modifier.padding(24.dp))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("track_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "collection_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.search_results, state.submittedQuery), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.tracks_count, state.tracks.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(items = state.tracks, key = { it.id }) { track ->
                    TrackCard(track = track, onTrackClick = onTrackClick)
                }
            }
        }
    }
}
