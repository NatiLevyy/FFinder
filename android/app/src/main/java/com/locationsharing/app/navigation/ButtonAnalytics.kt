package com.locationsharing.app.navigation

/**
 * Interface for tracking button interaction analytics.
 * Provides methods to track button clicks, response times, and interaction patterns.
 */
interface ButtonAnalytics {
    
    /**
     * Track a button click event.
     * @param buttonId The unique identifier of the button
     * @param buttonType The type of button (navigation, action, etc.)
     * @param responseTime The time taken for the button to respond in milliseconds
     */
    fun trackButtonClick(buttonId: String, buttonType: String, responseTime: Long)
    
    /**
     * Track button interaction failure.
     * @param buttonId The unique identifier of the button
     * @param errorType The type of error that occurred
     */
    fun trackButtonInteractionError(buttonId: String, errorType: String)
    
    /**
     * Track button loading state duration.
     * @param buttonId The unique identifier of the button
     * @param loadingDuration The duration the button was in loading state in milliseconds
     */
    fun trackButtonLoadingDuration(buttonId: String, loadingDuration: Long)
    
    /**
     * Track button accessibility usage.
     * @param buttonId The unique identifier of the button
     * @param accessibilityFeature The accessibility feature used (screen reader, voice control, etc.)
     */
    fun trackButtonAccessibilityUsage(buttonId: String, accessibilityFeature: String)
    
    /**
     * Track button double-click prevention.
     * @param buttonId The unique identifier of the button
     * @param preventionCount The number of prevented double-clicks
     */
    fun trackButtonDoubleClickPrevention(buttonId: String, preventionCount: Int)
}