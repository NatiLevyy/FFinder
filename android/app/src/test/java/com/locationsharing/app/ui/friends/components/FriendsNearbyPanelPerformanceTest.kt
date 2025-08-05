package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance tests for FriendsNearbyPanel with large datasets.
 * Tests smooth scrolling performance with 500+ friends.
 * 
 * Requirements tested:
 * - 6.6: Optimize LazyColumn performance for large friend lists (500+ items)
 * - 8.6: Create performance tests for smooth scrolling with large datasets
 */
@RunWith(AndroidJUnit4::class)
class FriendsNearbyPanelPerformanceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test smooth scrolling performance with 100 friends
     */
    @Test
    fun testScrollingPerformanceWith100Friends() {
        val friends = generateLargeFriendsList(100)
        var scrollTime = 0L
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends
                    ),
                    onEvent = {}
                )
            }
        }
        
        // Measure scrolling performance
        scrollTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                .performScrollToIndex(50)
            
            composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                .performScrollToIndex(90)
            
            composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                .performScrollToIndex(10)
        }
        
        // Assert scrolling performance is acceptable (< 500ms for 100 items)
        assert(scrollTime < 500) {
            "Scrolling 100 friends took ${scrollTime}ms, expected < 500ms"
        }
    }

    /**
     * Test smooth scrolling performance with 500 friends
     */
    @Test
    fun testScrollingPerformanceWith500Friends() {
        val friends = generateLargeFriendsList(500)
        var scrollTime = 0L
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends
                    ),
                    onEvent = {}
                )
            }
        }
        
        // Measure scrolling performance
        scrollTime = measureTimeMillis {
            composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                .performScrollToIndex(250)
            
            composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                .performScrollToIndex(450)
            
            composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                .performScrollToIndex(50)
        }
        
        // Assert scrolling performance is acceptable (< 1000ms for 500 items)
        assert(scrollTime < 1000) {
            "Scrolling 500 friends took ${scrollTime}ms, expected < 1000ms"
        }
    }

    /**
     * Test search performance with large friend list
     */
    @Test
    fun testSearchPerformanceWithLargeFriendsList() {
        val friends = generateLargeFriendsList(1000)
        var searchTime = 0L
        var eventReceived = false
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends,
                        searchQuery = "Alice"
                    ),
                    onEvent = { event ->
                        if (event is NearbyPanelEvent.SearchQuery) {
                            eventReceived = true
                        }
                    }
                )
            }
        }
        
        // Measure search filtering performance
        searchTime = measureTimeMillis {
            // The filtering happens in the UI state computation
            composeTestRule.waitForIdle()
        }
        
        // Assert search performance is acceptable (< 100ms for 1000 items)
        assert(searchTime < 100) {
            "Search filtering 1000 friends took ${searchTime}ms, expected < 100ms"
        }
    }

    /**
     * Test memory usage with very large friend list
     */
    @Test
    fun testMemoryUsageWithVeryLargeFriendsList() {
        val friends = generateLargeFriendsList(2000)
        
        // Get initial memory usage
        val runtime = Runtime.getRuntime()
        runtime.gc() // Force garbage collection
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends
                    ),
                    onEvent = {}
                )
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Get memory usage after rendering
        runtime.gc() // Force garbage collection
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Assert memory increase is reasonable (< 50MB for 2000 items)
        val maxMemoryIncrease = 50 * 1024 * 1024 // 50MB in bytes
        assert(memoryIncrease < maxMemoryIncrease) {
            "Memory increase was ${memoryIncrease / (1024 * 1024)}MB, expected < 50MB"
        }
    }

    /**
     * Test LazyColumn item recycling efficiency
     */
    @Test
    fun testLazyColumnItemRecycling() {
        val friends = generateLargeFriendsList(200)
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends
                    ),
                    onEvent = {}
                )
            }
        }
        
        // Test rapid scrolling to trigger item recycling
        val scrollTime = measureTimeMillis {
            repeat(10) { index ->
                composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
                    .performScrollToIndex(index * 20)
                composeTestRule.waitForIdle()
            }
        }
        
        // Assert recycling performance is good (< 2000ms for 10 scroll operations)
        assert(scrollTime < 2000) {
            "Item recycling test took ${scrollTime}ms, expected < 2000ms"
        }
    }

    /**
     * Test configuration change state preservation
     */
    @Test
    fun testConfigurationChangeStatePreservation() {
        val friends = generateLargeFriendsList(100)
        val searchQuery = "Test Query"
        var preserveStateEventReceived = false
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends,
                        searchQuery = searchQuery,
                        scrollPosition = 25
                    ),
                    onEvent = { event ->
                        if (event is NearbyPanelEvent.PreserveState) {
                            preserveStateEventReceived = true
                        }
                    }
                )
            }
        }
        
        // Simulate configuration change by recreating the composable
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = friends,
                        searchQuery = searchQuery,
                        scrollPosition = 25
                    ),
                    onEvent = { event ->
                        if (event is NearbyPanelEvent.PreserveState) {
                            preserveStateEventReceived = true
                        }
                    }
                )
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Verify the UI still works correctly after configuration change
        composeTestRule.onNodeWithContentDescription("Friends list with ${friends.size} friends")
            .assertExists()
    }

    /**
     * Generate a large list of test friends for performance testing
     */
    private fun generateLargeFriendsList(count: Int): List<NearbyFriend> {
        return (1..count).map { index ->
            NearbyFriend(
                id = "friend_$index",
                displayName = when {
                    index % 10 == 0 -> "Alice Friend $index"
                    index % 7 == 0 -> "Bob Friend $index"
                    index % 5 == 0 -> "Charlie Friend $index"
                    else -> "Friend $index"
                },
                avatarUrl = null,
                distance = (index * 10.0) + (index % 100), // Varied distances
                isOnline = index % 3 == 0, // Mix of online/offline
                lastUpdated = System.currentTimeMillis() - (index * 1000),
                latLng = LatLng(
                    37.7749 + (index * 0.001), // Varied locations around SF
                    -122.4194 + (index * 0.001)
                )
            )
        }
    }
}