package com.locationsharing.app.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for location services in MapScreen
 * Tests requirements 2.2, 2.6, 7.4, 7.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationServicesIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var locationService: EnhancedLocationService
    private lateinit var friendsRepository: FriendsRepository
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase
    private lateinit var viewModel: MapScreenViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        friendsRepository = mockk(relaxed = true)
        getNearbyFriendsUseCase = mockk(relaxed = true)
        
        // Mock static methods
        mockkStatic(ContextCompat::class)
        
        // Setup default mocks
        coEvery { getNearbyFriendsUseCase() } returns flowOf(emptyList())
        every { locationService.getLocationUpdates() } returns flowOf()
        every { locationService.getCurrentLocation() } returns flowOf(null)
    }
    
    @Test
    fun `location permission granted should start location updates`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.hasLocationPermission)
        verify { locationService.getLocationUpdates() }
    }
    
    @Test
    fun `location permission denied should show error`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.hasLocationPermission)
        assertNotNull(viewModel.state.value.locationError)
        assertTrue(viewModel.state.value.locationError!!.contains("permission"))
    }
    
    @Test
    fun `location permission granted should clear error`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()
        
        // When - permission granted
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionGranted)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.hasLocationPermission)
        assertNull(viewModel.state.value.locationError)
        assertFalse(viewModel.state.value.isLocationPermissionRequested)
    }
    
    @Test
    fun `location update should update current location`() = testScope.runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        advanceUntilIdle()
        
        // Then
        assertEquals(testLocation, viewModel.state.value.currentLocation)
        assertEquals(testLocation, viewModel.state.value.mapCenter)
        assertFalse(viewModel.state.value.isLocationLoading)
        assertNull(viewModel.state.value.locationError)
    }
    
    @Test
    fun `self location center should request location when no current location`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isLocationLoading)
        verify { locationService.getCurrentLocation() }
    }
    
    @Test
    fun `self location center should center map when current location exists`() = testScope.runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then
        assertEquals(testLocation, viewModel.state.value.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, viewModel.state.value.mapZoom)
    }
    
    @Test
    fun `self location center without permission should request permission`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isLocationPermissionRequested)
        assertFalse(viewModel.state.value.hasLocationPermission)
    }
    
    @Test
    fun `location sharing should enable high accuracy mode`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isLocationSharingActive)
        assertTrue(viewModel.state.value.isStatusSheetVisible)
        verify { locationService.enableHighAccuracyMode(true) }
    }
    
    @Test
    fun `stop location sharing should disable high accuracy mode`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnStopLocationSharing)
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.isLocationSharingActive)
        assertFalse(viewModel.state.value.isStatusSheetVisible)
        verify { locationService.enableHighAccuracyMode(false) }
    }
    
    @Test
    fun `retry should restart location updates on error`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(LatLng(0.0, 0.0)))
        
        // Simulate error
        val currentState = viewModel.state.value
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()
        
        // When - retry
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel.onEvent(MapScreenEvent.OnRetry)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isRetrying)
        assertTrue(viewModel.state.value.retryCount > 0)
    }
    
    @Test
    fun `error dismiss should clear all errors`() = testScope.runTest {
        // Given
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnErrorDismiss)
        advanceUntilIdle()
        
        // Then
        assertNull(viewModel.state.value.locationError)
        assertNull(viewModel.state.value.friendsError)
        assertNull(viewModel.state.value.locationSharingError)
        assertNull(viewModel.state.value.generalError)
    }
    
    @Test
    fun `location ready state should be true when permission granted and location available`() = testScope.runTest {
        // Given
        val testLocation = LatLng(37.7749, -122.4194)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        
        // When
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(testLocation))
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isLocationReady)
        assertEquals(testLocation, viewModel.state.value.currentLocation)
        assertTrue(viewModel.state.value.hasLocationPermission)
        assertFalse(viewModel.state.value.isLocationLoading)
    }
    
    @Test
    fun `location ready state should be false when permission denied`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.isLocationReady)
        assertFalse(viewModel.state.value.hasLocationPermission)
    }
    
    @Test
    fun `location ready state should be false when location is loading`() = testScope.runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        
        // When - trigger location loading
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        
        // Then
        assertFalse(viewModel.state.value.isLocationReady)
        assertTrue(viewModel.state.value.isLocationLoading)
    }
}