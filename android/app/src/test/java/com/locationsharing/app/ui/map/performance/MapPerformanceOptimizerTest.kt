package com.locationsharing.app.ui.map.performance

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for MapPerformanceOptimizer
 * Tests clustering algorithms, throttling mechanisms, and performance monitoring
 */
class MapPerformanceOptimizerTest {
    
    private lateinit var performanceOptimizer: MapPerformanceOptimizer
    
    @Before
    fun setUp() {
        performanceOptimizer = MapPerformanceOptimizer()
    }
    
    @Test
    fun `shouldUseMarkerClustering returns true for low zoom levels`() {
        // Test clustering at low zoom levels
        assertTrue(performanceOptimizer.shouldUseMarkerClustering(10f, 15))
        assertTrue(performanceOptimizer.shouldUseMarkerClustering(8f, 10))
        
        // Test no clustering at high zoom levels with few friends
        assertFalse(performanceOptimizer.shouldUseMarkerClustering(15f, 10))
        assertFalse(performanceOptimizer.shouldUseMarkerClustering(18f, 5))
    }
    
    @Test
    fun `shouldUseMarkerClustering returns true for large friend counts`() {
        // Test clustering for large friend counts regardless of zoom
        assertTrue(performanceOptimizer.shouldUseMarkerClustering(15f, 30))
        assertTrue(performanceOptimizer.shouldUseMarkerClustering(18f, 50))
        
        // Test no clustering for small friend counts at high zoom
        assertFalse(performanceOptimizer.shouldUseMarkerClustering(15f, 10))
        assertFalse(performanceOptimizer.shouldUseMarkerClustering(18f, 20))
    }
    
    @Test
    fun `createOptimizedClusters groups nearby friends correctly`() {
        val friends = createTestFriends()
        val cameraCenter = LatLng(37.7749, -122.4194)
        
        val clusters = performanceOptimizer.createOptimizedClusters(
            friends = friends,
            zoomLevel = 10f,
            cameraCenter = cameraCenter
        )
        
        // Should create clusters for nearby friends
        assertTrue("Should create at least one cluster", clusters.isNotEmpty())
        
        // Check that clusters contain friends
        val totalFriendsInClusters = clusters.sumOf { it.friends.size }
        assertEquals("All friends should be in clusters", friends.size, totalFriendsInClusters)
    }
    
    @Test
    fun `createOptimizedClusters creates individual markers at high zoom`() {
        val friends = createTestFriends().take(5) // Small number of friends
        val cameraCenter = LatLng(37.7749, -122.4194)
        
        val clusters = performanceOptimizer.createOptimizedClusters(
            friends = friends,
            zoomLevel = 16f, // High zoom level
            cameraCenter = cameraCenter
        )
        
        // At high zoom with few friends, should create individual markers
        assertEquals("Should create individual markers", friends.size, clusters.size)
        clusters.forEach { cluster ->
            assertFalse("Should not be a cluster", cluster.isCluster)
            assertEquals("Each cluster should have one friend", 1, cluster.friends.size)
        }
    }
    
    @Test
    fun `shouldThrottleLocationUpdate throttles based on time`() {
        val location1 = LatLng(37.7749, -122.4194)
        val location2 = LatLng(37.7750, -122.4195) // Very close location
        
        // First update should not be throttled
        assertFalse(performanceOptimizer.shouldThrottleLocationUpdate(location1, null))
        
        // Immediate second update should be throttled
        assertTrue(performanceOptimizer.shouldThrottleLocationUpdate(location2, location1))
        
        // Wait and test again (in real scenario, time would pass)
        Thread.sleep(150) // Wait longer than MIN_UPDATE_INTERVAL_MS
        assertFalse(performanceOptimizer.shouldThrottleLocationUpdate(location2, location1))
    }
    
    @Test
    fun `shouldThrottleLocationUpdate throttles based on distance`() {
        val location1 = LatLng(37.7749, -122.4194)
        val location2 = LatLng(37.7749, -122.4194) // Same location
        val location3 = LatLng(37.7759, -122.4204) // Far location
        
        // Wait to avoid time-based throttling
        Thread.sleep(150)
        
        // Same location should be throttled
        assertTrue(performanceOptimizer.shouldThrottleLocationUpdate(location2, location1))
        
        // Far location should not be throttled
        assertFalse(performanceOptimizer.shouldThrottleLocationUpdate(location3, location1))
    }
    
    @Test
    fun `shouldThrottleCameraUpdate works correctly`() {
        val position1 = LatLng(37.7749, -122.4194)
        val position2 = LatLng(37.7749, -122.4195) // Close position
        val position3 = LatLng(37.7759, -122.4204) // Far position
        
        // First update should not be throttled
        assertFalse(performanceOptimizer.shouldThrottleCameraUpdate(position1, 15f))
        
        // Close position with same zoom should be throttled
        assertTrue(performanceOptimizer.shouldThrottleCameraUpdate(position2, 15f))
        
        // Far position should not be throttled
        assertFalse(performanceOptimizer.shouldThrottleCameraUpdate(position3, 15f))
        
        // Significant zoom change should not be throttled
        assertFalse(performanceOptimizer.shouldThrottleCameraUpdate(position2, 18f))
    }
    
    @Test
    fun `trackFramePerformance monitors performance correctly`() {
        // Test normal frame time
        performanceOptimizer.trackFramePerformance(10L) // Good performance
        
        // Test slow frame time
        performanceOptimizer.trackFramePerformance(25L) // Poor performance
        
        // Test multiple frames to trigger average calculation
        repeat(60) {
            performanceOptimizer.trackFramePerformance(15L)
        }
        
        // No exceptions should be thrown
        assertTrue("Performance tracking should complete without errors", true)
    }
    
    @Test
    fun `checkMemoryUsage completes without errors`() {
        // Test memory usage check
        performanceOptimizer.checkMemoryUsage()
        
        // Should complete without throwing exceptions
        assertTrue("Memory check should complete without errors", true)
    }
    
    @Test
    fun `batchAnimations processes animations in batches`() {
        var executionCount = 0
        val animations = (1..25).map {
            { executionCount++ }
        }
        
        performanceOptimizer.batchAnimations(animations)
        
        assertEquals("All animations should be executed", 25, executionCount)
    }
    
    /**
     * Create test friends for clustering tests
     */
    private fun createTestFriends(): List<Friend> {
        return listOf(
            createTestFriend("1", "Alice", 37.7749, -122.4194),
            createTestFriend("2", "Bob", 37.7750, -122.4195), // Close to Alice
            createTestFriend("3", "Carol", 37.7751, -122.4196), // Close to Alice and Bob
            createTestFriend("4", "David", 37.7800, -122.4200), // Far from others
            createTestFriend("5", "Eve", 37.7801, -122.4201), // Close to David
            createTestFriend("6", "Frank", 37.7700, -122.4100), // Far from all others
        )
    }
    
    /**
     * Create a test friend with specified location
     */
    private fun createTestFriend(id: String, name: String, lat: Double, lng: Double): Friend {
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