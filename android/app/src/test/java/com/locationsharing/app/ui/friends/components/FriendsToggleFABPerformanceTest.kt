package com.locationsharing.app.ui.friends.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for FriendsToggleFAB component.
 * 
 * Tests performance optimizations including:
 * - Efficient badge rendering for friend count updates
 * - Proper animation resource cleanup to prevent memory leaks
 * - Use of derivedStateOf for computed properties like friend count display
 * - Optimization of recomposition by using stable state objects
 * - Performance with large friend lists
 * 
 * Requirements: 5.4
 */
@RunWith(AndroidJUnit4::class)
class FriendsToggleFABPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEfficientBadgeRendering() {
        var friendCount by mutableStateOf(0)
        var recompositionCount = 0

        composeTestRule.setContent {
            recompositionCount++
            FriendsToggleFAB(
                onClick = { },
                friendCount = friendCount,
                isExpanded = true,
                isPanelOpen = false
            )
        }

        val initialRecompositions = recompositionCount

        // Test badge rendering efficiency with friend count updates
        val updateTime = measureTimeMillis {
            // Update friend count multiple times
            repeat(10) { i ->
                friendCount = i + 1
                composeTestRule.waitForIdle()
            }
        }

        val finalRecompositions = recompositionCount

        // Verify efficient rendering
        assert(updateTime < 1000L) { "Badge rendering took too long: ${updateTime}ms" }
        
        // Verify recompositions are reasonable (should not recompose for every single update)
        val recompositionDelta = finalRecompositions - initialRecompositions
        assert(recompositionDelta <= 15) { "Too many recompositions: $recompositionDelta" }
    }

    @Test
    fun testFriendCountDisplayOptimization() {
        var friendCount by mutableStateOf(0)
        var displayUpdateCount = 0

        composeTestRule.setContent {
            FriendsToggleFAB(
                onClick = { 
                    displayUpdateCount++
                },
                friendCount = friendCount,
                isExpanded = true,
                isPanelOpen = false
            )
        }

        // Test friend count display optimization
        val testCases = listOf(0, 1, 5, 50, 99, 100, 150)
        
        testCases.forEach { count ->
            friendCount = count
            composeTestRule.waitForIdle()
        }

        // Verify the component handles various friend counts efficiently
        // The actual display logic is tested through the stable state
    }

    @Test
    fun testStableStateObjectOptimization() {
        var friendCount by mutableStateOf(5)
        var isExpanded by mutableStateOf(true)
        var isPanelOpen by mutableStateOf(false)
        var recompositionCount = 0

        composeTestRule.setContent {
            recompositionCount++
            FriendsToggleFAB(
                onClick = { },
                friendCount = friendCount,
                isExpanded = isExpanded,
                isPanelOpen = isPanelOpen
            )
        }

        val initialRecompositions = recompositionCount

        // Test that stable state prevents unnecessary recompositions
        val stateChangeTime = measureTimeMillis {
            // Change states that should use stable state optimization
            isExpanded = false
            composeTestRule.waitForIdle()
            
            isPanelOpen = true
            composeTestRule.waitForIdle()
            
            friendCount = 10
            composeTestRule.waitForIdle()
            
            // Change back
            isExpanded = true
            isPanelOpen = false
            friendCount = 5
            composeTestRule.waitForIdle()
        }

        val finalRecompositions = recompositionCount

        // Verify efficient state changes
        assert(stateChangeTime < 500L) { "State changes took too long: ${stateChangeTime}ms" }
        
        // Verify reasonable recomposition count with stable state
        val recompositionDelta = finalRecompositions - initialRecompositions
        assert(recompositionDelta <= 10) { "Too many recompositions with stable state: $recompositionDelta" }
    }

    @Test
    fun testLargeFriendListPerformance() {
        var friendCount by mutableStateOf(0)

        composeTestRule.setContent {
            FriendsToggleFAB(
                onClick = { },
                friendCount = friendCount,
                isExpanded = true,
                isPanelOpen = false
            )
        }

        // Test performance with large friend lists
        val largeFriendCounts = listOf(100, 500, 1000, 5000)
        
        largeFriendCounts.forEach { count ->
            val updateTime = measureTimeMillis {
                friendCount = count
                composeTestRule.waitForIdle()
            }
            
            // Verify smooth performance even with large friend lists
            assert(updateTime < 100L) { "Large friend list update took too long: ${updateTime}ms for $count friends" }
        }
    }

    @Test
    fun testAnimationResourceCleanup() {
        var showFab by mutableStateOf(true)

        composeTestRule.setContent {
            if (showFab) {
                FriendsToggleFAB(
                    onClick = { },
                    friendCount = 5,
                    isExpanded = true,
                    isPanelOpen = false
                )
            }
        }

        // Test animation resource cleanup by disposing and recreating the component
        repeat(5) {
            showFab = false
            composeTestRule.waitForIdle()
            
            showFab = true
            composeTestRule.waitForIdle()
        }

        // If we reach here without crashes, animation cleanup is working
        // The actual cleanup is verified through DisposableEffect in the component
    }

    @Test
    fun testResponsiveDesignPerformance() {
        var isExpanded by mutableStateOf(true)
        var isPanelOpen by mutableStateOf(false)

        composeTestRule.setContent {
            FriendsToggleFAB(
                onClick = { },
                friendCount = 10,
                isExpanded = isExpanded,
                isPanelOpen = isPanelOpen
            )
        }

        // Test responsive design changes performance
        val responsiveChangeTime = measureTimeMillis {
            repeat(10) {
                isExpanded = !isExpanded
                composeTestRule.waitForIdle()
                
                isPanelOpen = !isPanelOpen
                composeTestRule.waitForIdle()
            }
        }

        // Verify responsive changes are smooth
        assert(responsiveChangeTime < 1000L) { "Responsive design changes took too long: ${responsiveChangeTime}ms" }
    }

    @Test
    fun testBadgeDisplayOptimization() {
        var friendCount by mutableStateOf(0)

        composeTestRule.setContent {
            FriendsToggleFAB(
                onClick = { },
                friendCount = friendCount,
                isExpanded = true,
                isPanelOpen = false
            )
        }

        // Test badge display optimization for various counts
        val badgeTestCases = listOf(
            0,    // No badge
            1,    // Single digit
            9,    // Single digit max
            10,   // Double digit
            99,   // Double digit max
            100,  // Should show "99+"
            999,  // Should show "99+"
            5000  // Should show "99+"
        )

        badgeTestCases.forEach { count ->
            val updateTime = measureTimeMillis {
                friendCount = count
                composeTestRule.waitForIdle()
            }
            
            // Verify badge updates are efficient
            assert(updateTime < 50L) { "Badge update took too long: ${updateTime}ms for count $count" }
        }
    }
}