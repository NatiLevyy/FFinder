package com.locationsharing.app.navigation.debug

import androidx.compose.runtime.Stable
import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Simulator for navigation errors that allows testing error handling
 * and recovery mechanisms in development builds.
 */
class NavigationErrorSimulator {
    
    private val _simulationState = MutableStateFlow(NavigationErrorSimulationState())
    val simulationState: StateFlow<NavigationErrorSimulationState> = _simulationState.asStateFlow()
    
    private val errorScenarios = mutableMapOf<String, ErrorScenario>()
    
    init {
        setupDefaultErrorScenarios()
    }
    
    /**
     * Enables error simulation with specified configuration.
     */
    fun enableErrorSimulation(config: ErrorSimulationConfig) {
        _simulationState.value = _simulationState.value.copy(
            isEnabled = true,
            config = config,
            activeScenarios = config.enabledScenarios.toSet()
        )
    }
    
    /**
     * Disables error simulation.
     */
    fun disableErrorSimulation() {
        _simulationState.value = _simulationState.value.copy(
            isEnabled = false,
            activeScenarios = emptySet()
        )
    }
    
    /**
     * Checks if an error should be simulated for the given navigation.
     */
    suspend fun shouldSimulateError(
        fromScreen: Screen?,
        toScreen: Screen,
        navigationType: NavigationType = NavigationType.STANDARD
    ): NavigationError? {
        val state = _simulationState.value
        if (!state.isEnabled) return null
        
        // Check if any active scenarios should trigger
        for (scenarioId in state.activeScenarios) {
            val scenario = errorScenarios[scenarioId] ?: continue
            
            if (scenario.shouldTrigger(fromScreen, toScreen, navigationType)) {
                // Simulate delay if specified
                if (scenario.simulatedDelay > 0) {
                    delay(scenario.simulatedDelay)
                }
                
                // Record the simulated error
                recordSimulatedError(scenario, fromScreen, toScreen)
                
                return scenario.error
            }
        }
        
        // Random error simulation based on probability
        if (Random.nextFloat() < state.config.randomErrorProbability) {
            val randomError = getRandomError()
            recordSimulatedError(
                ErrorScenario(
                    id = "random",
                    name = "Random Error",
                    description = "Randomly generated error for testing",
                    error = randomError,
                    triggerProbability = state.config.randomErrorProbability
                ),
                fromScreen,
                toScreen
            )
            return randomError
        }
        
        return null
    }
    
    /**
     * Simulates a specific error scenario immediately.
     */
    suspend fun simulateErrorScenario(scenarioId: String): NavigationError? {
        val scenario = errorScenarios[scenarioId] ?: return null
        
        if (scenario.simulatedDelay > 0) {
            delay(scenario.simulatedDelay)
        }
        
        recordSimulatedError(scenario, null, Screen.HOME)
        return scenario.error
    }
    
    /**
     * Adds a custom error scenario.
     */
    fun addCustomErrorScenario(scenario: ErrorScenario) {
        errorScenarios[scenario.id] = scenario
    }
    
    /**
     * Removes an error scenario.
     */
    fun removeErrorScenario(scenarioId: String) {
        errorScenarios.remove(scenarioId)
        
        val state = _simulationState.value
        _simulationState.value = state.copy(
            activeScenarios = state.activeScenarios - scenarioId
        )
    }
    
    /**
     * Gets all available error scenarios.
     */
    fun getAvailableScenarios(): List<ErrorScenario> {
        return errorScenarios.values.toList()
    }
    
    /**
     * Gets simulation statistics.
     */
    fun getSimulationStatistics(): ErrorSimulationStatistics {
        val state = _simulationState.value
        
        return ErrorSimulationStatistics(
            totalSimulatedErrors = state.simulatedErrors.size,
            errorsByType = state.simulatedErrors.groupBy { it.errorType }.mapValues { it.value.size },
            errorsByScenario = state.simulatedErrors.groupBy { it.scenarioId }.mapValues { it.value.size },
            recentErrors = state.simulatedErrors.takeLast(10)
        )
    }
    
    /**
     * Clears simulation history.
     */
    fun clearSimulationHistory() {
        _simulationState.value = _simulationState.value.copy(
            simulatedErrors = emptyList()
        )
    }
    
    /**
     * Creates a test suite of error scenarios for comprehensive testing.
     */
    fun createErrorTestSuite(): List<ErrorTestCase> {
        return listOf(
            ErrorTestCase(
                name = "Navigation Timeout Test",
                description = "Tests timeout handling during navigation",
                scenario = errorScenarios["timeout"]!!,
                expectedBehavior = "Should show timeout error and provide retry option",
                testSteps = listOf(
                    "Navigate to any screen",
                    "Verify timeout error is displayed",
                    "Verify retry button is available",
                    "Test retry functionality"
                )
            ),
            ErrorTestCase(
                name = "Invalid Route Test",
                description = "Tests handling of invalid navigation routes",
                scenario = errorScenarios["invalid_route"]!!,
                expectedBehavior = "Should fallback to home screen with error message",
                testSteps = listOf(
                    "Trigger navigation to invalid route",
                    "Verify fallback to home screen",
                    "Verify error message is shown"
                )
            ),
            ErrorTestCase(
                name = "Controller Loss Test",
                description = "Tests recovery when navigation controller is lost",
                scenario = errorScenarios["controller_lost"]!!,
                expectedBehavior = "Should recreate navigation controller and continue",
                testSteps = listOf(
                    "Simulate controller loss",
                    "Attempt navigation",
                    "Verify controller recreation",
                    "Verify navigation continues normally"
                )
            ),
            ErrorTestCase(
                name = "Memory Pressure Test",
                description = "Tests navigation under memory pressure",
                scenario = errorScenarios["memory_pressure"]!!,
                expectedBehavior = "Should handle gracefully with appropriate cleanup",
                testSteps = listOf(
                    "Simulate low memory conditions",
                    "Attempt multiple navigations",
                    "Verify memory cleanup occurs",
                    "Verify navigation remains functional"
                )
            )
        )
    }
    
    private fun setupDefaultErrorScenarios() {
        errorScenarios["timeout"] = ErrorScenario(
            id = "timeout",
            name = "Navigation Timeout",
            description = "Simulates navigation timeout after 5 seconds",
            error = NavigationError.NavigationTimeout,
            triggerProbability = 0.1f,
            simulatedDelay = 5000L
        )
        
        errorScenarios["invalid_route"] = ErrorScenario(
            id = "invalid_route",
            name = "Invalid Route",
            description = "Simulates navigation to invalid route",
            error = NavigationError.InvalidRoute("invalid_route"),
            triggerProbability = 0.05f,
            routeSpecific = true,
            targetRoutes = setOf("invalid_route", "non_existent")
        )
        
        errorScenarios["controller_lost"] = ErrorScenario(
            id = "controller_lost",
            name = "Navigation Controller Lost",
            description = "Simulates loss of navigation controller",
            error = NavigationError.NavigationControllerNotFound,
            triggerProbability = 0.02f
        )
        
        errorScenarios["memory_pressure"] = ErrorScenario(
            id = "memory_pressure",
            name = "Memory Pressure",
            description = "Simulates navigation failure due to memory pressure",
            error = NavigationError.UnknownError(OutOfMemoryError("Simulated memory pressure")),
            triggerProbability = 0.01f,
            simulatedDelay = 2000L
        )
        
        errorScenarios["network_dependent"] = ErrorScenario(
            id = "network_dependent",
            name = "Network Dependent Navigation",
            description = "Simulates failure when navigation requires network data",
            error = NavigationError.UnknownError(Exception("Network unavailable")),
            triggerProbability = 0.08f,
            screenSpecific = true,
            targetScreens = setOf(Screen.MAP, Screen.FRIENDS)
        )
        
        errorScenarios["rapid_navigation"] = ErrorScenario(
            id = "rapid_navigation",
            name = "Rapid Navigation Error",
            description = "Simulates errors during rapid successive navigations",
            error = NavigationError.UnknownError(IllegalStateException("Navigation in progress")),
            triggerProbability = 0.15f,
            requiresRapidNavigation = true
        )
    }
    
    private fun getRandomError(): NavigationError {
        val errors = listOf(
            NavigationError.NavigationTimeout,
            NavigationError.InvalidRoute("random_invalid_route"),
            NavigationError.NavigationControllerNotFound,
            NavigationError.UnknownError(Exception("Random simulated error"))
        )
        return errors.random()
    }
    
    private fun recordSimulatedError(
        scenario: ErrorScenario,
        fromScreen: Screen?,
        toScreen: Screen
    ) {
        val simulatedError = SimulatedErrorRecord(
            timestamp = System.currentTimeMillis(),
            scenarioId = scenario.id,
            scenarioName = scenario.name,
            errorType = scenario.error::class.simpleName ?: "Unknown",
            fromScreen = fromScreen?.route,
            toScreen = toScreen.route,
            description = scenario.description
        )
        
        val state = _simulationState.value
        val updatedErrors = (state.simulatedErrors + simulatedError).takeLast(MAX_ERROR_RECORDS)
        
        _simulationState.value = state.copy(
            simulatedErrors = updatedErrors
        )
    }
    
    companion object {
        private const val MAX_ERROR_RECORDS = 100
    }
}

// Data classes for error simulation

@Stable
data class NavigationErrorSimulationState(
    val isEnabled: Boolean = false,
    val config: ErrorSimulationConfig = ErrorSimulationConfig(),
    val activeScenarios: Set<String> = emptySet(),
    val simulatedErrors: List<SimulatedErrorRecord> = emptyList()
)

@Stable
data class ErrorSimulationConfig(
    val enabledScenarios: List<String> = emptyList(),
    val randomErrorProbability: Float = 0.05f,
    val enableNetworkErrors: Boolean = true,
    val enableMemoryErrors: Boolean = true,
    val enableTimeoutErrors: Boolean = true,
    val maxErrorsPerSession: Int = 50
)

@Stable
data class ErrorScenario(
    val id: String,
    val name: String,
    val description: String,
    val error: NavigationError,
    val triggerProbability: Float = 0.1f,
    val simulatedDelay: Long = 0L,
    val screenSpecific: Boolean = false,
    val targetScreens: Set<Screen> = emptySet(),
    val routeSpecific: Boolean = false,
    val targetRoutes: Set<String> = emptySet(),
    val requiresRapidNavigation: Boolean = false,
    val maxTriggersPerSession: Int = Int.MAX_VALUE
) {
    private var triggersThisSession = 0
    
    fun shouldTrigger(
        fromScreen: Screen?,
        toScreen: Screen,
        navigationType: NavigationType
    ): Boolean {
        if (triggersThisSession >= maxTriggersPerSession) return false
        
        if (screenSpecific && toScreen !in targetScreens) return false
        if (routeSpecific && toScreen.route !in targetRoutes) return false
        
        val shouldTrigger = Random.nextFloat() < triggerProbability
        
        if (shouldTrigger) {
            triggersThisSession++
        }
        
        return shouldTrigger
    }
    
    fun resetSessionTriggers() {
        triggersThisSession = 0
    }
}

@Stable
data class SimulatedErrorRecord(
    val timestamp: Long,
    val scenarioId: String,
    val scenarioName: String,
    val errorType: String,
    val fromScreen: String?,
    val toScreen: String,
    val description: String
)

@Stable
data class ErrorSimulationStatistics(
    val totalSimulatedErrors: Int,
    val errorsByType: Map<String, Int>,
    val errorsByScenario: Map<String, Int>,
    val recentErrors: List<SimulatedErrorRecord>
)

@Stable
data class ErrorTestCase(
    val name: String,
    val description: String,
    val scenario: ErrorScenario,
    val expectedBehavior: String,
    val testSteps: List<String>
)