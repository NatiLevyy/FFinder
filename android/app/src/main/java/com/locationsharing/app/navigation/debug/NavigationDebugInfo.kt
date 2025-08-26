package com.locationsharing.app.navigation.debug

import androidx.compose.runtime.Stable
import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class containing debug information about navigation system performance and state.
 */
@Stable
data class NavigationDebugInfo(
    val averageNavigationTime: Long = 0L,
    val totalNavigations: Int = 0,
    val errorCount: Int = 0,
    val successRate: Float = 100f,
    val memoryUsage: Long = 0L,
    val recentErrors: List<NavigationDebugError> = emptyList(),
    val isErrorSimulationEnabled: Boolean = false,
    val performanceMetrics: NavigationPerformanceMetrics = NavigationPerformanceMetrics()
)

/**
 * Represents a navigation error for debugging purposes.
 */
@Stable
data class NavigationDebugError(
    val type: String,
    val message: String,
    val timestamp: String,
    val stackTrace: String? = null,
    val fromScreen: String? = null,
    val toScreen: String? = null
)

/**
 * Performance metrics for navigation operations.
 */
@Stable
data class NavigationPerformanceMetrics(
    val navigationTimes: List<Long> = emptyList(),
    val slowNavigations: Int = 0,
    val fastNavigations: Int = 0,
    val timeoutCount: Int = 0,
    val retryCount: Int = 0,
    val cacheHitRate: Float = 0f
)

/**
 * Navigation event for debugging and analytics.
 */
@Stable
data class NavigationDebugEvent(
    val type: NavigationEventType,
    val fromScreen: Screen?,
    val toScreen: Screen?,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long? = null,
    val error: NavigationError? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of navigation events for debugging.
 */
enum class NavigationEventType {
    NAVIGATION_STARTED,
    NAVIGATION_COMPLETED,
    NAVIGATION_FAILED,
    NAVIGATION_CANCELLED,
    BACK_NAVIGATION,
    DEEP_LINK_NAVIGATION,
    ERROR_RECOVERY,
    TIMEOUT_OCCURRED,
    RETRY_ATTEMPTED
}

/**
 * Manager for collecting and providing navigation debug information.
 */
class NavigationDebugInfoManager {
    private val events = mutableListOf<NavigationDebugEvent>()
    private val errors = mutableListOf<NavigationDebugError>()
    private val navigationTimes = mutableListOf<Long>()
    private var isErrorSimulationEnabled = false
    
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Records a navigation event for debugging purposes.
     */
    fun recordEvent(event: NavigationDebugEvent) {
        events.add(event)
        
        // Keep only recent events to prevent memory issues
        if (events.size > MAX_EVENTS) {
            events.removeAt(0)
        }
        
        // Record navigation time if available
        event.duration?.let { duration ->
            navigationTimes.add(duration)
            if (navigationTimes.size > MAX_NAVIGATION_TIMES) {
                navigationTimes.removeAt(0)
            }
        }
        
        // Record error if present
        event.error?.let { error ->
            recordError(
                type = error::class.simpleName ?: "Unknown",
                message = error.toString(),
                fromScreen = event.fromScreen?.route,
                toScreen = event.toScreen?.route
            )
        }
    }
    
    /**
     * Records a navigation error for debugging.
     */
    fun recordError(
        type: String,
        message: String,
        stackTrace: String? = null,
        fromScreen: String? = null,
        toScreen: String? = null
    ) {
        val error = NavigationDebugError(
            type = type,
            message = message,
            timestamp = dateFormatter.format(Date()),
            stackTrace = stackTrace,
            fromScreen = fromScreen,
            toScreen = toScreen
        )
        
        errors.add(error)
        
        // Keep only recent errors
        if (errors.size > MAX_ERRORS) {
            errors.removeAt(0)
        }
    }
    
    /**
     * Gets current debug information.
     */
    fun getDebugInfo(): NavigationDebugInfo {
        val totalNavigations = events.count { 
            it.type == NavigationEventType.NAVIGATION_COMPLETED || 
            it.type == NavigationEventType.NAVIGATION_FAILED 
        }
        
        val successfulNavigations = events.count { 
            it.type == NavigationEventType.NAVIGATION_COMPLETED 
        }
        
        val averageTime = if (navigationTimes.isNotEmpty()) {
            navigationTimes.average().toLong()
        } else 0L
        
        val successRate = if (totalNavigations > 0) {
            (successfulNavigations.toFloat() / totalNavigations) * 100f
        } else 100f
        
        val memoryUsage = Runtime.getRuntime().let { runtime ->
            (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        }
        
        val performanceMetrics = NavigationPerformanceMetrics(
            navigationTimes = navigationTimes.toList(),
            slowNavigations = navigationTimes.count { it > SLOW_NAVIGATION_THRESHOLD },
            fastNavigations = navigationTimes.count { it < FAST_NAVIGATION_THRESHOLD },
            timeoutCount = events.count { it.type == NavigationEventType.TIMEOUT_OCCURRED },
            retryCount = events.count { it.type == NavigationEventType.RETRY_ATTEMPTED },
            cacheHitRate = calculateCacheHitRate()
        )
        
        return NavigationDebugInfo(
            averageNavigationTime = averageTime,
            totalNavigations = totalNavigations,
            errorCount = errors.size,
            successRate = successRate,
            memoryUsage = memoryUsage,
            recentErrors = errors.takeLast(MAX_RECENT_ERRORS),
            isErrorSimulationEnabled = isErrorSimulationEnabled,
            performanceMetrics = performanceMetrics
        )
    }
    
    /**
     * Toggles error simulation for testing purposes.
     */
    fun toggleErrorSimulation() {
        isErrorSimulationEnabled = !isErrorSimulationEnabled
    }
    
    /**
     * Clears all recorded debug information.
     */
    fun clearDebugInfo() {
        events.clear()
        errors.clear()
        navigationTimes.clear()
    }
    
    /**
     * Gets recent navigation events for inspection.
     */
    fun getRecentEvents(count: Int = 10): List<NavigationDebugEvent> {
        return events.takeLast(count)
    }
    
    /**
     * Simulates a navigation timeout for testing.
     */
    fun simulateTimeout() {
        recordEvent(
            NavigationDebugEvent(
                type = NavigationEventType.TIMEOUT_OCCURRED,
                fromScreen = null,
                toScreen = null,
                error = NavigationError.NavigationTimeout
            )
        )
    }
    
    /**
     * Checks if error simulation should trigger for testing.
     */
    fun shouldSimulateError(): Boolean {
        return isErrorSimulationEnabled && Math.random() < ERROR_SIMULATION_PROBABILITY
    }
    
    private fun calculateCacheHitRate(): Float {
        // Simplified cache hit rate calculation
        val cacheEvents = events.filter { 
            it.metadata.containsKey("cache_hit") 
        }
        
        if (cacheEvents.isEmpty()) return 0f
        
        val hits = cacheEvents.count { 
            it.metadata["cache_hit"] == true 
        }
        
        return (hits.toFloat() / cacheEvents.size) * 100f
    }
    
    companion object {
        private const val MAX_EVENTS = 100
        private const val MAX_ERRORS = 50
        private const val MAX_NAVIGATION_TIMES = 50
        private const val MAX_RECENT_ERRORS = 10
        private const val SLOW_NAVIGATION_THRESHOLD = 1000L // 1 second
        private const val FAST_NAVIGATION_THRESHOLD = 100L // 100ms
        private const val ERROR_SIMULATION_PROBABILITY = 0.1 // 10% chance
    }
}