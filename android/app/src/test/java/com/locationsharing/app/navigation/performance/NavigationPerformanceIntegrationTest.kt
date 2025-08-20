package com.locationsharing.app.navigation.performance

import android.content.Context
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.navigation.NavigationManagerImpl
import com.locationsharing.app.navigation.NavigationStateTracker
import com.locationsharing.app.navigation.NavigationStateTrackerImpl
import com.locationsharing.app.navigation.NavigationErrorHandler
import com.locationsharing.app.navigation.NavigationAnalytics
import com.locationsharing.app.navigation.Screen
import com.locationsharing.app.ui.components.feedback.HapticFeedbackManager
import com.locationsharing.app.ui.components.feedback.VisualFeedbackManager
import io.mockk.coEvery
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
 * Integration tests for navigation performance optimizations.
 * Tests the complete navigation system with performance enhancements.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NavigationPerformanceIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var navController: NavController
    private lateinit var navigationManager: NavigationManager
    private lateinit var navigationStateTracker: NavigationStateTracker
    private lateinit var performanceMonitor: NavigationPerformanceMonitor
    private lateinit var navigationCache: NavigationCache
    private lateinit var destinationLoader: NavigationDestinationLoader
    private lateinit var optimizedButtonManager: OptimizedButtonResponseManager
    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: StandardTestDispatcher
    
    // Mocked dependencies
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    private lateinit var visualFeedbackManager: VisualFeedbackManager
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private lateinit var navigationAnalytics: NavigationAnalytics
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        
        // Mock dependencies
        navController = mockk(relaxed = true)
        navigationErrorHandler = mockk(relaxed = true)
        visualFeedbackManager = mockk(relaxed = true)
        hapticFeedbackManager = mockk(relaxed = true)
        navigationAnalytics = mockk(relaxed = true)
        
        // Create real instances
        performanceMonitor = NavigationPerformanceMonitor(context)
        navigationCache = NavigationCache(context, performanceMonitor)
        destinationLoader = NavigationDestinationLoader(navigationCache, performanceMonitor)
        optimizedButtonManager = OptimizedButtonResponseManager(performanceMonitor)
        navigationStateTracker = NavigationStateTrackerImpl(context)
        
        navigationManager = NavigationManagerImpl(
            navigationStateTracker = navigationStateTracker,
            navigationErrorHandler = navigationErrorHandler,
            visualFeedbackManager = visualFeedbackManager,
            hapticFeedbackManager = hapticFeedbackManager,
            navigationAnalytics = navigationAnalytics
        )
        
        navigationManager.setNavController(navController)
        
        // Setup mock behaviors
        every { navController.navigate(any<String>()) } returns Unit
        every { navController.navigate(any<String>(), any()) } returns Unit
        every { navController.popBackStack() } returns true
        coEvery { hapticFeedbackManager.triggerNavigationStart() } returns Unit
        coEvery { hapticFeedbackManager.triggerNavigationSuccess() } returns Unit
        coEvery { visualFeedbackManager.triggerFeedback(any(), any()) } returns Unit
    }
    
    @After
    fun tearDown() {
        // Clean up test data
        context.getSharedPreferences("navigation_performance_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("navigation_cache_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("navigation_state_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
    
    @Test
    fun `complete navigation flow should be optimized`() = testScope.runTest {
        // Given
        val startTime = System.currentTimeMillis()
        
        // When - Perform navigation with all optimizations
        navigationManager.navigateToMap()
        advanceTimeBy(100L)
        
        // Then
        val navigationTime = System.currentTimeMillis() - startTime
        assertTrue("Navigation should be fast", navigationTime < 500L)
        
        // Verify navigation was tracked
        verify { navigationAnalytics.trackSuccessfulNavigation(any(), any(), any()) }
        verify { navigationAnalytics.trackUserJourney(any(), any()) }
        
        // Verify feedback was triggered
        verify { hapticFeedbackManager.triggerNavigationStart() }
        verify { hapticFeedbackManager.triggerNavigationSuccess() }
    }
    
    @Test
    fun `navigation with preloaded destinations should be faster`() = testScope.runTest {
        // Given - Preload destinations
        navigationCache.cacheDestination("map")
        navigationCache.cacheDestination("friends")
        
        // When - Navigate to preloaded destination
        val startTime = System.currentTimeMillis()
        navigationManager.navigateToMap()
        advanceTimeBy(50L)
        val preloadedTime = System.currentTimeMillis() - startTime
        
        // Then - Navigate to non-preloaded destination
        val startTime2 = System.currentTimeMillis()
        navigationManager.navigateToSettings()
        advanceTimeBy(50L)
        val nonPreloadedTime = System.currentTimeMillis() - startTime2
        
        assertTrue("Preloaded navigation should be faster", preloadedTime <= nonPreloadedTime)
    }
    
    @Test
    fun `button interactions should be optimized for common buttons`() = testScope.runTest {
        // Given
        var homeNavigationCalled = false
        var mapNavigationCalled = false
        
        // When - Click common navigation buttons
        val homeStartTime = System.currentTimeMillis()
        optimizedButtonManager.handleButtonClick("home_button") {
            homeNavigationCalled = true
            navigationManager.navigateToHome()
        }
        advanceTimeBy(50L)
        val homeResponseTime = System.currentTimeMillis() - homeStartTime
        
        val mapStartTime = System.currentTimeMillis()
        optimizedButtonManager.handleButtonClick("map_button") {
            mapNavigationCalled = true
            navigationManager.navigateToMap()
        }
        advanceTimeBy(50L)
        val mapResponseTime = System.currentTimeMillis() - mapStartTime
        
        // Then
        assertTrue("Home navigation should be called", homeNavigationCalled)
        assertTrue("Map navigation should be called", mapNavigationCalled)
        assertTrue("Home button should respond quickly", homeResponseTime < 200L)
        assertTrue("Map button should respond quickly", mapResponseTime < 200L)
        
        val stats = optimizedButtonManager.getButtonPerformanceStats()
        assertTrue("Should have preloaded common buttons", stats.commonButtonsPreloaded > 0)
    }
    
    @Test
    fun `navigation state should be cached and restored efficiently`() = testScope.runTest {
        // Given - Navigate and create state
        navigationManager.navigateToMap()
        advanceTimeBy(50L)
        
        val mapState = mapOf("zoom" to 15, "center" to "user_location")
        navigationCache.cacheNavigationState("map", mapState)
        
        // When - Navigate away and back
        navigationManager.navigateToHome()
        advanceTimeBy(50L)
        
        val startTime = System.currentTimeMillis()
        navigationManager.navigateToMap()
        val cachedState = navigationCache.getCachedNavigationState("map")
        val restoreTime = System.currentTimeMillis() - startTime
        
        // Then
        assertNotNull("State should be cached", cachedState)
        assertEquals("Zoom should be restored", "15", cachedState?.get("zoom").toString())
        assertTrue("State restoration should be fast", restoreTime < 100L)
    }
    
    @Test
    fun `performance monitoring should track all operations`() = testScope.runTest {
        // Given - Perform various operations
        navigationManager.navigateToMap()
        advanceTimeBy(100L)
        
        optimizedButtonManager.handleButtonClick("test_button") {
            delay(50L) // Simulate some work
        }
        advanceTimeBy(100L)
        
        navigationCache.cacheDestination("friends")
        destinationLoader.preloadDestinations("map", listOf("home", "friends"))
        advanceTimeBy(200L)
        
        // When
        val summary = performanceMonitor.getPerformanceSummary()
        val recommendations = performanceMonitor.getPerformanceRecommendations()
        
        // Then
        assertTrue("Should track navigations", summary.totalNavigations > 0)
        assertTrue("Should have performance grade", summary.performanceGrade != null)
        assertNotNull("Should provide recommendations", recommendations)
    }
    
    @Test
    fun `cache optimization should improve overall performance`() = testScope.runTest {
        // Given - Fill cache with destinations
        val routes = listOf("home", "map", "friends", "settings")
        routes.forEach { route ->
            navigationCache.cacheDestination(route)
            navigationCache.cacheNavigationState(route, mapOf("cached" to true))
        }
        
        // When - Perform navigation operations
        val startTime = System.currentTimeMillis()
        
        routes.forEach { route ->
            val isCached = navigationCache.isDestinationCached(route)
            val cachedState = navigationCache.getCachedNavigationState(route)
            assertTrue("Route $route should be cached", isCached)
            assertNotNull("State for $route should be cached", cachedState)
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Cache operations should be fast", totalTime < 100L)
        
        val cacheStats = navigationCache.getCacheStats()
        assertEquals("Should have cached all destinations", routes.size, cacheStats.validDestinations)
        assertEquals("Should have cached all states", routes.size, cacheStats.validStates)
    }
    
    @Test
    fun `concurrent navigation operations should be handled efficiently`() = testScope.runTest {
        // Given - Multiple concurrent navigation requests
        val operations = listOf(
            { navigationManager.navigateToMap() },
            { navigationManager.navigateToFriends() },
            { navigationManager.navigateToSettings() },
            { navigationManager.navigateBack() }
        )
        
        // When - Execute operations concurrently
        val startTime = System.currentTimeMillis()
        operations.forEach { operation ->
            testScope.launch { operation() }
        }
        advanceTimeBy(200L)
        val totalTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Concurrent operations should complete quickly", totalTime < 500L)
        
        // Verify state tracker handled concurrent updates
        val currentState = navigationStateTracker.currentState.value
        assertNotNull("Should have valid current state", currentState.currentScreen)
        assertFalse("Should not be stuck in navigating state", currentState.isNavigating)
    }
    
    @Test
    fun `memory usage should be optimized`() = testScope.runTest {
        // Given - Create many cached items
        repeat(20) { index ->
            navigationCache.cacheDestination("route_$index")
            navigationCache.cacheNavigationState("state_$index", mapOf("data" to "value_$index"))
        }
        
        // When - Trigger optimization
        navigationCache.clearOldCache()
        optimizedButtonManager.optimizeButtonStates()
        advanceTimeBy(100L)
        
        // Then
        val cacheStats = navigationCache.getCacheStats()
        assertTrue("Cache should be optimized", cacheStats.totalDestinations <= 10)
        assertTrue("Memory usage should be reasonable", cacheStats.memoryUsageKB < 1000L)
        
        val buttonStats = optimizedButtonManager.getButtonPerformanceStats()
        assertTrue("Button states should be optimized", buttonStats.totalButtons <= 15)
    }
    
    @Test
    fun `error handling should not impact performance`() = testScope.runTest {
        // Given - Setup error conditions
        every { navController.navigate(any<String>()) } throws RuntimeException("Navigation error")
        
        // When - Attempt navigation with error
        val startTime = System.currentTimeMillis()
        navigationManager.navigateToMap()
        advanceTimeBy(100L)
        val errorHandlingTime = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Error handling should be fast", errorHandlingTime < 300L)
        
        // Verify error was handled
        verify { navigationErrorHandler.handleError(any(), any(), any()) }
        
        // Verify performance monitoring tracked the error
        val summary = performanceMonitor.getPerformanceSummary()
        assertTrue("Should track errors", summary.totalErrors > 0)
    }
    
    @Test
    fun `performance recommendations should be actionable`() = testScope.runTest {
        // Given - Create performance issues
        repeat(5) {
            performanceMonitor.recordNavigationTime("home", "map", 1200L) // Slow
            performanceMonitor.recordButtonResponseTime("slow_button", 400L) // Slow
            performanceMonitor.recordCacheMiss("destination", "route_$it")
        }
        
        // When
        val recommendations = performanceMonitor.getPerformanceRecommendations()
        
        // Then
        assertTrue("Should have recommendations", recommendations.isNotEmpty())
        
        val navigationRec = recommendations.find { it.type == RecommendationType.NAVIGATION_OPTIMIZATION }
        val cacheRec = recommendations.find { it.type == RecommendationType.CACHE_OPTIMIZATION }
        val buttonRec = recommendations.find { it.type == RecommendationType.BUTTON_OPTIMIZATION }
        
        assertNotNull("Should recommend navigation optimization", navigationRec)
        assertNotNull("Should recommend cache optimization", cacheRec)
        assertNotNull("Should recommend button optimization", buttonRec)
        
        recommendations.forEach { recommendation ->
            assertFalse("Description should not be empty", recommendation.description.isBlank())
            assertFalse("Actionable should not be empty", recommendation.actionable.isBlank())
            assertNotNull("Should have priority", recommendation.priority)
        }
    }
    
    @Test
    fun `performance metrics should persist and reload correctly`() = testScope.runTest {
        // Given - Record performance data
        performanceMonitor.recordNavigationTime("home", "map", 300L)
        performanceMonitor.recordButtonResponseTime("test_button", 150L)
        performanceMonitor.recordCacheHit("destination", "map")
        
        // When - Create new instance (simulating app restart)
        val newPerformanceMonitor = NavigationPerformanceMonitor(context)
        advanceTimeBy(200L) // Allow loading
        
        // Then
        val summary = newPerformanceMonitor.getPerformanceSummary()
        assertTrue("Should restore navigation data", summary.totalNavigations > 0)
        assertTrue("Should restore cache data", summary.cacheHitRate > 0)
    }
}