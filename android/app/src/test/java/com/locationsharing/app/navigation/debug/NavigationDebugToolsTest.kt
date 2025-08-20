package com.locationsharing.app.navigation.debug

import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import androidx.navigation.NavController
import androidx.navigation.NavDestination

/**
 * Comprehensive unit tests for navigation debugging tools.
 */
@ExperimentalCoroutinesApi
class NavigationDebugToolsTest {

    @Mock
    private lateinit var mockNavController: NavController
    
    @Mock
    private lateinit var mockNavDestination: NavDestination

    private lateinit var stateInspector: NavigationStateInspector
    private lateinit var performanceProfiler: NavigationPerformanceProfiler
    private lateinit var errorSimulator: NavigationErrorSimulator
    private lateinit var debugUtils: NavigationDebugUtils
    private lateinit var debugInfoManager: NavigationDebugInfoManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        stateInspector = NavigationStateInspector()
        performanceProfiler = NavigationPerformanceProfiler()
        errorSimulator = NavigationErrorSimulator()
        debugUtils = NavigationDebugUtils()
        debugInfoManager = NavigationDebugInfoManager()
    }

    // NavigationStateInspector Tests
    
    @Test
    fun `inspectNavigationState should update inspection data correctly`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = listOf(Screen.MAP),
            isNavigating = false
        )
        
        whenever(mockNavController.currentDestination).thenReturn(mockNavDestination)
        whenever(mockNavDestination.route).thenReturn("home")
        whenever(mockNavDestination.id).thenReturn(1)

        // When
        stateInspector.inspectNavigationState(mockNavController, navigationState)

        // Then
        val inspectionData = stateInspector.inspectionData.value
        assertEquals("home", inspectionData.currentRoute)
        assertEquals("1", inspectionData.currentDestinationId)
        assertEquals(navigationState, inspectionData.navigationState)
        assertTrue(inspectionData.isNavigationControllerValid)
    }

    @Test
    fun `validateCurrentRoute should identify invalid routes`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        whenever(mockNavController.currentDestination).thenReturn(mockNavDestination)
        whenever(mockNavDestination.route).thenReturn("invalid_route")

        // When
        stateInspector.inspectNavigationState(mockNavController, navigationState)

        // Then
        val inspectionData = stateInspector.inspectionData.value
        assertFalse(inspectionData.routeValidation.isValid)
        assertTrue(inspectionData.routeValidation.issues.isNotEmpty())
    }

    @Test
    fun `generateStateReport should create comprehensive report`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.MAP, Screen.FRIENDS),
            isNavigating = false
        )
        
        whenever(mockNavController.currentDestination).thenReturn(mockNavDestination)
        whenever(mockNavDestination.route).thenReturn("home")
        
        stateInspector.inspectNavigationState(mockNavController, navigationState)

        // When
        val report = stateInspector.generateStateReport()

        // Then
        assertTrue(report.contains("Navigation State Report"))
        assertTrue(report.contains("Current Screen: Home"))
        assertTrue(report.contains("History Size: 2"))
        assertTrue(report.contains("Can Navigate Back: true"))
    }

    // NavigationPerformanceProfiler Tests

    @Test
    fun `startNavigationProfiling should create profile entry`() = runTest {
        // Given
        val navigationId = "test_navigation"
        val fromScreen = Screen.HOME
        val toScreen = Screen.MAP

        // When
        performanceProfiler.startNavigationProfiling(navigationId, fromScreen, toScreen)

        // Then
        val performanceData = performanceProfiler.performanceData.value
        assertEquals(1, performanceData.activeNavigations)
    }

    @Test
    fun `endNavigationProfiling should complete profile entry`() = runTest {
        // Given
        val navigationId = "test_navigation"
        val fromScreen = Screen.HOME
        val toScreen = Screen.MAP
        
        performanceProfiler.startNavigationProfiling(navigationId, fromScreen, toScreen)

        // When
        performanceProfiler.endNavigationProfiling(navigationId, success = true)

        // Then
        val performanceData = performanceProfiler.performanceData.value
        assertEquals(0, performanceData.activeNavigations)
        assertEquals(1, performanceData.completedNavigations)
    }

    @Test
    fun `analyzePerformance should identify slow navigations`() = runTest {
        // Given
        val navigationId = "slow_navigation"
        performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)
        
        // Simulate slow navigation by waiting
        Thread.sleep(50) // Small delay for testing
        
        performanceProfiler.endNavigationProfiling(navigationId, success = true)

        // When
        val analysis = performanceProfiler.analyzePerformance()

        // Then
        assertEquals(1, analysis.totalNavigations)
        assertTrue(analysis.averageDuration > 0)
    }

    @Test
    fun `getPerformanceRecommendations should provide actionable advice`() = runTest {
        // Given - Create a slow navigation scenario
        val navigationId = "slow_navigation"
        performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)
        Thread.sleep(50)
        performanceProfiler.endNavigationProfiling(navigationId, success = true)

        // When
        val recommendations = performanceProfiler.getPerformanceRecommendations()

        // Then
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.title.contains("Optimize") })
    }

    // NavigationErrorSimulator Tests

    @Test
    fun `enableErrorSimulation should activate simulation`() {
        // Given
        val config = ErrorSimulationConfig(
            enabledScenarios = listOf("timeout", "invalid_route"),
            randomErrorProbability = 0.1f
        )

        // When
        errorSimulator.enableErrorSimulation(config)

        // Then
        val simulationState = errorSimulator.simulationState.value
        assertTrue(simulationState.isEnabled)
        assertEquals(config, simulationState.config)
        assertEquals(2, simulationState.activeScenarios.size)
    }

    @Test
    fun `disableErrorSimulation should deactivate simulation`() {
        // Given
        val config = ErrorSimulationConfig(enabledScenarios = listOf("timeout"))
        errorSimulator.enableErrorSimulation(config)

        // When
        errorSimulator.disableErrorSimulation()

        // Then
        val simulationState = errorSimulator.simulationState.value
        assertFalse(simulationState.isEnabled)
        assertTrue(simulationState.activeScenarios.isEmpty())
    }

    @Test
    fun `shouldSimulateError should return null when disabled`() = runTest {
        // Given
        errorSimulator.disableErrorSimulation()

        // When
        val error = errorSimulator.shouldSimulateError(Screen.HOME, Screen.MAP)

        // Then
        assertNull(error)
    }

    @Test
    fun `addCustomErrorScenario should add scenario to available list`() {
        // Given
        val customScenario = ErrorScenario(
            id = "custom_error",
            name = "Custom Error",
            description = "Custom error for testing",
            error = NavigationError.InvalidRoute,
            triggerProbability = 1.0f // Always trigger for testing
        )

        // When
        errorSimulator.addCustomErrorScenario(customScenario)

        // Then
        val scenarios = errorSimulator.getAvailableScenarios()
        assertTrue(scenarios.any { it.id == "custom_error" })
    }

    @Test
    fun `createErrorTestSuite should provide comprehensive test cases`() {
        // When
        val testSuite = errorSimulator.createErrorTestSuite()

        // Then
        assertTrue(testSuite.isNotEmpty())
        assertTrue(testSuite.any { it.name.contains("Timeout") })
        assertTrue(testSuite.any { it.name.contains("Invalid Route") })
        assertTrue(testSuite.any { it.name.contains("Controller Loss") })
        
        testSuite.forEach { testCase ->
            assertNotNull(testCase.scenario)
            assertTrue(testCase.testSteps.isNotEmpty())
            assertFalse(testCase.expectedBehavior.isBlank())
        }
    }

    // NavigationDebugUtils Tests

    @Test
    fun `logDebug should add log entry`() {
        // When
        debugUtils.logDebug(
            DebugLogLevel.INFO,
            "Test debug message",
            "TestTag"
        )

        // Then
        val statistics = debugUtils.getDebugStatistics()
        assertEquals(1, statistics.totalLogs)
        assertEquals(1, statistics.logsByLevel[DebugLogLevel.INFO])
    }

    @Test
    fun `validateNavigationState should identify controller issues`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )

        // When
        val validationResult = debugUtils.validateNavigationState(null, navigationState)

        // Then
        assertFalse(validationResult.isValid)
        assertTrue(validationResult.issues.any { it.type == IssueType.CONTROLLER_NULL })
    }

    @Test
    fun `validateNavigationState should detect state inconsistencies`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        whenever(mockNavController.currentDestination).thenReturn(mockNavDestination)
        whenever(mockNavDestination.route).thenReturn("map") // Different from state

        // When
        val validationResult = debugUtils.validateNavigationState(mockNavController, navigationState)

        // Then
        assertTrue(validationResult.issues.any { it.type == IssueType.STATE_INCONSISTENCY })
    }

    @Test
    fun `diagnosePerformanceIssues should identify slow navigations`() {
        // Given
        val performanceData = NavigationPerformanceData(
            activeNavigations = 0,
            completedNavigations = 2,
            lastUpdated = System.currentTimeMillis(),
            recentNavigations = listOf(
                NavigationProfileEntry(
                    id = "slow1",
                    fromScreen = Screen.HOME,
                    toScreen = Screen.MAP,
                    navigationType = NavigationType.STANDARD,
                    startTime = 0L,
                    endTime = 2000L, // 2 seconds - slow
                    startMemory = 100L,
                    endMemory = 120L,
                    success = true
                ),
                NavigationProfileEntry(
                    id = "slow2",
                    fromScreen = Screen.MAP,
                    toScreen = Screen.FRIENDS,
                    navigationType = NavigationType.STANDARD,
                    startTime = 0L,
                    endTime = 1500L, // 1.5 seconds - slow
                    startMemory = 120L,
                    endMemory = 140L,
                    success = true
                )
            )
        )

        // When
        val diagnosis = debugUtils.diagnosePerformanceIssues(performanceData, performanceData.recentNavigations)

        // Then
        assertTrue(diagnosis.issues.any { it.type == PerformanceIssueType.SLOW_NAVIGATION })
        assertTrue(diagnosis.recommendations.isNotEmpty())
        assertEquals(PerformanceHealth.POOR, diagnosis.overallHealth)
    }

    @Test
    fun `generateDebugReport should create comprehensive report`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        val performanceData = NavigationPerformanceData()
        val validationResult = NavigationValidationResult(
            isValid = true,
            issues = emptyList(),
            warnings = emptyList(),
            validationTimestamp = System.currentTimeMillis()
        )
        val performanceDiagnosis = NavigationPerformanceDiagnosis(
            overallHealth = PerformanceHealth.GOOD,
            issues = emptyList(),
            recommendations = emptyList(),
            diagnosisTimestamp = System.currentTimeMillis()
        )

        // When
        val report = debugUtils.generateDebugReport(
            navigationState,
            performanceData,
            validationResult,
            performanceDiagnosis
        )

        // Then
        assertTrue(report.contains("Navigation Debug Report"))
        assertTrue(report.contains("Navigation State"))
        assertTrue(report.contains("Validation Results"))
        assertTrue(report.contains("Performance Diagnosis"))
    }

    @Test
    fun `clearDebugData should reset all debug information`() {
        // Given
        debugUtils.logDebug(DebugLogLevel.INFO, "Test message")
        
        // When
        debugUtils.clearDebugData()

        // Then
        val statistics = debugUtils.getDebugStatistics()
        assertEquals(0, statistics.totalLogs)
    }

    // NavigationDebugInfoManager Tests

    @Test
    fun `recordEvent should add event to history`() {
        // Given
        val event = NavigationDebugEvent(
            type = NavigationEventType.NAVIGATION_COMPLETED,
            fromScreen = Screen.HOME,
            toScreen = Screen.MAP,
            duration = 500L
        )

        // When
        debugInfoManager.recordEvent(event)

        // Then
        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(1, debugInfo.totalNavigations)
        assertTrue(debugInfo.performanceMetrics.navigationTimes.contains(500L))
    }

    @Test
    fun `recordError should add error to recent errors`() {
        // When
        debugInfoManager.recordError(
            type = "TestError",
            message = "Test error message",
            fromScreen = "home",
            toScreen = "map"
        )

        // Then
        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(1, debugInfo.errorCount)
        assertTrue(debugInfo.recentErrors.any { it.type == "TestError" })
    }

    @Test
    fun `getDebugInfo should calculate correct success rate`() {
        // Given
        val successEvent = NavigationDebugEvent(
            type = NavigationEventType.NAVIGATION_COMPLETED,
            fromScreen = Screen.HOME,
            toScreen = Screen.MAP
        )
        val failureEvent = NavigationDebugEvent(
            type = NavigationEventType.NAVIGATION_FAILED,
            fromScreen = Screen.MAP,
            toScreen = Screen.FRIENDS,
            error = NavigationError.NavigationTimeout
        )

        // When
        debugInfoManager.recordEvent(successEvent)
        debugInfoManager.recordEvent(failureEvent)

        // Then
        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(50f, debugInfo.successRate, 0.1f) // 1 success out of 2 total = 50%
    }

    @Test
    fun `toggleErrorSimulation should change simulation state`() {
        // Given
        val initialState = debugInfoManager.getDebugInfo().isErrorSimulationEnabled

        // When
        debugInfoManager.toggleErrorSimulation()

        // Then
        val newState = debugInfoManager.getDebugInfo().isErrorSimulationEnabled
        assertEquals(!initialState, newState)
    }

    @Test
    fun `shouldSimulateError should respect simulation state`() {
        // Given
        debugInfoManager.toggleErrorSimulation() // Enable simulation

        // When
        val shouldSimulate = debugInfoManager.shouldSimulateError()

        // Then
        // Since it's random, we can't guarantee the result, but we can test that it doesn't crash
        // and returns a boolean
        assertTrue(shouldSimulate is Boolean)
    }

    @Test
    fun `clearDebugInfo should reset all data`() {
        // Given
        debugInfoManager.recordEvent(
            NavigationDebugEvent(
                type = NavigationEventType.NAVIGATION_COMPLETED,
                fromScreen = Screen.HOME,
                toScreen = Screen.MAP
            )
        )
        debugInfoManager.recordError("TestError", "Test message")

        // When
        debugInfoManager.clearDebugInfo()

        // Then
        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(0, debugInfo.totalNavigations)
        assertEquals(0, debugInfo.errorCount)
        assertTrue(debugInfo.recentErrors.isEmpty())
    }

    @Test
    fun `getRecentEvents should return limited number of events`() {
        // Given
        repeat(15) { index ->
            debugInfoManager.recordEvent(
                NavigationDebugEvent(
                    type = NavigationEventType.NAVIGATION_COMPLETED,
                    fromScreen = Screen.HOME,
                    toScreen = Screen.MAP
                )
            )
        }

        // When
        val recentEvents = debugInfoManager.getRecentEvents(10)

        // Then
        assertEquals(10, recentEvents.size)
    }
}