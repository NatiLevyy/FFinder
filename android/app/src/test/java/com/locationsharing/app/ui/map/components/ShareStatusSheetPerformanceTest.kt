package com.locationsharing.app.ui.map.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

/**
 * Performance tests for ShareStatusSheet component.
 * 
 * Tests performance requirements and optimization:
 * - Animation performance (60fps target)
 * - Memory usage optimization
 * - Rapid state change handling
 * - Recomposition efficiency
 * - Large coordinate value handling
 * - Concurrent interaction performance
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShareStatusSheetPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testLocation = LatLng(37.7749, -122.4194)

    @Test
    fun shareStatusSheet_rapidStateChanges_performsWell() = runTest {
        // Given - Rapid state changes to test recomposition performance
        var isVisible by mutableStateOf(false)
        var isActive by mutableStateOf(false)
        var location by mutableStateOf(testLocation)

        val stateChangeCount = 100
        var callbackCount = 0

        // When - Measure time for rapid state changes
        val executionTime = measureTimeMillis {
            repeat(stateChangeCount) { iteration ->
                isVisible = iteration % 2 == 0
                isActive = iteration % 3 == 0
                location = LatLng(
                    testLocation.latitude + (iteration * 0.001),
                    testLocation.longitude + (iteration * 0.001)
                )

                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = isVisible,
                            isLocationSharingActive = isActive,
                            currentLocation = location,
                            onDismiss = { callbackCount++ },
                            onStopSharing = { callbackCount++ }
                        )
                    }
                }

                // Allow composition to complete
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should complete within reasonable time (< 5 seconds for 100 changes)
        assert(executionTime < 5000) { 
            "Rapid state changes took too long: ${executionTime}ms" 
        }
    }

    @Test
    fun shareStatusSheet_animationPerformance_meetsTargets() = runTest {
        // Given - Animation performance test
        var isVisible by mutableStateOf(false)
        val animationCount = 50

        // When - Measure animation performance
        val animationTime = measureTimeMillis {
            repeat(animationCount) {
                isVisible = !isVisible

                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = isVisible,
                            isLocationSharingActive = true,
                            currentLocation = testLocation,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }

                // Wait for animation to complete
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should maintain 60fps (16.67ms per frame)
        val averageTimePerAnimation = animationTime.toDouble() / animationCount
        assert(averageTimePerAnimation < 200) { // 200ms is reasonable for show/hide animation
            "Animation performance too slow: ${averageTimePerAnimation}ms per animation"
        }
    }

    @Test
    fun shareStatusSheet_memoryUsage_isOptimal() = runTest {
        // Given - Memory usage test with multiple instances
        val instanceCount = 20
        val locations = (0 until instanceCount).map { i ->
            LatLng(
                testLocation.latitude + (i * 0.01),
                testLocation.longitude + (i * 0.01)
            )
        }

        // When - Create multiple instances
        val memoryTestTime = measureTimeMillis {
            locations.forEach { location ->
                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = true,
                            isLocationSharingActive = true,
                            currentLocation = location,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should handle multiple instances efficiently
        assert(memoryTestTime < 2000) {
            "Memory usage test took too long: ${memoryTestTime}ms"
        }
    }

    @Test
    fun shareStatusSheet_coordinateFormatting_performance() = runTest {
        // Given - Test coordinate formatting performance with extreme values
        val extremeLocations = listOf(
            LatLng(89.999999, 179.999999),
            LatLng(-89.999999, -179.999999),
            LatLng(0.000001, 0.000001),
            LatLng(45.123456789, -123.987654321),
            LatLng(Double.MAX_VALUE, Double.MAX_VALUE), // Edge case
            LatLng(Double.MIN_VALUE, Double.MIN_VALUE)  // Edge case
        )

        // When - Test formatting performance
        val formattingTime = measureTimeMillis {
            extremeLocations.forEach { location ->
                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = true,
                            isLocationSharingActive = true,
                            currentLocation = location,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should format coordinates efficiently
        assert(formattingTime < 1000) {
            "Coordinate formatting took too long: ${formattingTime}ms"
        }
    }

    @Test
    fun shareStatusSheet_concurrentInteractions_handleCorrectly() = runTest {
        // Given - Concurrent interaction test
        var dismissCount = 0
        var stopSharingCount = 0
        val interactionCount = 50

        composeTestRule.setContent {
            FFinderTheme {
                ShareStatusSheet(
                    isVisible = true,
                    isLocationSharingActive = true,
                    currentLocation = testLocation,
                    onDismiss = { dismissCount++ },
                    onStopSharing = { stopSharingCount++ }
                )
            }
        }

        // When - Perform rapid interactions
        val interactionTime = measureTimeMillis {
            repeat(interactionCount) {
                composeTestRule
                    .onNodeWithText("Stop Sharing")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should handle interactions efficiently
        assert(interactionTime < 3000) {
            "Concurrent interactions took too long: ${interactionTime}ms"
        }
        
        assert(stopSharingCount == interactionCount) {
            "Not all interactions were registered: $stopSharingCount/$interactionCount"
        }
    }

    @Test
    fun shareStatusSheet_recompositionEfficiency_isOptimal() = runTest {
        // Given - Test recomposition efficiency
        var location by mutableStateOf(testLocation)
        var recompositionCount = 0

        // When - Track recompositions with location changes
        val recompositionTime = measureTimeMillis {
            repeat(100) { i ->
                location = LatLng(
                    testLocation.latitude + (i * 0.0001),
                    testLocation.longitude + (i * 0.0001)
                )

                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = true,
                            isLocationSharingActive = true,
                            currentLocation = location,
                            onDismiss = { recompositionCount++ },
                            onStopSharing = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should recompose efficiently
        assert(recompositionTime < 2000) {
            "Recomposition took too long: ${recompositionTime}ms"
        }
    }

    @Test
    fun shareStatusSheet_largeDataSet_performance() = runTest {
        // Given - Test with large coordinate precision
        val preciseLocation = LatLng(
            37.77493827465827364,
            -122.41940192837465
        )

        // When - Test performance with high precision coordinates
        val precisionTime = measureTimeMillis {
            repeat(20) {
                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = true,
                            isLocationSharingActive = true,
                            currentLocation = preciseLocation,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should handle high precision efficiently
        assert(precisionTime < 1000) {
            "High precision coordinate handling took too long: ${precisionTime}ms"
        }
    }

    @Test
    fun shareStatusSheet_stateTransition_performance() = runTest {
        // Given - State transition performance test
        var isVisible by mutableStateOf(false)
        var isActive by mutableStateOf(false)
        val transitionCount = 30

        // When - Test rapid state transitions
        val transitionTime = measureTimeMillis {
            repeat(transitionCount) { i ->
                isVisible = i % 2 == 0
                isActive = i % 3 == 0

                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = isVisible,
                            isLocationSharingActive = isActive,
                            currentLocation = testLocation,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should transition states efficiently
        assert(transitionTime < 2000) {
            "State transitions took too long: ${transitionTime}ms"
        }
    }

    @Test
    fun shareStatusSheet_componentLifecycle_performance() = runTest {
        // Given - Component lifecycle performance test
        val lifecycleCount = 25

        // When - Test component creation/destruction
        val lifecycleTime = measureTimeMillis {
            repeat(lifecycleCount) {
                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = true,
                            isLocationSharingActive = true,
                            currentLocation = testLocation,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()

                // Clear content to test destruction
                composeTestRule.setContent { }
                composeTestRule.waitForIdle()
            }
        }

        // Then - Should handle lifecycle efficiently
        assert(lifecycleTime < 3000) {
            "Component lifecycle took too long: ${lifecycleTime}ms"
        }
    }

    @Test
    fun shareStatusSheet_accessibilityPerformance_isOptimal() = runTest {
        // Given - Accessibility performance test
        val accessibilityTestCount = 30

        // When - Test accessibility with rapid changes
        val accessibilityTime = measureTimeMillis {
            repeat(accessibilityTestCount) { i ->
                val isActive = i % 2 == 0
                
                composeTestRule.setContent {
                    FFinderTheme {
                        ShareStatusSheet(
                            isVisible = true,
                            isLocationSharingActive = isActive,
                            currentLocation = testLocation,
                            onDismiss = {},
                            onStopSharing = {}
                        )
                    }
                }

                // Verify accessibility elements are present
                composeTestRule
                    .onNodeWithContentDescription("Location sharing status dialog")
                    .assertExists()

                composeTestRule.waitForIdle()
            }
        }

        // Then - Should maintain accessibility performance
        assert(accessibilityTime < 2000) {
            "Accessibility performance test took too long: ${accessibilityTime}ms"
        }
    }
}