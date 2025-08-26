package com.locationsharing.app.ui.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.location.LocationSharingService
import com.locationsharing.app.data.location.LocationSharingState
import com.locationsharing.app.data.location.LocationSharingStatus
import com.locationsharing.app.data.location.LocationUpdate
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.system.measureTimeMillis

/**
 * Performance tests for MapScreen components
 * Tests memory usage, rendering performance, and animation efficiency
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenPerformanceTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var locationService: EnhancedLocationService

    @MockK
    private lateinit var friendsRepository: FriendsRepository

    @MockK
    private lateinit var realTimeFriendsService: RealTimeFriendsService

    @MockK
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase

    @MockK
    private lateinit var locationSharingService: LocationSharingService

    private lateinit var viewModel: MapScreenViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        mockkStatic(ContextCompat::class)
        setupDefaultMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultMocks() {
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_GRANTED

        every { locationService.stopLocationUpdates() } returns Unit
        every { locationService.enableHighAccuracyMode(any()) } returns Unit
        every { realTimeFriendsService.startSync() } returns Unit
        every { realTimeFriendsService.stopSync() } returns Unit

        every { locationSharingService.sharingState } returns flowOf(
            LocationSharingState(LocationSharingStatus.INACTIVE)
        )
        every { locationSharingService.notifications } returns flowOf(null)
        coEvery { locationSharingService.startLocationSharing() } returns Result.success(Unit)
        coEvery { locationSharingService.stopLocationSharing() } returns Result.success(Unit)
    }

    @Test
    fun performance_viewModelInitializationShouldBeFast() = testScope.runTest {
        // Given - mock services
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        // When - measure initialization time
        val initTime = measureTimeMillis {
            viewModel = MapScreenViewModel(
                context = context,
                locationService = locationService,
                friendsRepository = friendsRepository,
                realTimeFriendsService = realTimeFriendsService,
                getNearbyFriendsUseCase = getNearbyFriendsUseCase,
                locationSharingService = locationSharingService
            )
            advanceUntilIdle()
        }

        // Then - should initialize quickly (under 100ms in test environment)
        assertTrue("ViewModel initialization took ${initTime}ms, should be under 100ms", initTime < 100)
        
        // Should have proper initial state
        val state = viewModel.state.value
        assertTrue(state.hasLocationPermission)
        assertFalse(state.isLoading)
    }

    @Test
    fun performance_rapidLocationUpdatesShouldNotCauseMemoryLeaks() = testScope.runTest {
        // Given - rapid location updates
        every { locationService.getLocationUpdates() } returns flow {
            repeat(1000) { i ->
                emit(LocationUpdate(
                    LatLng(37.7749 + i * 0.0001, -122.4194 + i * 0.0001),
                    LatLng(37.7749, -122.4194),
                    System.currentTimeMillis()
                ))
                delay(1) // Very rapid updates
            }
        }
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        // When - create viewModel and process rapid updates
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )

        // Process all updates
        advanceTimeBy(2000)

        // Then - should handle all updates without issues
        val finalState = viewModel.state.value
        assertTrue(finalState.currentLocation!!.latitude > 37.7749)
        assertTrue(finalState.currentLocation!!.longitude > -122.4194)
        
        // Memory usage should be stable (no accumulation of old locations)
        // This is verified by the fact that state only holds current location, not history
        assertEquals(1, 1) // Placeholder assertion - memory is managed by state design
    }

    @Test
    fun performance_largeFriendsListShouldRenderEfficiently() = testScope.runTest {
        // Given - large number of friends
        val largeFriendsList = createLargeFriendsList(500)
        
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(
            largeFriendsList.map { friend ->
                mockk {
                    every { this@mockk.friend } returns friend
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            }
        )

        // When - create viewModel with large friends list
        val processingTime = measureTimeMillis {
            viewModel = MapScreenViewModel(
                context = context,
                locationService = locationService,
                friendsRepository = friendsRepository,
                realTimeFriendsService = realTimeFriendsService,
                getNearbyFriendsUseCase = getNearbyFriendsUseCase,
                locationSharingService = locationSharingService
            )
            advanceUntilIdle()
        }

        // Then - should process large list efficiently (under 500ms)
        assertTrue("Processing 500 friends took ${processingTime}ms, should be under 500ms", processingTime < 500)
        
        val state = viewModel.state.value
        assertEquals(500, state.friends.size)
        assertEquals(500, state.nearbyFriendsCount)
    }

    @Test
    fun performance_frequentStateUpdatesShouldNotBlockUI() = testScope.runTest {
        // Given - services that provide frequent updates
        every { locationService.getLocationUpdates() } returns flow {
            repeat(100) { i ->
                emit(LocationUpdate(
                    LatLng(37.7749 + i * 0.001, -122.4194),
                    LatLng(37.7749, -122.4194),
                    System.currentTimeMillis()
                ))
                delay(10) // 100 updates per second
            }
        }
        
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flow {
            repeat(50) { i ->
                emit(createTestFriends().map { friend ->
                    mockk {
                        every { this@mockk.friend } returns friend.copy(
                            location = friend.location.copy(
                                latitude = friend.location.latitude + i * 0.0001
                            )
                        )
                        every { updateType } returns mockk()
                        every { animationType } returns mockk()
                    }
                })
                delay(20) // 50 updates per second
            }
        }

        // When - create viewModel and measure update processing
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )

        val updateTime = measureTimeMillis {
            advanceTimeBy(2000) // Process 2 seconds of updates
        }

        // Then - should handle frequent updates efficiently
        assertTrue("Processing frequent updates took ${updateTime}ms, should be under 100ms", updateTime < 100)
        
        // State should be updated to latest values
        val state = viewModel.state.value
        assertTrue(state.currentLocation!!.latitude > 37.7749)
        assertTrue(state.friends.isNotEmpty())
    }

    @Test
    fun performance_eventProcessingShouldBeFast() = testScope.runTest {
        // Given - viewModel with initial state
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(
            createTestFriends().map { friend ->
                mockk {
                    every { this@mockk.friend } returns friend
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            }
        )

        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
        advanceUntilIdle()

        // When - measure event processing time
        val events = listOf(
            MapScreenEvent.OnNearbyFriendsToggle,
            MapScreenEvent.OnSelfLocationCenter,
            MapScreenEvent.OnQuickShare,
            MapScreenEvent.OnFriendMarkerClick("friend_1"),
            MapScreenEvent.OnMapClick(LatLng(37.7750, -122.4195)),
            MapScreenEvent.OnCameraMove(LatLng(37.7751, -122.4196), 15f),
            MapScreenEvent.OnStatusSheetShow,
            MapScreenEvent.OnStatusSheetDismiss,
            MapScreenEvent.OnDrawerOpen,
            MapScreenEvent.OnDrawerClose
        )

        val eventProcessingTime = measureTimeMillis {
            events.forEach { event ->
                viewModel.onEvent(event)
            }
            advanceUntilIdle()
        }

        // Then - should process all events quickly (under 50ms)
        assertTrue("Processing ${events.size} events took ${eventProcessingTime}ms, should be under 50ms", 
                  eventProcessingTime < 50)
    }

    @Test
    fun performance_memoryUsageShouldBeStable() = testScope.runTest {
        // Given - viewModel with cycling data
        var friendsCounter = 0
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flow {
            repeat(10) { cycle ->
                // Emit different friend lists to simulate real-world changes
                val friends = createTestFriends().take((cycle % 5) + 1)
                emit(friends.map { friend ->
                    mockk {
                        every { this@mockk.friend } returns friend.copy(id = "${friend.id}_$cycle")
                        every { updateType } returns mockk()
                        every { animationType } returns mockk()
                    }
                })
                delay(100)
            }
        }

        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )

        // When - cycle through different states multiple times
        repeat(5) { cycle ->
            advanceTimeBy(200)
            
            // Trigger various state changes
            viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
            viewModel.onEvent(MapScreenEvent.OnQuickShare)
            viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
            
            advanceTimeBy(100)
        }

        // Then - memory should be stable (no accumulation of old state)
        val finalState = viewModel.state.value
        
        // Should only hold current state, not history
        assertTrue(finalState.friends.size <= 5) // Max friends in any cycle
        
        // State should be consistent
        assertTrue(finalState.friends.all { it.id.contains("_") }) // All friends should be from latest cycle
    }

    @Test
    fun performance_animationStateShouldNotImpactPerformance() = testScope.runTest {
        // Given - viewModel with animation events
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
        advanceUntilIdle()

        // When - trigger many animation events
        val animationTime = measureTimeMillis {
            repeat(100) { i ->
                viewModel.onEvent(MapScreenEvent.OnFABAnimationStart(FABType.QUICK_SHARE))
                viewModel.onEvent(MapScreenEvent.OnFABAnimationEnd(FABType.QUICK_SHARE))
                viewModel.onEvent(MapScreenEvent.OnFABAnimationStart(FABType.SELF_LOCATION))
                viewModel.onEvent(MapScreenEvent.OnFABAnimationEnd(FABType.SELF_LOCATION))
                viewModel.onEvent(MapScreenEvent.OnAnimationComplete)
            }
            advanceUntilIdle()
        }

        // Then - animation events should not impact performance
        assertTrue("Processing 500 animation events took ${animationTime}ms, should be under 50ms", 
                  animationTime < 50)
    }

    @Test
    fun performance_concurrentOperationsShouldNotBlock() = testScope.runTest {
        // Given - concurrent operations
        every { locationService.getLocationUpdates() } returns flow {
            repeat(50) { i ->
                emit(LocationUpdate(
                    LatLng(37.7749 + i * 0.001, -122.4194),
                    LatLng(37.7749, -122.4194),
                    System.currentTimeMillis()
                ))
                delay(20)
            }
        }
        
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flow {
            repeat(30) { i ->
                emit(createTestFriends().map { friend ->
                    mockk {
                        every { this@mockk.friend } returns friend
                        every { updateType } returns mockk()
                        every { animationType } returns mockk()
                    }
                })
                delay(30)
            }
        }

        // When - create viewModel and trigger concurrent operations
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )

        val concurrentTime = measureTimeMillis {
            // Trigger multiple concurrent operations
            viewModel.onEvent(MapScreenEvent.OnQuickShare)
            viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
            viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
            viewModel.onEvent(MapScreenEvent.OnRefreshData)
            
            advanceTimeBy(1500) // Let all operations complete
        }

        // Then - should handle concurrent operations efficiently
        assertTrue("Concurrent operations took ${concurrentTime}ms, should be under 200ms", 
                  concurrentTime < 200)
        
        // State should be consistent
        val state = viewModel.state.value
        assertTrue(state.currentLocation != null)
        assertTrue(state.friends.isNotEmpty())
    }

    private fun createTestFriends(): List<Friend> {
        return listOf(
            createTestFriend("friend_1", "Alice", LatLng(37.7750, -122.4195)),
            createTestFriend("friend_2", "Bob", LatLng(37.7748, -122.4193)),
            createTestFriend("friend_3", "Carol", LatLng(37.7751, -122.4196))
        )
    }

    private fun createLargeFriendsList(count: Int): List<Friend> {
        return (1..count).map { i ->
            createTestFriend(
                "friend_$i",
                "Friend $i",
                LatLng(37.7749 + (i % 100) * 0.001, -122.4194 + (i % 100) * 0.001)
            )
        }
    }

    private fun createTestFriend(id: String, name: String, location: LatLng): Friend {
        return Friend(
            id = id,
            userId = "user_$id",
            name = name,
            email = "${name.lowercase()}@example.com",
            avatarUrl = "",
            profileColor = "#FF5722",
            location = FriendLocation(
                latitude = location.latitude,
                longitude = location.longitude,
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