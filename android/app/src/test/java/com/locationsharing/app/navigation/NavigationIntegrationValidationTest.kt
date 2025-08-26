package com.locationsharing.app.navigation

import androidx.navigation.NavController
import com.locationsharing.app.navigation.performance.NavigationCache
import com.locationsharing.app.navigation.performance.NavigationDestinationLoader
import com.locationsharing.app.navigation.performance.NavigationPerformanceMonitor
import com.locationsharing.app.ui.components.feedback.HapticFeedbackManager
import com.locationsharing.app.ui.components.feedback.VisualFeedbackManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive integration test for the complete navigation system.
 * Validates all navigation components working together correctly.
 */
@ExperimentalCoroutinesApi
class NavigationIntegrationValidationTest {
    
    private lateinit var navigationManager: NavigationManagerImpl
    private lateinit var navigationStateTracker: NavigationStateTracker
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    private lateinit var visualFeedbackManager: VisualFeedbackManager
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private lateinit var navigationAnalytics: NavigationAnalytics
    private lateinit var performanceMonitor: NavigationPerformanceMonitor
    private lateinit var navigationCache: NavigationCache
    private lateinit var destinationLoader: NavigationDestinationLoader
    private lateinit var navController: NavController
    
    @Before
    fun setup() {
        // Mock all dependencies
        navigationStateTracker = mockk(relaxed = true)
        navigationErrorHandler = mockk(relaxed = true)
        visualFeedbackManager = mockk(relaxed = true)
        hapticFeedbackManager = mockk(relaxed = true)
        navigationAnalytics = mockk(relaxed = true)
        performanceMonitor = mockk(relaxed = true)
        navigationCache = mockk(relaxed = true)
        destinationLoader = mockk(relaxed = true)
        navController = mockk(relaxed = true)
        
        // Setup default state
        every { navigationStateTracker.currentState } returns MutableStateFlow(
            NavigationState(
                currentScreen = Screen.HOME,
                canNavigateBack = false,
                navigationHistory = emptyList(),
                isNavigating = false
            )
        )
        
        // Create NavigationManager instance
        navigationManager = NavigationManagerImpl(
            navigationStateTracker = navigationStateTracker,
            navigationErrorHandler = navigationErrorHandler,
            visualFeedbackManager = visualFeedbackManager,
            hapticFeedbackManager = hapticFeedbackManager,
            navigationAnalytics = navigationAnalytics,
            performanceMonitor = performanceMonitor,
            navigationCache = navigationCache,
            destinationLoader = destinationLoader
        )
        
        navigationManager.setNavController(navController)
    }
    
    @Test
    fun `complete navigation flow from home to map validates all requirements`() = runTest {
        // Given - Setup for successful navigation
        every { navigationCache.isDestinationCached(Screen.MAP.route) } returns false
        coEvery { hapticFeedbackManager.triggerNavigationStart() } returns Unit
        coEvery { hapticFeedbackManager.triggerNavigationSuccess() } returns Unit
        coEvery { visualFeedbackManager.triggerFeedback(any(), any()) } returns Unit
        
        // When - Navigate to map
        navigationManager.navigateToMap()
        
        // Then - Verify all requirements are met
        
        // Requirement 1.1 & 1.2: Button responsiveness and visual feedback
        coVerify { hapticFeedbackManager.triggerNavigationStart() }
        coVerify { hapticFeedbackManager.triggerNavigationSuccess() }
        coVerify { visualFeedbackManager.triggerFeedback(any(), any()) }
        
        // Requirement 2.1 & 2.2: Navigation functionality
        verify { navController.navigate(Screen.MAP.route) }
        verify { navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP) }
        verify { navigationStateTracker.updateCurrentScreen(Screen.MAP) }
        
        // Requirement 3.1: Consistent navigation patterns
        verify { navigationStateTracker.setNavigationInProgress(true) }
        verify { navigationStateTracker.setNavigationInProgress(false) }
        
        // Requirement 4.3: Analytics and monitoring
        verify { navigationAnalytics.trackSuccessfulNavigation(any(), any(), any()) }
        verify { navigationAnalytics.trackUserJourney(Screen.MAP.route, "button") }
        
        // Performance optimizations
        verify { performanceMonitor.recordNavigationTime(any(), any(), any()) }
        verify { navigationCache.cacheDestination(Screen.MAP.route) }
        verify { destinationLoader.preloadDestinations(any(), any()) }
    }
    
    @Test
    fun `back navigation validates all requirements`() = runTest {
        // Given - Setup navigation state with history
        every { navigationStateTracker.currentState } returns MutableStateFlow(
            NavigationState(
                currentScreen = Screen.MAP,
                canNavigateBack = true,
                navigationHistory = listOf(Screen.HOME, Screen.MAP),
                isNavigating = false
            )
        )
        every { navigationStateTracker.getPreviousScreen() } returns Screen.HOME
        every { navController.popBackStack() } returns true
        
        // When - Navigate back
        val result = navigationManager.navigateBack()
        
        // Then - Verify all requirements
        assertTrue("Back navigation should succeed", result)
        
        // Requirement 2.3: Back navigation functionality
        verify { navController.popBackStack() }
        verify { navigationStateTracker.updateCurrentScreen(Screen.HOME) }
        
        // Requirement 4.3: Analytics tracking
        verify { navigationAnalytics.trackSuccessfulNavigation(any(), any(), any()) }
        verify { navigationAnalytics.trackUserJourney(Screen.HOME.route, "back_button") }
    }
    
    @Test
    fun `navigation error handling validates all requirements`() = runTest {
        // Given - Setup for navigation failure
        val error = NavigationError.NavigationTimeout
        every { navController.navigate(any<String>()) } throws RuntimeException("Navigation failed")
        
        // When - Attempt navigation that fails
        navigationManager.navigateToMap()
        
        // Then - Verify error handling requirements
        
        // Requirement 4.1 & 4.2: Error handling and recovery
        verify { navigationErrorHandler.handleError(any(), any(), any()) }
        verify { navigationAnalytics.trackNavigationError(any()) }
        
        // Requirement 5.1: Visual feedback for errors
        coVerify { hapticFeedbackManager.triggerNavigationError() }
        coVerify { visualFeedbackManager.triggerFeedback(any(), any()) }
        
        // Navigation state should be reset
        verify { navigationStateTracker.setNavigationInProgress(false) }
    }
    
    @Test
    fun `button response system validates all requirements`() = runTest {
        // Given - Setup for button interaction
        every { navigationStateTracker.currentState } returns MutableStateFlow(
            NavigationState(
                currentScreen = Screen.HOME,
                canNavigateBack = false,
                navigationHistory = emptyList(),
                isNavigating = true // Navigation in progress
            )
        )
        
        // When - Attempt navigation while already navigating
        navigationManager.navigateToFriends()
        
        // Then - Verify button response requirements
        
        // Requirement 1.3: Prevent double-clicks/navigation
        verify(exactly = 0) { navController.navigate(Screen.FRIENDS.route) }
        
        // Performance monitoring should record blocked navigation
        verify { performanceMonitor.recordNavigationTime(any(), any(), 0L) }
    }
    
    @Test
    fun `navigation state management validates all requirements`() = runTest {
        // Given - Multiple navigation operations
        every { navigationCache.isDestinationCached(any()) } returns false
        coEvery { hapticFeedbackManager.triggerNavigationStart() } returns Unit
        coEvery { hapticFeedbackManager.triggerNavigationSuccess() } returns Unit
        coEvery { visualFeedbackManager.triggerFeedback(any(), any()) } returns Unit
        
        // When - Perform multiple navigations
        navigationManager.navigateToMap()
        navigationManager.navigateToFriends()
        navigationManager.navigateToSettings()
        
        // Then - Verify state management requirements
        
        // Requirement 3.1 & 3.3: Navigation state tracking
        verify(exactly = 3) { navigationStateTracker.recordNavigation(any(), any()) }
        verify(exactly = 3) { navigationStateTracker.updateCurrentScreen(any()) }
        verify(exactly = 6) { navigationStateTracker.setNavigationInProgress(any()) } // 2 calls per navigation
        
        // Requirement 3.4: State persistence
        verify(atLeast = 3) { navigationCache.cacheNavigationState(any(), any()) }
    }
    
    @Test
    fun `performance optimizations validate all requirements`() = runTest {
        // Given - Setup for performance testing
        every { navigationCache.isDestinationCached(Screen.MAP.route) } returns true
        coEvery { hapticFeedbackManager.triggerNavigationStart() } returns Unit
        coEvery { hapticFeedbackManager.triggerNavigationSuccess() } returns Unit
        coEvery { visualFeedbackManager.triggerFeedback(any(), any()) } returns Unit
        
        // When - Navigate to cached destination
        navigationManager.navigateToMap()
        
        // Then - Verify performance requirements
        
        // Requirement 1.2 & 2.2: Performance optimizations
        verify { navigationCache.isDestinationCached(Screen.MAP.route) }
        verify { performanceMonitor.recordNavigationTime(any(), any(), any()) }
        
        // Cached destination should not be cached again
        verify(exactly = 0) { navigationCache.cacheDestination(Screen.MAP.route) }
        
        // Preloading should still occur
        verify { destinationLoader.preloadDestinations(any(), any()) }
    }
    
    @Test
    fun `accessibility support validates all requirements`() = runTest {
        // Given - Setup for accessibility testing
        coEvery { hapticFeedbackManager.triggerNavigationStart() } returns Unit
        coEvery { hapticFeedbackManager.triggerNavigationSuccess() } returns Unit
        coEvery { visualFeedbackManager.triggerFeedback(any(), any()) } returns Unit
        
        // When - Perform navigation
        navigationManager.navigateToMap()
        
        // Then - Verify accessibility requirements
        
        // Requirement 5.1, 5.2, 5.3: Haptic and visual feedback
        coVerify { hapticFeedbackManager.triggerNavigationStart() }
        coVerify { hapticFeedbackManager.triggerNavigationSuccess() }
        coVerify(exactly = 2) { visualFeedbackManager.triggerFeedback(any(), any()) }
    }
    
    @Test
    fun `navigation security validates all requirements`() = runTest {
        // Given - Setup for security testing
        val invalidRoute = "invalid_route"
        every { navController.navigate(invalidRoute) } throws IllegalArgumentException("Invalid route")
        
        // When - Attempt navigation to invalid route (simulated)
        try {
            navController.navigate(invalidRoute)
        } catch (e: Exception) {
            navigationManager.handleNavigationError(NavigationError.UnknownError(e))
        }
        
        // Then - Verify security requirements
        
        // Requirement 3.1 & 4.1: Route validation and security
        verify { navigationErrorHandler.handleError(any(), any(), any()) }
        verify { navigationAnalytics.trackNavigationError(any()) }
        
        // Navigation state should be protected
        verify { navigationStateTracker.setNavigationInProgress(false) }
    }
    
    @Test
    fun `complete user journey validates all requirements end-to-end`() = runTest {
        // Given - Setup for complete user journey
        every { navigationCache.isDestinationCached(any()) } returns false
        every { navigationStateTracker.getPreviousScreen() } returns Screen.HOME
        every { navController.popBackStack() } returns true
        coEvery { hapticFeedbackManager.triggerNavigationStart() } returns Unit
        coEvery { hapticFeedbackManager.triggerNavigationSuccess() } returns Unit
        coEvery { visualFeedbackManager.triggerFeedback(any(), any()) } returns Unit
        
        // Simulate state changes for back navigation
        val stateFlow = MutableStateFlow(
            NavigationState(
                currentScreen = Screen.HOME,
                canNavigateBack = false,
                navigationHistory = emptyList(),
                isNavigating = false
            )
        )
        every { navigationStateTracker.currentState } returns stateFlow
        
        // When - Complete user journey: Home -> Map -> Friends -> Back to Home
        navigationManager.navigateToMap()
        
        // Update state for map screen
        stateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME, Screen.MAP),
            isNavigating = false
        )
        
        navigationManager.navigateToFriends()
        
        // Update state for friends screen
        stateFlow.value = NavigationState(
            currentScreen = Screen.FRIENDS,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME, Screen.MAP, Screen.FRIENDS),
            isNavigating = false
        )
        
        val backResult = navigationManager.navigateBack()
        
        // Then - Verify complete journey requirements
        assertTrue("Back navigation should succeed", backResult)
        
        // All navigation operations should be tracked
        verify(exactly = 2) { navController.navigate(any<String>()) }
        verify(exactly = 1) { navController.popBackStack() }
        
        // All state changes should be recorded
        verify(exactly = 2) { navigationStateTracker.recordNavigation(any(), any()) }
        verify(exactly = 3) { navigationStateTracker.updateCurrentScreen(any()) }
        
        // All analytics should be tracked
        verify(exactly = 3) { navigationAnalytics.trackSuccessfulNavigation(any(), any(), any()) }
        verify(exactly = 3) { navigationAnalytics.trackUserJourney(any(), any()) }
        
        // All feedback should be provided
        coVerify(exactly = 2) { hapticFeedbackManager.triggerNavigationStart() }
        coVerify(exactly = 3) { hapticFeedbackManager.triggerNavigationSuccess() }
        
        // Performance should be monitored
        verify(exactly = 3) { performanceMonitor.recordNavigationTime(any(), any(), any()) }
        
        // Destinations should be cached and preloaded
        verify(exactly = 2) { navigationCache.cacheDestination(any()) }
        verify(exactly = 2) { destinationLoader.preloadDestinations(any(), any()) }
    }
}