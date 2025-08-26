package com.locationsharing.app.navigation.performance

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.locationsharing.app.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Performance tests for navigation system components.
 * Tests navigation timing, cache performance, and optimization effectiveness.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NavigationPerformanceTest {
    
    private lateinit var context: Context
    private lateinit var performanceMonitor: NavigationPerformanceMonitor
    private lateinit var navigationCache: NavigationCache
    private lateinit var destinationLoader: NavigationDestinationLoader
    private lateinit var optimizedButtonManager: OptimizedButtonResponseManager
    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: StandardTestDispatcher
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        
        performanceMonitor = NavigationPerformanceMonitor(context)
        navigationCache = NavigationCache(context, performanceMonitor)
        destinationLoader = NavigationDestinationLoader(navigationCache, performanceMonitor)
        optimizedButtonManager = OptimizedButtonResponseManager(performanceMonitor)
    }
    
    @After
    fun tearDown() {
        // Clean up test data
        context.getSharedPreferences("navigation_performance_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("navigation_cache_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
    
    @Test
    fun `navigation timing should be recorded accurately`() = testScope.runTest {
        // Given
        val fromRoute = "home"
        val toRoute = "map"
        val navigationTime = 250L
        
        // When
        performanceMonitor.recordNavigationTime(fromRoute, toRoute, navigationTime)
        
        // Then
        val stats = performanceMonitor.getRoutePerformanceStats("home")
        assertEquals(250.0, stats.averageNavigationTime, 0.1)
        assertEquals(1, stats.navigationCount)
    }
    
    @Test
    fun `slow navigation should be detected and logged`() = testScope.runTest {
        // Given
        val slowNavigationTime = 1500L // Above threshold
        
        // When
        performanceMonitor.recordNavigationTime("home", "map", slowNavigationTime)
        
        // Then
        val summary = performanceMonitor.getPerformanceSummary()
        assertEquals(1L, summary.slowNavigationCount)
        assertTrue(summary.averageNavigationTime > 1000)
    }
    
    @Test
    fun `cache hit rate should be calculated correctly`() = testScope.runTest {
        // Given
        repeat(7) { performanceMonitor.recordCacheHit("destination", "route_$it") }
        repeat(3) { performanceMonitor.recordCacheMiss("destination", "route_miss_$it") }
        
        // When
        val summary = performanceMonitor.getPerformanceSummary()
        
        // Then
        assertEquals(0.7, summary.cacheHitRate, 0.01)
    }
    
    @Test
    fun `destination caching should improve load times`() = testScope.runTest {
        // Given
        val route = "map"
        
        // When - First load (cache miss)
        val firstLoadStart = System.currentTimeMillis()
        navigationCache.cacheDestination(route)
        val firstLoadTime = System.currentTimeMillis() - firstLoadStart
        
        // Then - Second load (cache hit)
        val secondLoadStart = System.currentTimeMillis()
        val isCached = navigationCache.isDestinationCached(route)
        val secondLoadTime = System.currentTimeMillis() - secondLoadStart
        
        assertTrue("Destination should be cached", isCached)
        assertTrue("Cached load should be faster", secondLoadTime < firstLoadTime)
    }
    
    @Test
    fun `destination preloading should work correctly`() = testScope.runTest {
        // Given
        val currentRoute = "home"
        val navigationHistory = listOf("map", "friends", "map")
        
        // When
        destinationLoader.preloadDestinations(currentRoute, navigationHistory)
        advanceTimeBy(200L) // Allow preloading to complete
        
        // Then
        val stats = destinationLoader.getLoadingStats()
        assertTrue("Should have preloaded destinations", stats.loadedDestinations > 0)
        assertTrue("Should have cached destinations", stats.cachedDestinations > 0)
    }
    
    @Test
    fun `optimized button manager should have faster response times`() = testScope.runTest {
        // Given
        val buttonId = "home_button" // Common button
        var actionExecuted = false
        val action = { actionExecuted = true }
        
        // When
        val startTime = System.currentTimeMillis()
        optimizedButtonManager.handleButtonClick(buttonId, action)
        advanceTimeBy(100L)
        val responseTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Action should be executed", actionExecuted)
        assertTrue("Response time should be fast", responseTime < 200L)
        
        val stats = optimizedButtonManager.getButtonPerformanceStats()
        assertTrue("Should have active buttons", stats.activeButtons > 0)
    }
    
    @Test
    fun `button debouncing should prevent double clicks`() = testScope.runTest {
        // Given
        val buttonId = "test_button"
        var clickCount = 0
        val action = { clickCount++ }
        
        // When - Rapid clicks
        optimizedButtonManager.handleButtonClick(buttonId, action)
        optimizedButtonManager.handleButtonClick(buttonId, action) // Should be debounced
        advanceTimeBy(100L)
        
        // Then
        assertEquals("Only one click should be processed", 1, clickCount)
    }
    
    @Test
    fun `cache should expire old entries`() = testScope.runTest {
        // Given
        val route = "test_route"
        navigationCache.cacheDestination(route)
        
        // When - Simulate time passing beyond expiry
        advanceTimeBy(31 * 60 * 1000L) // 31 minutes
        navigationCache.clearOldCache()
        
        // Then
        assertFalse("Expired cache should be cleared", navigationCache.isDestinationCached(route))
    }
    
    @Test
    fun `performance recommendations should be generated correctly`() = testScope.runTest {
        // Given - Create poor performance conditions
        repeat(10) {
            performanceMonitor.recordNavigationTime("home", "map", 1200L) // Slow navigation
            performanceMonitor.recordButtonResponseTime("button_$it", 300L) // Slow button
        }
        repeat(8) { performanceMonitor.recordCacheMiss("destination", "route_$it") }
        repeat(2) { performanceMonitor.recordCacheHit("destination", "route_hit_$it") }
        
        // When
        val recommendations = performanceMonitor.getPerformanceRecommendations()
        
        // Then
        assertTrue("Should have navigation optimization recommendation", 
            recommendations.any { it.type == RecommendationType.NAVIGATION_OPTIMIZATION })
        assertTrue("Should have cache optimization recommendation",
            recommendations.any { it.type == RecommendationType.CACHE_OPTIMIZATION })
        assertTrue("Should have button optimization recommendation",
            recommendations.any { it.type == RecommendationType.BUTTON_OPTIMIZATION })
    }
    
    @Test
    fun `performance grade should be calculated correctly`() = testScope.runTest {
        // Given - Excellent performance metrics
        repeat(10) {
            performanceMonitor.recordNavigationTime("home", "map", 200L) // Fast navigation
            performanceMonitor.recordCacheHit("destination", "route_$it") // Good cache hit rate
        }
        
        // When
        val summary = performanceMonitor.getPerformanceSummary()
        
        // Then
        assertEquals("Should have excellent performance grade", 
            PerformanceGrade.EXCELLENT, summary.performanceGrade)
    }
    
    @Test
    fun `navigation state caching should preserve state`() = testScope.runTest {
        // Given
        val route = "map"
        val state = mapOf("zoom" to 15, "center" to "user_location")
        
        // When
        navigationCache.cacheNavigationState(route, state)
        val retrievedState = navigationCache.getCachedNavigationState(route)
        
        // Then
        assertNotNull("State should be cached", retrievedState)
        assertEquals("Zoom level should be preserved", "15", retrievedState?.get("zoom").toString())
        assertEquals("Center should be preserved", "user_location", retrievedState?.get("center"))
    }
    
    @Test
    fun `concurrent button actions should be limited`() = testScope.runTest {
        // Given
        val buttonIds = (1..5).map { "button_$it" }
        val executedActions = mutableListOf<String>()
        
        // When - Try to execute many actions concurrently
        buttonIds.forEach { buttonId ->
            optimizedButtonManager.handleButtonClick(buttonId) {
                executedActions.add(buttonId)
            }
        }
        advanceTimeBy(500L)
        
        // Then - Should limit concurrent actions
        val stats = optimizedButtonManager.getButtonPerformanceStats()
        assertTrue("Should limit concurrent actions", stats.activeActionsCount <= 3)
    }
    
    @Test
    fun `cache memory usage should be monitored`() = testScope.runTest {
        // Given
        repeat(5) { navigationCache.cacheDestination("route_$it") }
        repeat(3) { 
            navigationCache.cacheNavigationState("state_$it", mapOf("data" to "value_$it"))
        }
        
        // When
        val stats = navigationCache.getCacheStats()
        
        // Then
        assertTrue("Should track destination cache", stats.totalDestinations > 0)
        assertTrue("Should track state cache", stats.totalStates > 0)
        assertTrue("Should estimate memory usage", stats.memoryUsageKB > 0)
    }
    
    @Test
    fun `performance metrics should persist across restarts`() = testScope.runTest {
        // Given
        performanceMonitor.recordNavigationTime("home", "map", 300L)
        performanceMonitor.recordButtonResponseTime("test_button", 150L)
        
        // When - Create new instance (simulating app restart)
        val newPerformanceMonitor = NavigationPerformanceMonitor(context)
        advanceTimeBy(100L) // Allow loading to complete
        
        // Then
        val summary = newPerformanceMonitor.getPerformanceSummary()
        assertTrue("Should persist navigation metrics", summary.totalNavigations > 0)
    }
    
    @Test
    fun `batch button state updates should be efficient`() = testScope.runTest {
        // Given
        val updates = mapOf(
            "button1" to com.locationsharing.app.ui.components.button.ButtonState().withEnabled(false),
            "button2" to com.locationsharing.app.ui.components.button.ButtonState().withLoading(true),
            "button3" to com.locationsharing.app.ui.components.button.ButtonState().withFeedback(true)
        )
        
        // When
        val startTime = System.currentTimeMillis()
        optimizedButtonManager.batchUpdateButtonStates(updates)
        val updateTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Batch update should be fast", updateTime < 50L)
        
        // Verify states were updated
        updates.forEach { (buttonId, expectedState) ->
            val actualState = optimizedButtonManager.getButtonState(buttonId).value
            assertEquals("Button $buttonId enabled state", expectedState.isEnabled, actualState.isEnabled)
            assertEquals("Button $buttonId loading state", expectedState.isLoading, actualState.isLoading)
            assertEquals("Button $buttonId feedback state", expectedState.showFeedback, actualState.showFeedback)
        }
    }
}