package com.locationsharing.app.ui.map

import android.content.Context
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.location.LocationSharingService
import com.locationsharing.app.data.location.LocationSharingStatus
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for MapScreenViewModel location sharing functionality
 * Tests requirements 3.3, 5.6, 5.7
 */
@ExperimentalCoroutinesApi
class MapScreenViewModelLocationSharingTest {
    
    private lateinit var viewModel: MapScreenViewModel
    private lateinit var mockContext: Context
    private lateinit var mockLocationService: EnhancedLocationService
    private lateinit var mockFriendsRepository: FriendsRepository
    private lateinit var mockRealTimeFriendsService: RealTimeFriendsService
    private lateinit var mockGetNearbyFriendsUseCase: GetNearbyFriendsUseCase
    private lateinit var mockLocationSharingService: LocationSharingService
    
    private val mockSharingStateFlow = MutableStateFlow(
        com.locationsharing.app.data.location.LocationSharingState()
    )
    private val mockNotificationsFlow = MutableStateFlow<com.locationsharing.app.data.location.LocationSharingNotification?>(null)
    
    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockLocationService = mockk(relaxed = true)
        mockFriendsRepository = mockk(relaxed = true)
        mockRealTimeFriendsService = mockk(relaxed = true)
        mockGetNearbyFriendsUseCase = mockk(relaxed = true)
        mockLocationSharingService = mockk(relaxed = true)
        
        // Mock location permission as granted
        every { mockContext.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // Mock location sharing service flows
        every { mockLocationSharingService.sharingState } returns mockSharingStateFlow
        every { mockLocationSharingService.notifications } returns mockNotificationsFlow
        
        // Mock real-time friends service
        every { mockRealTimeFriendsService.startSync() } returns Unit
        every { mockRealTimeFriendsService.stopSync() } returns Unit
        every { mockRealTimeFriendsService.getFriendUpdatesWithAnimations() } returns kotlinx.coroutines.flow.emptyFlow()
        
        viewModel = MapScreenViewModel(
            context = mockContext,
            locationService = mockLocationService,
            friendsRepository = mockFriendsRepository,
            realTimeFriendsService = mockRealTimeFriendsService,
            getNearbyFriendsUseCase = mockGetNearbyFriendsUseCase,
            locationSharingService = mockLocationSharingService
        )
    }
    
    @Test
    fun `onQuickShare should start location sharing when inactive`() = runTest {
        // Given
        coEvery { mockLocationSharingService.startLocationSharing() } returns Result.success(Unit)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnQuickShare)
        
        // Then
        coVerify { mockLocationSharingService.startLocationSharing() }
        
        val state = viewModel.state.first()
        assertTrue(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onQuickShare should show status sheet when already active`() = runTest {
        // Given - location sharing is active
        mockSharingStateFlow.value = com.locationsharing.app.data.location.LocationSharingState(
            status = LocationSharingStatus.ACTIVE
        )
        
        // When
        viewModel.onEvent(MapScreenEvent.OnQuickShare)
        
        // Then
        coVerify(exactly = 0) { mockLocationSharingService.startLocationSharing() }
        
        val state = viewModel.state.first()
        assertTrue(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onStartLocationSharing should request permission when not granted`() = runTest {
        // Given - no location permission
        every { mockContext.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_DENIED
        
        // Recreate viewModel with denied permission
        viewModel = MapScreenViewModel(
            context = mockContext,
            locationService = mockLocationService,
            friendsRepository = mockFriendsRepository,
            realTimeFriendsService = mockRealTimeFriendsService,
            getNearbyFriendsUseCase = mockGetNearbyFriendsUseCase,
            locationSharingService = mockLocationSharingService
        )
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        
        // Then
        coVerify(exactly = 0) { mockLocationSharingService.startLocationSharing() }
        
        val state = viewModel.state.first()
        assertTrue(state.isLocationPermissionRequested)
    }
    
    @Test
    fun `onStartLocationSharing should start sharing when permission granted`() = runTest {
        // Given
        coEvery { mockLocationSharingService.startLocationSharing() } returns Result.success(Unit)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        
        // Then
        coVerify { mockLocationSharingService.startLocationSharing() }
        
        val state = viewModel.state.first()
        assertTrue(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onStartLocationSharing should handle error`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { mockLocationSharingService.startLocationSharing() } returns Result.failure(exception)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        
        // Then
        coVerify { mockLocationSharingService.startLocationSharing() }
        
        val state = viewModel.state.first()
        assertEquals("Network error", state.locationSharingError)
    }
    
    @Test
    fun `onStopLocationSharing should stop sharing successfully`() = runTest {
        // Given
        coEvery { mockLocationSharingService.stopLocationSharing() } returns Result.success(Unit)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStopLocationSharing)
        
        // Then
        coVerify { mockLocationSharingService.stopLocationSharing() }
        
        val state = viewModel.state.first()
        assertFalse(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onStopLocationSharing should handle error`() = runTest {
        // Given
        val exception = Exception("Stop error")
        coEvery { mockLocationSharingService.stopLocationSharing() } returns Result.failure(exception)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStopLocationSharing)
        
        // Then
        coVerify { mockLocationSharingService.stopLocationSharing() }
        
        val state = viewModel.state.first()
        assertEquals("Stop error", state.locationSharingError)
    }
    
    @Test
    fun `onStatusSheetShow should show status sheet`() = runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)
        
        // Then
        val state = viewModel.state.first()
        assertTrue(state.isStatusSheetVisible)
    }
    
    @Test
    fun `onStatusSheetDismiss should hide status sheet`() = runTest {
        // Given - sheet is visible
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStatusSheetDismiss)
        
        // Then
        val state = viewModel.state.first()
        assertFalse(state.isStatusSheetVisible)
    }
    
    @Test
    fun `location sharing state should sync with service`() = runTest {
        // When location sharing service state changes
        mockSharingStateFlow.value = com.locationsharing.app.data.location.LocationSharingState(
            status = LocationSharingStatus.ACTIVE,
            error = null
        )
        
        // Then ViewModel state should update
        val state = viewModel.state.first()
        assertTrue(state.isLocationSharingActive)
        assertEquals(null, state.locationSharingError)
        
        // When service has error
        mockSharingStateFlow.value = com.locationsharing.app.data.location.LocationSharingState(
            status = LocationSharingStatus.ERROR,
            error = "Service error"
        )
        
        // Then ViewModel should reflect error
        val errorState = viewModel.state.first()
        assertFalse(errorState.isLocationSharingActive)
        assertEquals("Service error", errorState.locationSharingError)
    }
    
    @Test
    fun `onRetry should retry location sharing when in error state`() = runTest {
        // Given - location sharing error
        mockSharingStateFlow.value = com.locationsharing.app.data.location.LocationSharingState(
            status = LocationSharingStatus.ERROR,
            error = "Previous error"
        )
        
        coEvery { mockLocationSharingService.retryLocationSharing() } returns Result.success(Unit)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnRetry)
        
        // Then
        coVerify { mockLocationSharingService.retryLocationSharing() }
    }
    
    @Test
    fun `location sharing notifications should be observed`() = runTest {
        // When notification is emitted
        val notification = com.locationsharing.app.data.location.LocationSharingNotification(
            type = com.locationsharing.app.data.location.NotificationType.SUCCESS,
            message = "Sharing started",
            timestamp = System.currentTimeMillis()
        )
        mockNotificationsFlow.value = notification
        
        // Then notification should be processed (logged in this case)
        // This test verifies the flow is being observed
        // In a real implementation, this might trigger UI updates
        verify(timeout = 1000) { mockLocationSharingService.notifications }
    }
    
    @Test
    fun `state should provide correct status text`() = runTest {
        // Test inactive state
        val inactiveState = viewModel.state.first()
        assertEquals("Location Sharing Off", inactiveState.locationSharingStatusText)
        
        // Test active state
        mockSharingStateFlow.value = com.locationsharing.app.data.location.LocationSharingState(
            status = LocationSharingStatus.ACTIVE
        )
        
        val activeState = viewModel.state.first()
        assertEquals("Location Sharing Active", activeState.locationSharingStatusText)
    }
    
    @Test
    fun `state should provide coordinates text when location available`() = runTest {
        // Given - location is available
        val location = com.google.android.gms.maps.model.LatLng(37.7749, -122.4194)
        
        // Simulate location update
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(location))
        
        // Then
        val state = viewModel.state.first()
        val expectedText = "Lat: 37.774900, Lng: -122.419400"
        assertEquals(expectedText, state.coordinatesText)
    }
    
    @Test
    fun `state should show location unavailable when no location`() = runTest {
        // When no location is set
        val state = viewModel.state.first()
        
        // Then
        assertEquals("Location unavailable", state.coordinatesText)
    }
}