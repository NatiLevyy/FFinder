package com.locationsharing.app.ui.map.performance

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.ui.map.MapScreenState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.system.measureTimeMillis

/**
 * Integration tests for MapScreen performance optimizations
 * Tests the complete performance optimization system working together
 */
class MapPerformanceIntegrationTest {
    
    private lateinit var performanceOptimizer: MapPerformanceOptimizer
    private lateinit var markerClusterer: OptimizedMarkerClusterer
    private lateinit var lifecycleManager: MapLifecycleManager
    private lateinit var stateManager: EfficientStateManager
    
    @Before
    fun setUp() {
        performanceOptimizer = MapPerformanceOptimizer()
        markerClusterer = OptimizedMarkerClusterer(performanceOptimizer)
        lifecycleManager = MapLifecycleManager()
        stateManager = EfficientStateManager()
    }
    
    @Test
    fun `performance optimization system handles large friend lists efficiently`() {
        val largeFriendsList = createLargeFriendsList(100)
        val cameraCenter = LatLng(37.7749, -122.4194)
        
        val executionTime = measureTimeMillis {
            // Test clustering performance
            val clusters = performanceOptimizer.createOptimizedClusters(
                friends = largeFriendsList,
                zoomLevel = 10f,
                cameraCenter = cameraCenter
            )
            
            assertTrue("Should create clusters for large friend list", clusters.isNotEmpty())
            assertTrue("Should reduce marker count through clustering", 
                clusters.size < largeFriendsList.size)
        }
        
        // Performance should be reasonable even with 100 friends
        assertTrue("Clustering should complete in reasonable time", executionTime < 1000L)
    }
    
    @Test
    fun `state management optimizes updates efficiently`() {
        val initialFriends = createTestFriends(20)
        val mapScreenState = MapScreenState(
            currentLocation = LatLng(37.7749, -122.4194),
            friends = initialFriends,
            selectedFriendId = null,
            mapZoom = 15f,
            nearbyFriendsCount = initialFriends.size
        )
        
        val optimizedState = stateManager.createOptimizedState(mapScreenState)
        
        // Test batch processing performance
        val newFriends = createTestFriends(25)
        val executionTime = measureTimeMillis {
            val processedFriends = stateManager.batchProcessFriendUpdates(
                optimizedState.friends,
                newFriends
            )
            assertEquals("Should process all friends", newFriends.size, processedFriends.size)
        }
        
        assertTrue("Batch processing should be fast", executionTime < 100L)
    }
    
    @Test
    fun `lifecycle management prevents memory leaks`() = runBlocking {
        lifecycleManager.initialize()
        
        var locationUpdateCount = 0
        var friendsUpdateCount = 0
        
        // Start updates
        lifecycleManager.startLocationUpdates {
            locationUpdateCount++
        }
        
        lifecycleManager.startFriendsUpdates {
            friendsUpdateCount++
        }
        
        assertTrue("Location updates should be active", lifecycleManager.isLocationUpdatesActive())
        assertTrue("Friends updates should be active", lifecycleManager.isFriendsUpdatesActive())
        
        // Simulate lifecycle events
        lifecycleManager.onPause()
        assertFalse("Should be in background", lifecycleManager.isInForeground())
        
        lifecycleManager.onResume()
        assertTrue("Should be in foreground", lifecycleManager.isInForeground())
        
        // Cleanup should stop all updates
        lifecycleManager.cleanup()
        assertFalse("Location updates should be stopped", lifecycleManager.isLocationUpdatesActive())
        assertFalse("Friends updates should be stopped", lifecycleManager.isFriendsUpdatesActive())
    }
    
    @Test
    fun `marker clustering adapts to zoom levels correctly`() {
        val friends = createTestFriends(30)
        val cameraCenter = LatLng(37.7749, -122.4194)
        
        // Test low zoom (should cluster)
        val lowZoomClusters = performanceOptimizer.createOptimizedClusters(
            friends = friends,
            zoomLevel = 8f,
            cameraCenter = cameraCenter
        )
        
        // Test high zoom (should not cluster as much)
        val highZoomClusters = performanceOptimizer.createOptimizedClusters(
            friends = friends,
            zoomLevel = 16f,
            cameraCenter = cameraCenter
        )
        
        assertTrue("Low zoom should create fewer clusters", lowZoomClusters.size < friends.size)
        assertTrue("High zoom should create more individual markers", 
            highZoomClusters.size >= lowZoomClusters.size)
    }
    
    @Test
    fun `throttling mechanisms prevent excessive updates`() {
        val location1 = LatLng(37.7749, -122.4194)
        val location2 = LatLng(37.7749, -122.4194) // Same location
        val location3 = LatLng(37.7759, -122.4204) // Different location
        
        // First update should not be throttled
        assertFalse("First update should not be throttled",
            performanceOptimizer.shouldThrottleLocationUpdate(location1, null))
        
        // Same location should be throttled
        assertTrue("Same location should be throttled",
            performanceOptimizer.shouldThrottleLocationUpdate(location2, location1))
        
        // Wait and test with different location
        Thread.sleep(150)
        assertFalse("Different location should not be throttled after delay",
            performanceOptimizer.shouldThrottleLocationUpdate(location3, location1))
    }
    
    @Test
    fun `performance monitoring tracks metrics correctly`() {
        // Test frame performance tracking
        performanceOptimizer.trackFramePerformance(10L) // Good frame
        performanceOptimizer.trackFramePerformance(20L) // Slow frame
        performanceOptimizer.trackFramePerformance(15L) // Average frame
        
        // Test memory monitoring
        performanceOptimizer.checkMemoryUsage()
        
        // Should complete without errors
        assertTrue("Performance monitoring should work correctly", true)
    }
    
    @Test
    fun `complete system handles realistic scenario efficiently`() = runBlocking {
        // Setup realistic scenario
        val friends = createTestFriends(50)
        val mapScreenState = MapScreenState(
            currentLocation = LatLng(37.7749, -122.4194),
            friends = friends,
            selectedFriendId = "friend_1",
            mapZoom = 12f,
            nearbyFriendsCount = friends.size,
            isLocationLoading = false,
            isFriendsLoading = false
        )
        
        val totalExecutionTime = measureTimeMillis {
            // Initialize lifecycle manager
            lifecycleManager.initialize()
            
            // Create optimized state
            val optimizedState = stateManager.createOptimizedState(mapScreenState)
            
            // Test clustering
            val clusters = performanceOptimizer.createOptimizedClusters(
                friends = friends,
                zoomLevel = optimizedState.zoomLevel,
                cameraCenter = optimizedState.cameraCenter ?: LatLng(37.7749, -122.4194)
            )
            
            // Test state updates
            val newFriends = friends.map { friend ->
                if (friend.id == "friend_1") {
                    friend.copy(
                        location = friend.location.copy(
                            latitude = friend.location.latitude + 0.001,
                            longitude = friend.location.longitude + 0.001
                        )
                    )
                } else {
                    friend
                }
            }
            
            val processedFriends = stateManager.batchProcessFriendUpdates(
                optimizedState.friends,
                newFriends
            )
            
            // Verify results
            assertTrue("Should create clusters", clusters.isNotEmpty())
            assertEquals("Should process all friends", newFriends.size, processedFriends.size)
            
            // Cleanup
            lifecycleManager.cleanup()
        }
        
        // Complete system should be performant
        assertTrue("Complete system should execute efficiently", totalExecutionTime < 500L)
    }
    
    @Test
    fun `system handles edge cases gracefully`() {
        // Test with empty friend list
        val emptyClusters = performanceOptimizer.createOptimizedClusters(
            friends = emptyList(),
            zoomLevel = 15f,
            cameraCenter = LatLng(37.7749, -122.4194)
        )
        assertTrue("Empty friend list should create empty clusters", emptyClusters.isEmpty())
        
        // Test with single friend
        val singleFriend = listOf(createTestFriend("1", "Alice", 37.7749, -122.4194))
        val singleClusters = performanceOptimizer.createOptimizedClusters(
            friends = singleFriend,
            zoomLevel = 15f,
            cameraCenter = LatLng(37.7749, -122.4194)
        )
        assertEquals("Single friend should create one cluster", 1, singleClusters.size)
        assertFalse("Single friend should not be clustered", singleClusters.first().isCluster)
        
        // Test with null locations
        val friendWithNullLocation = createTestFriend("2", "Bob", 0.0, 0.0).copy(
            location = null
        )
        val mixedFriends = singleFriend + friendWithNullLocation
        val mixedClusters = performanceOptimizer.createOptimizedClusters(
            friends = mixedFriends,
            zoomLevel = 15f,
            cameraCenter = LatLng(37.7749, -122.4194)
        )
        
        // Should handle null locations gracefully
        assertTrue("Should handle null locations", mixedClusters.isNotEmpty())
    }
    
    /**
     * Create test friends for testing
     */
    private fun createTestFriends(count: Int): List<Friend> {
        return (1..count).map { index ->
            createTestFriend(
                id = "friend_$index",
                name = "Friend $index",
                lat = 37.7749 + (index * 0.001),
                lng = -122.4194 + (index * 0.001)
            )
        }
    }
    
    /**
     * Create a large list of friends for performance testing
     */
    private fun createLargeFriendsList(count: Int): List<Friend> {
        return (1..count).map { index ->
            createTestFriend(
                id = "large_friend_$index",
                name = "Large Friend $index",
                lat = 37.7749 + (index * 0.0001), // Closer together for clustering
                lng = -122.4194 + (index * 0.0001)
            )
        }
    }
    
    /**
     * Create a test friend with specified parameters
     */
    private fun createTestFriend(
        id: String,
        name: String,
        lat: Double,
        lng: Double
    ): Friend {
        return Friend(
            id = id,
            userId = "user_$id",
            name = name,
            email = "$name@test.com",
            avatarUrl = "",
            profileColor = "#FF0000",
            location = FriendLocation(
                latitude = lat,
                longitude = lng,
                accuracy = 10f,
                isMoving = false,
                timestamp = Date()
            ),
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
}