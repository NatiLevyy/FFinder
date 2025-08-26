package com.locationsharing.app.navigation.debug

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.Screen
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the navigation debug overlay components.
 */
@RunWith(AndroidJUnit4::class)
class NavigationDebugOverlayUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun debugOverlay_shouldShowToggleButton() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo()
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        // When
        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldShowMiniOverlayWhenToggled() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo(
            totalNavigations = 5,
            errorCount = 2,
            successRate = 80f
        )
        
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Current: Map")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("History: 1")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Errors: 2")
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldShowFullDialogWhenExpanded() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.FRIENDS,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME, Screen.MAP),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo(
            totalNavigations = 10,
            errorCount = 1,
            successRate = 90f,
            averageNavigationTime = 250L,
            memoryUsage = 45L
        )
        
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("Navigation Debug Console")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Current State")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Performance Metrics")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Navigation History")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Debug Controls")
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldShowCurrentStateInformation() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = true
        )
        
        val debugInfo = NavigationDebugInfo()
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("Map")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("true") // Can navigate back
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("1") // History size
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldShowPerformanceMetrics() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo(
            totalNavigations = 25,
            errorCount = 3,
            successRate = 88f,
            averageNavigationTime = 450L,
            memoryUsage = 67L
        )
        
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("450ms") // Average navigation time
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("25") // Total navigations
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("88%") // Success rate
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("67MB") // Memory usage
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldShowNavigationHistory() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.FRIENDS,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME, Screen.MAP, Screen.SETTINGS),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo()
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("3. Settings")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("2. Map")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("1. Home")
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldShowErrorInformation() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        val recentErrors = listOf(
            NavigationDebugError(
                type = "NavigationTimeout",
                message = "Navigation timed out after 5 seconds",
                timestamp = "12:34:56.789",
                fromScreen = "home",
                toScreen = "map"
            ),
            NavigationDebugError(
                type = "InvalidRoute",
                message = "Route not found: invalid_screen",
                timestamp = "12:35:12.456",
                fromScreen = "map",
                toScreen = "invalid_screen"
            )
        )
        
        val debugInfo = NavigationDebugInfo(
            errorCount = 2,
            recentErrors = recentErrors
        )
        
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("NavigationTimeout")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Navigation timed out after 5 seconds")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("InvalidRoute")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Route not found: invalid_screen")
            .assertIsDisplayed()
    }

    @Test
    fun debugOverlay_shouldHandleDebugControlInteractions() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo(isErrorSimulationEnabled = false)
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()
        
        var errorSimulationToggled = false
        var timeoutSimulated = false

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = { errorSimulationToggled = true },
                    onClearHistory = {},
                    onSimulateTimeout = { timeoutSimulated = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText("Error Simulation")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Simulate Timeout")
            .assertIsDisplayed()

        // When - Click error simulation toggle
        composeTestRule
            .onNodeWithText("Error Simulation")
            .performClick()

        // Then
        assert(errorSimulationToggled)

        // When - Click simulate timeout
        composeTestRule
            .onNodeWithText("Simulate Timeout")
            .performClick()

        // Then
        assert(timeoutSimulated)
    }

    @Test
    fun debugOverlay_shouldHandleClearHistoryAction() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME, Screen.FRIENDS),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo()
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()
        
        var historyCleared = false

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = { historyCleared = true },
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Find and click the Clear button in Navigation History section
        composeTestRule
            .onAllNodesWithText("Clear")
            .onFirst()
            .performClick()

        // Then
        assert(historyCleared)
    }

    @Test
    fun debugOverlay_shouldCloseWhenDismissed() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        val debugInfo = NavigationDebugInfo()
        val stateInspector = NavigationStateInspector()
        val performanceProfiler = NavigationPerformanceProfiler()
        val errorSimulator = NavigationErrorSimulator()
        val debugUtils = NavigationDebugUtils()

        composeTestRule.setContent {
            FFinderTheme {
                NavigationDebugOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onToggleErrorSimulation = {},
                    onClearHistory = {},
                    onSimulateTimeout = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Show Debug Overlay")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Navigation Debug")
            .performClick()

        // Verify dialog is shown
        composeTestRule
            .onNodeWithText("Navigation Debug Console")
            .assertIsDisplayed()

        // When - Close dialog
        composeTestRule
            .onNodeWithContentDescription("Close Debug Console")
            .performClick()

        // Then - Dialog should be dismissed
        composeTestRule
            .onNodeWithText("Navigation Debug Console")
            .assertDoesNotExist()
    }
}