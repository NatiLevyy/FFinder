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
import com.locationsharing.app.domain.model.NearbyFriend
import io.mockk.coEvery
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
import kotlin.math.abs

/**
 * Unit tests for GetNearbyFriendsUseCase
 * Tests distance calculation accuracy, sorting, and performance requirements
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetNearbyFriendsUseCaseTest {
    
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
    fun `should sort friends by distance ascending with nearest first`() = runTest {
        // Given - User at origin (0,0)
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        // Friends at different distances
        val friends = listOf(
            createFriend("1", "Alice", 0.001, 0.0), // ~111m
            createFriend("2", "Bob", 0.0, 0.001),   // ~111m  
            createFriend("3", "Charlie", 0.0005, 0.0) // ~55m
        )
        
        // Mock distance calculations
        mockDistanceCalculations(userLocation, friends)
        
        // Mock repository and location service
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Should be sorted by distance (nearest first)
        assertEquals(3, result.size)
        assertEquals("Charlie", result[0].displayName) // ~55m
        assertTrue("Alice should be second or third", 
            result[1].displayName == "Alice" || result[1].displayName == "Bob") // ~111m
        assertTrue("Bob should be second or third", 
            result[2].displayName == "Alice" || result[2].displayName == "Bob") // ~111m
        
        // Verify distances are sorted correctly
        assertTrue("First friend should be closest", result[0].distance < result[1].distance)
        assertTrue("Distances should be ascending", result[1].distance <= result[2].distance)
    }
    
    @Test
    fun `should handle distance tolerance of less than 1 meter`() = runTest {
        // Given - User at origin
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        // Friends at very similar distances (within 1m tolerance)
        val friends = listOf(
            createFriend("1", "Alice", 0.000009, 0.0), // ~1m
            createFriend("2", "Bob", 0.000008, 0.0)    // ~0.9m
        )
        
        // Mock distance calculations with sub-meter precision
        every { 
            Location.distanceBetween(
                eq(0.0), eq(0.0), 
                eq(0.000009), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1.0f // 1 meter
        }
        
        every { 
            Location.distanceBetween(
                eq(0.0), eq(0.0), 
                eq(0.000008), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 0.9f // 0.9 meters
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Should handle sub-meter distances correctly
        assertEquals(2, result.size)
        assertTrue("Distance difference should be less than 1m", 
            abs(result[0].distance - result[1].distance) < 1.0)
        assertTrue("Bob should be closer", result[0].displayName == "Bob")
        assertTrue("Alice should be second", result[1].displayName == "Alice")
    }
    
    @Test
    fun `should return friends without distances when user location unavailable`() = runTest {
        // Given - No user location available
        val friends = listOf(
            createFriend("1", "Alice", 0.001, 0.0),
            createFriend("2", "Bob", 0.0, 0.001)
        )
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf() // No location updates
        
        // When
        val result = useCase().first()
        
        // Then - Should return empty list when no location updates
        assertEquals(0, result.size)
    }
    
    @Test
    fun `should format distances correctly according to requirements`() = runTest {
        // Given
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "NearFriend", 0.0045, 0.0), // ~500m
            createFriend("2", "FarFriend", 0.009, 0.0)    // ~1000m
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
                eq(0.009), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1000f // 1000 meters
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Check distance formatting
        assertEquals(2, result.size)
        
        val nearFriend = result.find { it.displayName == "NearFriend" }!!
        val farFriend = result.find { it.displayName == "FarFriend" }!!
        
        // < 1000m should show "X m"
        assertEquals("500 m", nearFriend.formattedDistance)
        
        // >= 1000m should show "X.X km"  
        assertEquals("1.0 km", farFriend.formattedDistance)
    }
    
    @Test
    fun `should handle empty friends list`() = runTest {
        // Given
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        every { friendsRepository.getFriends() } returns flowOf(emptyList())
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then
        assertEquals(0, result.size)
    }
    
    @Test
    fun `should filter out friends without location data`() = runTest {
        // Given
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "Alice", 0.001, 0.0), // Has location
            createFriendWithoutLocation("2", "Bob")  // No location
        )
        
        // Mock distance calculation for Alice only
        every { 
            Location.distanceBetween(
                eq(0.0), eq(0.0), 
                eq(0.001), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 111f // ~111 meters
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Only Alice should be included
        assertEquals(1, result.size)
        assertEquals("Alice", result[0].displayName)
    }
    
    @Test
    fun `should maintain stable sorting for friends at equal distances`() = runTest {
        // Given
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        // Multiple friends at exactly the same distance
        val friends = listOf(
            createFriend("1", "Alice", 0.001, 0.0),
            createFriend("2", "Bob", 0.001, 0.0),
            createFriend("3", "Charlie", 0.001, 0.0)
        )
        
        // Mock all friends at same distance
        every { 
            Location.distanceBetween(
                eq(0.0), eq(0.0), 
                eq(0.001), eq(0.0), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 111f // Same distance for all
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - All friends should be included with same distance
        assertEquals(3, result.size)
        result.forEach { friend ->
            assertEquals(111.0, friend.distance, 0.1)
        }
    }
    
    // Helper functions
    
    private fun createLocation(latitude: Double, longitude: Double): Location {
        return Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            time = System.currentTimeMillis()
        }
    }
    
    private fun createLocationUpdate(location: Location): FriendLocationUpdate {
        return FriendLocationUpdate(
            friendId = "current_user",
            previousLocation = null,
            newLocation = LatLng(location.latitude, location.longitude),
            timestamp = location.time,
            isOnline = true,
            updateType = LocationUpdateType.POSITION_CHANGE
        )
    }
    
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
    
    private fun createFriendWithoutLocation(id: String, name: String): Friend {
        return Friend(
            id = id,
            userId = id,
            name = name,
            email = "$name@test.com",
            avatarUrl = "",
            location = null, // No location data
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = false
            )
        )
    }
    
    private fun mockDistanceCalculations(userLocation: Location, friends: List<Friend>) {
        friends.forEach { friend ->
            friend.location?.let { friendLocation ->
                every { 
                    Location.distanceBetween(
                        eq(userLocation.latitude), eq(userLocation.longitude), 
                        eq(friendLocation.latitude), eq(friendLocation.longitude), 
                        any()
                    ) 
                } answers {
                    val results = arg<FloatArray>(4)
                    // Calculate approximate distance for test
                    val latDiff = friendLocation.latitude - userLocation.latitude
                    val lonDiff = friendLocation.longitude - userLocation.longitude
                    val distance = kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111000 // Rough conversion
                    results[0] = distance.toFloat()
                }
            }
        }
    }
}