package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun carStreamNavigationAndFullscreenToggleWorks() {
        // Verify Stream Deck screen and Fullscreen toggle button are displayed on launch
        composeTestRule.onNodeWithTag("stream_deck_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_toggle_fullscreen_top").assertIsDisplayed()

        // Navigate to Bookmarks & History screen
        composeTestRule.onNodeWithTag("nav_tab_bookmarks_history").performClick()
        composeTestRule.onNodeWithTag("bookmarks_history_screen").assertIsDisplayed()

        // Navigate to Engine Fixes screen
        composeTestRule.onNodeWithTag("nav_tab_engine_fixes").performClick()
        composeTestRule.onNodeWithTag("engine_fixes_screen").assertIsDisplayed()
    }
}
