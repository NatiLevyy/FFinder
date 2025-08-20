package com.locationsharing.app.navigation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ButtonAnalyticsImpl.
 * Tests button interaction tracking, error monitoring, and performance metrics.
 */
class ButtonAnalyticsImplTest {
    
    private lateinit var buttonAnalytics: ButtonAnalyticsImpl
    
    @BeforeEach
    fun setUp() {
        buttonAnalytics = ButtonAnalyticsImpl()
    }
    
    @Test
    fun `trackButtonClick should record button interactions`() {
        // Given
        val buttonId = "home_button"
        val buttonType = "navigation"
        val responseTime = 150L
        
        // When
        buttonAnalytics.trackButtonClick(buttonId, buttonType, responseTime)
        buttonAnalytics.trackButtonClick(buttonId, buttonType, 200L)
        
        // Then
        val stats = buttonAnalytics.getButtonInteractionStats()
        assertEquals(2, stats[buttonId]?.totalClicks)
        assertEquals(175.0, stats[buttonId]?.averageResponseTime)
    }
    
    @Test
    fun `trackButtonClick should identify slow responses`() {
        // Given
        val buttonId = "slow_button"
        val buttonType = "action"
        val slowResponseTime = 300L // Above 200ms threshold
        
        // When
        buttonAnalytics.trackButtonClick(buttonId, buttonType, slowResponseTime)
        
        // Then
        val slowButtons = buttonAnalytics.getSlowResponseButtons()
        assertTrue(slowButtons.contains(buttonId))
    }
    
    @Test
    fun `trackButtonInteractionError should record errors`() {
        // Given
        val buttonId = "error_button"
        val errorType = "click_failed"
        
        // When
        buttonAnalytics.trackButtonInteractionError(buttonId, errorType)
        buttonAnalytics.trackButtonInteractionError(buttonId, "timeout")
        
        // Then
        val stats = buttonAnalytics.getButtonInteractionStats()
        assertEquals(2, stats[buttonId]?.errorCount)
    }
    
    @Test
    fun `trackButtonLoadingDuration should record loading times`() {
        // Given
        val buttonId = "loading_button"
        val loadingDuration = 500L
        
        // When
        buttonAnalytics.trackButtonLoadingDuration(buttonId, loadingDuration)
        buttonAnalytics.trackButtonLoadingDuration(buttonId, 300L)
        
        // Then
        val stats = buttonAnalytics.getButtonInteractionStats()
        assertEquals(400.0, stats[buttonId]?.averageLoadingDuration)
    }
    
    @Test
    fun `trackButtonAccessibilityUsage should record accessibility interactions`() {
        // Given
        val buttonId = "accessible_button"
        val accessibilityFeature = "screen_reader"
        
        // When
        buttonAnalytics.trackButtonAccessibilityUsage(buttonId, accessibilityFeature)
        buttonAnalytics.trackButtonAccessibilityUsage(buttonId, "voice_control")
        
        // Then
        val stats = buttonAnalytics.getButtonInteractionStats()
        assertEquals(2, stats[buttonId]?.accessibilityUsageCount)
    }
    
    @Test
    fun `trackButtonDoubleClickPrevention should record prevention counts`() {
        // Given
        val buttonId = "prevention_button"
        val preventionCount = 3
        
        // When
        buttonAnalytics.trackButtonDoubleClickPrevention(buttonId, preventionCount)
        buttonAnalytics.trackButtonDoubleClickPrevention(buttonId, 2)
        
        // Then
        val stats = buttonAnalytics.getButtonInteractionStats()
        assertEquals(5, stats[buttonId]?.doubleClickPreventions)
    }
    
    @Test
    fun `getButtonErrorRate should calculate correct error percentage`() {
        // Given
        val buttonId = "test_button"
        buttonAnalytics.trackButtonClick(buttonId, "test", 100L)
        buttonAnalytics.trackButtonClick(buttonId, "test", 150L)
        buttonAnalytics.trackButtonInteractionError(buttonId, "error")
        
        // When
        val errorRate = buttonAnalytics.getButtonErrorRate(buttonId)
        
        // Then
        assertEquals(33.33, errorRate, 0.01) // 1 error out of 3 total interactions
    }
    
    @Test
    fun `getButtonErrorRate should return zero when no interactions`() {
        // Given
        val buttonId = "no_interactions_button"
        
        // When
        val errorRate = buttonAnalytics.getButtonErrorRate(buttonId)
        
        // Then
        assertEquals(0.0, errorRate)
    }
    
    @Test
    fun `getSlowResponseButtons should identify buttons with slow responses`() {
        // Given
        buttonAnalytics.trackButtonClick("fast_button", "test", 100L)
        buttonAnalytics.trackButtonClick("slow_button1", "test", 250L)
        buttonAnalytics.trackButtonClick("slow_button2", "test", 300L)
        
        // When
        val slowButtons = buttonAnalytics.getSlowResponseButtons()
        
        // Then
        assertEquals(2, slowButtons.size)
        assertTrue(slowButtons.contains("slow_button1"))
        assertTrue(slowButtons.contains("slow_button2"))
    }
    
    @Test
    fun `getButtonInteractionStats should return comprehensive statistics`() {
        // Given
        val buttonId = "comprehensive_button"
        buttonAnalytics.trackButtonClick(buttonId, "test", 100L)
        buttonAnalytics.trackButtonClick(buttonId, "test", 200L)
        buttonAnalytics.trackButtonInteractionError(buttonId, "error")
        buttonAnalytics.trackButtonLoadingDuration(buttonId, 400L)
        buttonAnalytics.trackButtonAccessibilityUsage(buttonId, "screen_reader")
        buttonAnalytics.trackButtonDoubleClickPrevention(buttonId, 2)
        
        // When
        val stats = buttonAnalytics.getButtonInteractionStats()[buttonId]
        
        // Then
        assertEquals(2, stats?.totalClicks)
        assertEquals(150.0, stats?.averageResponseTime)
        assertEquals(1, stats?.errorCount)
        assertEquals(400.0, stats?.averageLoadingDuration)
        assertEquals(1, stats?.accessibilityUsageCount)
        assertEquals(2, stats?.doubleClickPreventions)
    }
    
    @Test
    fun `clearButtonAnalyticsData should reset all data`() {
        // Given
        buttonAnalytics.trackButtonClick("test_button", "test", 100L)
        buttonAnalytics.trackButtonInteractionError("test_button", "error")
        
        // When
        buttonAnalytics.clearButtonAnalyticsData()
        
        // Then
        assertEquals(emptyMap(), buttonAnalytics.getButtonInteractionStats())
        assertEquals(emptyList(), buttonAnalytics.getSlowResponseButtons())
        assertEquals(0.0, buttonAnalytics.getButtonErrorRate("test_button"))
    }
    
    @Test
    fun `multiple button tracking should maintain separate statistics`() {
        // Given
        buttonAnalytics.trackButtonClick("button1", "nav", 100L)
        buttonAnalytics.trackButtonClick("button2", "action", 200L)
        buttonAnalytics.trackButtonInteractionError("button1", "error")
        
        // When
        val stats = buttonAnalytics.getButtonInteractionStats()
        
        // Then
        assertEquals(1, stats["button1"]?.totalClicks)
        assertEquals(1, stats["button1"]?.errorCount)
        assertEquals(1, stats["button2"]?.totalClicks)
        assertEquals(0, stats["button2"]?.errorCount)
    }
    
    @Test
    fun `button statistics should handle edge cases`() {
        // Given - no data for button
        val buttonId = "empty_button"
        
        // When
        val stats = buttonAnalytics.getButtonInteractionStats()[buttonId]
        val errorRate = buttonAnalytics.getButtonErrorRate(buttonId)
        
        // Then
        assertEquals(null, stats)
        assertEquals(0.0, errorRate)
    }
}