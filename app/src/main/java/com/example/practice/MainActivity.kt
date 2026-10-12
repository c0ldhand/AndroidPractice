package com.example.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practice.navigation.AppNavigation
import com.example.practice.ui.theme.PracticeTheme
import com.example.practice.viewmodel.TrackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticeTheme {
                val factory = (application as PracticeApplication).container.viewModelFactory
                val trackViewModel: TrackViewModel = viewModel(factory = factory)
                AppNavigation(viewModel = trackViewModel, viewModelFactory = factory)
            }
        }
    }
}
