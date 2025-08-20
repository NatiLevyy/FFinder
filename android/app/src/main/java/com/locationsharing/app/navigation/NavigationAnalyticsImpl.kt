package com.locationsharing.app.navigation

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NavigationAnalytics for tracking navigation metrics.
 * Provides comprehensive analytics for navigation success rates, errors, and user patterns.
 */
@Singleton
class NavigationAnalyticsImpl @Inject constructor() : NavigationAnalytics {
    
    companion object {
        private const val TAG = "NavigationAnalytics"
    }
    
    // In-memory storage for analytics (in production, this would integrate with analytics services)
    private val errorCounts = mutableMapOf<String, Int>()
    private val successfulNavigations = mutableListOf<NavigationEvent>()
    private val criticalErrors = mutableListOf<CriticalErrorEvent>()
    private val fallbackUsage = mutableMapOf<String, Int>()
    private val retryAttempts = mutableMapOf<String, Int>()
    private val userJourneys = mutableListOf<UserJourneyEvent>()
    
    override fun trackNavigationError(error: NavigationError) {
        val errorType = when (error) {
            is NavigationError.NavigationTimeout -> "timeout"
            is NavigationError.InvalidRoute -> "invalid_route"
            is NavigationError.NavigationControllerNotFound -> "controller_not_found"
            is NavigationError.UnknownError -> "unknown_error"
            is NavigationError.InvalidNavigationState -> "invalid_state"
            is NavigationError.NavigationInProgress -> "navigation_in_progress"
        }
        
        errorCounts[errorType] = errorCounts.getOrDefault(errorType, 0) + 1
        
        Timber.d(TAG, "Navigation error tracked: $errorType (total: ${errorCounts[errorType]})")
        
        // In production, this would send to analytics service
        logAnalyticsEvent("navigation_error", mapOf(
            "error_type" to errorType,
            "total_count" to errorCounts[errorType].toString()
        ))
    }
    
    override fun trackCriticalNavigationError(errorType: String) {
        val event = CriticalErrorEvent(
            errorType = errorType,
            timestamp = System.currentTimeMillis()
        )
        
        criticalErrors.add(event)
        
        Timber.e(TAG, "Critical navigation error tracked: $errorType")
        
        // In production, this would trigger immediate alerts
        logAnalyticsEvent("critical_navigation_error", mapOf(
            "error_type" to errorType,
            "timestamp" to event.timestamp.toString()
        ))
    }
    
    override fun trackSuccessfulNavigation(fromRoute: String, toRoute: String, duration: Long) {
        val event = NavigationEvent(
            fromRoute = fromRoute,
            toRoute = toRoute,
            duration = duration,
            timestamp = System.currentTimeMillis()
        )
        
        successfulNavigations.add(event)
        
        Timber.d(TAG, "Successful navigation tracked: $fromRoute -> $toRoute (${duration}ms)")
        
        logAnalyticsEvent("successful_navigation", mapOf(
            "from_route" to fromRoute,
            "to_route" to toRoute,
            "duration_ms" to duration.toString()
        ))
    }
    
    override fun trackFallbackNavigation(fallbackType: String) {
        fallbackUsage[fallbackType] = fallbackUsage.getOrDefault(fallbackType, 0) + 1
        
        Timber.d(TAG, "Fallback navigation tracked: $fallbackType (total: ${fallbackUsage[fallbackType]})")
        
        logAnalyticsEvent("fallback_navigation", mapOf(
            "fallback_type" to fallbackType,
            "total_count" to fallbackUsage[fallbackType].toString()
        ))
    }
    
    override fun trackNavigationRetry(errorType: String, attemptNumber: Int) {
        val retryKey = "${errorType}_retry"
        retryAttempts[retryKey] = retryAttempts.getOrDefault(retryKey, 0) + 1
        
        Timber.d(TAG, "Navigation retry tracked: $errorType (attempt $attemptNumber)")
        
        logAnalyticsEvent("navigation_retry", mapOf(
            "error_type" to errorType,
            "attempt_number" to attemptNumber.toString(),
            "total_retries" to retryAttempts[retryKey].toString()
        ))
    }
    
    override fun trackUserJourney(route: String, source: String) {
        val event = UserJourneyEvent(
            route = route,
            source = source,
            timestamp = System.currentTimeMillis()
        )
        
        userJourneys.add(event)
        
        Timber.d(TAG, "User journey tracked: $route from $source")
        
        logAnalyticsEvent("user_journey", mapOf(
            "route" to route,
            "source" to source
        ))
    }
    
    /**
     * Get navigation error statistics for debugging and monitoring.
     */
    fun getErrorStatistics(): Map<String, Int> {
        return errorCounts.toMap()
    }
    
    /**
     * Get navigation success rate for monitoring.
     */
    fun getNavigationSuccessRate(): Double {
        val totalErrors = errorCounts.values.sum()
        val totalSuccessful = successfulNavigations.size
        val total = totalErrors + totalSuccessful
        
        return if (total > 0) {
            (totalSuccessful.toDouble() / total) * 100
        } else {
            100.0
        }
    }
    
    /**
     * Get critical errors for immediate attention.
     */
    fun getCriticalErrors(): List<CriticalErrorEvent> {
        return criticalErrors.toList()
    }
    
    /**
     * Get error frequency analysis for monitoring.
     */
    fun getErrorFrequencyAnalysis(): ErrorFrequencyAnalysis {
        val totalErrors = errorCounts.values.sum()
        val mostFrequentError = errorCounts.maxByOrNull { it.value }
        val errorTrends = calculateErrorTrends()
        
        return ErrorFrequencyAnalysis(
            totalErrors = totalErrors,
            mostFrequentErrorType = mostFrequentError?.key ?: "none",
            mostFrequentErrorCount = mostFrequentError?.value ?: 0,
            errorTrends = errorTrends,
            criticalErrorCount = criticalErrors.size
        )
    }
    
    /**
     * Get user journey analysis for navigation patterns.
     */
    fun getUserJourneyAnalysis(): UserJourneyAnalysis {
        val totalJourneys = userJourneys.size
        val routeFrequency = userJourneys.groupingBy { it.route }.eachCount()
        val sourceFrequency = userJourneys.groupingBy { it.source }.eachCount()
        val commonPaths = calculateCommonNavigationPaths()
        
        return UserJourneyAnalysis(
            totalJourneys = totalJourneys,
            mostVisitedRoute = routeFrequency.maxByOrNull { it.value }?.key ?: "none",
            mostCommonSource = sourceFrequency.maxByOrNull { it.value }?.key ?: "none",
            commonNavigationPaths = commonPaths,
            averageJourneysPerSession = calculateAverageJourneysPerSession()
        )
    }
    
    /**
     * Get comprehensive navigation health metrics.
     */
    fun getNavigationHealthMetrics(): NavigationHealthMetrics {
        val successRate = getNavigationSuccessRate()
        val errorFrequency = getErrorFrequencyAnalysis()
        val averageNavigationTime = calculateAverageNavigationTime()
        val fallbackUsageRate = calculateFallbackUsageRate()
        
        return NavigationHealthMetrics(
            successRate = successRate,
            averageNavigationTime = averageNavigationTime,
            totalErrors = errorFrequency.totalErrors,
            criticalErrors = errorFrequency.criticalErrorCount,
            fallbackUsageRate = fallbackUsageRate,
            healthScore = calculateNavigationHealthScore(successRate, errorFrequency.totalErrors)
        )
    }
    
    /**
     * Calculate error trends over time.
     */
    private fun calculateErrorTrends(): Map<String, Double> {
        // In a real implementation, this would analyze error patterns over time
        // For now, return current error distribution
        val total = errorCounts.values.sum().toDouble()
        return if (total > 0) {
            errorCounts.mapValues { (it.value / total) * 100 }
        } else {
            emptyMap()
        }
    }
    
    /**
     * Calculate common navigation paths from user journeys.
     */
    private fun calculateCommonNavigationPaths(): List<NavigationPath> {
        // Group consecutive journeys to identify common paths
        val paths = mutableListOf<NavigationPath>()
        
        for (i in 0 until userJourneys.size - 1) {
            val current = userJourneys[i]
            val next = userJourneys[i + 1]
            
            // Simple path detection - in production this would be more sophisticated
            if (next.timestamp - current.timestamp < 60000) { // Within 1 minute
                val pathKey = "${current.route}->${next.route}"
                val existingPath = paths.find { it.path == pathKey }
                
                if (existingPath != null) {
                    paths[paths.indexOf(existingPath)] = existingPath.copy(frequency = existingPath.frequency + 1)
                } else {
                    paths.add(NavigationPath(pathKey, 1))
                }
            }
        }
        
        return paths.sortedByDescending { it.frequency }.take(10)
    }
    
    /**
     * Calculate average journeys per session.
     */
    private fun calculateAverageJourneysPerSession(): Double {
        // Simple implementation - in production this would track actual sessions
        return if (userJourneys.isNotEmpty()) {
            userJourneys.size.toDouble() / maxOf(1, userJourneys.size / 10) // Assume 10 journeys per session
        } else {
            0.0
        }
    }
    
    /**
     * Calculate average navigation time.
     */
    private fun calculateAverageNavigationTime(): Double {
        return if (successfulNavigations.isNotEmpty()) {
            successfulNavigations.map { it.duration }.average()
        } else {
            0.0
        }
    }
    
    /**
     * Calculate fallback usage rate.
     */
    private fun calculateFallbackUsageRate(): Double {
        val totalFallbacks = fallbackUsage.values.sum()
        val totalNavigations = successfulNavigations.size + errorCounts.values.sum()
        
        return if (totalNavigations > 0) {
            (totalFallbacks.toDouble() / totalNavigations) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Calculate overall navigation health score (0-100).
     */
    private fun calculateNavigationHealthScore(successRate: Double, totalErrors: Int): Double {
        val baseScore = successRate
        val errorPenalty = minOf(totalErrors * 2.0, 30.0) // Max 30 point penalty
        val criticalErrorPenalty = criticalErrors.size * 10.0 // 10 points per critical error
        
        return maxOf(0.0, baseScore - errorPenalty - criticalErrorPenalty)
    }
    
    /**
     * Clear analytics data (for testing purposes).
     */
    fun clearAnalyticsData() {
        errorCounts.clear()
        successfulNavigations.clear()
        criticalErrors.clear()
        fallbackUsage.clear()
        retryAttempts.clear()
        userJourneys.clear()
        
        Timber.d(TAG, "Analytics data cleared")
    }
    
    /**
     * Log analytics event (in production, this would integrate with analytics services).
     */
    private fun logAnalyticsEvent(eventName: String, parameters: Map<String, String>) {
        // In production, this would send to Firebase Analytics, Mixpanel, etc.
        Timber.v(TAG, "Analytics Event: $eventName with parameters: $parameters")
    }
    
    /**
     * Data class for navigation events.
     */
    data class NavigationEvent(
        val fromRoute: String,
        val toRoute: String,
        val duration: Long,
        val timestamp: Long
    )
    
    /**
     * Data class for critical error events.
     */
    data class CriticalErrorEvent(
        val errorType: String,
        val timestamp: Long
    )
    
    /**
     * Data class for user journey events.
     */
    data class UserJourneyEvent(
        val route: String,
        val source: String,
        val timestamp: Long
    )
    
    /**
     * Data class for error frequency analysis.
     */
    data class ErrorFrequencyAnalysis(
        val totalErrors: Int,
        val mostFrequentErrorType: String,
        val mostFrequentErrorCount: Int,
        val errorTrends: Map<String, Double>,
        val criticalErrorCount: Int
    )
    
    /**
     * Data class for user journey analysis.
     */
    data class UserJourneyAnalysis(
        val totalJourneys: Int,
        val mostVisitedRoute: String,
        val mostCommonSource: String,
        val commonNavigationPaths: List<NavigationPath>,
        val averageJourneysPerSession: Double
    )
    
    /**
     * Data class for navigation paths.
     */
    data class NavigationPath(
        val path: String,
        val frequency: Int
    )
    
    /**
     * Data class for comprehensive navigation health metrics.
     */
    data class NavigationHealthMetrics(
        val successRate: Double,
        val averageNavigationTime: Double,
        val totalErrors: Int,
        val criticalErrors: Int,
        val fallbackUsageRate: Double,
        val healthScore: Double
    )
}