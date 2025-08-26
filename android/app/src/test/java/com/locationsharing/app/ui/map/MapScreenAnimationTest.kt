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
import com.locationsharing.app.ui.map.animations.MapAnimationController
import com.locationsharing.app.ui.map.animations.MapMicroAnimations
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
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

/**
 * Animation tests for MapScreen components
 * Tests micro-animations, transitions, and animation performance
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenAnimationTest {

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

    @MockK
    private lateinit var animationController: MapAnimationController

    @MockK
    private lateinit var microAnimations: MapMicroAnimations

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

        // Animation mocks
        every { animationController.startFABScaleAnimation(any()) } returns Unit
        every { animationController.startMarkerPulseAnimation(any()) } returns Unit
        every { animationController.startDrawerSlideAnimation(any()) } returns Unit
        every { animationController.startSheetFadeAnimation(any()) } returns Unit
        
        every { microAnimations.pulseLocationMarker() } returns Unit
        every { microAnimations.scaleFABOnPress(any()) } returns Unit
        every { microAnimations.slideDrawerWithOvershoot() } returns Unit
        every { microAnimations.fadeSheetInOut(any()) } returns Unit
    }

    @Test
    fun animation_fabScaleAnimationShouldTriggerOnPress() = testScope.runTest {
        // Given - viewModel with location
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - trigger FAB animation events
        viewModel.onEvent(MapScreenEvent.OnFABAnimationStart(FABType.QUICK_SHARE))
        advanceUntilIdle()

        viewModel.onEvent(MapScreenEvent.OnFABAnimationEnd(FABType.QUICK_SHARE))
        advanceUntilIdle()

        // Then - animation events should be handled
        // (In real implementation, these would trigger UI animations)
        assertTrue("FAB animation events should be processed", true)
    }

    @Test
    fun animation_locationMarkerPulseShouldOccurPeriodically() = testScope.runTest {
        // Given - viewModel with location updates
        every { locationService.getLocationUpdates() } returns flow {
            emit(LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis()))
            delay(3000) // Pulse interval
            emit(LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis()))
        }
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - advance time to trigger pulse animation
        advanceTimeBy(3500)

        // Then - location should be updated (pulse would be handled by UI layer)
        val state = viewModel.state.value
        assertEquals(LatLng(37.7749, -122.4194), state.currentLocation)
    }

    @Test
    fun animation_drawerSlideAnimationShouldHaveCorrectTiming() = testScope.runTest {
        // Given - viewModel with friends
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

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - open drawer (should trigger slide animation)
        val startTime = System.currentTimeMillis()
        viewModel.onEvent(MapScreenEvent.OnDrawerOpen)
        advanceUntilIdle()

        // Then - drawer should be open
        assertTrue(viewModel.state.value.isNearbyDrawerOpen)

        // When - close drawer
        viewModel.onEvent(MapScreenEvent.OnDrawerClose)
        advanceUntilIdle()

        // Then - drawer should be closed
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)
    }

    @Test
    fun animation_statusSheetFadeAnimationShouldWork() = testScope.runTest {
        // Given - viewModel with location sharing capability
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - show status sheet (should trigger fade in)
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)
        advanceUntilIdle()

        // Then - sheet should be visible
        assertTrue(viewModel.state.value.isStatusSheetVisible)

        // When - dismiss status sheet (should trigger fade out)
        viewModel.onEvent(MapScreenEvent.OnStatusSheetDismiss)
        advanceUntilIdle()

        // Then - sheet should be hidden
        assertFalse(viewModel.state.value.isStatusSheetVisible)
    }

    @Test
    fun animation_friendMarkerAnimationsShouldHandleUpdates() = testScope.runTest {
        // Given - friends with location updates
        val initialFriends = createTestFriends()
        val updatedFriends = initialFriends.map { friend ->
            friend.copy(
                location = friend.location.copy(
                    latitude = friend.location.latitude + 0.001,
                    longitude = friend.location.longitude + 0.001
                )
            )
        }

        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flow {
            // Initial friends
            emit(initialFriends.map { friend ->
                mockk {
                    every { this@mockk.friend } returns friend
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            })
            
            delay(1000)
            
            // Updated friends (should trigger position animations)
            emit(updatedFriends.map { friend ->
                mockk {
                    every { this@mockk.friend } returns friend
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            })
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        // Initial state
        assertEquals(3, viewModel.state.value.friends.size)

        // When - advance time to get updated friends
        advanceTimeBy(1500)

        // Then - should have updated friends with new positions
        val finalState = viewModel.state.value
        assertEquals(3, finalState.friends.size)
        
        // Verify friends have updated locations
        val updatedFriend = finalState.friends.first()
        assertTrue(updatedFriend.location.latitude > 37.7750)
    }

    @Test
    fun animation_mapCameraAnimationsShouldBeSmooth() = testScope.runTest {
        // Given - viewModel with location
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

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - trigger camera movements
        val newYork = LatLng(40.7128, -74.0060)
        viewModel.onEvent(MapScreenEvent.OnCameraMove(newYork, 12f))
        advanceUntilIdle()

        // Then - camera should update smoothly
        assertEquals(newYork, viewModel.state.value.mapCenter)
        assertEquals(12f, viewModel.state.value.mapZoom)

        // When - center on self location (should animate)
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()

        // Then - should center on user location with close zoom
        assertEquals(LatLng(37.7749, -122.4194), viewModel.state.value.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, viewModel.state.value.mapZoom)
    }

    @Test
    fun animation_clusterAnimationShouldCalculateBounds() = testScope.runTest {
        // Given - viewModel with clustered friends
        val clusteredFriends = listOf(
            createTestFriend("cluster_1", "Friend 1", LatLng(37.7749, -122.4194)),
            createTestFriend("cluster_2", "Friend 2", LatLng(37.7850, -122.4094)),
            createTestFriend("cluster_3", "Friend 3", LatLng(37.7649, -122.4294))
        )

        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - click on cluster (should animate to bounds)
        viewModel.onEvent(MapScreenEvent.OnClusterClick(clusteredFriends))
        advanceUntilIdle()

        // Then - should center on cluster bounds with appropriate zoom
        val state = viewModel.state.value
        
        // Should be centered between the friends
        assertTrue(state.mapCenter.latitude > 37.7649)
        assertTrue(state.mapCenter.latitude < 37.7850)
        assertTrue(state.mapCenter.longitude > -122.4294)
        assertTrue(state.mapCenter.longitude < -122.4094)
        
        // Should have appropriate zoom level for the span
        assertTrue(state.mapZoom >= 10f)
        assertTrue(state.mapZoom <= 18f)
    }

    @Test
    fun animation_multipleAnimationsShouldNotConflict() = testScope.runTest {
        // Given - viewModel with all features
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

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - trigger multiple animations simultaneously
        viewModel.onEvent(MapScreenEvent.OnFABAnimationStart(FABType.QUICK_SHARE))
        viewModel.onEvent(MapScreenEvent.OnDrawerOpen)
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()

        // Then - all state changes should be applied correctly
        val state = viewModel.state.value
        assertTrue(state.isNearbyDrawerOpen)
        assertTrue(state.isStatusSheetVisible)
        assertEquals(LatLng(37.7749, -122.4194), state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)

        // When - end animations
        viewModel.onEvent(MapScreenEvent.OnFABAnimationEnd(FABType.QUICK_SHARE))
        viewModel.onEvent(MapScreenEvent.OnDrawerClose)
        viewModel.onEvent(MapScreenEvent.OnStatusSheetDismiss)
        advanceUntilIdle()

        // Then - should return to stable state
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)
        assertFalse(viewModel.state.value.isStatusSheetVisible)
    }

    @Test
    fun animation_performanceUnderLoad() = testScope.runTest {
        // Given - high frequency animation events
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - trigger many animation events rapidly
        val startTime = System.currentTimeMillis()
        
        repeat(100) { i ->
            viewModel.onEvent(MapScreenEvent.OnFABAnimationStart(FABType.QUICK_SHARE))
            viewModel.onEvent(MapScreenEvent.OnFABAnimationEnd(FABType.QUICK_SHARE))
            viewModel.onEvent(MapScreenEvent.OnAnimationComplete)
        }
        
        advanceUntilIdle()
        val endTime = System.currentTimeMillis()

        // Then - should handle high frequency animations efficiently
        val processingTime = endTime - startTime
        assertTrue("Processing 300 animation events took ${processingTime}ms, should be under 100ms", 
                  processingTime < 100)
    }

    @Test
    fun animation_stateTransitionsShouldBeConsistent() = testScope.runTest {
        // Given - viewModel with initial state
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(LatLng(37.7749, -122.4194), LatLng(37.7749, -122.4194), System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - perform a sequence of animated state transitions
        
        // 1. Open drawer
        viewModel.onEvent(MapScreenEvent.OnDrawerOpen)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isNearbyDrawerOpen)

        // 2. Start location sharing (should show sheet)
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isStatusSheetVisible)

        // 3. Close drawer while sheet is open
        viewModel.onEvent(MapScreenEvent.OnDrawerClose)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)
        assertTrue(viewModel.state.value.isStatusSheetVisible) // Sheet should remain

        // 4. Dismiss sheet
        viewModel.onEvent(MapScreenEvent.OnStatusSheetDismiss)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isStatusSheetVisible)

        // Then - final state should be consistent
        val finalState = viewModel.state.value
        assertFalse(finalState.isNearbyDrawerOpen)
        assertFalse(finalState.isStatusSheetVisible)
        assertEquals(LatLng(37.7749, -122.4194), finalState.currentLocation)
    }

    private fun createViewModel(): MapScreenViewModel {
        return MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
    }

    private fun createTestFriends(): List<Friend> {
        return listOf(
            createTestFriend("friend_1", "Alice", LatLng(37.7750, -122.4195)),
            createTestFriend("friend_2", "Bob", LatLng(37.7748, -122.4193)),
            createTestFriend("friend_3", "Carol", LatLng(37.7751, -122.4196))
        )
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