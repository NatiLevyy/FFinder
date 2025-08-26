package com.locationsharing.app.ui.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocationUpdate
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.LocationUpdateType
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for MapScreenViewModel
 * Tests state management, event handling, and lifecycle management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Mocks
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockLocationService = mockk<EnhancedLocationService>(relaxed = true)
    private val mockFriendsRepository = mockk<FriendsRepository>(relaxed = true)
    private val mockGetNearbyFriendsUseCase = mockk<GetNearbyFriendsUseCase>(relaxed = true)
    
    private lateinit var viewModel: MapScreenViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        every { mockContext.checkSelfPermission(any()) } returns PackageManager.PERMISSION_GRANTED
        every { mockLocationService.getLocationUpdates() } returns flowOf()
        coEvery { mockGetNearbyFriendsUseCase() } returns flowOf(emptyList())
        
        viewModel = MapScreenViewModel(
            context = mockContext,
            locationService = mockLocationService,
            friendsRepository = mockFriendsRepository,
            getNearbyFriendsUseCase = mockGetNearbyFriendsUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state should have correct default values`() {
        val state = viewModel.state.value
        
        assertNull(state.currentLocation)
        assertFalse(state.isLocationLoading)
        assertNull(state.locationError)
        assertTrue(state.hasLocationPermission) // Mocked as granted
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
        assertEquals(MapScreenConstants.Map.DEFAULT_ZOOM, state.mapZoom)
        assertFalse(state.isMapLoading)
        assertEquals(100, state.batteryLevel)
        assertFalse(state.isHighAccuracyMode)
        assertEquals(5000L, state.locationUpdateInterval)
        assertNull(state.generalError)
        assertFalse(state.isRetrying)
        assertEquals(0, state.retryCount)
    }
    
    @Test
    fun `onLocationPermissionGranted should update state and start location updates`() = runTest {
        // Given
        val mockLocationUpdate = FriendLocationUpdate(
            friendId = "current_user",
            previousLocation = null,
            newLocation = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis(),
            isOnline = true,
            updateType = LocationUpdateType.INITIAL_LOAD
        )
        every { mockLocationService.getLocationUpdates() } returns flowOf(mockLocationUpdate)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionGranted)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
        assertNull(state.locationError)
        verify { mockLocationService.getLocationUpdates() }
    }
    
    @Test
    fun `onLocationPermissionDenied should update state with error`() {
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        
        // Then
        val state = viewModel.state.value
        assertFalse(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
        assertNotNull(state.locationError)
        assertFalse(state.isLocationLoading)
    }
    
    @Test
    fun `onLocationUpdated should update current location and map center`() {
        // Given
        val testLocation = LatLng(40.7128, -74.0060) // New York
        
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        
        // Then
        val state = viewModel.state.value
        assertEquals(testLocation, state.currentLocation)
        assertEquals(testLocation, state.mapCenter)
        assertFalse(state.isLocationLoading)
        assertNull(state.locationError)
    }
    
    @Test
    fun `onStartLocationSharing should enable location sharing and high accuracy mode`() {
        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        
        // Then
        val state = viewModel.state.value
        assertTrue(state.isLocationSharingActive)
        assertNull(state.locationSharingError)
        assertTrue(state.isStatusSheetVisible)
        verify { mockLocationService.enableHighAccuracyMode(true) }
    }
    
    @Test
    fun `onStopLocationSharing should disable location sharing and high accuracy mode`() {
        // Given - start sharing first
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStopLocationSharing)
        
        // Then
        val state = viewModel.state.value
        assertFalse(state.isLocationSharingActive)
        assertNull(state.locationSharingError)
        assertFalse(state.isStatusSheetVisible)
        verify { mockLocationService.enableHighAccuracyMode(false) }
    }
    
    @Test
    fun `onQuickShare should start location sharing when inactive`() {
        // When
        viewModel.onEvent(MapScreenEvent.OnQuickShare)
        
        // Then
        val state = viewModel.state.value
        assertTrue(state.isLocationSharingActive)
        assertTrue(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onQuickShare should show status sheet when already active`() {
        // Given - start sharing first
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        viewModel.onEvent(MapScreenEvent.OnStatusSheetDismiss)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnQuickShare)
        
        // Then
        val state = viewModel.state.value
        assertTrue(state.isLocationSharingActive)
        assertTrue(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onNearbyFriendsToggle should toggle drawer state`() {
        // When - first toggle
        viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
        
        // Then
        assertTrue(viewModel.state.value.isNearbyDrawerOpen)
        
        // When - second toggle
        viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
        
        // Then
        assertFalse(viewModel.state.value.isNearbyDrawerOpen)
    }
    
    @Test
    fun `onFriendMarkerClick should select friend and center map`() = runTest {
        // Given
        val testFriend = createTestFriend("friend_1", "Test Friend", 40.7128, -74.0060)
        coEvery { mockGetNearbyFriendsUseCase() } returns flowOf(listOf(testFriend))
        
        // Initialize with friends
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend_1"))
        
        // Then
        val state = viewModel.state.value
        assertEquals("friend_1", state.selectedFriendId)
        assertEquals(testFriend.getLatLng(), state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)
    }
    
    @Test
    fun `onMapClick should clear friend selection`() {
        // Given - select a friend first
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend_1"))
        
        // When
        viewModel.onEvent(MapScreenEvent.OnMapClick(LatLng(0.0, 0.0)))
        
        // Then
        assertNull(viewModel.state.value.selectedFriendId)
    }
    
    @Test
    fun `onSelfLocationCenter should update map center when location available`() {
        // Given
        val testLocation = LatLng(40.7128, -74.0060)
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        
        // Then
        val state = viewModel.state.value
        assertEquals(testLocation, state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)
    }
    
    @Test
    fun `onSelfLocationCenter should request permission when not available`() {
        // Given - no permission
        every { mockContext.checkSelfPermission(any()) } returns PackageManager.PERMISSION_DENIED
        val viewModelNoPermission = MapScreenViewModel(
            context = mockContext,
            locationService = mockLocationService,
            friendsRepository = mockFriendsRepository,
            getNearbyFriendsUseCase = mockGetNearbyFriendsUseCase
        )
        
        // When
        viewModelNoPermission.onEvent(MapScreenEvent.OnSelfLocationCenter)
        
        // Then
        assertTrue(viewModelNoPermission.state.value.isLocationPermissionRequested)
    }
    
    @Test
    fun `onCameraMove should update map center and zoom`() {
        // Given
        val newCenter = LatLng(51.5074, -0.1278) // London
        val newZoom = 12f
        
        // When
        viewModel.onEvent(MapScreenEvent.OnCameraMove(newCenter, newZoom))
        
        // Then
        val state = viewModel.state.value
        assertEquals(newCenter, state.mapCenter)
        assertEquals(newZoom, state.mapZoom)
    }
    
    @Test
    fun `onEnableHighAccuracyMode should enable high accuracy`() {
        // When
        viewModel.onEvent(MapScreenEvent.OnEnableHighAccuracyMode)
        
        // Then
        assertTrue(viewModel.state.value.isHighAccuracyMode)
        verify { mockLocationService.enableHighAccuracyMode(true) }
    }
    
    @Test
    fun `onDisableHighAccuracyMode should disable high accuracy`() {
        // Given - enable first
        viewModel.onEvent(MapScreenEvent.OnEnableHighAccuracyMode)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnDisableHighAccuracyMode)
        
        // Then
        assertFalse(viewModel.state.value.isHighAccuracyMode)
        verify { mockLocationService.enableHighAccuracyMode(false) }
    }
    
    @Test
    fun `onBatteryLevelChanged should update battery level`() {
        // Given
        val newBatteryLevel = 75
        
        // When
        viewModel.onEvent(MapScreenEvent.OnBatteryLevelChanged(newBatteryLevel))
        
        // Then
        assertEquals(newBatteryLevel, viewModel.state.value.batteryLevel)
    }
    
    @Test
    fun `onErrorDismiss should clear all errors`() {
        // Given - set some errors
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnErrorDismiss)
        
        // Then
        val state = viewModel.state.value
        assertNull(state.locationError)
        assertNull(state.friendsError)
        assertNull(state.locationSharingError)
        assertNull(state.generalError)
    }
    
    @Test
    fun `state helper methods should work correctly`() {
        // Test isLoading
        assertFalse(viewModel.state.value.isLoading)
        
        // Test hasError
        assertFalse(viewModel.state.value.hasError)
        
        // Test isLocationReady
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(LatLng(0.0, 0.0)))
        assertTrue(viewModel.state.value.isLocationReady)
        
        // Test canRetry
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        assertTrue(viewModel.state.value.canRetry)
    }
    
    // Helper methods
    
    private fun createTestFriend(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double
    ): Friend {
        return Friend(
            id = id,
            userId = "user_$id",
            name = name,
            email = "${name.lowercase().replace(" ", ".")}@example.com",
            location = com.locationsharing.app.data.friends.FriendLocation(
                latitude = latitude,
                longitude = longitude,
                accuracy = 10f,
                isMoving = false,
                timestamp = java.util.Date()
            ),
            status = com.locationsharing.app.data.friends.FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
}