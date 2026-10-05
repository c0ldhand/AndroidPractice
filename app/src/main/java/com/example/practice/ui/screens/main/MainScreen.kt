package com.example.practice.ui.screens.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.practice.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    selectedTab: Screen?,
    onBottomItemClick: (Screen) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = if (selectedTab != null) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
        topBar = {
            if (selectedTab != null) {
                TopAppBar(title = { Text(stringResource(selectedTab.titleRes)) })
            }
        },
        bottomBar = {
            if (selectedTab != null) {
                NavigationBar {
                    Screen.bottomItems.forEach { screen ->
                        NavigationBarItem(
                            selected = screen == selectedTab,
                            onClick = { onBottomItemClick(screen) },
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Tracks -> Icons.AutoMirrored.Filled.List
                                        Screen.Favorites -> Icons.Default.Favorite
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(screen.titleRes)) }
                        )
                    }
                }
            }
        },
        content = content
    )
}
