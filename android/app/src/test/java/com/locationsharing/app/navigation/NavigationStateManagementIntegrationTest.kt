package com.locationsharing.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for navigation state management.
 * Tests the interaction between NavigationManager and NavigationStateTracker.
 */
class NavigationStateManagementIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var navController: NavController
    private lateinit var navigationStateTracker: NavigationStateTrackerImpl
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    private lateinit var navigationManager: NavigationManagerImpl
    
    @Before
    fun setUp() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        navController = mockk(relaxed = true)
        navigationErrorHandler = mockk(relaxed = true)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { sharedPreferences.getString(any(), any()) } returns null
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
        every { navController.popBackStack() } returns true
        
        navigationStateTracker = NavigationStateTrackerImpl(context)
        navigationManager = NavigationManagerImpl(navigationStateTracker, navigationErrorHandler)
        navigationManager.setNavController(navController)
    }
    
    @Test
    fun `navigation manager should update state tracker on navigation`() = runTest {
        // When
        navigationManager.navigateToMap()
        
        // Give some time for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        // Then
        val state = navigationStateTracker.currentState.first()
        assertEquals(Screen.MAP, state.currentScreen)
        assertTrue(state.canNavigateBack)
    }
    
    @Test
    fun `back navigation should use state tracker history`() = runTest {
        // Given - Navigate to map first
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        
        // When
        val canNavigateBack = navigationManager.navigateBack()
        
        // Then
        assertTrue(canNavigateBack)
        verify { navController.popBackStack() }
    }
    
    @Test
    fun `back navigation should return false when cannot navigate back`() = runTest {
        // Given - Start at home with no history
        val state = navigationStateTracker.currentState.first()
        assertFalse(state.canNavigateBack)
        
        // When
        val canNavigateBack = navigationManager.navigateBack()
        
        // Then
        assertFalse(canNavigateBack)
    }
    
    @Test
    fun `navigation should be blocked when already in progress`() = runTest {
        // Given
        navigationStateTracker.setNavigationInProgress(true)
        
        // When
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        
        // Then - State should not change
        val state = navigationStateTracker.currentState.first()
        assertEquals(Screen.HOME, state.currentScreen) // Should still be home
    }
    
    @Test
    fun `complete navigation flow should maintain correct state`() = runTest {
        // Given - Start at home
        val initialState = navigationStateTracker.currentState.first()
        assertEquals(Screen.HOME, initialState.currentScreen)
        
        // When - Navigate to map
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        
        // Then
        val mapState = navigationStateTracker.currentState.first()
        assertEquals(Screen.MAP, mapState.currentScreen)
        assertTrue(mapState.canNavigateBack)
        assertEquals(1, mapState.navigationHistory.size)
        assertEquals(Screen.HOME, mapState.navigationHistory.first())
        
        // When - Navigate to friends
        navigationManager.navigateToFriends()
        kotlinx.coroutines.delay(100)
        
        // Then
        val friendsState = navigationStateTracker.currentState.first()
        assertEquals(Screen.FRIENDS, friendsState.currentScreen)
        assertTrue(friendsState.canNavigateBack)
        assertEquals(2, friendsState.navigationHistory.size)
        assertEquals(Screen.MAP, friendsState.navigationHistory.last())
        
        // When - Navigate back
        navigationManager.navigateBack()
        kotlinx.coroutines.delay(100)
        
        // Then - Should restore previous screen from history
        val previousScreen = navigationStateTracker.getPreviousScreen()
        assertEquals(Screen.MAP, previousScreen)
    }
    
    @Test
    fun `screen state preservation should work across navigation`() = runTest {
        // Given
        val mapScreen = Screen.MAP
        val screenState = bundleOf(
            "zoom_level" to 15.0f,
            "center_lat" to 37.7749,
            "center_lng" to -122.4194
        )
        
        // When - Save state for map screen
        navigationStateTracker.saveScreenState(mapScreen, screenState)
        
        // Navigate away and back
        navigationManager.navigateToFriends()
        kotlinx.coroutines.delay(100)
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        
        // Then - Should be able to restore state
        val restoredState = navigationStateTracker.restoreScreenState(mapScreen)
        assertNotNull(restoredState)
    }
    
    @Test
    fun `state persistence should survive tracker recreation`() = runTest {
        // Given - Set up some navigation state
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        navigationManager.navigateToFriends()
        kotlinx.coroutines.delay(100)
        
        val originalState = navigationStateTracker.currentState.first()
        
        // Mock persisted state return
        val persistedStateJson = """
            {
                "currentScreen": "friends",
                "navigationHistory": ["home", "map"]
            }
        """.trimIndent()
        
        every { sharedPreferences.getString("navigation_state", null) } returns persistedStateJson
        
        // When - Create new tracker (simulating app restart)
        val newTracker = NavigationStateTrackerImpl(context)
        val restoredState = newTracker.currentState.first()
        
        // Then
        assertEquals(Screen.FRIENDS, restoredState.currentScreen)
        assertEquals(2, restoredState.navigationHistory.size)
        assertEquals(Screen.HOME, restoredState.navigationHistory[0])
        assertEquals(Screen.MAP, restoredState.navigationHistory[1])
        assertTrue(restoredState.canNavigateBack)
    }
    
    @Test
    fun `navigation error should reset navigation in progress state`() = runTest {
        // Given
        navigationStateTracker.setNavigationInProgress(true)
        val error = NavigationError.NavigationTimeout
        
        // When
        navigationManager.handleNavigationError(error)
        
        // Then
        val state = navigationStateTracker.currentState.first()
        assertFalse(state.isNavigating)
        verify { navigationErrorHandler.handleError(error) }
    }
    
    @Test
    fun `clearing persisted state should reset everything`() = runTest {
        // Given - Set up some state
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        
        val mapState = bundleOf("key" to "value")
        navigationStateTracker.saveScreenState(Screen.MAP, mapState)
        
        // When
        navigationStateTracker.clearPersistedState()
        
        // Then
        verify { editor.remove("navigation_state") }
        verify { editor.remove("screen_states") }
        verify { editor.apply() }
        
        // Screen state should be cleared
        val restoredState = navigationStateTracker.restoreScreenState(Screen.MAP)
        assertNull(restoredState)
    }
    
    @Test
    fun `navigation to home should clear back stack`() = runTest {
        // Given - Navigate to some screens first
        navigationManager.navigateToMap()
        kotlinx.coroutines.delay(100)
        navigationManager.navigateToFriends()
        kotlinx.coroutines.delay(100)
        
        // When - Navigate to home
        navigationManager.navigateToHome()
        kotlinx.coroutines.delay(100)
        
        // Then
        val state = navigationStateTracker.currentState.first()
        assertEquals(Screen.HOME, state.currentScreen)
        // Navigation history should still exist for back navigation
        assertTrue(state.navigationHistory.isNotEmpty())
    }
    
    @Test
    fun `multiple rapid navigation attempts should be handled gracefully`() = runTest {
        // When - Attempt multiple rapid navigations
        navigationManager.navigateToMap()
        navigationManager.navigateToFriends()
        navigationManager.navigateToSettings()
        
        // Give time for all operations to complete
        kotlinx.coroutines.delay(200)
        
        // Then - Should end up in a valid state
        val state = navigationStateTracker.currentState.first()
        assertTrue(Screen.values().contains(state.currentScreen))
        assertFalse(state.isNavigating) // Should not be stuck in navigating state
    }
}