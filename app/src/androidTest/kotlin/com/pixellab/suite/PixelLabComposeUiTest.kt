package com.pixellab.suite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelLabComposeUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun commandCenter_rendersHeaderAndNavigatesToCoreModules() {
        composeTestRule.onNodeWithText("PIXEL LAB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Twin").performClick()
        composeTestRule.onNodeWithText("MODULE 01 — PIXEL TWIN").assertIsDisplayed()

        composeTestRule.onNodeWithText("Sensors").performClick()
        composeTestRule.onNodeWithText("MODULE 03 — SENSORIUM").assertIsDisplayed()

        composeTestRule.onNodeWithText("LocalMind").performClick()
        composeTestRule.onNodeWithText("MODULE 04 — LOCALMIND").assertIsDisplayed()

        composeTestRule.onNodeWithText("EdgeFlow").performClick()
        composeTestRule.onNodeWithText("MODULE 08 — EDGEFLOW").assertIsDisplayed()
    }
}
