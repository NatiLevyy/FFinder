package com.locationsharing.app.navigation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive analytics manager that combines navigation and button analytics.
 * Provides a unified interface for tracking all user interaction metrics.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val navigationAnalytics: NavigationAnalyticsImpl,
    private val buttonAnalytics: ButtonAnalyticsImpl
) {
    
    /**
     * Get comprehensive analytics dashboard data.
     */
    fun getAnalyticsDashboard(): AnalyticsDashboard {
        val navigationHealth = navigationAnalytics.getNavigationHealthMetrics()
        val buttonStats = buttonAnalytics.getButtonInteractionStats()
        val errorFrequency = navigationAnalytics.getErrorFrequencyAnalysis()
        val userJourney = navigationAnalytics.getUserJourneyAnalysis()
        
        return AnalyticsDashboard(
            navigationHealth = navigationHealth,
            buttonInteractionSummary = createButtonInteractionSummary(buttonStats),
            errorFrequencyAnalysis = errorFrequency,
            userJourneyAnalysis = userJourney,
            recommendations = generateRecommendations(navigationHealth, buttonStats)
        )
    }
    
    /**
     * Get real-time performance metrics for monitoring.
     */
    fun getRealTimeMetrics(): RealTimeMetrics {
        val navigationSuccessRate = navigationAnalytics.getNavigationSuccessRate()
        val criticalErrors = navigationAnalytics.getCriticalErrors()
        val slowButtons = buttonAnalytics.getSlowResponseButtons()
        
        return RealTimeMetrics(
            navigationSuccessRate = navigationSuccessRate,
            activeCriticalErrors = criticalErrors.size,
            slowResponseButtons = slowButtons.size,
            overallHealthStatus = determineHealthStatus(navigationSuccessRate, criticalErrors.size, slowButtons.size)
        )
    }
    
    /**
     * Generate performance alerts based on current metrics.
     */
    fun generatePerformanceAlerts(): List<PerformanceAlert> {
        val alerts = mutableListOf<PerformanceAlert>()
        
        // Navigation success rate alerts
        val successRate = navigationAnalytics.getNavigationSuccessRate()
        if (successRate < 90.0) {
            alerts.add(PerformanceAlert(
                type = AlertType.LOW_SUCCESS_RATE,
                severity = if (successRate < 70.0) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                message = "Navigation success rate is ${String.format("%.1f", successRate)}%",
                recommendation = "Review navigation error patterns and implement fixes"
            ))
        }
        
        // Critical error alerts
        val criticalErrors = navigationAnalytics.getCriticalErrors()
        if (criticalErrors.isNotEmpty()) {
            alerts.add(PerformanceAlert(
                type = AlertType.CRITICAL_ERRORS,
                severity = AlertSeverity.CRITICAL,
                message = "${criticalErrors.size} critical navigation errors detected",
                recommendation = "Immediate investigation required for critical errors"
            ))
        }
        
        // Slow button response alerts
        val slowButtons = buttonAnalytics.getSlowResponseButtons()
        if (slowButtons.isNotEmpty()) {
            alerts.add(PerformanceAlert(
                type = AlertType.SLOW_BUTTONS,
                severity = AlertSeverity.WARNING,
                message = "${slowButtons.size} buttons have slow response times",
                recommendation = "Optimize button response performance for better UX"
            ))
        }
        
        return alerts
    }
    
    /**
     * Export analytics data for external analysis.
     */
    fun exportAnalyticsData(): AnalyticsExport {
        return AnalyticsExport(
            navigationErrors = navigationAnalytics.getErrorStatistics(),
            navigationEvents = navigationAnalytics.getNavigationSuccessRate(),
            buttonStats = buttonAnalytics.getButtonInteractionStats(),
            userJourneys = navigationAnalytics.getUserJourneyAnalysis(),
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Clear all analytics data (for testing purposes).
     */
    fun clearAllAnalyticsData() {
        navigationAnalytics.clearAnalyticsData()
        buttonAnalytics.clearButtonAnalyticsData()
    }
    
    /**
     * Create button interaction summary from detailed stats.
     */
    private fun createButtonInteractionSummary(buttonStats: Map<String, ButtonAnalyticsImpl.ButtonStats>): ButtonInteractionSummary {
        val totalClicks = buttonStats.values.sumOf { it.totalClicks }
        val totalErrors = buttonStats.values.sumOf { it.errorCount }
        val averageResponseTime = if (buttonStats.isNotEmpty()) {
            buttonStats.values.map { it.averageResponseTime }.average()
        } else 0.0
        
        val slowButtons = buttonStats.filter { it.value.averageResponseTime > 200.0 }.keys.toList()
        val errorProneButtons = buttonStats.filter { 
            val errorRate = if (it.value.totalClicks > 0) {
                (it.value.errorCount.toDouble() / it.value.totalClicks) * 100
            } else 0.0
            errorRate > 5.0 // More than 5% error rate
        }.keys.toList()
        
        return ButtonInteractionSummary(
            totalButtonClicks = totalClicks,
            totalButtonErrors = totalErrors,
            averageResponseTime = averageResponseTime,
            slowResponseButtons = slowButtons,
            errorProneButtons = errorProneButtons
        )
    }
    
    /**
     * Generate recommendations based on analytics data.
     */
    private fun generateRecommendations(
        navigationHealth: NavigationAnalyticsImpl.NavigationHealthMetrics,
        buttonStats: Map<String, ButtonAnalyticsImpl.ButtonStats>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (navigationHealth.successRate < 95.0) {
            recommendations.add("Improve navigation reliability - current success rate is ${String.format("%.1f", navigationHealth.successRate)}%")
        }
        
        if (navigationHealth.averageNavigationTime > 300.0) {
            recommendations.add("Optimize navigation performance - average time is ${String.format("%.0f", navigationHealth.averageNavigationTime)}ms")
        }
        
        val slowButtons = buttonStats.filter { it.value.averageResponseTime > 200.0 }
        if (slowButtons.isNotEmpty()) {
            recommendations.add("Optimize ${slowButtons.size} slow-responding buttons for better user experience")
        }
        
        if (navigationHealth.fallbackUsageRate > 10.0) {
            recommendations.add("High fallback usage (${String.format("%.1f", navigationHealth.fallbackUsageRate)}%) indicates navigation issues")
        }
        
        return recommendations
    }
    
    /**
     * Determine overall health status based on metrics.
     */
    private fun determineHealthStatus(successRate: Double, criticalErrors: Int, slowButtons: Int): HealthStatus {
        return when {
            criticalErrors > 0 -> HealthStatus.CRITICAL
            successRate < 80.0 || slowButtons > 5 -> HealthStatus.WARNING
            successRate < 95.0 || slowButtons > 2 -> HealthStatus.FAIR
            else -> HealthStatus.GOOD
        }
    }
    
    /**
     * Data class for analytics dashboard.
     */
    data class AnalyticsDashboard(
        val navigationHealth: NavigationAnalyticsImpl.NavigationHealthMetrics,
        val buttonInteractionSummary: ButtonInteractionSummary,
        val errorFrequencyAnalysis: NavigationAnalyticsImpl.ErrorFrequencyAnalysis,
        val userJourneyAnalysis: NavigationAnalyticsImpl.UserJourneyAnalysis,
        val recommendations: List<String>
    )
    
    /**
     * Data class for real-time metrics.
     */
    data class RealTimeMetrics(
        val navigationSuccessRate: Double,
        val activeCriticalErrors: Int,
        val slowResponseButtons: Int,
        val overallHealthStatus: HealthStatus
    )
    
    /**
     * Data class for button interaction summary.
     */
    data class ButtonInteractionSummary(
        val totalButtonClicks: Int,
        val totalButtonErrors: Int,
        val averageResponseTime: Double,
        val slowResponseButtons: List<String>,
        val errorProneButtons: List<String>
    )
    
    /**
     * Data class for performance alerts.
     */
    data class PerformanceAlert(
        val type: AlertType,
        val severity: AlertSeverity,
        val message: String,
        val recommendation: String
    )
    
    /**
     * Data class for analytics export.
     */
    data class AnalyticsExport(
        val navigationErrors: Map<String, Int>,
        val navigationEvents: Double,
        val buttonStats: Map<String, ButtonAnalyticsImpl.ButtonStats>,
        val userJourneys: NavigationAnalyticsImpl.UserJourneyAnalysis,
        val exportTimestamp: Long
    )
    
    /**
     * Enum for alert types.
     */
    enum class AlertType {
        LOW_SUCCESS_RATE,
        CRITICAL_ERRORS,
        SLOW_BUTTONS,
        HIGH_ERROR_RATE
    }
    
    /**
     * Enum for alert severity.
     */
    enum class AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
    
    /**
     * Enum for health status.
     */
    enum class HealthStatus {
        GOOD,
        FAIR,
        WARNING,
        CRITICAL
    }
}