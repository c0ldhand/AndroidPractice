package com.example.practice.ui.screens.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practice.R
import com.example.practice.model.Track
import com.example.practice.ui.components.TrackCard
import com.example.practice.data.MockTrackRepository
import com.example.practice.viewmodel.TrackListViewModel
import com.example.practice.viewmodel.TrackListViewModelFactory

@Composable
fun TrackListRoute(onTrackClick: (Long) -> Unit) {
    val factory = remember { TrackListViewModelFactory(MockTrackRepository()) }
    val viewModel: TrackListViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackListScreen(tracks = state.tracks, onTrackClick = onTrackClick)
}

@Composable
fun TrackListScreen(tracks: List<Track>, onTrackClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("track_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "collection_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.local_collection), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.tracks_count, tracks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(items = tracks, key = { it.id }) { track ->
            TrackCard(track = track, onTrackClick = onTrackClick)
        }
    }
}
