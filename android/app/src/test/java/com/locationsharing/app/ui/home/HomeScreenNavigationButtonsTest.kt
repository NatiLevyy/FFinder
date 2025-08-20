package com.locationsharing.app.ui.home

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.navigation.NavigationManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HomeScreen navigation buttons functionality.
 * Tests the enhanced navigation state management, loading states,
 * and proper integration with NavigationManager.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenNavigationButtonsTest {
    
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var mockNavigationManager: NavigationManager
    
    @Before
    fun setUp() {
        mockNavigationManager = mockk(relaxed = true)
        viewModel = HomeScreenViewModel()
    }
    
    @Test
    fun `startSharing event sets navigation loading state`() = runTest {
        // Given
        val initialState = HomeScreenState(
            hasLocationPermission = true,
            mapPreviewLocation = LatLng(37.7749, -122.4194)
        )
        
        // When
        viewModel.onEvent(HomeScreenEvent.StartSharing)
        
        // Then
        val currentState = viewModel.state.value
        assertTrue("Should be navigating to map", currentState.isNavigatingToMap)
        assertTrue("Should be in navigation state", currentState.isNavigating)
        assertTrue("Should be location sharing", currentState.isLocationSharing)
    }
    
    @Test
    fun `startSharing without location permission shows error`() = runTest {
        // Given
        val initialState = HomeScreenState(
            hasLocationPermission = false
        )
        
        // When
        viewModel.onEvent(HomeScreenEvent.StartSharing)
        
        // Then
        val currentState = viewModel.state.value
        assertFalse("Should not be navigating to map", currentState.isNavigatingToMap)
        assertFalse("Should not be in navigation state", currentState.isNavigating)
        assertEquals(
            "Should show permission error",
            "Location permission required to start sharing",
            currentState.locationError
        )
    }
    
    @Test
    fun `navigateToFriends event sets friends loading state`() = runTest {
        // Given
        val initialState = HomeScreenState()
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigateToFriends)
        
        // Then
        val currentState = viewModel.state.value
        assertTrue("Should be navigating to friends", currentState.isNavigatingToFriends)
        assertTrue("Should be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigateToSettings event sets settings loading state`() = runTest {
        // Given
        val initialState = HomeScreenState()
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigateToSettings)
        
        // Then
        val currentState = viewModel.state.value
        assertTrue("Should be navigating to settings", currentState.isNavigatingToSettings)
        assertTrue("Should be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationToMapStarted event sets correct state`() = runTest {
        // Given
        val initialState = HomeScreenState()
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationToMapStarted)
        
        // Then
        val currentState = viewModel.state.value
        assertTrue("Should be navigating to map", currentState.isNavigatingToMap)
        assertTrue("Should be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationToMapCompleted event clears loading state`() = runTest {
        // Given - Start with navigation in progress
        viewModel.onEvent(HomeScreenEvent.NavigationToMapStarted)
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationToMapCompleted)
        
        // Then
        val currentState = viewModel.state.value
        assertFalse("Should not be navigating to map", currentState.isNavigatingToMap)
        assertFalse("Should not be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationToFriendsStarted event sets correct state`() = runTest {
        // Given
        val initialState = HomeScreenState()
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationToFriendsStarted)
        
        // Then
        val currentState = viewModel.state.value
        assertTrue("Should be navigating to friends", currentState.isNavigatingToFriends)
        assertTrue("Should be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationToFriendsCompleted event clears loading state`() = runTest {
        // Given - Start with navigation in progress
        viewModel.onEvent(HomeScreenEvent.NavigationToFriendsStarted)
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationToFriendsCompleted)
        
        // Then
        val currentState = viewModel.state.value
        assertFalse("Should not be navigating to friends", currentState.isNavigatingToFriends)
        assertFalse("Should not be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationToSettingsStarted event sets correct state`() = runTest {
        // Given
        val initialState = HomeScreenState()
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationToSettingsStarted)
        
        // Then
        val currentState = viewModel.state.value
        assertTrue("Should be navigating to settings", currentState.isNavigatingToSettings)
        assertTrue("Should be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationToSettingsCompleted event clears loading state`() = runTest {
        // Given - Start with navigation in progress
        viewModel.onEvent(HomeScreenEvent.NavigationToSettingsStarted)
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationToSettingsCompleted)
        
        // Then
        val currentState = viewModel.state.value
        assertFalse("Should not be navigating to settings", currentState.isNavigatingToSettings)
        assertFalse("Should not be in navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `navigationFailed event clears all loading states and shows error`() = runTest {
        // Given - Start with navigation in progress
        viewModel.onEvent(HomeScreenEvent.NavigationToMapStarted)
        val errorMessage = "Navigation timeout"
        
        // When
        viewModel.onEvent(HomeScreenEvent.NavigationFailed(errorMessage))
        
        // Then
        val currentState = viewModel.state.value
        assertFalse("Should not be navigating", currentState.isNavigating)
        assertFalse("Should not be navigating to map", currentState.isNavigatingToMap)
        assertFalse("Should not be navigating to friends", currentState.isNavigatingToFriends)
        assertFalse("Should not be navigating to settings", currentState.isNavigatingToSettings)
        assertEquals(
            "Should show navigation error",
            "Navigation failed: $errorMessage",
            currentState.locationError
        )
    }
    
    @Test
    fun `multiple navigation states can be tracked independently`() = runTest {
        // Given
        val initialState = HomeScreenState()
        
        // When - Start navigation to friends
        viewModel.onEvent(HomeScreenEvent.NavigationToFriendsStarted)
        val friendsState = viewModel.state.value
        
        // Then
        assertTrue("Should be navigating to friends", friendsState.isNavigatingToFriends)
        assertTrue("Should be in navigation state", friendsState.isNavigating)
        assertFalse("Should not be navigating to map", friendsState.isNavigatingToMap)
        assertFalse("Should not be navigating to settings", friendsState.isNavigatingToSettings)
        
        // When - Complete friends navigation and start settings navigation
        viewModel.onEvent(HomeScreenEvent.NavigationToFriendsCompleted)
        viewModel.onEvent(HomeScreenEvent.NavigationToSettingsStarted)
        val settingsState = viewModel.state.value
        
        // Then
        assertFalse("Should not be navigating to friends", settingsState.isNavigatingToFriends)
        assertTrue("Should be navigating to settings", settingsState.isNavigatingToSettings)
        assertTrue("Should be in navigation state", settingsState.isNavigating)
        assertFalse("Should not be navigating to map", settingsState.isNavigatingToMap)
    }
    
    @Test
    fun `clearError clears location error but preserves navigation state`() = runTest {
        // Given - Set error and navigation state
        viewModel.onEvent(HomeScreenEvent.NavigationFailed("Test error"))
        viewModel.onEvent(HomeScreenEvent.NavigationToMapStarted)
        
        // When
        viewModel.clearError()
        
        // Then
        val currentState = viewModel.state.value
        assertNull("Should clear location error", currentState.locationError)
        assertTrue("Should preserve navigation state", currentState.isNavigatingToMap)
        assertTrue("Should preserve general navigation state", currentState.isNavigating)
    }
    
    @Test
    fun `homeScreenState navigation properties have correct default values`() {
        // Given
        val defaultState = HomeScreenState()
        
        // Then
        assertFalse("isNavigating should default to false", defaultState.isNavigating)
        assertFalse("isNavigatingToMap should default to false", defaultState.isNavigatingToMap)
        assertFalse("isNavigatingToFriends should default to false", defaultState.isNavigatingToFriends)
        assertFalse("isNavigatingToSettings should default to false", defaultState.isNavigatingToSettings)
    }
    
    @Test
    fun `homeScreenState can be created with navigation properties`() {
        // Given
        val state = HomeScreenState(
            isNavigating = true,
            isNavigatingToMap = true,
            isNavigatingToFriends = false,
            isNavigatingToSettings = false
        )
        
        // Then
        assertTrue("isNavigating should be true", state.isNavigating)
        assertTrue("isNavigatingToMap should be true", state.isNavigatingToMap)
        assertFalse("isNavigatingToFriends should be false", state.isNavigatingToFriends)
        assertFalse("isNavigatingToSettings should be false", state.isNavigatingToSettings)
    }
}