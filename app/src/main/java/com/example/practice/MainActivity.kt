package com.example.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practice.data.MockTrackRepository
import com.example.practice.navigation.AppNavigation
import com.example.practice.ui.theme.PracticeTheme
import com.example.practice.viewmodel.TrackViewModel
import com.example.practice.viewmodel.TrackViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeTheme {
                val factory = remember { TrackViewModelFactory(MockTrackRepository()) }
                val trackViewModel: TrackViewModel = viewModel(factory = factory)
                AppNavigation(viewModel = trackViewModel)
            }
        }
    }
}
