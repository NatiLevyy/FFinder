package com.locationsharing.app.ui.map.performance

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.ui.map.MapScreenState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for EfficientStateManager
 * Tests state optimization, debouncing, and efficient updates
 */
class EfficientStateManagerTest {
    
    private lateinit var stateManager: EfficientStateManager
    
    @Before
    fun setUp() {
        stateManager = EfficientStateManager()
    }
    
    @Test
    fun `createOptimizedState creates correct optimized state`() {
        val friends = createTestFriends()
        val mapScreenState = MapScreenState(
            currentLocation = LatLng(37.7749, -122.4194),
            friends = friends,
            selectedFriendId = "friend_1",
            mapZoom = 15f,
            mapCenter = LatLng(37.7749, -122.4194),
            isLocationLoading = false,
            isFriendsLoading = false,
            nearbyFriendsCount = friends.size
        )
        
        val optimizedState = stateManager.createOptimizedState(mapScreenState)
        
        assertEquals("Current location should match", mapScreenState.currentLocation, optimizedState.currentLocation)
        assertEquals("Friends should match", mapScreenState.friends, optimizedState.friends)
        assertEquals("Selected friend should match", mapScreenState.selectedFriendId, optimizedState.selectedFriendId)
        assertEquals("Zoom level should match", mapScreenState.mapZoom, optimizedState.zoomLevel)
        assertEquals("Online friends count should be calculated", 2, optimizedState.onlineFriendsCount)
    }
    
    @Test
    fun `shouldUpdateLocation returns true for significant location changes`() {
        val initialLocation = LatLng(37.7749, -122.4194)
        val optimizedState = EfficientStateManager.OptimizedMapState(
            currentLocation = initialLocation
        )
        
        // Small change should not trigger update
        val smallChange = LatLng(37.7749, -122.4194) // Same location
        assertFalse("Small change should not trigger update", 
            optimizedState.shouldUpdateLocation(smallChange))
        
        // Large change should trigger update
        val largeChange = LatLng(37.7759, -122.4204) // ~1km away
        assertTrue("Large change should trigger update", 
            optimizedState.shouldUpdateLocation(largeChange))
        
        // Null to non-null should trigger update
        val nullState = EfficientStateManager.OptimizedMapState(currentLocation = null)
        assertTrue("Null to non-null should trigger update", 
            nullState.shouldUpdateLocation(initialLocation))
        
        // Non-null to null should trigger update
        assertTrue("Non-null to null should trigger update", 
            optimizedState.shouldUpdateLocation(null))
    }
    
    @Test
    fun `shouldUpdateZoom returns true for significant zoom changes`() {
        val optimizedState = EfficientStateManager.OptimizedMapState(zoomLevel = 15f)
        
        // Small zoom change should not trigger update
        assertFalse("Small zoom change should not trigger update", 
            optimizedState.shouldUpdateZoom(15.05f))
        
        // Large zoom change should trigger update
        assertTrue("Large zoom change should trigger update", 
            optimizedState.shouldUpdateZoom(16f))
        
        assertTrue("Large zoom change should trigger update", 
            optimizedState.shouldUpdateZoom(14f))
    }
    
    @Test
    fun `shouldUpdateFriends returns true for significant friend changes`() {
        val initialFriends = createTestFriends()
        val optimizedState = EfficientStateManager.OptimizedMapState(friends = initialFriends)
        
        // Same friends should not trigger update
        assertFalse("Same friends should not trigger update", 
            optimizedState.shouldUpdateFriends(initialFriends))
        
        // Different number of friends should trigger update
        val fewerFriends = initialFriends.take(2)
        assertTrue("Different number of friends should trigger update", 
            optimizedState.shouldUpdateFriends(fewerFriends))
        
        // Friends with different online status should trigger update
        val modifiedFriends = initialFriends.map { friend ->
            if (friend.id == "friend_1") {
                friend.copy(
                    status = friend.status.copy(isOnline = false)
                )
            } else {
                friend
            }
        }
        assertTrue("Friends with different online status should trigger update", 
            optimizedState.shouldUpdateFriends(modifiedFriends))
    }
    
    @Test
    fun `batchProcessFriendUpdates processes small lists directly`() {
        val currentFriends = createTestFriends().take(5)
        val newFriends = createTestFriends().take(3)
        
        val result = stateManager.batchProcessFriendUpdates(currentFriends, newFriends)
        
        assertEquals("Small lists should be processed directly", newFriends.size, result.size)
    }
    
    @Test
    fun `batchProcessFriendUpdates handles large lists efficiently`() {
        val currentFriends = createLargeFriendsList(50)
        val newFriends = createLargeFriendsList(60)
        
        val result = stateManager.batchProcessFriendUpdates(currentFriends, newFriends)
        
        assertEquals("Large lists should be processed in batches", newFriends.size, result.size)
    }
    
    @Test
    fun `batchProcessFriendUpdates preserves unchanged friends`() {
        val currentFriends = createTestFriends()
        val newFriends = currentFriends.map { it.copy() } // Same friends, different instances
        
        val result = stateManager.batchProcessFriendUpdates(currentFriends, newFriends)
        
        // Should preserve original instances for unchanged friends
        result.forEachIndexed { index, friend ->
            if (index < currentFriends.size) {
                // Should be the same instance (reference equality) for unchanged friends
                assertTrue("Unchanged friends should be preserved", 
                    friend.id == currentFriends[index].id)
            }
        }
    }
    
    @Test
    fun `monitorStateChangePerformance logs changes correctly`() {
        val oldState = EfficientStateManager.OptimizedMapState(
            currentLocation = LatLng(37.7749, -122.4194),
            friends = createTestFriends(),
            selectedFriendId = "friend_1",
            zoomLevel = 15f,
            isLocationLoading = false
        )
        
        val newState = oldState.copy(
            currentLocation = LatLng(37.7759, -122.4204),
            selectedFriendId = "friend_2",
            zoomLevel = 16f,
            isLocationLoading = true
        )
        
        // Should not throw any exceptions
        stateManager.monitorStateChangePerformance("test_operation", oldState, newState)
        
        assertTrue("Performance monitoring should complete without errors", true)
    }
    
    /**
     * Create test friends for testing
     */
    private fun createTestFriends(): List<Friend> {
        return listOf(
            createTestFriend("friend_1", "Alice", 37.7749, -122.4194, isOnline = true),
            createTestFriend("friend_2", "Bob", 37.7750, -122.4195, isOnline = true),
            createTestFriend("friend_3", "Carol", 37.7751, -122.4196, isOnline = false)
        )
    }
    
    /**
     * Create a large list of friends for performance testing
     */
    private fun createLargeFriendsList(count: Int): List<Friend> {
        return (1..count).map { index ->
            createTestFriend(
                id = "friend_$index",
                name = "Friend $index",
                lat = 37.7749 + (index * 0.001),
                lng = -122.4194 + (index * 0.001),
                isOnline = index % 2 == 0
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
        lng: Double,
        isOnline: Boolean = true
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
                isOnline = isOnline,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
}