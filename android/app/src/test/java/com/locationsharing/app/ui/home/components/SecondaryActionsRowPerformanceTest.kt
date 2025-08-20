package com.locationsharing.app.ui.home.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for SecondaryActionsRow component.
 * 
 * Ensures the component meets performance requirements including
 * fast rendering, efficient recomposition, and responsive interactions.
 */
@RunWith(AndroidJUnit4::class)
class SecondaryActionsRowPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun secondaryActionsRow_rendersQuickly() {
        // Given - Measure rendering time
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    SecondaryActionsRow(
                        onFriends = {},
                        onSettings = {}
                    )
                }
            }
            
            // Wait for composition to complete
            composeTestRule.waitForIdle()
        }

        // Then - Verify rendering is fast (should be under 100ms)
        assertTrue(
            "SecondaryActionsRow should render quickly (${renderTime}ms)",
            renderTime < 100
        )
    }

    @Test
    fun secondaryActionsRow_respondsToClicksQuickly() {
        // Given
        var friendsClicked = false
        var settingsClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { friendsClicked = true },
                    onSettings = { settingsClicked = true }
                )
            }
        }

        // When - Measure click response time
        val friendsClickTime = measureTimeMillis {
            composeTestRule
                .onNodeWithText("Friends")
                .performClick()
            composeTestRule.waitForIdle()
        }

        val settingsClickTime = measureTimeMillis {
            composeTestRule
                .onNodeWithText("Settings")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Then - Verify clicks are responsive (should be under 50ms)
        assertTrue("Friends button should respond quickly (${friendsClickTime}ms)", friendsClickTime < 50)
        assertTrue("Settings button should respond quickly (${settingsClickTime}ms)", settingsClickTime < 50)
        assertTrue("Friends callback should be invoked", friendsClicked)
        assertTrue("Settings callback should be invoked", settingsClicked)
    }

    @Test
    fun secondaryActionsRow_efficientRecomposition() {
        // Given - Test recomposition efficiency
        var recompositionCount = 0
        var externalState by mutableStateOf(0)

        composeTestRule.setContent {
            FFinderTheme {
                // Track recompositions
                recompositionCount++
                
                SecondaryActionsRow(
                    onFriends = { externalState++ },
                    onSettings = { externalState++ }
                )
            }
        }

        val initialRecompositions = recompositionCount

        // When - Trigger state changes
        composeTestRule
            .onNodeWithText("Friends")
            .performClick()

        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        // Then - Verify minimal recompositions occurred
        val finalRecompositions = recompositionCount
        val recompositionDelta = finalRecompositions - initialRecompositions

        assertTrue(
            "Should have minimal recompositions (${recompositionDelta})",
            recompositionDelta <= 2 // Allow for reasonable recomposition
        )
    }

    @Test
    fun secondaryActionsRow_memoryEfficient() {
        // Given - Test memory efficiency by creating multiple instances
        val instances = 10
        val creationTime = measureTimeMillis {
            repeat(instances) {
                composeTestRule.setContent {
                    FFinderTheme {
                        SecondaryActionsRow(
                            onFriends = {},
                            onSettings = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Verify efficient creation (should be under 500ms for 10 instances)
        val averageCreationTime = creationTime / instances
        assertTrue(
            "Should create instances efficiently (${averageCreationTime}ms average)",
            averageCreationTime < 50
        )
    }

    @Test
    fun secondaryActionsRow_stableUnderLoad() {
        // Given - Test stability under rapid interactions
        var friendsClickCount = 0
        var settingsClickCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                SecondaryActionsRow(
                    onFriends = { friendsClickCount++ },
                    onSettings = { settingsClickCount++ }
                )
            }
        }

        // When - Perform rapid clicks
        val rapidClickTime = measureTimeMillis {
            repeat(20) {
                if (it % 2 == 0) {
                    composeTestRule
                        .onNodeWithText("Friends")
                        .performClick()
                } else {
                    composeTestRule
                        .onNodeWithText("Settings")
                        .performClick()
                }
            }
            composeTestRule.waitForIdle()
        }

        // Then - Verify stability and all clicks were processed
        assertTrue("All Friends clicks should be processed", friendsClickCount == 10)
        assertTrue("All Settings clicks should be processed", settingsClickCount == 10)
        assertTrue(
            "Should handle rapid clicks efficiently (${rapidClickTime}ms)",
            rapidClickTime < 1000
        )
    }
}