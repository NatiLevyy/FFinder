package com.locationsharing.app.navigation

/**
 * Interface for tracking navigation analytics and metrics.
 * Provides methods to track navigation success, errors, and user patterns.
 */
interface NavigationAnalytics {
    
    /**
     * Track a navigation error for analytics and monitoring.
     * @param error The navigation error that occurred
     */
    fun trackNavigationError(error: NavigationError)
    
    /**
     * Track a critical navigation error that needs immediate attention.
     * @param errorType The type of critical error
     */
    fun trackCriticalNavigationError(errorType: String)
    
    /**
     * Track a successful navigation for performance monitoring.
     * @param fromRoute The route navigated from
     * @param toRoute The route navigated to
     * @param duration The time taken for navigation in milliseconds
     */
    fun trackSuccessfulNavigation(fromRoute: String, toRoute: String, duration: Long)
    
    /**
     * Track when fallback navigation is used.
     * @param fallbackType The type of fallback used
     */
    fun trackFallbackNavigation(fallbackType: String)
    
    /**
     * Track navigation retry attempts.
     * @param errorType The type of error that caused the retry
     * @param attemptNumber The retry attempt number
     */
    fun trackNavigationRetry(errorType: String, attemptNumber: Int)
    
    /**
     * Track user journey patterns for navigation optimization.
     * @param route The route being navigated to
     * @param source The source of the navigation (button, gesture, etc.)
     */
    fun trackUserJourney(route: String, source: String)
}