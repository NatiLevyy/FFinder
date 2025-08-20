package com.locationsharing.app.ui.map

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests for MapScreenState data class
 * Tests state properties, helper methods, and computed values
 */
class MapScreenStateTest {
    
    @Test
    fun `default state should have correct initial values`() {
        val state = MapScreenState()
        
        assertNull(state.currentLocation)
        assertFalse(state.isLocationLoading)
        assertNull(state.locationError)
        assertFalse(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
        assertTrue(state.friends.isEmpty())
        assertEquals(0, state.nearbyFriendsCount)
        assertNull(state.selectedFriendId)
        assertFalse(state.isFriendsLoading)
        assertNull(state.friendsError)
        assertFalse(state.isLocationSharingActive)
        assertNull(state.locationSharingError)
        assertFalse(state.isNearbyDrawerOpen)
        assertFalse(state.isStatusSheetVisible)
        assertFalse(state.isDebugMode)
        assertEquals(MapScreenConstants.Map.DEFAULT_ZOOM, state.mapZoom)
        assertEquals(LatLng(37.7749, -122.4194), state.mapCenter)
        assertFalse(state.isMapLoading)
        assertEquals(100, state.batteryLevel)
        assertFalse(state.isHighAccuracyMode)
        assertEquals(5000L, state.locationUpdateInterval)
        assertNull(state.generalError)
        assertFalse(state.isRetrying)
        assertEquals(0, state.retryCount)
    }
    
    @Test
    fun `isLoading should return true when any loading state is active`() {
        // Test location loading
        val state1 = MapScreenState(isLocationLoading = true)
        assertTrue(state1.isLoading)
        
        // Test friends loading
        val state2 = MapScreenState(isFriendsLoading = true)
        assertTrue(state2.isLoading)
        
        // Test map loading
        val state3 = MapScreenState(isMapLoading = true)
        assertTrue(state3.isLoading)
        
        // Test no loading
        val state4 = MapScreenState()
        assertFalse(state4.isLoading)
    }
    
    @Test
    fun `hasError should return true when any error is present`() {
        // Test location error
        val state1 = MapScreenState(locationError = "Location error")
        assertTrue(state1.hasError)
        
        // Test friends error
        val state2 = MapScreenState(friendsError = "Friends error")
        assertTrue(state2.hasError)
        
        // Test location sharing error
        val state3 = MapScreenState(locationSharingError = "Sharing error")
        assertTrue(state3.hasError)
        
        // Test general error
        val state4 = MapScreenState(generalError = "General error")
        assertTrue(state4.hasError)
        
        // Test no error
        val state5 = MapScreenState()
        assertFalse(state5.hasError)
    }
    
    @Test
    fun `primaryError should return first non-null error`() {
        // Test location error priority
        val state1 = MapScreenState(
            locationError = "Location error",
            friendsError = "Friends error",
            generalError = "General error"
        )
        assertEquals("Location error", state1.primaryError)
        
        // Test friends error when location error is null
        val state2 = MapScreenState(
            friendsError = "Friends error",
            generalError = "General error"
        )
        assertEquals("Friends error", state2.primaryError)
        
        // Test general error when others are null
        val state3 = MapScreenState(generalError = "General error")
        assertEquals("General error", state3.primaryError)
        
        // Test no error
        val state4 = MapScreenState()
        assertNull(state4.primaryError)
    }
    
    @Test
    fun `isLocationReady should return true when location is available and ready`() {
        // Test ready state
        val state1 = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(40.7128, -74.0060),
            isLocationLoading = false
        )
        assertTrue(state1.isLocationReady)
        
        // Test not ready - no permission
        val state2 = MapScreenState(
            hasLocationPermission = false,
            currentLocation = LatLng(40.7128, -74.0060),
            isLocationLoading = false
        )
        assertFalse(state2.isLocationReady)
        
        // Test not ready - no location
        val state3 = MapScreenState(
            hasLocationPermission = true,
            currentLocation = null,
            isLocationLoading = false
        )
        assertFalse(state3.isLocationReady)
        
        // Test not ready - loading
        val state4 = MapScreenState(
            hasLocationPermission = true,
            currentLocation = LatLng(40.7128, -74.0060),
            isLocationLoading = true
        )
        assertFalse(state4.isLocationReady)
    }
    
    @Test
    fun `isFriendsReady should return true when friends data is ready`() {
        // Test ready state
        val state1 = MapScreenState(
            isFriendsLoading = false,
            friendsError = null
        )
        assertTrue(state1.isFriendsReady)
        
        // Test not ready - loading
        val state2 = MapScreenState(
            isFriendsLoading = true,
            friendsError = null
        )
        assertFalse(state2.isFriendsReady)
        
        // Test not ready - error
        val state3 = MapScreenState(
            isFriendsLoading = false,
            friendsError = "Error loading friends"
        )
        assertFalse(state3.isFriendsReady)
    }
    
    @Test
    fun `getNearbyFriends should filter friends within radius`() {
        val userLocation = LatLng(40.7128, -74.0060) // New York
        
        // Create test friends at different distances
        val nearbyFriend = createTestFriend("nearby", 40.7130, -74.0062) // ~20 meters
        val farFriend = createTestFriend("far", 41.0000, -75.0000) // ~100+ km
        val noLocationFriend = createTestFriend("no_location", null, null)
        
        val state = MapScreenState(
            currentLocation = userLocation,
            friends = listOf(nearbyFriend, farFriend, noLocationFriend)
        )
        
        // Test default radius (10km)
        val nearbyFriends = state.getNearbyFriends()
        assertEquals(1, nearbyFriends.size)
        assertEquals("nearby", nearbyFriends[0].id)
        
        // Test larger radius (200km)
        val allNearbyFriends = state.getNearbyFriends(200.0)
        assertEquals(2, allNearbyFriends.size)
        
        // Test smaller radius (0.01km = 10m)
        val veryNearbyFriends = state.getNearbyFriends(0.01)
        assertEquals(0, veryNearbyFriends.size)
    }
    
    @Test
    fun `getNearbyFriends should return empty list when user location is null`() {
        val friend = createTestFriend("friend", 40.7128, -74.0060)
        val state = MapScreenState(
            currentLocation = null,
            friends = listOf(friend)
        )
        
        val nearbyFriends = state.getNearbyFriends()
        assertTrue(nearbyFriends.isEmpty())
    }
    
    @Test
    fun `getSelectedFriend should return correct friend`() {
        val friend1 = createTestFriend("friend1", 40.7128, -74.0060)
        val friend2 = createTestFriend("friend2", 40.7130, -74.0062)
        
        val state = MapScreenState(
            friends = listOf(friend1, friend2),
            selectedFriendId = "friend2"
        )
        
        val selectedFriend = state.getSelectedFriend()
        assertEquals("friend2", selectedFriend?.id)
        assertEquals(friend2, selectedFriend)
    }
    
    @Test
    fun `getSelectedFriend should return null when no friend selected`() {
        val friend = createTestFriend("friend", 40.7128, -74.0060)
        val state = MapScreenState(
            friends = listOf(friend),
            selectedFriendId = null
        )
        
        assertNull(state.getSelectedFriend())
    }
    
    @Test
    fun `getSelectedFriend should return null when selected friend not found`() {
        val friend = createTestFriend("friend", 40.7128, -74.0060)
        val state = MapScreenState(
            friends = listOf(friend),
            selectedFriendId = "nonexistent"
        )
        
        assertNull(state.getSelectedFriend())
    }
    
    @Test
    fun `canRetry should return true when conditions are met`() {
        // Test can retry
        val state1 = MapScreenState(
            locationError = "Some error",
            isRetrying = false,
            retryCount = 1
        )
        assertTrue(state1.canRetry)
        
        // Test cannot retry - no error
        val state2 = MapScreenState(
            isRetrying = false,
            retryCount = 1
        )
        assertFalse(state2.canRetry)
        
        // Test cannot retry - already retrying
        val state3 = MapScreenState(
            locationError = "Some error",
            isRetrying = true,
            retryCount = 1
        )
        assertFalse(state3.canRetry)
        
        // Test cannot retry - max attempts reached
        val state4 = MapScreenState(
            locationError = "Some error",
            isRetrying = false,
            retryCount = MapScreenConstants.States.MAX_RETRY_ATTEMPTS
        )
        assertFalse(state4.canRetry)
    }
    
    @Test
    fun `locationSharingStatusText should return correct status`() {
        // Test active sharing
        val state1 = MapScreenState(isLocationSharingActive = true)
        assertEquals("Location Sharing Active", state1.locationSharingStatusText)
        
        // Test inactive sharing
        val state2 = MapScreenState(isLocationSharingActive = false)
        assertEquals("Location Sharing Off", state2.locationSharingStatusText)
    }
    
    @Test
    fun `coordinatesText should return formatted coordinates`() {
        // Test with location
        val location = LatLng(40.712800, -74.006000)
        val state1 = MapScreenState(currentLocation = location)
        assertEquals("Lat: 40.712800, Lng: -74.006000", state1.coordinatesText)
        
        // Test without location
        val state2 = MapScreenState(currentLocation = null)
        assertEquals("Location unavailable", state2.coordinatesText)
    }
    
    // Helper method to create test friends
    private fun createTestFriend(
        id: String,
        latitude: Double?,
        longitude: Double?
    ): Friend {
        val location = if (latitude != null && longitude != null) {
            FriendLocation(
                latitude = latitude,
                longitude = longitude,
                accuracy = 10f,
                isMoving = false,
                timestamp = Date()
            )
        } else {
            null
        }
        
        return Friend(
            id = id,
            userId = "user_$id",
            name = "Friend $id",
            email = "friend$id@example.com",
            location = location,
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
}