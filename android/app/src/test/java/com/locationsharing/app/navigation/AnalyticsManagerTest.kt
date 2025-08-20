package com.locationsharing.app.navigation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for AnalyticsManager.
 * Tests comprehensive analytics dashboard, real-time metrics, and performance alerts.
 */
class AnalyticsManagerTest {
    
    private lateinit var navigationAnalytics: NavigationAnalyticsImpl
    private lateinit var buttonAnalytics: ButtonAnalyticsImpl
    private lateinit var analyticsManager: AnalyticsManager
    
    @BeforeEach
    fun setUp() {
        navigationAnalytics = NavigationAnalyticsImpl()
        buttonAnalytics = ButtonAnalyticsImpl()
        analyticsManager = AnalyticsManager(navigationAnalytics, buttonAnalytics)
    }
    
    @Test
    fun `getAnalyticsDashboard should return comprehensive analytics data`() {
        // Given
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L)
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        buttonAnalytics.trackButtonClick("test_button", "navigation", 100L)
        
        // When
        val dashboard = analyticsManager.getAnalyticsDashboard()
        
        // Then
        assertTrue(dashboard.navigationHealth.successRate > 0)
        assertTrue(dashboard.buttonInteractionSummary.totalButtonClicks > 0)
        assertTrue(dashboard.recommendations.isNotEmpty())
    }
    
    @Test
    fun `getRealTimeMetrics should return current performance status`() {
        // Given
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L)
        navigationAnalytics.trackSuccessfulNavigation("map", "friends", 200L)
        
        // When
        val metrics = analyticsManager.getRealTimeMetrics()
        
        // Then
        assertEquals(100.0, metrics.navigationSuccessRate)
        assertEquals(0, metrics.activeCriticalErrors)
        assertEquals(AnalyticsManager.HealthStatus.GOOD, metrics.overallHealthStatus)
    }
    
    @Test
    fun `generatePerformanceAlerts should create alerts for low success rate`() {
        // Given - create scenario with low success rate
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        navigationAnalytics.trackNavigationError(NavigationError.InvalidRoute)
        navigationAnalytics.trackNavigationError(NavigationError.NavigationControllerNotFound)
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L) // Only 25% success rate
        
        // When
        val alerts = analyticsManager.generatePerformanceAlerts()
        
        // Then
        val lowSuccessRateAlert = alerts.find { it.type == AnalyticsManager.AlertType.LOW_SUCCESS_RATE }
        assertTrue(lowSuccessRateAlert != null)
        assertEquals(AnalyticsManager.AlertSeverity.CRITICAL, lowSuccessRateAlert?.severity)
    }
    
    @Test
    fun `generatePerformanceAlerts should create alerts for critical errors`() {
        // Given
        navigationAnalytics.trackCriticalNavigationError("controller_not_found")
        navigationAnalytics.trackCriticalNavigationError("fallback_failed")
        
        // When
        val alerts = analyticsManager.generatePerformanceAlerts()
        
        // Then
        val criticalErrorAlert = alerts.find { it.type == AnalyticsManager.AlertType.CRITICAL_ERRORS }
        assertTrue(criticalErrorAlert != null)
        assertEquals(AnalyticsManager.AlertSeverity.CRITICAL, criticalErrorAlert?.severity)
        assertTrue(criticalErrorAlert?.message?.contains("2 critical") == true)
    }
    
    @Test
    fun `generatePerformanceAlerts should create alerts for slow buttons`() {
        // Given
        buttonAnalytics.trackButtonClick("slow_button1", "navigation", 250L)
        buttonAnalytics.trackButtonClick("slow_button2", "action", 300L)
        
        // When
        val alerts = analyticsManager.generatePerformanceAlerts()
        
        // Then
        val slowButtonAlert = alerts.find { it.type == AnalyticsManager.AlertType.SLOW_BUTTONS }
        assertTrue(slowButtonAlert != null)
        assertEquals(AnalyticsManager.AlertSeverity.WARNING, slowButtonAlert?.severity)
    }
    
    @Test
    fun `generatePerformanceAlerts should return empty list when no issues`() {
        // Given - good performance scenario
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L)
        navigationAnalytics.trackSuccessfulNavigation("map", "friends", 200L)
        buttonAnalytics.trackButtonClick("fast_button", "navigation", 100L)
        
        // When
        val alerts = analyticsManager.generatePerformanceAlerts()
        
        // Then
        assertTrue(alerts.isEmpty())
    }
    
    @Test
    fun `exportAnalyticsData should return comprehensive export data`() {
        // Given
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L)
        buttonAnalytics.trackButtonClick("test_button", "navigation", 100L)
        
        // When
        val export = analyticsManager.exportAnalyticsData()
        
        // Then
        assertTrue(export.navigationErrors.isNotEmpty())
        assertTrue(export.navigationEvents > 0)
        assertTrue(export.buttonStats.isNotEmpty())
        assertTrue(export.exportTimestamp > 0)
    }
    
    @Test
    fun `clearAllAnalyticsData should reset all analytics`() {
        // Given
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        buttonAnalytics.trackButtonClick("test_button", "navigation", 100L)
        
        // When
        analyticsManager.clearAllAnalyticsData()
        
        // Then
        val dashboard = analyticsManager.getAnalyticsDashboard()
        assertEquals(100.0, dashboard.navigationHealth.successRate) // Reset to default
        assertEquals(0, dashboard.buttonInteractionSummary.totalButtonClicks)
    }
    
    @Test
    fun `health status should be determined correctly based on metrics`() {
        // Test CRITICAL status
        navigationAnalytics.trackCriticalNavigationError("critical_error")
        var metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.CRITICAL, metrics.overallHealthStatus)
        
        // Reset and test WARNING status
        analyticsManager.clearAllAnalyticsData()
        repeat(8) { navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout) }
        repeat(2) { navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L) }
        metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.WARNING, metrics.overallHealthStatus)
        
        // Reset and test GOOD status
        analyticsManager.clearAllAnalyticsData()
        repeat(10) { navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L) }
        metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.GOOD, metrics.overallHealthStatus)
    }
    
    @Test
    fun `button interaction summary should calculate correct metrics`() {
        // Given
        buttonAnalytics.trackButtonClick("button1", "nav", 100L)
        buttonAnalytics.trackButtonClick("button2", "nav", 300L) // Slow
        buttonAnalytics.trackButtonInteractionError("button1", "error")
        buttonAnalytics.trackButtonClick("button3", "action", 150L)
        buttonAnalytics.trackButtonInteractionError("button3", "error")
        buttonAnalytics.trackButtonInteractionError("button3", "error") // High error rate
        
        // When
        val dashboard = analyticsManager.getAnalyticsDashboard()
        val summary = dashboard.buttonInteractionSummary
        
        // Then
        assertEquals(3, summary.totalButtonClicks)
        assertEquals(3, summary.totalButtonErrors)
        assertTrue(summary.slowResponseButtons.contains("button2"))
        assertTrue(summary.errorProneButtons.contains("button3")) // >5% error rate
    }
    
    @Test
    fun `recommendations should be generated based on performance issues`() {
        // Given - create various performance issues
        repeat(7) { navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout) }
        repeat(3) { navigationAnalytics.trackSuccessfulNavigation("home", "map", 400L) } // Slow navigation
        buttonAnalytics.trackButtonClick("slow_button", "nav", 300L)
        navigationAnalytics.trackFallbackNavigation("home")
        navigationAnalytics.trackFallbackNavigation("error_recovery")
        
        // When
        val dashboard = analyticsManager.getAnalyticsDashboard()
        val recommendations = dashboard.recommendations
        
        // Then
        assertTrue(recommendations.any { it.contains("navigation reliability") })
        assertTrue(recommendations.any { it.contains("navigation performance") })
        assertTrue(recommendations.any { it.contains("slow-responding buttons") })
        assertTrue(recommendations.any { it.contains("fallback usage") })
    }
}