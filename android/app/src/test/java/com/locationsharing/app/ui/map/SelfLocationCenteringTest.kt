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
import io.mockk.unmockkStatic
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test suite for self-location centering functionality in MapScreenViewModel.
 * 
 * Tests the implementation of requirements:
 * - 7.1: Self-location FAB functionality
 * - 7.2: Map camera animation to center on user location
 * - 7.3: Loading state indicator and permission handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelfLocationCenteringTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var context: Context
    private lateinit var locationService: EnhancedLocationService
    private lateinit var friendsRepository: FriendsRepository
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase
    private lateinit var viewModel: MapScreenViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock dependencies
        context = mockk(relaxed = true)
        locationService = mockk(relaxed = true)
        friendsRepository = mockk(relaxed = true)
        getNearbyFriendsUseCase = mockk(relaxed = true)
        
        // Mock static methods
        mockkStatic(ContextCompat::class)
        
        // Setup default mock behaviors
        coEvery { getNearbyFriendsUseCase() } returns flowOf(emptyList())
        every { locationService.getLocationUpdates() } returns flowOf()
        every { locationService.getCurrentLocation() } returns flowOf()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(ContextCompat::class)
    }
    
    @Test
    fun `onSelfLocationCenter with current location updates map center and zoom`() = runTest {
        // Given
        val currentLocation = LatLng(37.7749, -122.4194)
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // Set current location in state
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(currentLocation))
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assertEquals(currentLocation, state.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, state.mapZoom)
    }
    
    @Test
    fun `onSelfLocationCenter without current location but with permission requests fresh location`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { locationService.getCurrentLocation() } returns flowOf(LatLng(37.7749, -122.4194))
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        // Should have triggered location loading
        assertFalse(state.isLocationLoading) // Will be false after location is received
        assertNotNull(state.currentLocation) // Should have received location
    }
    
    @Test
    fun `onSelfLocationCenter without permission requests permission`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state.isLocationPermissionRequested)
        assertFalse(state.hasLocationPermission)
    }
    
    @Test
    fun `onSelfLocationCenter sets loading state when requesting fresh location`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // Mock location service to not immediately return location
        every { locationService.getCurrentLocation() } returns flowOf()
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        
        // Then - should show loading state immediately
        val state = viewModel.state.value
        assertTrue(state.isLocationLoading)
    }
    
    @Test
    fun `onSelfLocationCenter with permission granted updates state correctly`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // When - first request permission
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()
        
        // Then - permission should be requested
        assertTrue(viewModel.state.value.isLocationPermissionRequested)
        
        // When - permission is granted
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionGranted)
        advanceUntilIdle()
        
        // Then - state should be updated
        val state = viewModel.state.value
        assertTrue(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
    }
    
    @Test
    fun `onSelfLocationCenter with permission denied shows error`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_DENIED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // When - request permission and it's denied
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assertFalse(state.hasLocationPermission)
        assertFalse(state.isLocationPermissionRequested)
        assertNotNull(state.locationError)
        assertTrue(state.locationError!!.contains("Location permission is required"))
    }
    
    @Test
    fun `location update after self-location center updates map center`() = runTest {
        // Given
        val newLocation = LatLng(40.7128, -74.0060) // New York
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
        } returns PackageManager.PERMISSION_GRANTED
        
        viewModel = MapScreenViewModel(context, locationService, friendsRepository, getNearbyFriendsUseCase)
        advanceUntilIdle()
        
        // When - center on self location then receive location update
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        viewModel.onEvent(MapScreenEvent.OnLocationUpdated(newLocation))
        advanceUntilIdle()
        
        // Then
        val state = viewModel.state.value
        assertEquals(newLocation, state.currentLocation)
        assertEquals(newLocation, state.mapCenter)
        assertFalse(state.isLocationLoading)
    }
}