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
import kotlin.math.abs

/**
 * Comprehensive tests for distance calculation accuracy and sorting requirements.
 * 
 * Tests requirements:
 * - 4.1: Haversine formula using Location.distanceBetween
 * - 4.2: Background dispatcher usage for distance calculations
 * - 4.4: Distance sorting (nearest first) with < 1m tolerance
 * - 4.5: Distance formatting (< 1000m shows "X m", >= 1000m shows "X.X km")
 * - 8.1: Unit tests for distance calculation accuracy and sorting
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DistanceCalculationAccuracyTest {
    
    private lateinit var friendsRepository: FriendsRepository
    private lateinit var locationService: EnhancedLocationService
    private lateinit var useCase: GetNearbyFriendsUseCase
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        friendsRepository = mockk()
        locationService = mockk()
        
        val performanceMonitor = mockk<com.locationsharing.app.ui.friends.components.NearbyPanelPerformanceMonitor>(relaxed = true)
        
        useCase = GetNearbyFriendsUseCase(
            friendsRepository = friendsRepository,
            locationService = locationService,
            performanceMonitor = performanceMonitor,
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
    fun distanceCalculation_usesHaversineFormulaViaLocationDistanceBetween() = runTest {
        // Given - User at San Francisco coordinates
        val userLocation = createLocation(37.7749, -122.4194)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        // Friend at known distance (approximately 1km north)
        val friend = createFriend("1", "Alice", 37.7839, -122.4194) // ~1km north
        
        // Mock Location.distanceBetween to return precise distance
        every { 
            Location.distanceBetween(
                eq(37.7749), eq(-122.4194), 
                eq(37.7839), eq(-122.4194), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1000.0f // Exactly 1km
        }
        
        every { friendsRepository.getFriends() } returns flowOf(listOf(friend))
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Should use Location.distanceBetween for accurate calculation
        assertEquals(1, result.size)
        assertEquals(1000.0, result[0].distance, 0.1)
        assertEquals("1.0 km", result[0].formattedDistance)
    }
    
    @Test
    fun distanceCalculation_accuracyWithRealWorldCoordinates() = runTest {
        // Given - Real world coordinates with known distances
        val userLocation = createLocation(40.7128, -74.0060) // New York City
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "Brooklyn", 40.6782, -74.0442), // ~8.5km to Brooklyn
            createFriend("2", "Manhattan", 40.7589, -73.9851), // ~5.2km to Central Park
            createFriend("3", "Nearby", 40.7138, -74.0050)    // ~100m nearby
        )
        
        // Mock realistic distance calculations
        every { 
            Location.distanceBetween(
                eq(40.7128), eq(-74.0060), 
                eq(40.6782), eq(-74.0442), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 8500.0f // 8.5km to Brooklyn
        }
        
        every { 
            Location.distanceBetween(
                eq(40.7128), eq(-74.0060), 
                eq(40.7589), eq(-73.9851), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 5200.0f // 5.2km to Central Park
        }
        
        every { 
            Location.distanceBetween(
                eq(40.7128), eq(-74.0060), 
                eq(40.7138), eq(-74.0050), 
                any()
            ) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 100.0f // 100m nearby
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Should be sorted by distance (nearest first)
        assertEquals(3, result.size)
        assertEquals("Nearby", result[0].displayName) // 100m
        assertEquals("Manhattan", result[1].displayName) // 5.2km
        assertEquals("Brooklyn", result[2].displayName) // 8.5km
        
        // Verify distance accuracy
        assertEquals(100.0, result[0].distance, 0.1)
        assertEquals(5200.0, result[1].distance, 0.1)
        assertEquals(8500.0, result[2].distance, 0.1)
    }
    
    @Test
    fun distanceFormatting_metersUnder1000() = runTest {
        // Given - Friends at various distances under 1000m
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "VeryClose", 0.0001, 0.0),   // ~11m
            createFriend("2", "Close", 0.001, 0.0),        // ~111m
            createFriend("3", "Medium", 0.005, 0.0),       // ~555m
            createFriend("4", "AlmostKm", 0.009, 0.0)      // ~999m
        )
        
        // Mock distance calculations for sub-1000m distances
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.0001), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 11.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.001), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 111.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.005), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 555.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.009), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 999.0f
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - All distances under 1000m should show "X m" format
        assertEquals("11 m", result.find { it.displayName == "VeryClose" }?.formattedDistance)
        assertEquals("111 m", result.find { it.displayName == "Close" }?.formattedDistance)
        assertEquals("555 m", result.find { it.displayName == "Medium" }?.formattedDistance)
        assertEquals("999 m", result.find { it.displayName == "AlmostKm" }?.formattedDistance)
    }
    
    @Test
    fun distanceFormatting_kilometersOver1000() = runTest {
        // Given - Friends at various distances over 1000m
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "OneKm", 0.01, 0.0),         // 1.0km
            createFriend("2", "OnePointFive", 0.015, 0.0), // 1.5km
            createFriend("3", "TwoPointThree", 0.023, 0.0), // 2.3km
            createFriend("4", "TenKm", 0.1, 0.0)           // 10.0km
        )
        
        // Mock distance calculations for over-1000m distances
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.01), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1000.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.015), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1500.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.023), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 2300.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.1), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 10000.0f
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - All distances over 1000m should show "X.X km" format
        assertEquals("1.0 km", result.find { it.displayName == "OneKm" }?.formattedDistance)
        assertEquals("1.5 km", result.find { it.displayName == "OnePointFive" }?.formattedDistance)
        assertEquals("2.3 km", result.find { it.displayName == "TwoPointThree" }?.formattedDistance)
        assertEquals("10.0 km", result.find { it.displayName == "TenKm" }?.formattedDistance)
    }
    
    @Test
    fun distanceSorting_nearestFirstWithSubMeterTolerance() = runTest {
        // Given - Friends at very similar distances (testing < 1m tolerance)
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "Friend1", 0.000009, 0.0), // ~1.0m
            createFriend("2", "Friend2", 0.000008, 0.0), // ~0.9m
            createFriend("3", "Friend3", 0.000007, 0.0), // ~0.8m
            createFriend("4", "Friend4", 0.000010, 0.0)  // ~1.1m
        )
        
        // Mock precise sub-meter distance calculations
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.000009), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1.0f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.000008), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 0.9f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.000007), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 0.8f
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.000010), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 1.1f
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Should be sorted by distance with sub-meter precision
        assertEquals(4, result.size)
        assertEquals("Friend3", result[0].displayName) // 0.8m
        assertEquals("Friend2", result[1].displayName) // 0.9m
        assertEquals("Friend1", result[2].displayName) // 1.0m
        assertEquals("Friend4", result[3].displayName) // 1.1m
        
        // Verify distances are within tolerance
        assertTrue("Distance differences should be less than 1m", 
            abs(result[0].distance - result[1].distance) < 1.0)
        assertTrue("Distance differences should be less than 1m", 
            abs(result[1].distance - result[2].distance) < 1.0)
    }
    
    @Test
    fun distanceCalculation_backgroundDispatcherUsage() = runTest {
        // Given - Use case configured with background dispatcher
        val backgroundDispatcher = StandardTestDispatcher()
        val backgroundUseCase = GetNearbyFriendsUseCase(
            friendsRepository = friendsRepository,
            locationService = locationService,
            performanceMonitor = performanceMonitor,
            dispatcher = backgroundDispatcher
        )
        
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        val friend = createFriend("1", "Alice", 0.001, 0.0)
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.001), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 111.0f
        }
        
        every { friendsRepository.getFriends() } returns flowOf(listOf(friend))
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When - Execute on background dispatcher
        val result = backgroundUseCase().first()
        backgroundDispatcher.scheduler.advanceUntilIdle()
        
        // Then - Calculation should complete successfully on background thread
        assertEquals(1, result.size)
        assertEquals(111.0, result[0].distance, 0.1)
    }
    
    @Test
    fun distanceCalculation_performanceWithLargeFriendsList() = runTest {
        // Given - Large list of friends (500+ for performance testing)
        val userLocation = createLocation(0.0, 0.0)
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val largeFriendsList = (1..500).map { index ->
            createFriend(index.toString(), "Friend$index", index * 0.001, 0.0)
        }
        
        // Mock distance calculations for all friends
        largeFriendsList.forEachIndexed { index, friend ->
            every { 
                Location.distanceBetween(
                    eq(0.0), eq(0.0), 
                    eq(friend.location!!.latitude), eq(0.0), 
                    any()
                ) 
            } answers {
                val results = arg<FloatArray>(4)
                results[0] = (index + 1) * 111.0f // Increasing distances
            }
        }
        
        every { friendsRepository.getFriends() } returns flowOf(largeFriendsList)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When - Measure calculation time
        val startTime = System.currentTimeMillis()
        val result = useCase().first()
        val endTime = System.currentTimeMillis()
        val calculationTime = endTime - startTime
        
        // Then - Should handle large lists efficiently
        assertEquals(500, result.size)
        assertTrue("Calculation should complete quickly for 500 friends", calculationTime < 1000)
        
        // Verify sorting is correct
        assertTrue("First friend should be closest", result[0].distance < result[1].distance)
        assertTrue("Last friend should be farthest", 
            result[499].distance > result[498].distance)
    }
    
    @Test
    fun distanceCalculation_edgeCaseCoordinates() = runTest {
        // Given - Edge case coordinates (poles, equator, date line)
        val userLocation = createLocation(0.0, 0.0) // Equator, Prime Meridian
        val userLocationUpdate = createLocationUpdate(userLocation)
        
        val friends = listOf(
            createFriend("1", "NorthPole", 90.0, 0.0),     // North Pole
            createFriend("2", "SouthPole", -90.0, 0.0),    // South Pole
            createFriend("3", "DateLine", 0.0, 180.0),     // International Date Line
            createFriend("4", "AntiMeridian", 0.0, -180.0) // Anti-meridian
        )
        
        // Mock distance calculations for edge cases
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(90.0), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 10007543.0f // ~10,000km to North Pole
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(-90.0), eq(0.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 10007543.0f // ~10,000km to South Pole
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.0), eq(180.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 20015086.0f // ~20,000km to opposite side
        }
        
        every { 
            Location.distanceBetween(eq(0.0), eq(0.0), eq(0.0), eq(-180.0), any()) 
        } answers {
            val results = arg<FloatArray>(4)
            results[0] = 20015086.0f // ~20,000km to opposite side
        }
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(userLocationUpdate)
        
        // When
        val result = useCase().first()
        
        // Then - Should handle edge case coordinates without errors
        assertEquals(4, result.size)
        
        // Verify extreme distances are formatted correctly
        val northPole = result.find { it.displayName == "NorthPole" }!!
        val southPole = result.find { it.displayName == "SouthPole" }!!
        
        assertTrue("North Pole distance should be very large", northPole.distance > 1000000)
        assertTrue("South Pole distance should be very large", southPole.distance > 1000000)
        assertTrue("Distance formatting should handle large numbers", 
            northPole.formattedDistance.contains("km"))
    }
    
    // Helper functions
    
    private fun createLocation(latitude: Double, longitude: Double): Location {
        return Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            time = System.currentTimeMillis()
            accuracy = 10.0f
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
}