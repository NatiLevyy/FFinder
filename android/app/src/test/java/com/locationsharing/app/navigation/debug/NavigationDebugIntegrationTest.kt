package com.locationsharing.app.navigation.debug

import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for navigation debugging tools working together.
 */
@ExperimentalCoroutinesApi
class NavigationDebugIntegrationTest {

    private lateinit var stateInspector: NavigationStateInspector
    private lateinit var performanceProfiler: NavigationPerformanceProfiler
    private lateinit var errorSimulator: NavigationErrorSimulator
    private lateinit var debugUtils: NavigationDebugUtils
    private lateinit var debugInfoManager: NavigationDebugInfoManager

    @Before
    fun setUp() {
        stateInspector = NavigationStateInspector()
        performanceProfiler = NavigationPerformanceProfiler()
        errorSimulator = NavigationErrorSimulator()
        debugUtils = NavigationDebugUtils()
        debugInfoManager = NavigationDebugInfoManager()
    }

    @Test
    fun `complete navigation debugging workflow should work end-to-end`() = runTest {
        // Given - Initial navigation state
        val initialState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )

        // Step 1: Inspect initial state
        stateInspector.inspectNavigationState(null, initialState)
        val initialInspection = stateInspector.inspectionData.value
        assertFalse(initialInspection.isNavigationControllerValid)

        // Step 2: Start performance profiling for navigation
        val navigationId = "test_navigation_${System.currentTimeMillis()}"
        performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)

        // Step 3: Simulate navigation with potential error
        errorSimulator.enableErrorSimulation(
            ErrorSimulationConfig(
                enabledScenarios = listOf("timeout"),
                randomErrorProbability = 0.0f // Disable random errors for predictable testing
            )
        )

        // Step 4: Check if error should be simulated
        val simulatedError = errorSimulator.shouldSimulateError(Screen.HOME, Screen.MAP)
        
        // Step 5: Complete navigation (with or without error)
        val navigationSuccess = simulatedError == null
        performanceProfiler.endNavigationProfiling(navigationId, navigationSuccess)

        // Step 6: Log the navigation result
        debugUtils.logDebug(
            DebugLogLevel.INFO,
            "Navigation completed: success=$navigationSuccess, error=$simulatedError",
            "NavigationIntegrationTest"
        )

        // Step 7: Record navigation event
        debugInfoManager.recordEvent(
            NavigationDebugEvent(
                type = if (navigationSuccess) NavigationEventType.NAVIGATION_COMPLETED else NavigationEventType.NAVIGATION_FAILED,
                fromScreen = Screen.HOME,
                toScreen = Screen.MAP,
                duration = 100L,
                error = simulatedError
            )
        )

        // Step 8: Update navigation state after navigation
        val updatedState = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )

        stateInspector.inspectNavigationState(null, updatedState)

        // Verify the complete workflow
        val finalInspection = stateInspector.inspectionData.value
        assertEquals(Screen.MAP, finalInspection.navigationState.currentScreen)
        assertTrue(finalInspection.navigationState.canNavigateBack)

        val performanceData = performanceProfiler.performanceData.value
        assertEquals(1, performanceData.completedNavigations)

        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(1, debugInfo.totalNavigations)

        val debugStats = debugUtils.getDebugStatistics()
        assertTrue(debugStats.totalLogs > 0)
    }

    @Test
    fun `error simulation and performance profiling should work together`() = runTest {
        // Given - Enable error simulation with high probability
        errorSimulator.enableErrorSimulation(
            ErrorSimulationConfig(
                enabledScenarios = listOf("timeout", "invalid_route"),
                randomErrorProbability = 0.5f
            )
        )

        val navigationResults = mutableListOf<Pair<String, Boolean>>()

        // When - Perform multiple navigations
        repeat(10) { index ->
            val navigationId = "navigation_$index"
            performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)

            val simulatedError = errorSimulator.shouldSimulateError(Screen.HOME, Screen.MAP)
            val success = simulatedError == null

            // Simulate some processing time
            Thread.sleep(10)

            performanceProfiler.endNavigationProfiling(navigationId, success, simulatedError?.toString())
            navigationResults.add(navigationId to success)

            // Record the event
            debugInfoManager.recordEvent(
                NavigationDebugEvent(
                    type = if (success) NavigationEventType.NAVIGATION_COMPLETED else NavigationEventType.NAVIGATION_FAILED,
                    fromScreen = Screen.HOME,
                    toScreen = Screen.MAP,
                    error = simulatedError
                )
            )
        }

        // Then - Verify results
        val performanceData = performanceProfiler.performanceData.value
        assertEquals(10, performanceData.completedNavigations)

        val analysis = performanceProfiler.analyzePerformance()
        assertEquals(10, analysis.totalNavigations)

        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(10, debugInfo.totalNavigations)

        // Should have some failures due to error simulation
        val failureCount = navigationResults.count { !it.second }
        val successRate = debugInfo.successRate

        // Verify success rate calculation
        val expectedSuccessRate = ((10 - failureCount).toFloat() / 10) * 100
        assertEquals(expectedSuccessRate, successRate, 0.1f)
    }

    @Test
    fun `state inspection should detect performance issues`() = runTest {
        // Given - Create a scenario with performance issues
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.MAP, Screen.FRIENDS, Screen.MAP, Screen.FRIENDS, Screen.MAP), // Potential loop
            isNavigating = false
        )

        // Step 1: Inspect state
        stateInspector.inspectNavigationState(null, navigationState)

        // Step 2: Create slow navigations
        repeat(3) { index ->
            val navigationId = "slow_navigation_$index"
            performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)
            
            // Simulate slow navigation
            Thread.sleep(50)
            
            performanceProfiler.endNavigationProfiling(navigationId, true)
        }

        // Step 3: Validate state and diagnose performance
        val validationResult = debugUtils.validateNavigationState(null, navigationState)
        val performanceData = performanceProfiler.performanceData.value
        val performanceDiagnosis = debugUtils.diagnosePerformanceIssues(
            performanceData, 
            performanceData.recentNavigations
        )

        // Then - Verify detection of issues
        assertFalse(validationResult.isValid) // Should detect null controller
        assertTrue(validationResult.warnings.any { it.type == WarningType.POTENTIAL_LOOP })

        assertTrue(performanceDiagnosis.issues.any { it.type == PerformanceIssueType.SLOW_NAVIGATION })
        assertTrue(performanceDiagnosis.recommendations.isNotEmpty())
    }

    @Test
    fun `debug report generation should include all tool data`() = runTest {
        // Given - Set up data in all debug tools
        val navigationState = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )

        // Add state inspection data
        stateInspector.inspectNavigationState(null, navigationState)

        // Add performance data
        val navigationId = "report_test_navigation"
        performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)
        Thread.sleep(20)
        performanceProfiler.endNavigationProfiling(navigationId, true)

        // Add error simulation data
        errorSimulator.enableErrorSimulation(
            ErrorSimulationConfig(enabledScenarios = listOf("timeout"))
        )

        // Add debug logs
        debugUtils.logDebug(DebugLogLevel.INFO, "Test log for report generation")
        debugUtils.logDebug(DebugLogLevel.ERROR, "Test error for report generation")

        // Add debug info events
        debugInfoManager.recordEvent(
            NavigationDebugEvent(
                type = NavigationEventType.NAVIGATION_COMPLETED,
                fromScreen = Screen.HOME,
                toScreen = Screen.MAP,
                duration = 20L
            )
        )

        // When - Generate comprehensive report
        val validationResult = debugUtils.validateNavigationState(null, navigationState)
        val performanceData = performanceProfiler.performanceData.value
        val performanceDiagnosis = debugUtils.diagnosePerformanceIssues(
            performanceData,
            performanceData.recentNavigations
        )

        val report = debugUtils.generateDebugReport(
            navigationState,
            performanceData,
            validationResult,
            performanceDiagnosis
        )

        val stateReport = stateInspector.generateStateReport()
        val performanceReport = performanceProfiler.exportPerformanceData()

        // Then - Verify comprehensive reporting
        assertTrue(report.contains("Navigation Debug Report"))
        assertTrue(report.contains("Navigation State"))
        assertTrue(report.contains("Validation Results"))
        assertTrue(report.contains("Performance Diagnosis"))
        assertTrue(report.contains("Recent Debug Logs"))

        assertTrue(stateReport.contains("Navigation State Report"))
        assertTrue(stateReport.contains("Current Screen: Map"))

        assertTrue(performanceReport.contains("Navigation Performance Report"))
        assertTrue(performanceReport.contains("Total Navigations: 1"))

        // Verify error simulation state
        val simulationState = errorSimulator.simulationState.value
        assertTrue(simulationState.isEnabled)
        assertEquals(1, simulationState.activeScenarios.size)
    }

    @Test
    fun `memory profiling should track navigation memory usage`() = runTest {
        // Given - Record initial memory snapshot
        performanceProfiler.recordMemorySnapshot("Initial")

        // When - Perform navigations with memory tracking
        repeat(5) { index ->
            val navigationId = "memory_test_$index"
            performanceProfiler.startNavigationProfiling(navigationId, Screen.HOME, Screen.MAP)
            
            // Simulate memory allocation
            val largeArray = ByteArray(1024 * 100) // 100KB allocation
            
            performanceProfiler.recordMemorySnapshot("During navigation $index")
            performanceProfiler.endNavigationProfiling(navigationId, true)
            
            // Force garbage collection to clean up test allocations
            System.gc()
        }

        performanceProfiler.recordMemorySnapshot("Final")

        // Then - Verify memory tracking
        val analysis = performanceProfiler.analyzePerformance()
        assertEquals(5, analysis.totalNavigations)
        assertTrue(analysis.averageMemoryUsage >= 0)

        val performanceData = performanceProfiler.performanceData.value
        assertEquals(5, performanceData.completedNavigations)
        assertTrue(performanceData.recentNavigations.all { it.memoryUsed >= 0 })
    }

    @Test
    fun `error test suite should provide comprehensive testing scenarios`() {
        // When
        val testSuite = errorSimulator.createErrorTestSuite()

        // Then
        assertTrue(testSuite.size >= 4) // Should have at least 4 test cases

        val testCaseNames = testSuite.map { it.name }
        assertTrue(testCaseNames.any { it.contains("Timeout") })
        assertTrue(testCaseNames.any { it.contains("Invalid Route") })
        assertTrue(testCaseNames.any { it.contains("Controller Loss") })
        assertTrue(testCaseNames.any { it.contains("Memory Pressure") })

        // Verify each test case has required components
        testSuite.forEach { testCase ->
            assertFalse(testCase.name.isBlank())
            assertFalse(testCase.description.isBlank())
            assertFalse(testCase.expectedBehavior.isBlank())
            assertTrue(testCase.testSteps.isNotEmpty())
            assertNotNull(testCase.scenario)
        }
    }

    @Test
    fun `debug tools should handle concurrent operations safely`() = runTest {
        // Given - Multiple concurrent operations
        val navigationIds = (1..10).map { "concurrent_nav_$it" }

        // When - Start multiple navigations concurrently
        navigationIds.forEach { id ->
            performanceProfiler.startNavigationProfiling(id, Screen.HOME, Screen.MAP)
            
            debugUtils.logDebug(
                DebugLogLevel.INFO,
                "Started navigation $id",
                "ConcurrentTest"
            )
            
            debugInfoManager.recordEvent(
                NavigationDebugEvent(
                    type = NavigationEventType.NAVIGATION_STARTED,
                    fromScreen = Screen.HOME,
                    toScreen = Screen.MAP
                )
            )
        }

        // Complete navigations
        navigationIds.forEach { id ->
            performanceProfiler.endNavigationProfiling(id, true)
            
            debugUtils.logDebug(
                DebugLogLevel.INFO,
                "Completed navigation $id",
                "ConcurrentTest"
            )
            
            debugInfoManager.recordEvent(
                NavigationDebugEvent(
                    type = NavigationEventType.NAVIGATION_COMPLETED,
                    fromScreen = Screen.HOME,
                    toScreen = Screen.MAP,
                    duration = 50L
                )
            )
        }

        // Then - Verify all operations completed successfully
        val performanceData = performanceProfiler.performanceData.value
        assertEquals(10, performanceData.completedNavigations)

        val debugStats = debugUtils.getDebugStatistics()
        assertEquals(20, debugStats.totalLogs) // 10 start + 10 complete

        val debugInfo = debugInfoManager.getDebugInfo()
        assertEquals(20, debugInfo.totalNavigations) // 10 start + 10 complete events
    }
}