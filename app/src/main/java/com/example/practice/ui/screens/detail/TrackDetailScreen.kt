package com.example.practice.ui.screens.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.practice.R
import com.example.practice.domain.model.Track
import com.example.practice.ui.components.TrackArtwork
import com.example.practice.ui.components.NetworkError
import com.example.practice.ui.components.NetworkLoading
import com.example.practice.viewmodel.TrackDetailUiState
import com.example.practice.viewmodel.TrackDetailViewModel
import com.example.practice.ui.formatting.formatDuration
import com.example.practice.ui.formatting.formatPrice
import com.example.practice.ui.formatting.formatReleaseDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailRoute(viewModel: TrackDetailViewModel, onBackClick: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrackDetailScreen(state, viewModel::retry, onBackClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(state: TrackDetailUiState, onRetry: () -> Unit, onBackClick: () -> Unit) {
    val track = state.track
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.track_details)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            NetworkLoading(Modifier.padding(innerPadding))
        } else if (state.error != null) {
            NetworkError(state.error, onRetry, Modifier.padding(innerPadding))
        } else if (track == null) {
            TrackNotFound(onBackClick, Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("track_details"),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TrackArtwork(Modifier.size(200.dp))
                        Text(
                            track.trackName,
                            modifier = Modifier.testTag("detail_title"),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(track.artistName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { TrackInformation(track) }
            }
        }
    }
}

@Composable
private fun TrackInformation(track: Track) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.track_information), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            InformationField(R.string.track_name, track.trackName)
            InformationField(R.string.artist, track.artistName)
            InformationField(R.string.album, track.collectionName)
            InformationField(R.string.genre, track.genre)
            InformationField(R.string.release_date, formatReleaseDate(track.releaseDate))
            InformationField(R.string.duration, formatDuration(track.trackTimeMillis))
            InformationField(R.string.price, formatPrice(track.trackPrice, track.currency) ?: stringResource(R.string.price_unavailable))
            InformationField(R.string.currency, track.currency)
            InformationField(R.string.country, track.country)
            InformationField(
                R.string.streaming,
                stringResource(if (track.isStreamable) R.string.streaming_available else R.string.streaming_unavailable)
            )
        }
    }
}

@Composable
private fun InformationField(@StringRes labelRes: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TrackNotFound(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.track_not_found), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.track_not_found_description), textAlign = TextAlign.Center)
        Button(onClick = onBackClick) { Text(stringResource(R.string.back)) }
    }
}
