package com.locationsharing.app.navigation

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ButtonAnalytics for tracking button interaction metrics.
 * Provides comprehensive analytics for button responsiveness, errors, and accessibility usage.
 */
@Singleton
class ButtonAnalyticsImpl @Inject constructor() : ButtonAnalytics {
    
    companion object {
        private const val TAG = "ButtonAnalytics"
        private const val SLOW_RESPONSE_THRESHOLD_MS = 200L
    }
    
    // In-memory storage for analytics (in production, this would integrate with analytics services)
    private val buttonClicks = mutableMapOf<String, MutableList<ButtonClickEvent>>()
    private val buttonErrors = mutableMapOf<String, MutableList<ButtonErrorEvent>>()
    private val loadingDurations = mutableMapOf<String, MutableList<Long>>()
    private val accessibilityUsage = mutableMapOf<String, MutableList<AccessibilityEvent>>()
    private val doubleClickPreventions = mutableMapOf<String, Int>()
    
    override fun trackButtonClick(buttonId: String, buttonType: String, responseTime: Long) {
        val event = ButtonClickEvent(
            buttonId = buttonId,
            buttonType = buttonType,
            responseTime = responseTime,
            timestamp = System.currentTimeMillis()
        )
        
        buttonClicks.getOrPut(buttonId) { mutableListOf() }.add(event)
        
        // Track slow responses
        if (responseTime > SLOW_RESPONSE_THRESHOLD_MS) {
            Timber.w(TAG, "Slow button response: $buttonId took ${responseTime}ms")
            logAnalyticsEvent("slow_button_response", mapOf(
                "button_id" to buttonId,
                "button_type" to buttonType,
                "response_time_ms" to responseTime.toString()
            ))
        }
        
        Timber.d(TAG, "Button click tracked: $buttonId ($buttonType) - ${responseTime}ms")
        
        logAnalyticsEvent("button_click", mapOf(
            "button_id" to buttonId,
            "button_type" to buttonType,
            "response_time_ms" to responseTime.toString()
        ))
    }
    
    override fun trackButtonInteractionError(buttonId: String, errorType: String) {
        val event = ButtonErrorEvent(
            buttonId = buttonId,
            errorType = errorType,
            timestamp = System.currentTimeMillis()
        )
        
        buttonErrors.getOrPut(buttonId) { mutableListOf() }.add(event)
        
        Timber.e(TAG, "Button interaction error: $buttonId - $errorType")
        
        logAnalyticsEvent("button_interaction_error", mapOf(
            "button_id" to buttonId,
            "error_type" to errorType
        ))
    }
    
    override fun trackButtonLoadingDuration(buttonId: String, loadingDuration: Long) {
        loadingDurations.getOrPut(buttonId) { mutableListOf() }.add(loadingDuration)
        
        Timber.d(TAG, "Button loading duration tracked: $buttonId - ${loadingDuration}ms")
        
        logAnalyticsEvent("button_loading_duration", mapOf(
            "button_id" to buttonId,
            "loading_duration_ms" to loadingDuration.toString()
        ))
    }
    
    override fun trackButtonAccessibilityUsage(buttonId: String, accessibilityFeature: String) {
        val event = AccessibilityEvent(
            buttonId = buttonId,
            accessibilityFeature = accessibilityFeature,
            timestamp = System.currentTimeMillis()
        )
        
        accessibilityUsage.getOrPut(buttonId) { mutableListOf() }.add(event)
        
        Timber.d(TAG, "Button accessibility usage tracked: $buttonId - $accessibilityFeature")
        
        logAnalyticsEvent("button_accessibility_usage", mapOf(
            "button_id" to buttonId,
            "accessibility_feature" to accessibilityFeature
        ))
    }
    
    override fun trackButtonDoubleClickPrevention(buttonId: String, preventionCount: Int) {
        doubleClickPreventions[buttonId] = doubleClickPreventions.getOrDefault(buttonId, 0) + preventionCount
        
        Timber.d(TAG, "Button double-click prevention: $buttonId - $preventionCount prevented")
        
        logAnalyticsEvent("button_double_click_prevention", mapOf(
            "button_id" to buttonId,
            "prevention_count" to preventionCount.toString(),
            "total_preventions" to doubleClickPreventions[buttonId].toString()
        ))
    }
    
    /**
     * Get button interaction statistics for monitoring.
     */
    fun getButtonInteractionStats(): Map<String, ButtonStats> {
        return buttonClicks.mapValues { (buttonId, clicks) ->
            val errors = buttonErrors[buttonId] ?: emptyList()
            val loadings = loadingDurations[buttonId] ?: emptyList()
            val accessibility = accessibilityUsage[buttonId] ?: emptyList()
            val preventions = doubleClickPreventions[buttonId] ?: 0
            
            ButtonStats(
                totalClicks = clicks.size,
                averageResponseTime = if (clicks.isNotEmpty()) clicks.map { it.responseTime }.average() else 0.0,
                errorCount = errors.size,
                averageLoadingDuration = if (loadings.isNotEmpty()) loadings.average() else 0.0,
                accessibilityUsageCount = accessibility.size,
                doubleClickPreventions = preventions
            )
        }
    }
    
    /**
     * Get button error rate for monitoring.
     */
    fun getButtonErrorRate(buttonId: String): Double {
        val clicks = buttonClicks[buttonId]?.size ?: 0
        val errors = buttonErrors[buttonId]?.size ?: 0
        val total = clicks + errors
        
        return if (total > 0) {
            (errors.toDouble() / total) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Get slow response buttons for optimization.
     */
    fun getSlowResponseButtons(): List<String> {
        return buttonClicks.filter { (_, clicks) ->
            clicks.any { it.responseTime > SLOW_RESPONSE_THRESHOLD_MS }
        }.keys.toList()
    }
    
    /**
     * Clear button analytics data (for testing purposes).
     */
    fun clearButtonAnalyticsData() {
        buttonClicks.clear()
        buttonErrors.clear()
        loadingDurations.clear()
        accessibilityUsage.clear()
        doubleClickPreventions.clear()
        
        Timber.d(TAG, "Button analytics data cleared")
    }
    
    /**
     * Log analytics event (in production, this would integrate with analytics services).
     */
    private fun logAnalyticsEvent(eventName: String, parameters: Map<String, String>) {
        // In production, this would send to Firebase Analytics, Mixpanel, etc.
        Timber.v(TAG, "Button Analytics Event: $eventName with parameters: $parameters")
    }
    
    /**
     * Data class for button click events.
     */
    data class ButtonClickEvent(
        val buttonId: String,
        val buttonType: String,
        val responseTime: Long,
        val timestamp: Long
    )
    
    /**
     * Data class for button error events.
     */
    data class ButtonErrorEvent(
        val buttonId: String,
        val errorType: String,
        val timestamp: Long
    )
    
    /**
     * Data class for accessibility events.
     */
    data class AccessibilityEvent(
        val buttonId: String,
        val accessibilityFeature: String,
        val timestamp: Long
    )
    
    /**
     * Data class for button statistics.
     */
    data class ButtonStats(
        val totalClicks: Int,
        val averageResponseTime: Double,
        val errorCount: Int,
        val averageLoadingDuration: Double,
        val accessibilityUsageCount: Int,
        val doubleClickPreventions: Int
    )
}