package com.locationsharing.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.os.bundleOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Performance tests for navigation state management.
 * Ensures navigation operations complete within acceptable time limits.
 */
class NavigationStateManagementPerformanceTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var navigationStateTracker: NavigationStateTrackerImpl
    
    companion object {
        private const val MAX_OPERATION_TIME_MS = 50L
        private const val MAX_PERSISTENCE_TIME_MS = 100L
        private const val LARGE_HISTORY_SIZE = 1000
    }
    
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
    fun `updateCurrentScreen should complete quickly`() = runTest {
        // When
        val executionTime = measureTimeMillis {
            navigationStateTracker.updateCurrentScreen(Screen.MAP)
        }
        
        // Then
        assertTrue(
            executionTime < MAX_OPERATION_TIME_MS,
            "updateCurrentScreen took ${executionTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `recordNavigation should complete quickly`() = runTest {
        // When
        val executionTime = measureTimeMillis {
            navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        }
        
        // Then
        assertTrue(
            executionTime < MAX_OPERATION_TIME_MS,
            "recordNavigation took ${executionTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `state access should be fast`() = runTest {
        // Given
        navigationStateTracker.updateCurrentScreen(Screen.MAP)
        
        // When
        val executionTime = measureTimeMillis {
            navigationStateTracker.currentState.first()
        }
        
        // Then
        assertTrue(
            executionTime < MAX_OPERATION_TIME_MS,
            "State access took ${executionTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `large navigation history should not impact performance`() = runTest {
        // Given - Create large navigation history
        repeat(LARGE_HISTORY_SIZE) { index ->
            val fromScreen = if (index % 2 == 0) Screen.HOME else Screen.MAP
            val toScreen = if (index % 2 == 0) Screen.MAP else Screen.FRIENDS
            navigationStateTracker.recordNavigation(fromScreen, toScreen)
        }
        
        // When
        val executionTime = measureTimeMillis {
            navigationStateTracker.updateCurrentScreen(Screen.SETTINGS)
            navigationStateTracker.currentState.first()
        }
        
        // Then
        assertTrue(
            executionTime < MAX_OPERATION_TIME_MS,
            "Operations with large history took ${executionTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `screen state operations should be performant`() = runTest {
        // Given
        val screenState = bundleOf(
            "key1" to "value1",
            "key2" to 42,
            "key3" to true,
            "key4" to 3.14f
        )
        
        // When - Save state
        val saveTime = measureTimeMillis {
            navigationStateTracker.saveScreenState(Screen.MAP, screenState)
        }
        
        // When - Restore state
        val restoreTime = measureTimeMillis {
            navigationStateTracker.restoreScreenState(Screen.MAP)
        }
        
        // Then
        assertTrue(
            saveTime < MAX_OPERATION_TIME_MS,
            "saveScreenState took ${saveTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
        assertTrue(
            restoreTime < MAX_OPERATION_TIME_MS,
            "restoreScreenState took ${restoreTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `multiple screen states should not impact performance`() = runTest {
        // Given - Save state for all screens
        Screen.values().forEach { screen ->
            val state = bundleOf(
                "screen" to screen.route,
                "timestamp" to System.currentTimeMillis(),
                "data" to "test_data_for_${screen.route}"
            )
            navigationStateTracker.saveScreenState(screen, state)
        }
        
        // When
        val executionTime = measureTimeMillis {
            Screen.values().forEach { screen ->
                navigationStateTracker.restoreScreenState(screen)
            }
        }
        
        // Then
        assertTrue(
            executionTime < MAX_OPERATION_TIME_MS,
            "Multiple screen state operations took ${executionTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `persistence operations should complete within reasonable time`() = runTest {
        // Given - Set up some state
        navigationStateTracker.updateCurrentScreen(Screen.MAP)
        navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        navigationStateTracker.recordNavigation(Screen.MAP, Screen.FRIENDS)
        
        // When
        val persistTime = measureTimeMillis {
            navigationStateTracker.persistState()
        }
        
        // Then
        assertTrue(
            persistTime < MAX_PERSISTENCE_TIME_MS,
            "persistState took ${persistTime}ms, expected < ${MAX_PERSISTENCE_TIME_MS}ms"
        )
    }
    
    @Test
    fun `clearing operations should be fast`() = runTest {
        // Given - Set up some state
        navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        navigationStateTracker.saveScreenState(Screen.MAP, bundleOf("key" to "value"))
        
        // When - Clear history
        val clearHistoryTime = measureTimeMillis {
            navigationStateTracker.clearHistory()
        }
        
        // When - Clear screen state
        val clearScreenStateTime = measureTimeMillis {
            navigationStateTracker.clearScreenState(Screen.MAP)
        }
        
        // When - Clear all persisted state
        val clearPersistedTime = measureTimeMillis {
            navigationStateTracker.clearPersistedState()
        }
        
        // Then
        assertTrue(
            clearHistoryTime < MAX_OPERATION_TIME_MS,
            "clearHistory took ${clearHistoryTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
        assertTrue(
            clearScreenStateTime < MAX_OPERATION_TIME_MS,
            "clearScreenState took ${clearScreenStateTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
        assertTrue(
            clearPersistedTime < MAX_OPERATION_TIME_MS,
            "clearPersistedState took ${clearPersistedTime}ms, expected < ${MAX_OPERATION_TIME_MS}ms"
        )
    }
    
    @Test
    fun `concurrent state updates should not cause performance degradation`() = runTest {
        // When - Perform multiple concurrent operations
        val executionTime = measureTimeMillis {
            repeat(100) { index ->
                when (index % 4) {
                    0 -> navigationStateTracker.updateCurrentScreen(Screen.MAP)
                    1 -> navigationStateTracker.recordNavigation(Screen.HOME, Screen.FRIENDS)
                    2 -> navigationStateTracker.setNavigationInProgress(index % 2 == 0)
                    3 -> navigationStateTracker.currentState.first()
                }
            }
        }
        
        // Then
        assertTrue(
            executionTime < 500L, // Allow more time for 100 operations
            "100 concurrent operations took ${executionTime}ms, expected < 500ms"
        )
    }
    
    @Test
    fun `initialization with persisted state should be fast`() = runTest {
        // Given - Mock persisted state
        val persistedStateJson = """
            {
                "currentScreen": "map",
                "navigationHistory": ["home", "friends", "settings"]
            }
        """.trimIndent()
        
        every { sharedPreferences.getString("navigation_state", null) } returns persistedStateJson
        
        // When
        val initTime = measureTimeMillis {
            NavigationStateTrackerImpl(context)
        }
        
        // Then
        assertTrue(
            initTime < MAX_PERSISTENCE_TIME_MS,
            "Initialization with persisted state took ${initTime}ms, expected < ${MAX_PERSISTENCE_TIME_MS}ms"
        )
    }
    
    @Test
    fun `memory usage should remain stable with extensive use`() = runTest {
        // Given - Perform many operations
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // When - Perform extensive operations
        repeat(1000) { index ->
            navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
            navigationStateTracker.updateCurrentScreen(Screen.values()[index % Screen.values().size])
            navigationStateTracker.saveScreenState(
                Screen.values()[index % Screen.values().size],
                bundleOf("iteration" to index)
            )
            
            // Periodically clear to simulate real usage
            if (index % 100 == 0) {
                navigationStateTracker.clearHistory()
            }
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Then - Memory increase should be reasonable (less than 10MB)
        assertTrue(
            memoryIncrease < 10 * 1024 * 1024,
            "Memory increased by ${memoryIncrease / 1024 / 1024}MB after extensive use"
        )
    }
}