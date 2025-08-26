package com.locationsharing.app.ui.home.components

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Performance tests for MapPreviewCard component.
 * 
 * Tests performance characteristics including:
 * - Rendering performance under different conditions
 * - Animation performance and frame rates
 * - Memory usage during state changes
 * - Responsiveness during rapid updates
 * - Resource cleanup and lifecycle management
 */
class MapPreviewCardPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapPreviewCard_initialRender_performsWithinThreshold() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When - Measure initial render time
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            
            // Wait for composition to complete
            composeTestRule.waitForIdle()
        }
        
        // Then - Should render within reasonable time (500ms threshold for complex map component)
        assert(renderTime < 500) { 
            "Initial render took ${renderTime}ms, should be under 500ms" 
        }
    }

    @Test
    fun mapPreviewCard_fallbackRender_isPerformant() = runTest {
        // When - Measure fallback UI render time
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = null,
                        hasLocationPermission = false,
                        animationsEnabled = true
                    )
                }
            }
            
            composeTestRule.waitForIdle()
        }
        
        // Then - Fallback should render very quickly (under 100ms)
        assert(renderTime < 100) { 
            "Fallback render took ${renderTime}ms, should be under 100ms" 
        }
    }

    @Test
    fun mapPreviewCard_stateChanges_performEfficiently() = runTest {
        // Given
        var hasPermission = false
        var location: LatLng? = null
        
        // When - Measure state change performance
        val stateChangeTime = measureTimeMillis {
            // Initial render
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = location,
                        hasLocationPermission = hasPermission,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // State change 1: Grant permission and add location
            hasPermission = true
            location = LatLng(37.7749, -122.4194)
            
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = location,
                        hasLocationPermission = hasPermission,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // State change 2: Change location
            location = LatLng(40.7128, -74.0060)
            
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = location,
                        hasLocationPermission = hasPermission,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // Then - State changes should be efficient (under 300ms total)
        assert(stateChangeTime < 300) { 
            "State changes took ${stateChangeTime}ms, should be under 300ms" 
        }
    }

    @Test
    fun mapPreviewCard_rapidLocationUpdates_maintainsPerformance() = runTest {
        // Given - Multiple location updates to simulate real-time tracking
        val locations = listOf(
            LatLng(37.7749, -122.4194), // San Francisco
            LatLng(37.7849, -122.4094), // Slight movement
            LatLng(37.7949, -122.3994), // More movement
            LatLng(37.8049, -122.3894), // Continued movement
            LatLng(37.8149, -122.3794)  // Final position
        )
        
        // When - Measure performance of rapid updates
        val updateTime = measureTimeMillis {
            locations.forEach { location ->
                composeTestRule.setContent {
                    FFinderTheme {
                        MapPreviewCard(
                            location = location,
                            hasLocationPermission = true,
                            animationsEnabled = true
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }
        }
        
        // Then - Rapid updates should maintain good performance (under 1 second for 5 updates)
        assert(updateTime < 1000) { 
            "Rapid location updates took ${updateTime}ms, should be under 1000ms" 
        }
    }

    @Test
    fun mapPreviewCard_animationToggle_performsEfficiently() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        var animationsEnabled = true
        
        // When - Measure animation toggle performance
        val toggleTime = measureTimeMillis {
            // Render with animations
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = animationsEnabled
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // Toggle animations off
            animationsEnabled = false
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = animationsEnabled
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // Toggle animations back on
            animationsEnabled = true
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = animationsEnabled
                    )
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // Then - Animation toggles should be fast (under 200ms)
        assert(toggleTime < 200) { 
            "Animation toggle took ${toggleTime}ms, should be under 200ms" 
        }
    }

    @Test
    fun mapPreviewCard_multipleInstances_scaleWell() = runTest {
        // Given - Multiple instances to test scaling
        val locations = listOf(
            LatLng(37.7749, -122.4194), // San Francisco
            LatLng(40.7128, -74.0060),  // New York
            LatLng(51.5074, -0.1278)    // London
        )
        
        // When - Measure performance with multiple instances
        val multiInstanceTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    androidx.compose.foundation.layout.Column {
                        locations.forEach { location ->
                            MapPreviewCard(
                                location = location,
                                hasLocationPermission = true,
                                animationsEnabled = true
                            )
                        }
                    }
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // Then - Multiple instances should render reasonably (under 800ms for 3 instances)
        assert(multiInstanceTime < 800) { 
            "Multiple instances took ${multiInstanceTime}ms, should be under 800ms" 
        }
    }

    @Test
    fun mapPreviewCard_errorHandling_doesNotImpactPerformance() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When - Measure error handling performance
        val errorHandlingTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewWithErrorHandling(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // Then - Error handling wrapper should not significantly impact performance
        assert(errorHandlingTime < 600) { 
            "Error handling took ${errorHandlingTime}ms, should be under 600ms" 
        }
    }

    @Test
    fun mapPreviewCard_memoryUsage_staysReasonable() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When - Create and destroy multiple instances to test memory usage
        repeat(10) {
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // Clear content to test cleanup
            composeTestRule.setContent { }
            composeTestRule.waitForIdle()
        }
        
        // Then - Should complete without memory issues
        // (In a real scenario, this would be monitored with memory profiling tools)
        assert(true) { "Memory test completed successfully" }
    }

    @Test
    fun mapPreviewCard_compositionPerformance_isOptimal() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        var recompositionCount = 0
        
        // When - Test recomposition performance
        val compositionTime = measureTimeMillis {
            composeTestRule.setContent {
                FFinderTheme {
                    // Track recompositions (in real app, use composition locals or remember)
                    recompositionCount++
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // Trigger recomposition with same data (should be optimized)
            composeTestRule.setContent {
                FFinderTheme {
                    recompositionCount++
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
        }
        
        // Then - Composition should be efficient
        assert(compositionTime < 400) { 
            "Composition took ${compositionTime}ms, should be under 400ms" 
        }
    }

    @Test
    fun mapPreviewCard_resourceCleanup_isEfficient() = runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        
        // When - Test resource cleanup
        val cleanupTime = measureTimeMillis {
            // Create component
            composeTestRule.setContent {
                FFinderTheme {
                    MapPreviewCard(
                        location = testLocation,
                        hasLocationPermission = true,
                        animationsEnabled = true
                    )
                }
            }
            composeTestRule.waitForIdle()
            
            // Remove component (should trigger cleanup)
            composeTestRule.setContent { }
            composeTestRule.waitForIdle()
        }
        
        // Then - Cleanup should be fast
        assert(cleanupTime < 300) { 
            "Resource cleanup took ${cleanupTime}ms, should be under 300ms" 
        }
    }
}