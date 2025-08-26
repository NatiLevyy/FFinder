package com.locationsharing.app.navigation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for the complete navigation analytics system.
 * Tests the interaction between NavigationAnalytics, ButtonAnalytics, and AnalyticsManager.
 */
class NavigationAnalyticsIntegrationTest {
    
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
    fun `complete user journey should be tracked across all analytics components`() {
        // Given - simulate a complete user journey
        
        // User clicks home button
        buttonAnalytics.trackButtonClick("home_button", "navigation", 120L)
        navigationAnalytics.trackSuccessfulNavigation("splash", "home", 150L)
        navigationAnalytics.trackUserJourney("home", "button")
        
        // User clicks map button (slow response)
        buttonAnalytics.trackButtonClick("map_button", "navigation", 250L)
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 200L)
        navigationAnalytics.trackUserJourney("map", "button")
        
        // Navigation error occurs
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        
        // User retries navigation
        navigationAnalytics.trackNavigationRetry("timeout", 1)
        buttonAnalytics.trackButtonClick("map_button", "navigation", 180L)
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 180L)
        
        // User uses back navigation
        navigationAnalytics.trackSuccessfulNavigation("map", "home", 100L)
        navigationAnalytics.trackUserJourney("home", "back_button")
        
        // When
        val dashboard = analyticsManager.getAnalyticsDashboard()
        val realTimeMetrics = analyticsManager.getRealTimeMetrics()
        val alerts = analyticsManager.generatePerformanceAlerts()
        
        // Then - verify comprehensive tracking
        assertEquals(80.0, dashboard.navigationHealth.successRate) // 4 success, 1 error
        assertEquals(3, dashboard.buttonInteractionSummary.totalButtonClicks)
        assertEquals(4, dashboard.userJourneyAnalysis.totalJourneys)
        assertEquals("home", dashboard.userJourneyAnalysis.mostVisitedRoute)
        
        // Verify real-time metrics
        assertEquals(80.0, realTimeMetrics.navigationSuccessRate)
        assertEquals(0, realTimeMetrics.activeCriticalErrors)
        assertEquals(1, realTimeMetrics.slowResponseButtons) // map_button had 250ms response
        
        // Verify alerts for slow button
        assertTrue(alerts.any { it.type == AnalyticsManager.AlertType.SLOW_BUTTONS })
        
        // Verify recommendations
        assertTrue(dashboard.recommendations.any { it.contains("slow-responding buttons") })
    }
    
    @Test
    fun `error scenarios should be comprehensively tracked`() {
        // Given - simulate various error scenarios
        
        // Critical navigation error
        navigationAnalytics.trackCriticalNavigationError("controller_not_found")
        
        // Button interaction errors
        buttonAnalytics.trackButtonInteractionError("broken_button", "click_failed")
        buttonAnalytics.trackButtonInteractionError("broken_button", "timeout")
        
        // Navigation errors
        navigationAnalytics.trackNavigationError(NavigationError.InvalidRoute)
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        
        // Fallback usage
        navigationAnalytics.trackFallbackNavigation("home")
        navigationAnalytics.trackFallbackNavigation("error_recovery")
        
        // When
        val dashboard = analyticsManager.getAnalyticsDashboard()
        val alerts = analyticsManager.generatePerformanceAlerts()
        val errorAnalysis = navigationAnalytics.getErrorFrequencyAnalysis()
        
        // Then
        assertEquals(0.0, dashboard.navigationHealth.successRate) // No successful navigations
        assertEquals(2, errorAnalysis.totalErrors)
        assertEquals(1, errorAnalysis.criticalErrorCount)
        
        // Verify critical error alert
        assertTrue(alerts.any { 
            it.type == AnalyticsManager.AlertType.CRITICAL_ERRORS && 
            it.severity == AnalyticsManager.AlertSeverity.CRITICAL 
        })
        
        // Verify low success rate alert
        assertTrue(alerts.any { 
            it.type == AnalyticsManager.AlertType.LOW_SUCCESS_RATE && 
            it.severity == AnalyticsManager.AlertSeverity.CRITICAL 
        })
        
        // Verify health score is severely impacted
        assertTrue(dashboard.navigationHealth.healthScore < 50.0)
    }
    
    @Test
    fun `performance optimization scenarios should be identified`() {
        // Given - simulate performance issues
        
        // Slow button responses
        buttonAnalytics.trackButtonClick("slow_button1", "navigation", 300L)
        buttonAnalytics.trackButtonClick("slow_button2", "action", 350L)
        buttonAnalytics.trackButtonClick("slow_button3", "navigation", 280L)
        
        // Slow navigation
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 450L)
        navigationAnalytics.trackSuccessfulNavigation("map", "friends", 500L)
        
        // High error rate button
        buttonAnalytics.trackButtonClick("error_prone_button", "action", 150L)
        repeat(3) { 
            buttonAnalytics.trackButtonInteractionError("error_prone_button", "action_failed") 
        }
        
        // When
        val dashboard = analyticsManager.getAnalyticsDashboard()
        val slowButtons = buttonAnalytics.getSlowResponseButtons()
        val buttonStats = buttonAnalytics.getButtonInteractionStats()
        
        // Then
        assertEquals(3, slowButtons.size)
        assertTrue(dashboard.recommendations.any { it.contains("navigation performance") })
        assertTrue(dashboard.recommendations.any { it.contains("slow-responding buttons") })
        
        // Verify error-prone button is identified
        assertTrue(dashboard.buttonInteractionSummary.errorProneButtons.contains("error_prone_button"))
        
        // Verify average response times
        assertTrue(dashboard.buttonInteractionSummary.averageResponseTime > 200.0)
        assertTrue(dashboard.navigationHealth.averageNavigationTime > 400.0)
    }
    
    @Test
    fun `accessibility usage should be tracked`() {
        // Given - simulate accessibility interactions
        buttonAnalytics.trackButtonAccessibilityUsage("home_button", "screen_reader")
        buttonAnalytics.trackButtonAccessibilityUsage("map_button", "voice_control")
        buttonAnalytics.trackButtonAccessibilityUsage("home_button", "screen_reader")
        
        // When
        val buttonStats = buttonAnalytics.getButtonInteractionStats()
        
        // Then
        assertEquals(2, buttonStats["home_button"]?.accessibilityUsageCount)
        assertEquals(1, buttonStats["map_button"]?.accessibilityUsageCount)
    }
    
    @Test
    fun `double click prevention should be tracked`() {
        // Given - simulate double-click scenarios
        buttonAnalytics.trackButtonDoubleClickPrevention("eager_button", 3)
        buttonAnalytics.trackButtonDoubleClickPrevention("eager_button", 2)
        buttonAnalytics.trackButtonDoubleClickPrevention("another_button", 1)
        
        // When
        val buttonStats = buttonAnalytics.getButtonInteractionStats()
        
        // Then
        assertEquals(5, buttonStats["eager_button"]?.doubleClickPreventions)
        assertEquals(1, buttonStats["another_button"]?.doubleClickPreventions)
    }
    
    @Test
    fun `analytics export should contain all relevant data`() {
        // Given - create comprehensive data
        navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L)
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        navigationAnalytics.trackUserJourney("map", "button")
        buttonAnalytics.trackButtonClick("test_button", "navigation", 120L)
        
        // When
        val export = analyticsManager.exportAnalyticsData()
        
        // Then
        assertTrue(export.navigationErrors.isNotEmpty())
        assertTrue(export.navigationEvents > 0)
        assertTrue(export.buttonStats.isNotEmpty())
        assertTrue(export.userJourneys.totalJourneys > 0)
        assertTrue(export.exportTimestamp > 0)
    }
    
    @Test
    fun `health status should reflect overall system performance`() {
        // Test GOOD health
        repeat(10) { 
            navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L)
            buttonAnalytics.trackButtonClick("fast_button", "navigation", 100L)
        }
        var metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.GOOD, metrics.overallHealthStatus)
        
        // Degrade to FAIR
        analyticsManager.clearAllAnalyticsData()
        repeat(9) { navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L) }
        navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout)
        buttonAnalytics.trackButtonClick("slow_button1", "navigation", 250L)
        buttonAnalytics.trackButtonClick("slow_button2", "navigation", 280L)
        buttonAnalytics.trackButtonClick("slow_button3", "navigation", 300L)
        
        metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.FAIR, metrics.overallHealthStatus)
        
        // Degrade to WARNING
        analyticsManager.clearAllAnalyticsData()
        repeat(7) { navigationAnalytics.trackSuccessfulNavigation("home", "map", 150L) }
        repeat(3) { navigationAnalytics.trackNavigationError(NavigationError.NavigationTimeout) }
        
        metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.WARNING, metrics.overallHealthStatus)
        
        // Degrade to CRITICAL
        navigationAnalytics.trackCriticalNavigationError("controller_not_found")
        
        metrics = analyticsManager.getRealTimeMetrics()
        assertEquals(AnalyticsManager.HealthStatus.CRITICAL, metrics.overallHealthStatus)
    }
}