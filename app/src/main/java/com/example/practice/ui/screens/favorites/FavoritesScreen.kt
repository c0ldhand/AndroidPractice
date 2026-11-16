package com.example.practice.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.practice.R
import com.example.practice.ui.components.TrackCard
import com.example.practice.ui.components.NetworkLoading
import com.example.practice.viewmodel.FavoritesUiState
import com.example.practice.viewmodel.FavoritesViewModel

@Composable
fun FavoritesRoute(viewModel: FavoritesViewModel, onTrackClick: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesScreen(state, viewModel::retry, onTrackClick)
}

@Composable
fun FavoritesScreen(state: FavoritesUiState, onRetry: () -> Unit, onTrackClick: (Long) -> Unit) {
    when {
        state.isLoading -> NetworkLoading()
        state.loadFailed -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.favorites_load_failed))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        state.tracks.isEmpty() -> Column(
            Modifier.fillMaxSize().padding(24.dp).testTag("empty_favorites"),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.no_favorites), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.no_favorites_description))
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().testTag("favorites_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(stringResource(R.string.favorites_offline), style = MaterialTheme.typography.bodyMedium) }
            items(state.tracks, key = { it.id }) { track -> TrackCard(track, onTrackClick) }
        }
    }
}
