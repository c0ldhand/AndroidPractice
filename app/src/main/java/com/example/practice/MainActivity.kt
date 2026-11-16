package com.example.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practice.navigation.AppNavigation
import com.example.practice.core.ui.theme.PracticeTheme
import com.example.practice.viewmodel.TrackListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeTheme {
                val container = (application as PracticeApplication).container
                val factory = container.viewModelFactory
                val trackViewModel: TrackListViewModel = viewModel(factory = factory)
                AppNavigation(
                    viewModel = trackViewModel,
                    viewModelFactory = factory,
                    profileComponent = container.profileComponent
                )
            }
        }
    }
}
