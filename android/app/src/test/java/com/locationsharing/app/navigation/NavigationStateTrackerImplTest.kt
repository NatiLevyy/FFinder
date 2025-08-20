package com.locationsharing.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.os.bundleOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
 * Unit tests for NavigationStateTrackerImpl.
 * Tests navigation state management, persistence, and screen state handling.
 */
class NavigationStateTrackerImplTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var navigationStateTracker: NavigationStateTrackerImpl
    
    @Before
    fun setUp() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { sharedPreferences.getString(any(), any()) } returns null
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
        
        navigationStateTracker = NavigationStateTrackerImpl(context)
    }
    
    @Test
    fun `initial state should be default home state`() = runTest {
        // When
        val initialState = navigationStateTracker.currentState.first()
        
        // Then
        assertEquals(Screen.HOME, initialState.currentScreen)
        assertFalse(initialState.canNavigateBack)
        assertTrue(initialState.navigationHistory.isEmpty())
        assertFalse(initialState.isNavigating)
    }
    
    @Test
    fun `updateCurrentScreen should update current screen and persist state`() = runTest {
        // Given
        val targetScreen = Screen.MAP
        
        // When
        navigationStateTracker.updateCurrentScreen(targetScreen)
        val updatedState = navigationStateTracker.currentState.first()
        
        // Then
        assertEquals(targetScreen, updatedState.currentScreen)
        assertTrue(updatedState.canNavigateBack)
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `updateCurrentScreen should not update if screen is already current`() = runTest {
        // Given
        val currentScreen = Screen.HOME
        
        // When
        navigationStateTracker.updateCurrentScreen(currentScreen)
        
        // Then
        val state = navigationStateTracker.currentState.first()
        assertEquals(currentScreen, state.currentScreen)
        // Should not persist since no change occurred
    }
    
    @Test
    fun `recordNavigation should add to history and persist state`() = runTest {
        // Given
        val fromScreen = Screen.HOME
        val toScreen = Screen.MAP
        
        // When
        navigationStateTracker.recordNavigation(fromScreen, toScreen)
        val updatedState = navigationStateTracker.currentState.first()
        
        // Then
        assertEquals(1, updatedState.navigationHistory.size)
        assertEquals(fromScreen, updatedState.navigationHistory.first())
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `recordNavigation should limit history size`() = runTest {
        // Given - Add more than MAX_HISTORY_SIZE items
        repeat(15) { index ->
            navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        }
        
        // When
        val state = navigationStateTracker.currentState.first()
        
        // Then
        assertTrue(state.navigationHistory.size <= 10) // MAX_HISTORY_SIZE
    }
    
    @Test
    fun `setNavigationInProgress should update navigation state`() = runTest {
        // When
        navigationStateTracker.setNavigationInProgress(true)
        val state = navigationStateTracker.currentState.first()
        
        // Then
        assertTrue(state.isNavigating)
        
        // When
        navigationStateTracker.setNavigationInProgress(false)
        val updatedState = navigationStateTracker.currentState.first()
        
        // Then
        assertFalse(updatedState.isNavigating)
    }
    
    @Test
    fun `clearHistory should clear navigation history and persist state`() = runTest {
        // Given - Add some history
        navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        navigationStateTracker.recordNavigation(Screen.MAP, Screen.FRIENDS)
        
        // When
        navigationStateTracker.clearHistory()
        val state = navigationStateTracker.currentState.first()
        
        // Then
        assertTrue(state.navigationHistory.isEmpty())
        assertFalse(state.canNavigateBack)
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getPreviousScreen should return last screen in history`() = runTest {
        // Given
        navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        navigationStateTracker.recordNavigation(Screen.MAP, Screen.FRIENDS)
        
        // When
        val previousScreen = navigationStateTracker.getPreviousScreen()
        
        // Then
        assertEquals(Screen.MAP, previousScreen)
    }
    
    @Test
    fun `getPreviousScreen should return null when history is empty`() = runTest {
        // When
        val previousScreen = navigationStateTracker.getPreviousScreen()
        
        // Then
        assertNull(previousScreen)
    }
    
    @Test
    fun `saveScreenState should store screen state and persist`() = runTest {
        // Given
        val screen = Screen.MAP
        val state = bundleOf("key" to "value")
        
        // When
        navigationStateTracker.saveScreenState(screen, state)
        
        // Then
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `restoreScreenState should return saved state`() = runTest {
        // Given
        val screen = Screen.MAP
        val savedState = bundleOf("key" to "value")
        navigationStateTracker.saveScreenState(screen, savedState)
        
        // When
        val restoredState = navigationStateTracker.restoreScreenState(screen)
        
        // Then
        assertNotNull(restoredState)
    }
    
    @Test
    fun `restoreScreenState should return null for non-existent state`() = runTest {
        // When
        val restoredState = navigationStateTracker.restoreScreenState(Screen.MAP)
        
        // Then
        assertNull(restoredState)
    }
    
    @Test
    fun `clearScreenState should remove saved state and persist`() = runTest {
        // Given
        val screen = Screen.MAP
        val state = bundleOf("key" to "value")
        navigationStateTracker.saveScreenState(screen, state)
        
        // When
        navigationStateTracker.clearScreenState(screen)
        val restoredState = navigationStateTracker.restoreScreenState(screen)
        
        // Then
        assertNull(restoredState)
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `persistState should save current state to SharedPreferences`() = runTest {
        // Given
        navigationStateTracker.updateCurrentScreen(Screen.MAP)
        navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        
        // When
        navigationStateTracker.persistState()
        
        // Then
        verify { editor.putString(any(), any()) }
        verify { editor.apply() }
    }
    
    @Test
    fun `clearPersistedState should remove all persisted data`() = runTest {
        // When
        navigationStateTracker.clearPersistedState()
        
        // Then
        verify { editor.remove(any()) }
        verify(atLeast = 2) { editor.remove(any()) } // Should remove both navigation state and screen states
        verify { editor.apply() }
    }
    
    @Test
    fun `should load persisted state on initialization`() = runTest {
        // Given
        val persistedStateJson = """
            {
                "currentScreen": "map",
                "navigationHistory": ["home"]
            }
        """.trimIndent()
        
        every { sharedPreferences.getString("navigation_state", null) } returns persistedStateJson
        
        // When
        val newTracker = NavigationStateTrackerImpl(context)
        val state = newTracker.currentState.first()
        
        // Then
        assertEquals(Screen.MAP, state.currentScreen)
        assertEquals(1, state.navigationHistory.size)
        assertEquals(Screen.HOME, state.navigationHistory.first())
        assertTrue(state.canNavigateBack)
    }
    
    @Test
    fun `should handle corrupted persisted state gracefully`() = runTest {
        // Given
        every { sharedPreferences.getString("navigation_state", null) } returns "invalid json"
        
        // When
        val newTracker = NavigationStateTrackerImpl(context)
        val state = newTracker.currentState.first()
        
        // Then - Should fall back to default state
        assertEquals(Screen.HOME, state.currentScreen)
        assertFalse(state.canNavigateBack)
        assertTrue(state.navigationHistory.isEmpty())
    }
    
    @Test
    fun `canNavigateBack should be true for non-home screens`() = runTest {
        // When
        navigationStateTracker.updateCurrentScreen(Screen.MAP)
        val state = navigationStateTracker.currentState.first()
        
        // Then
        assertTrue(state.canNavigateBack)
    }
    
    @Test
    fun `canNavigateBack should be true for home screen with history`() = runTest {
        // Given
        navigationStateTracker.recordNavigation(Screen.MAP, Screen.HOME)
        navigationStateTracker.updateCurrentScreen(Screen.HOME)
        val state = navigationStateTracker.currentState.first()
        
        // Then
        assertTrue(state.canNavigateBack)
    }
    
    @Test
    fun `canNavigateBack should be false for home screen without history`() = runTest {
        // Given - Default state
        val state = navigationStateTracker.currentState.first()
        
        // Then
        assertFalse(state.canNavigateBack)
    }
}