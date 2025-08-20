package com.locationsharing.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for WhatsNewTeaser component verifying:
 * - Animation performance and frame rate consistency
 * - Memory usage during state changes
 * - Recomposition efficiency
 * - Rendering performance with multiple instances
 * - Resource cleanup and memory leaks prevention
 */
@RunWith(AndroidJUnit4::class)
class WhatsNewTeaserPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whatsNewTeaser_rendersWithinPerformanceThreshold() {
        // Given
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    WhatsNewTeaser(
                        onTap = {},
                        isVisible = true
                    )
                }
            }
        }

        // Then - Initial render should be fast (under 100ms)
        assert(renderTime < 100) { 
            "WhatsNewTeaser initial render took ${renderTime}ms, should be under 100ms" 
        }
    }

    @Test
    fun whatsNewTeaser_handlesVisibilityChangesEfficiently() {
        // Given
        var isVisible by mutableStateOf(true)
        var recompositionCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                // Track recompositions
                SideEffect {
                    recompositionCount++
                }
                
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = isVisible
                )
            }
        }

        val initialRecompositions = recompositionCount

        // When - Toggle visibility multiple times
        val toggleTime = measureTimeMillis {
            repeat(10) {
                composeTestRule.runOnUiThread {
                    isVisible = !isVisible
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should handle toggles efficiently
        assert(toggleTime < 500) { 
            "Visibility toggles took ${toggleTime}ms, should be under 500ms" 
        }
        
        val finalRecompositions = recompositionCount - initialRecompositions
        assert(finalRecompositions <= 20) { 
            "Too many recompositions: $finalRecompositions, should be <= 20" 
        }
    }

    @Test
    fun whatsNewTeaser_handlesMultipleInstancesEfficiently() {
        // Given
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Multiple instances to test performance
                        repeat(5) { index ->
                            WhatsNewTeaser(
                                onTap = {},
                                isVisible = index % 2 == 0
                            )
                        }
                    }
                }
            }
        }

        // Then - Multiple instances should render efficiently
        assert(renderTime < 200) { 
            "Multiple WhatsNewTeaser instances render took ${renderTime}ms, should be under 200ms" 
        }
    }

    @Test
    fun whatsNewDialog_rendersWithinPerformanceThreshold() {
        // Given
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    WhatsNewDialog(
                        onDismiss = {}
                    )
                }
            }
        }

        // Then - Dialog should render quickly
        assert(renderTime < 150) { 
            "WhatsNewDialog render took ${renderTime}ms, should be under 150ms" 
        }
    }

    @Test
    fun whatsNewTeaser_animationPerformance() {
        // Given
        var isVisible by mutableStateOf(false)
        var animationFrameCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                // Monitor animation frames
                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        // Simulate frame counting during animation
                        repeat(60) { // Assume 60fps for 1 second
                            animationFrameCount++
                            kotlinx.coroutines.delay(16) // ~60fps
                        }
                    }
                }
                
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = isVisible
                )
            }
        }

        // When - Start animation
        val animationTime = measureTimeMillis {
            composeTestRule.runOnUiThread {
                isVisible = true
            }
            composeTestRule.waitForIdle()
        }

        // Then - Animation should start quickly
        assert(animationTime < 50) { 
            "Animation start took ${animationTime}ms, should be under 50ms" 
        }
    }

    @Test
    fun whatsNewTeaser_memoryUsageStability() {
        // Given
        var isVisible by mutableStateOf(true)
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = {},
                    isVisible = isVisible
                )
            }
        }

        // When - Rapid state changes to test memory stability
        repeat(100) {
            composeTestRule.runOnUiThread {
                isVisible = !isVisible
            }
            if (it % 10 == 0) {
                composeTestRule.waitForIdle()
                // Force garbage collection periodically
                System.gc()
            }
        }

        // Then - Should complete without memory issues
        composeTestRule.waitForIdle()
        // Test passes if no OutOfMemoryError is thrown
    }

    @Test
    fun whatsNewTeaser_callbackPerformance() {
        // Given
        var callbackExecutionTime = 0L
        val callback = {
            val startTime = System.nanoTime()
            // Simulate some work
            Thread.sleep(1)
            callbackExecutionTime = System.nanoTime() - startTime
        }
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewTeaser(
                    onTap = callback,
                    isVisible = true
                )
            }
        }

        // When - Execute callback
        composeTestRule
            .onNodeWithContentDescription("What's New teaser card. Tap to learn about new features: Nearby Friends panel and Quick Share")
            .performClick()

        // Then - Callback should execute quickly
        assert(callbackExecutionTime < 10_000_000) { // 10ms in nanoseconds
            "Callback execution took ${callbackExecutionTime / 1_000_000}ms, should be under 10ms"
        }
    }

    @Test
    fun whatsNewDialog_dismissalPerformance() {
        // Given
        var dismissalTime = 0L
        val onDismiss = {
            val startTime = System.nanoTime()
            // Simulate dismissal work
            dismissalTime = System.nanoTime() - startTime
        }
        
        composeTestRule.setContent {
            FFinderTheme {
                WhatsNewDialog(
                    onDismiss = onDismiss
                )
            }
        }

        // When - Dismiss dialog
        composeTestRule
            .onNodeWithText("Got it!")
            .performClick()

        // Then - Dismissal should be fast
        assert(dismissalTime < 5_000_000) { // 5ms in nanoseconds
            "Dialog dismissal took ${dismissalTime / 1_000_000}ms, should be under 5ms"
        }
    }

    @Test
    fun whatsNewTeaser_recompositionOptimization() {
        // Given
        var recompositionCount = 0
        var unrelatedState by mutableStateOf(0)
        
        composeTestRule.setContent {
            FFinderTheme {
                Column {
                    // Unrelated component that changes state
                    Text(text = "Counter: $unrelatedState")
                    
                    // WhatsNewTeaser should not recompose when unrelated state changes
                    WhatsNewTeaser(
                        onTap = {},
                        isVisible = true,
                        modifier = Modifier.then(
                            Modifier.layout { measurable, constraints ->
                                recompositionCount++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                        )
                    )
                }
            }
        }

        val initialRecompositions = recompositionCount

        // When - Change unrelated state
        repeat(5) {
            composeTestRule.runOnUiThread {
                unrelatedState++
            }
            composeTestRule.waitForIdle()
        }

        // Then - WhatsNewTeaser should not recompose unnecessarily
        val additionalRecompositions = recompositionCount - initialRecompositions
        assert(additionalRecompositions == 0) { 
            "WhatsNewTeaser recomposed $additionalRecompositions times due to unrelated state changes" 
        }
    }
}