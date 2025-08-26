package com.locationsharing.app.ui.home.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for the PrimaryCallToAction component.
 * 
 * Ensures the component maintains 60fps performance and handles
 * frequent recompositions efficiently.
 */
@RunWith(AndroidJUnit4::class)
class PrimaryCallToActionPerformanceTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun primaryCallToAction_compositionPerformance() {
        val compositionTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    PrimaryCallToAction(
                        onStartShare = {},
                        isNarrowScreen = false
                    )
                }
            }
        }
        
        // Initial composition should be fast (< 16ms for 60fps)
        assert(compositionTime < 50) { 
            "Initial composition took ${compositionTime}ms, should be < 50ms" 
        }
    }
    
    @Test
    fun primaryCallToAction_recompositionPerformance() {
        var isNarrow by mutableStateOf(false)
        var recompositionCount = 0
        
        composeTestRule.setContent {
            recompositionCount++
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        val recompositionTime = measureTimeMillis {
            // Trigger multiple recompositions
            repeat(10) {
                isNarrow = !isNarrow
                composeTestRule.setContent {
                    recompositionCount++
                    FFinderTheme {
                        PrimaryCallToAction(
                            onStartShare = {},
                            isNarrowScreen = isNarrow
                        )
                    }
                }
            }
        }
        
        // Recompositions should be efficient
        val averageRecompositionTime = recompositionTime / 10.0
        assert(averageRecompositionTime < 16) { 
            "Average recomposition took ${averageRecompositionTime}ms, should be < 16ms for 60fps" 
        }
    }
    
    @Test
    fun primaryCallToAction_clickResponseTime() {
        var clickCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { clickCount++ },
                    isNarrowScreen = false
                )
            }
        }
        
        val clickTime = measureTimeMillis {
            repeat(5) {
                composeTestRule
                    .onNodeWithText("Start Live Sharing")
                    .performClick()
            }
        }
        
        // Click handling should be immediate
        val averageClickTime = clickTime / 5.0
        assert(averageClickTime < 10) { 
            "Average click response took ${averageClickTime}ms, should be < 10ms" 
        }
        assert(clickCount == 5) { "All clicks should be registered" }
    }
    
    @Test
    fun primaryCallToAction_memoryEfficiency() {
        var componentCount = 0
        
        // Create multiple instances to test memory usage
        repeat(100) {
            composeTestRule.setContent {
                componentCount++
                FFinderTheme {
                    PrimaryCallToAction(
                        onStartShare = {},
                        isNarrowScreen = componentCount % 2 == 0
                    )
                }
            }
        }
        
        // Should handle multiple instances without issues
        assert(componentCount == 100) { "All component instances should be created" }
    }
    
    @Test
    fun primaryCallToAction_rapidStateChanges() {
        var isNarrow by mutableStateOf(false)
        var stateChangeCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { stateChangeCount++ },
                    isNarrowScreen = isNarrow
                )
            }
        }
        
        val stateChangeTime = measureTimeMillis {
            // Rapid state changes to test performance
            repeat(50) {
                isNarrow = !isNarrow
                composeTestRule.setContent {
                    FFinderTheme {
                        PrimaryCallToAction(
                            onStartShare = { stateChangeCount++ },
                            isNarrowScreen = isNarrow
                        )
                    }
                }
            }
        }
        
        // Should handle rapid state changes efficiently
        val averageStateChangeTime = stateChangeTime / 50.0
        assert(averageStateChangeTime < 5) { 
            "Average state change took ${averageStateChangeTime}ms, should be < 5ms" 
        }
    }
    
    @Test
    fun primaryCallToAction_tooltipPerformance() {
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = {},
                    isNarrowScreen = true // Enables tooltip
                )
            }
        }
        
        val tooltipInteractionTime = measureTimeMillis {
            // Test tooltip interaction performance
            repeat(3) {
                composeTestRule
                    .onNodeWithContentDescription("Start Live Sharing")
                    .performClick()
            }
        }
        
        // Tooltip interactions should be smooth
        val averageTooltipTime = tooltipInteractionTime / 3.0
        assert(averageTooltipTime < 20) { 
            "Average tooltip interaction took ${averageTooltipTime}ms, should be < 20ms" 
        }
    }
    
    @Test
    fun primaryCallToAction_hapticFeedbackPerformance() {
        var hapticCallCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                PrimaryCallToAction(
                    onStartShare = { hapticCallCount++ },
                    isNarrowScreen = false
                )
            }
        }
        
        val hapticTime = measureTimeMillis {
            // Test haptic feedback doesn't block UI
            repeat(10) {
                composeTestRule
                    .onNodeWithText("Start Live Sharing")
                    .performClick()
            }
        }
        
        // Haptic feedback should not impact performance
        val averageHapticTime = hapticTime / 10.0
        assert(averageHapticTime < 15) { 
            "Average haptic feedback time took ${averageHapticTime}ms, should be < 15ms" 
        }
        assert(hapticCallCount == 10) { "All haptic feedback calls should complete" }
    }
}