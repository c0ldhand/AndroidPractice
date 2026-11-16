package com.example.practice.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.practice.R
import com.example.practice.domain.model.TrackError

@Composable
fun NetworkLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().testTag("network_loading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading_tracks))
    }
}

@Composable
fun NetworkError(error: TrackError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = when (error) {
        TrackError.CONNECTION -> R.string.error_connection
        TrackError.TIMEOUT -> R.string.error_timeout
        TrackError.SERVER -> R.string.error_server
        TrackError.RATE_LIMIT -> R.string.error_rate_limit
        TrackError.INVALID_RESPONSE -> R.string.error_invalid_response
        TrackError.UNKNOWN -> R.string.error_unknown
    }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).testTag("network_error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(stringResource(R.string.loading_failed), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(message), textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.testTag("network_retry")) {
            Text(stringResource(R.string.retry))
        }
    }
}
