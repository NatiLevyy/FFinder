package com.locationsharing.app.domain.usecase

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.friends.FriendLocationUpdate
import com.locationsharing.app.data.friends.LocationUpdateType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test for GetNearbyFriendsUseCase
 * Tests the complete flow with realistic data
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetNearbyFriendsUseCaseIntegrationTest {
    
    private lateinit var friendsRepository: FriendsRepository
    private lateinit var locationService: EnhancedLocationService
    private lateinit var useCase: GetNearbyFriendsUseCase
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        friendsRepository = mockk()
        locationService = mockk()
        
        useCase = GetNearbyFriendsUseCase(
            friendsRepository = friendsRepository,
            locationService = locationService,
            dispatcher = testDispatcher
        )
        
        // Mock Location.distanceBetween static method
        mockkStatic(Location::class)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(Location::class)
    }
    
    @Test
    fun `integration test - should return sorted nearby friends with correct distances`() = runTest {
        // Given - User at specific location
        val userLocation = Location("test").apply {
            latitude = 37.7749
            longitude = -122.4194
            time = System.currentTimeMillis()
        }
        
        val userLocationUpdate = FriendLocationUpdate(
            friendId = "current_user",
            previousLocation = null,
            newLocation = LatLng(userLocation.latitude, userLocation.longitude),
            timestamp = userLocation.time,
            isOnline = true,
            updateType = LocationUpdateType.POSITION_CHANGE
        )
        
        // Friends at different distances from San Francisco
        val friends = listOf(
            createFriend("1", "Alice", 37.7849, -122.4194), // ~1.1 km north
            createFriend("2", "Bob", 37.7649, -122.4194),   // ~1.1 km south  
            createFriend("3", "Charlie", 37.7749, -122.4094) // ~1.1 km east
        )
        
        // Mock distance calculations (approximate)
        every { 
            Location.distanceBetween(
                eq(37.7749), eq(-122.4194), 
                eq(37.7849), eq(-122.4194), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1100f // ~1.1 km
        }
        
        every { 
            Location.distanceBetween(
                eq(37.7749), eq(-122.4194), 
                eq(37.7649), eq(-122.4194), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1100f // ~1.1 km
        }
        
        every { 
            Location.distanceBetween(
                eq(37.7749), eq(-122.4194), 
                eq(37.7749), eq(-122.4094), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1100f // ~1.1 km
        }
        
        // Mock repository and location service
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then
        assertEquals("Should return all friends", 3, result.size)
        
        // Verify all friends have correct distance formatting (>= 1000m should show km)
        result.forEach { nearbyFriend ->
            assertEquals("Distance should be formatted as km", "1.1 km", nearbyFriend.formattedDistance)
            assertTrue("Distance should be approximately 1100m", 
                nearbyFriend.distance >= 1000.0 && nearbyFriend.distance <= 1200.0)
        }
        
        // Verify friends are sorted by distance (all equal in this case)
        val distances = result.map { it.distance }
        assertEquals("All distances should be equal", distances.distinct().size, 1)
    }
    
    @Test
    fun `integration test - should handle mixed distance formatting`() = runTest {
        // Given - User location
        val userLocation = Location("test").apply {
            latitude = 0.0
            longitude = 0.0
            time = System.currentTimeMillis()
        }
        
        val userLocationUpdate = FriendLocationUpdate(
            friendId = "current_user",
            previousLocation = null,
            newLocation = LatLng(0.0, 0.0),
            timestamp = userLocation.time,
            isOnline = true,
            updateType = LocationUpdateType.POSITION_CHANGE
        )
        
        // Friends at different distances
        val friends = listOf(
            createFriend("1", "Near", 0.0045, 0.0),  // ~500m
            createFriend("2", "Far", 0.018, 0.0)     // ~2000m
        )
        
        // Mock distance calculations
        every { 
            Location.distanceBetween(
                eq(0.0), eq(0.0), 
                eq(0.0045), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 500f // 500 meters
        }
        
        every { 
            Location.distanceBetween(
                eq(0.0), eq(0.0), 
                eq(0.018), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 2000f // 2000 meters
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then
        assertEquals("Should return both friends", 2, result.size)
        
        // Verify sorting (nearest first)
        assertEquals("Near friend should be first", "Near", result[0].displayName)
        assertEquals("Far friend should be second", "Far", result[1].displayName)
        
        // Verify distance formatting
        assertEquals("Near friend should show meters", "500 m", result[0].formattedDistance)
        assertEquals("Far friend should show kilometers", "2.0 km", result[1].formattedDistance)
    }
    
    // Helper function to create test friends
    private fun createFriend(id: String, name: String, latitude: Double, longitude: Double): Friend {
        return Friend(
            id = id,
            userId = id,
            name = name,
            email = "$name@test.com",
            avatarUrl = "",
            location = FriendLocation(
                latitude = latitude,
                longitude = longitude,
                accuracy = 10f,
                timestamp = java.util.Date()
            ),
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
}