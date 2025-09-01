package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.StateDescription
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.MainActivity
import com.example.myapplication.ui.components.CaptureButton
import com.example.myapplication.ui.components.CompareSlider
import com.example.myapplication.ui.screens.GalleryAnimated
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiComponentsTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureButton_transitions_back_to_idle() {
        composeTestRule.setContent {
            CaptureButton(autoAdvance = true)
        }
        // Click
        composeTestRule.onNode(hasClickAction()).performClick()
        // Expect back to Idle within 1s
        composeTestRule.waitUntil(1000) {
            composeTestRule.onAllNodes(hasStateDescription("Idle")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun compareSlider_updates_fraction_on_drag() {
        composeTestRule.setContent {
            CompareSlider(
                before = {},
                after = {}
            )
        }
        val node = composeTestRule.onNodeWithContentDescription("Compare slider")
        node.assertExists()
        node.performTouchInput { swipeRight() }
        composeTestRule.waitUntil(500) {
            composeTestRule.onAllNodes(hasStateDescription("1.00")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun gallery_items_appear_with_semantics() {
        val items = (1..6).map { Uri.parse("file:///sdcard/Pictures/AI/${it}.jpg") }
        composeTestRule.setContent {
            GalleryAnimated(items = items, onItemClick = {})
        }
        // At least first item becomes visible and accessible
        composeTestRule.onNodeWithContentDescription("Image 1").assertExists()
    }
}

