package com.example.practice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.practice.data.MockTrackRepository
import com.example.practice.navigation.AppNavigation
import com.example.practice.navigation.Screen
import com.example.practice.ui.theme.PracticeTheme
import com.example.practice.viewmodel.TrackViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var navController: NavHostController

    @Before
    fun showApp() {
        val viewModel = TrackViewModel(MockTrackRepository())
        compose.setContent {
            navController = rememberNavController()
            PracticeTheme { AppNavigation(viewModel, navController) }
        }
    }

    @Test
    fun startsWithTracksAndScrollsThroughWholeCatalog() {
        compose.onNodeWithText("Треков: 18").assertIsDisplayed()
        tab("Треки").assertIsSelected()
        compose.onNodeWithTag("track_1").assertIsDisplayed()
        compose.onNodeWithTag("track_list").performScrollToNode(hasTestTag("track_18"))
        compose.onNodeWithText("Enjoy the Silence").assertIsDisplayed()
    }

    @Test
    fun opensTwoDifferentTracksAndReturnsUsingToolbarAndSystemBack() {
        compose.onNodeWithTag("track_1").performClick()
        compose.onNodeWithTag("track_details").assertIsDisplayed()
        compose.onNodeWithTag("detail_title").assertIsDisplayed().assertTextEquals("One More Time")
        tab("Избранное").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1L, navController.currentBackStackEntry?.arguments?.getLong(Screen.TrackDetail.TRACK_ID))
        }
        compose.onNodeWithTag("track_details").performScrollToNode(hasText("5:20"))
        compose.onNodeWithText("5:20").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("track_list").assertIsDisplayed()
        compose.onNodeWithTag("track_2").performClick()
        compose.onNodeWithTag("detail_title").assertIsDisplayed().assertTextEquals("Bohemian Rhapsody")
        compose.runOnIdle {
            assertEquals(2L, navController.currentBackStackEntry?.arguments?.getLong(Screen.TrackDetail.TRACK_ID))
        }
        Espresso.pressBack()
        compose.onNodeWithTag("track_list").assertIsDisplayed()
    }

    @Test
    fun switchesTabsWithoutDuplicatesAndRestoresListPosition() {
        compose.onNodeWithTag("track_list").performScrollToNode(hasTestTag("track_18"))
        repeat(5) {
            tab("Избранное").performClick()
            tab("Избранное").assertIsSelected().performClick()
            compose.onNodeWithText("Этот экран будет реализован в следующих практиках").assertIsDisplayed()
            tab("Профиль").performClick()
            tab("Профиль").assertIsSelected().performClick()
            compose.onNodeWithText("Этот экран будет реализован в следующих практиках").assertIsDisplayed()
            tab("Треки").performClick()
            tab("Треки").assertIsSelected().performClick()
            compose.onNodeWithText("Enjoy the Silence").assertIsDisplayed()
        }
        compose.runOnIdle {
            assertEquals(Screen.Tracks.route, navController.currentDestination?.route)
            assertNull(navController.previousBackStackEntry)
        }
    }

    @Test
    fun missingTrackShowsMessageAndAllowsReturning() {
        compose.runOnIdle { navController.navigate(Screen.TrackDetail.routeFor(-1)) }
        compose.onNodeWithText("Трек не найден").assertIsDisplayed()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithTag("track_list").assertIsDisplayed()
    }

    @Test
    fun missingPriceAndUnavailableStreamingHaveReadableLabels() {
        compose.onNodeWithTag("track_list").performScrollToNode(hasTestTag("track_12"))
        compose.onNodeWithTag("track_12").performClick()
        compose.onNodeWithTag("track_details").performScrollToNode(hasText("Цена недоступна"))
        compose.onNodeWithText("Цена недоступна").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("track_list").performScrollToNode(hasTestTag("track_18"))
        compose.onNodeWithTag("track_18").performClick()
        compose.onNodeWithTag("track_details").performScrollToNode(hasText("Недоступен"))
        compose.onNodeWithText("Недоступен").assertIsDisplayed()
    }

    private fun tab(title: String) = compose.onNode(hasText(title) and hasClickAction())
}
