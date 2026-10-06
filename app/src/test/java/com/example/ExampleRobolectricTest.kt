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
    fun carStreamNavigationAndAndroidAutoHudWorks() {
        // Verify Stream Deck screen is displayed on launch
        composeTestRule.onNodeWithTag("stream_deck_screen").assertIsDisplayed()

        // Navigate to Android Auto Head-Unit HUD screen
        composeTestRule.onNodeWithTag("nav_tab_car_head_unit_hud").performClick()
        composeTestRule.onNodeWithTag("android_auto_hud_screen").assertIsDisplayed()

        // Navigate to Engine Fixes screen
        composeTestRule.onNodeWithTag("nav_tab_engine_fixes").performClick()
        composeTestRule.onNodeWithTag("engine_fixes_screen").assertIsDisplayed()
    }
}
