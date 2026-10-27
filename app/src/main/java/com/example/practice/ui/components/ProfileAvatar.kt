package com.example.practice.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.practice.R
import com.example.practice.platform.images.decodeProfileImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileAvatar(uri: String?, modifier: Modifier = Modifier, circular: Boolean = true, description: String? = null) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(null, uri) {
        value = null
        if (uri != null) value = withContext(Dispatchers.IO) {
            try { decodeProfileImage(resolver, Uri.parse(uri), 512) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }
    }
    Box(modifier.then(if (circular) Modifier.clip(CircleShape) else Modifier).background(MaterialTheme.colorScheme.secondaryContainer)) {
        val image = bitmap
        if (image != null) {
            Image(image.asImageBitmap(), description ?: stringResource(R.string.profile_avatar), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.Person, description ?: stringResource(R.string.profile_avatar), Modifier.fillMaxSize().padding(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
