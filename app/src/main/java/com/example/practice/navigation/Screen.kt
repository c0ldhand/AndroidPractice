package com.example.practice.navigation

import com.example.practice.feature.profile.R as ProfileR
import androidx.annotation.StringRes
import com.example.practice.R

sealed class Screen(val route: String, @param:StringRes val titleRes: Int) {
    data object Tracks : Screen("tracks", R.string.tracks)
    data object Favorites : Screen("favorites", R.string.favorites)
    data object Profile : Screen("profile", ProfileR.string.profile)
    data object ProfileEdit : Screen("profile/edit", ProfileR.string.profile_edit)
    data object Filters : Screen("filters", R.string.filters)
    data object TrackDetail : Screen("track/{trackId}", R.string.track_details) {
        const val TRACK_ID = "trackId"
        fun routeFor(trackId: Long): String = "track/$trackId"
    }

    companion object {
        val bottomItems: List<Screen> = listOf(Tracks, Favorites, Profile)
    }
}
