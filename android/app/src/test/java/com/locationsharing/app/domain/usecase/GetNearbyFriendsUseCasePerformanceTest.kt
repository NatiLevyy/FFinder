package com.locationsharing.app.domain.usecase

import android.location.Location
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.location.LocationUpdate
import com.google.android.gms.maps.model.LatLng
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Performance tests for GetNearbyFriendsUseCase with large datasets.
 * Tests distance calculation throttling and memory efficiency.
 * 
 * Requirements tested:
 * - 6.6: Add distance calculation throttling (20m movement OR 10s interval)
 * - 8.6: Add memory leak prevention for location updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetNearbyFriendsUseCasePerformanceTest {

    private lateinit var friendsRepository: FriendsRepository
    private lateinit var locationService: EnhancedLocationService
    private lateinit var useCase: GetNearbyFriendsUseCase
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        friendsRepository = mockk()
        locationService = mockk()
        
        useCase = GetNearbyFriendsUseCase(
            friendsRepository = friendsRepository,
            locationService = locationService,
            dispatcher = testDispatcher
        )
    }

    /**
     * Test distance calculation performance with 100 friends
     */
    @Test
    fun testDistanceCalculationPerformanceWith100Friends() = runTest {
        val friends = generateLargeFriendsList(100)
        val userLocation = createTestLocation(37.7749, -122.4194)
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation.latitude, userLocation.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        val calculationTime = measureTimeMillis {
            val result = useCase().first()
            assert(result.size == friends.size) {
                "Expected ${friends.size} friends, got ${result.size}"
            }
        }
        
        // Assert calculation time is acceptable (< 500ms for 100 friends)
        assert(calculationTime < 500) {
            "Distance calculation for 100 friends took ${calculationTime}ms, expected < 500ms"
        }
    }

    /**
     * Test distance calculation performance with 500 friends
     */
    @Test
    fun testDistanceCalculationPerformanceWith500Friends() = runTest {
        val friends = generateLargeFriendsList(500)
        val userLocation = createTestLocation(37.7749, -122.4194)
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation.latitude, userLocation.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        val calculationTime = measureTimeMillis {
            val result = useCase().first()
            // Should be limited to MAX_CACHED_FRIENDS (1000)
            assert(result.size <= 500) {
                "Expected <= 500 friends, got ${result.size}"
            }
        }
        
        // Assert calculation time is acceptable (< 1000ms for 500 friends)
        assert(calculationTime < 1000) {
            "Distance calculation for 500 friends took ${calculationTime}ms, expected < 1000ms"
        }
    }

    /**
     * Test distance calculation performance with 1000+ friends (memory limit test)
     */
    @Test
    fun testDistanceCalculationPerformanceWith1000PlusFriends() = runTest {
        val friends = generateLargeFriendsList(1500) // More than MAX_CACHED_FRIENDS
        val userLocation = createTestLocation(37.7749, -122.4194)
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation.latitude, userLocation.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        val calculationTime = measureTimeMillis {
            val result = useCase().first()
            // Should be limited to MAX_CACHED_FRIENDS (1000)
            assert(result.size <= 1000) {
                "Expected <= 1000 friends due to memory limit, got ${result.size}"
            }
        }
        
        // Assert calculation time is still reasonable even with large input
        assert(calculationTime < 2000) {
            "Distance calculation for 1500 friends took ${calculationTime}ms, expected < 2000ms"
        }
    }

    /**
     * Test throttling behavior - should not recalculate if movement < 20m and time < 10s
     */
    @Test
    fun testDistanceCalculationThrottling() = runTest {
        val friends = generateLargeFriendsList(50)
        val userLocation1 = createTestLocation(37.7749, -122.4194)
        val userLocation2 = createTestLocation(37.7750, -122.4195) // ~15m movement
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        
        // First calculation
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation1.latitude, userLocation1.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        val firstCalculationTime = measureTimeMillis {
            useCase().first()
        }
        
        // Second calculation with small movement (should be throttled)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation2.latitude, userLocation2.longitude),
                timestamp = System.currentTimeMillis() + 5000, // 5 seconds later
                accuracy = 10f
            )
        )
        
        val secondCalculationTime = measureTimeMillis {
            useCase().first()
        }
        
        // Second calculation should be faster due to throttling/caching
        assert(secondCalculationTime < firstCalculationTime) {
            "Second calculation (${secondCalculationTime}ms) should be faster than first (${firstCalculationTime}ms) due to throttling"
        }
    }

    /**
     * Test that calculation is triggered when movement > 20m
     */
    @Test
    fun testDistanceCalculationTriggeredByMovement() = runTest {
        val friends = generateLargeFriendsList(50)
        val userLocation1 = createTestLocation(37.7749, -122.4194)
        val userLocation2 = createTestLocation(37.7770, -122.4194) // ~25m movement
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        
        // First calculation
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation1.latitude, userLocation1.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        val result1 = useCase().first()
        
        // Second calculation with significant movement (should trigger recalculation)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation2.latitude, userLocation2.longitude),
                timestamp = System.currentTimeMillis() + 5000, // 5 seconds later
                accuracy = 10f
            )
        )
        
        val result2 = useCase().first()
        
        // Results should be different due to new distances
        assert(result1.size == result2.size) {
            "Both results should have same number of friends"
        }
        
        // At least some distances should be different
        val differentDistances = result1.zip(result2).count { (friend1, friend2) ->
            friend1.id == friend2.id && kotlin.math.abs(friend1.distance - friend2.distance) > 1.0
        }
        
        assert(differentDistances > 0) {
            "Expected some distance differences due to movement, but found none"
        }
    }

    /**
     * Test that calculation is triggered after 10 seconds
     */
    @Test
    fun testDistanceCalculationTriggeredByTime() = runTest {
        val friends = generateLargeFriendsList(50)
        val userLocation = createTestLocation(37.7749, -122.4194)
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        
        // First calculation
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation.latitude, userLocation.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        useCase().first()
        
        // Second calculation after 11 seconds (should trigger recalculation)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation.latitude, userLocation.longitude),
                timestamp = System.currentTimeMillis() + 11000, // 11 seconds later
                accuracy = 10f
            )
        )
        
        val calculationTime = measureTimeMillis {
            useCase().first()
        }
        
        // Should perform full calculation due to time threshold
        assert(calculationTime > 10) {
            "Expected full calculation due to time threshold, but took only ${calculationTime}ms"
        }
    }

    /**
     * Test memory efficiency with repeated calculations
     */
    @Test
    fun testMemoryEfficiencyWithRepeatedCalculations() = runTest {
        val friends = generateLargeFriendsList(200)
        val userLocation = createTestLocation(37.7749, -122.4194)
        
        every { friendsRepository.getFriends() } returns flowOf(friends)
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(
                newLocation = LatLng(userLocation.latitude, userLocation.longitude),
                timestamp = System.currentTimeMillis(),
                accuracy = 10f
            )
        )
        
        // Get initial memory usage
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform multiple calculations
        repeat(10) {
            useCase().first()
        }
        
        // Check memory usage after calculations
        runtime.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Assert memory increase is reasonable (< 10MB for repeated calculations)
        val maxMemoryIncrease = 10 * 1024 * 1024 // 10MB in bytes
        assert(memoryIncrease < maxMemoryIncrease) {
            "Memory increase was ${memoryIncrease / (1024 * 1024)}MB, expected < 10MB"
        }
    }

    /**
     * Generate a large list of test friends for performance testing
     */
    private fun generateLargeFriendsList(count: Int): List<Friend> {
        return (1..count).map { index ->
            Friend(
                id = "friend_$index",
                name = "Friend $index",
                email = "friend$index@test.com",
                avatarUrl = "",
                location = FriendLocation(
                    latitude = 37.7749 + (index * 0.001), // Varied locations around SF
                    longitude = -122.4194 + (index * 0.001),
                    timestamp = System.currentTimeMillis() - (index * 1000)
                ),
                status = FriendStatus(
                    isOnline = index % 3 == 0,
                    lastSeen = System.currentTimeMillis() - (index * 1000),
                    isLocationSharing = true
                )
            )
        }
    }

    /**
     * Create a test Location object
     */
    private fun createTestLocation(latitude: Double, longitude: Double): Location {
        return Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            time = System.currentTimeMillis()
            accuracy = 10f
        }
    }
}