package com.locationsharing.app.ui.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for MapScreenViewModel state management
 * Tests all state transitions, event handling, and business logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenViewModelComprehensiveTest {

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

    private val testLocation = LatLng(37.7749, -122.4194)
    private val testFriend = mockk<Friend> {
        every { id } returns "test_friend_1"
        every { name } returns "Test Friend"
        every { getLatLng() } returns LatLng(37.7750, -122.4195)
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Mock static methods
        mockkStatic(ContextCompat::class)

        // Setup default mock behaviors
        setupDefaultMocks()

        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultMocks() {
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_GRANTED

        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(testLocation, testLocation, System.currentTimeMillis())
        )

        every { locationService.getCurrentLocation() } returns flowOf(testLocation)

        every { locationService.stopLocationUpdates() } returns Unit

        every { locationService.enableHighAccuracyMode(any()) } returns Unit

        every { realTimeFriendsService.startSync() } returns Unit

        every { realTimeFriendsService.stopSync() } returns Unit

        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        every { locationSharingService.sharingState } returns flowOf(
            LocationSharingState(LocationSharingStatus.INACTIVE)
        )

        every { locationSharingService.notifications } returns flowOf(null)

        coEvery { locationSharingService.startLocationSharing() } returns Result.success(Unit)

        coEvery { locationSharingService.stopLocationSharing() } returns Result.success(Unit)

        coEvery { locationSharingService.retryLocationSharing() } returns Result.success(Unit)
    }

    // Initial State Tests
    @Test
    fun `initial state should have correct default values`() = testScope.runTest {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLocationLoading)
        assertNull(state.currentLocation)
        assertTrue(state.friends.isEmpty())
        assertFalse(state.isLocationSharingActive)
        assertFalse(state.isNearbyDrawerOpen)
        assertFalse(state.isStatusSheetVisible)
        assertEquals(MapScreenConstants.Map.DEFAULT_ZOOM, state.mapZoom)
    }

    @Test
    fun `initial state should check location permission`() = testScope.runTest {
        advanceUntilIdle()

        verify { ContextCompat.checkSelfPermission(context, any()) }
        assertTrue(viewModel.state.value.hasLocationPermission)
    }

    // Location Permission Tests
    @Test
    fun `onLocationPermissionGranted should update state and start location updates`() = testScope.runTest {
        // Given
        val initialState = viewModel.state.value.copy(
            hasLocationPermission = false,
            isLocationPermissionRequested = true
        )

        // When
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionGranted)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
        assertNull(state.locationError)
        verify { locationService.getLocationUpdates() }
    }

    @Test
    fun `onLocationPermissionDenied should update state with error`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
        assertNotNull(state.locationError)
        assertFalse(state.isLocationLoading)
    }

    // Location Updates Tests
    @Test
    fun `onLocationUpdated should update current location and map center`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(testLocation, state.currentLocation)
        assertEquals(testLocation, state.mapCenter)
        assertFalse(state.isLocationLoading)
        assertNull(state.locationError)
    }

    @Test
    fun `onSelfLocationCenter with current location should update map center`() = testScope.runTest {
        // Given - set current location first
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(testLocation, state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)
    }

    @Test
    fun `onSelfLocationCenter without location should request permission`() = testScope.runTest {
        // Given - no location permission
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_DENIED

        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.isLocationPermissionRequested)
    }

    // Location Sharing Tests
    @Test
    fun `onQuickShare when inactive should start location sharing`() = testScope.runTest {
        // Given - location sharing is inactive
        every { locationSharingService.sharingState } returns flowOf(
            LocationSharingState(LocationSharingStatus.INACTIVE)
        )

        // When
        viewModel.onEvent(MapScreenEvent.OnQuickShare)
        advanceUntilIdle()

        // Then
        coVerify { locationSharingService.startLocationSharing() }
    }

    @Test
    fun `onQuickShare when active should show status sheet`() = testScope.runTest {
        // Given - location sharing is active
        every { locationSharingService.sharingState } returns flowOf(
            LocationSharingState(LocationSharingStatus.ACTIVE)
        )
        
        // Recreate viewModel to pick up the new mock
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnQuickShare)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.isStatusSheetVisible)
    }

    @Test
    fun `onStartLocationSharing should update state on success`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        advanceUntilIdle()

        // Then
        coVerify { locationSharingService.startLocationSharing() }
        val state = viewModel.state.value
        assertTrue(state.isStatusSheetVisible)
        assertNull(state.locationSharingError)
    }

    @Test
    fun `onStartLocationSharing should handle failure`() = testScope.runTest {
        // Given
        coEvery { locationSharingService.startLocationSharing() } returns 
            Result.failure(Exception("Network error"))

        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNotNull(state.locationSharingError)
        assertTrue(state.locationSharingError!!.contains("Network error"))
    }

    @Test
    fun `onStopLocationSharing should update state on success`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnStopLocationSharing)
        advanceUntilIdle()

        // Then
        coVerify { locationSharingService.stopLocationSharing() }
        val state = viewModel.state.value
        assertFalse(state.isStatusSheetVisible)
        assertNull(state.locationSharingError)
    }

    // Friend Interaction Tests
    @Test
    fun `onFriendMarkerClick should select friend and center map`() = testScope.runTest {
        // Given - add friend to state
        val friendsWithLocation = listOf(testFriend)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(
            friendsWithLocation.map { 
                mockk {
                    every { friend } returns it
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            }
        )

        // Recreate viewModel to pick up friends
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("test_friend_1"))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("test_friend_1", state.selectedFriendId)
        assertEquals(testFriend.getLatLng(), state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)
    }

    @Test
    fun `onFriendSelectionClear should clear selected friend`() = testScope.runTest {
        // Given - friend is selected
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("test_friend_1"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnFriendSelectionClear)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.state.value.selectedFriendId)
    }

    @Test
    fun `onMapClick should clear friend selection`() = testScope.runTest {
        // Given - friend is selected
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("test_friend_1"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnMapClick(testLocation))
        advanceUntilIdle()

        // Then
        assertNull(viewModel.state.value.selectedFriendId)
    }

    // Drawer State Tests
    @Test
    fun `onNearbyFriendsToggle should toggle drawer state`() = testScope.runTest {
        // Initially closed
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)

        // When - first toggle
        viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
        advanceUntilIdle()

        // Then - should be open
        assertTrue(viewModel.state.value.isNearbyDrawerOpen)

        // When - second toggle
        viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
        advanceUntilIdle()

        // Then - should be closed
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)
    }

    @Test
    fun `onDrawerClose should close drawer`() = testScope.runTest {
        // Given - drawer is open
        viewModel.onEvent(MapScreenEvent.OnDrawerOpen)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isNearbyDrawerOpen)

        // When
        viewModel.onEvent(MapScreenEvent.OnDrawerClose)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)
    }

    // Status Sheet Tests
    @Test
    fun `onStatusSheetShow should show status sheet`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.isStatusSheetVisible)
    }

    @Test
    fun `onStatusSheetDismiss should hide status sheet`() = testScope.runTest {
        // Given - sheet is visible
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnStatusSheetDismiss)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isStatusSheetVisible)
    }

    // Error Handling Tests
    @Test
    fun `onErrorDismiss should clear all errors`() = testScope.runTest {
        // Given - simulate errors
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.locationError)

        // When
        viewModel.onEvent(MapScreenEvent.OnErrorDismiss)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNull(state.locationError)
        assertNull(state.friendsError)
        assertNull(state.locationSharingError)
        assertNull(state.generalError)
    }

    @Test
    fun `onRetry should attempt to retry failed operations`() = testScope.runTest {
        // Given - simulate location error
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()

        // Grant permission for retry
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_GRANTED

        // When
        viewModel.onEvent(MapScreenEvent.OnRetry)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.retryCount > 0)
        verify(atLeast = 2) { locationService.getLocationUpdates() }
    }

    // Map Camera Tests
    @Test
    fun `onCameraMove should update map center and zoom`() = testScope.runTest {
        // Given
        val newCenter = LatLng(40.7128, -74.0060) // New York
        val newZoom = 15f

        // When
        viewModel.onEvent(MapScreenEvent.OnCameraMove(newCenter, newZoom))
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(newCenter, state.mapCenter)
        assertEquals(newZoom, state.mapZoom)
    }

    // Performance Tests
    @Test
    fun `onEnableHighAccuracyMode should update state and service`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnEnableHighAccuracyMode)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.isHighAccuracyMode)
        verify { locationService.enableHighAccuracyMode(true) }
    }

    @Test
    fun `onDisableHighAccuracyMode should update state and service`() = testScope.runTest {
        // Given - high accuracy is enabled
        viewModel.onEvent(MapScreenEvent.OnEnableHighAccuracyMode)
        advanceUntilIdle()

        // When
        viewModel.onEvent(MapScreenEvent.OnDisableHighAccuracyMode)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isHighAccuracyMode)
        verify { locationService.enableHighAccuracyMode(false) }
    }

    @Test
    fun `onBatteryLevelChanged should update battery level`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnBatteryLevelChanged(75))
        advanceUntilIdle()

        // Then
        assertEquals(75, viewModel.state.value.batteryLevel)
    }

    // Lifecycle Tests
    @Test
    fun `onScreenResume should restart location updates if permission granted`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnScreenResume)
        advanceUntilIdle()

        // Then
        verify(atLeast = 2) { locationService.getLocationUpdates() }
    }

    @Test
    fun `onRefreshData should restart all data loading`() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnRefreshData)
        advanceUntilIdle()

        // Then
        verify(atLeast = 2) { locationService.getLocationUpdates() }
        verify(atLeast = 2) { realTimeFriendsService.startSync() }
    }

    // State Computed Properties Tests
    @Test
    fun `state computed properties should work correctly`() = testScope.runTest {
        // Test isLoading
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        advanceUntilIdle()
        
        val state = viewModel.state.value
        
        // Test isLocationReady
        assertTrue(state.isLocationReady)
        
        // Test coordinatesText
        assertTrue(state.coordinatesText.contains("37.774900"))
        assertTrue(state.coordinatesText.contains("-122.419400"))
        
        // Test locationSharingStatusText
        assertEquals("Location Sharing Off", state.locationSharingStatusText)
    }

    // Debug Features Tests (only test structure, not actual debug functionality)
    @Test
    fun `debug events should be handled without errors`() = testScope.runTest {
        // These should not crash even if debug functionality is disabled
        viewModel.onEvent(MapScreenEvent.OnDebugAddFriends)
        viewModel.onEvent(MapScreenEvent.OnDebugClearFriends)
        viewModel.onEvent(MapScreenEvent.OnDebugToggleHighAccuracy)
        advanceUntilIdle()

        // Should complete without exceptions
        assertTrue(true)
    }

    // Animation Events Tests
    @Test
    fun `animation events should be handled without errors`() = testScope.runTest {
        // These should not crash
        viewModel.onEvent(MapScreenEvent.OnAnimationComplete)
        viewModel.onEvent(MapScreenEvent.OnFABAnimationStart(FABType.QUICK_SHARE))
        viewModel.onEvent(MapScreenEvent.OnFABAnimationEnd(FABType.SELF_LOCATION))
        advanceUntilIdle()

        // Should complete without exceptions
        assertTrue(true)
    }

    // Edge Cases Tests
    @Test
    fun `multiple rapid events should be handled correctly`() = testScope.runTest {
        // Rapid location updates
        repeat(10) { i ->
            val location = LatLng(37.7749 + i * 0.001, -122.4194 + i * 0.001)
            viewModel.onEvent(MapScreenEvent.OnLocationUpdated(location))
        }
        advanceUntilIdle()

        // Should have the last location
        val finalLocation = LatLng(37.7749 + 9 * 0.001, -122.4194 + 9 * 0.001)
        assertEquals(finalLocation, viewModel.state.value.currentLocation)
    }

    @Test
    fun `state should handle null values gracefully`() = testScope.runTest {
        // Test with null location
        val state = viewModel.state.value.copy(currentLocation = null)
        
        // These should not crash
        assertEquals("Location unavailable", state.coordinatesText)
        assertTrue(state.getNearbyFriends().isEmpty())
        assertNull(state.getSelectedFriend())
    }
}

/**
 * Enum for FAB types used in animation events
 */
enum class FABType {
    QUICK_SHARE,
    SELF_LOCATION,
    DEBUG
}