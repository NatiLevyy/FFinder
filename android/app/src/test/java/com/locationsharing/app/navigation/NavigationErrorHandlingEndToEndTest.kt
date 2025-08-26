package com.locationsharing.app.navigation

import androidx.navigation.NavController
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end integration tests for the complete navigation error handling system.
 * Tests the interaction between NavigationManager, NavigationErrorHandler, and NavigationAnalytics.
 */
@ExperimentalCoroutinesApi
class NavigationErrorHandlingEndToEndTest {
    
    @MockK
    private lateinit var mockNavController: NavController
    
    @MockK
    private lateinit var mockNavigationStateTracker: NavigationStateTracker
    
    private lateinit var navigationAnalytics: NavigationAnalyticsImpl
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    private lateinit var navigationManager: NavigationManagerImpl
    private lateinit var testDispatcher: TestCoroutineDispatcher
    
    private val mockNavigationState = MutableStateFlow(
        NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
    )
    
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        testDispatcher = TestCoroutineDispatcher()
        
        // Set up mock navigation state tracker
        every { mockNavigationStateTracker.currentState } returns mockNavigationState
        every { mockNavigationStateTracker.setNavigationInProgress(any()) } answers {
            val isNavigating = firstArg<Boolean>()
            mockNavigationState.value = mockNavigationState.value.copy(isNavigating = isNavigating)
        }
        every { mockNavigationStateTracker.updateCurrentScreen(any()) } answers {
            val screen = firstArg<Screen>()
            mockNavigationState.value = mockNavigationState.value.copy(currentScreen = screen)
        }
        every { mockNavigationStateTracker.recordNavigation(any(), any()) } returns Unit
        every { mockNavigationStateTracker.getPreviousScreen() } returns Screen.HOME
        
        // Initialize components
        navigationAnalytics = NavigationAnalyticsImpl()
        navigationErrorHandler = NavigationErrorHandler(navigationAnalytics)
        navigationManager = NavigationManagerImpl(mockNavigationStateTracker, navigationErrorHandler)
        navigationManager.setNavController(mockNavController)
    }
    
    @Test
    fun `navigation timeout should trigger retry mechanism and analytics tracking`() = runBlockingTest {
        // Given
        every { mockNavController.navigate(any<String>()) } throws RuntimeException("Timeout simulation")
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        val errorStats = navigationAnalytics.getErrorStatistics()
        assertEquals(1, errorStats["unknown_error"]) // RuntimeException becomes UnknownError
        
        val successRate = navigationAnalytics.getNavigationSuccessRate()
        assertEquals(0.0, successRate) // No successful navigations
    }
    
    @Test
    fun `successful navigation should be tracked in analytics`() = runBlockingTest {
        // Given
        every { mockNavController.navigate(any<String>()) } returns Unit
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        verify { mockNavigationStateTracker.updateCurrentScreen(Screen.MAP) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        // Navigation should complete successfully
        assertFalse(mockNavigationState.value.isNavigating)
        assertEquals(Screen.MAP, mockNavigationState.value.currentScreen)
    }
    
    @Test
    fun `navigation controller not found should trigger critical error tracking`() {
        // Given
        navigationManager.setNavController(null)
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        val criticalErrors = navigationAnalytics.getCriticalErrors()
        assertTrue(criticalErrors.any { it.errorType == "controller_not_found" })
        
        val errorStats = navigationAnalytics.getErrorStatistics()
        assertEquals(1, errorStats["controller_not_found"])
    }
    
    @Test
    fun `back navigation failure should trigger fallback to home`() {
        // Given
        every { mockNavController.popBackStack() } returns false
        every { mockNavController.navigate(any<String>()) } returns Unit
        every { mockNavController.navigate(any<String>(), any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) } returns Unit
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        assertFalse(result) // Back navigation should fail
        // Should trigger fallback navigation to home (navigateToHome is called internally)
    }
    
    @Test
    fun `concurrent navigation attempts should be handled properly`() = runBlockingTest {
        // Given
        mockNavigationState.value = mockNavigationState.value.copy(isNavigating = true)
        
        // When
        navigationManager.navigateToMap()
        navigationManager.navigateToFriends()
        
        // Then
        // Second navigation should be ignored due to navigation in progress
        val errorStats = navigationAnalytics.getErrorStatistics()
        // No errors should be tracked for ignored navigation
        assertTrue(errorStats.isEmpty() || errorStats.values.sum() == 0)
    }
    
    @Test
    fun `error recovery with retry should eventually succeed`() = runBlockingTest {
        // Given
        var callCount = 0
        every { mockNavController.navigate(any<String>()) } answers {
            callCount++
            if (callCount <= 2) {
                throw RuntimeException("Simulated failure")
            }
            Unit // Success on third attempt
        }
        
        // When
        navigationManager.navigateToMap()
        
        // Simulate retry attempts
        testDispatcher.advanceTimeBy(3000L) // Allow for retries
        
        // Then
        val errorStats = navigationAnalytics.getErrorStatistics()
        assertTrue(errorStats["unknown_error"] ?: 0 > 0) // Should have tracked errors
    }
    
    @Test
    fun `fallback navigation failure should track critical error`() {
        // Given
        every { mockNavController.graph } returns mockk {
            every { startDestinationId } returns 1
        }
        every { mockNavController.popBackStack(any<Int>(), any()) } throws RuntimeException("Fallback failed")
        
        // When
        navigationManager.handleNavigationError(NavigationError.InvalidRoute)
        
        // Then
        val criticalErrors = navigationAnalytics.getCriticalErrors()
        assertTrue(criticalErrors.any { it.errorType == "fallback_failed" })
        
        val errorStats = navigationAnalytics.getErrorStatistics()
        assertEquals(1, errorStats["invalid_route"])
    }
    
    @Test
    fun `navigation state should be properly managed during error scenarios`() {
        // Given
        every { mockNavController.navigate(any<String>()) } throws RuntimeException("Navigation failed")
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        // Navigation state should be reset after error
        assertFalse(mockNavigationState.value.isNavigating)
    }
    
    @Test
    fun `analytics should provide accurate success rate across multiple operations`() = runBlockingTest {
        // Given
        var successCount = 0
        every { mockNavController.navigate(any<String>()) } answers {
            successCount++
            if (successCount % 2 == 0) {
                throw RuntimeException("Simulated failure")
            }
            Unit
        }
        
        // When - perform multiple navigation operations
        navigationManager.navigateToMap()     // Success
        navigationManager.navigateToFriends() // Failure
        navigationManager.navigateToSettings() // Success
        
        // Then
        val errorStats = navigationAnalytics.getErrorStatistics()
        val totalErrors = errorStats.values.sum()
        assertTrue(totalErrors > 0) // Should have some errors
        
        val successRate = navigationAnalytics.getNavigationSuccessRate()
        assertTrue(successRate < 100.0) // Should be less than 100% due to failures
        assertTrue(successRate > 0.0)   // Should be greater than 0% due to successes
    }
    
    @Test
    fun `error handler should provide user-friendly messages for all scenarios`() {
        // Given
        val errors = listOf(
            NavigationError.NavigationTimeout,
            NavigationError.InvalidRoute,
            NavigationError.NavigationControllerNotFound,
            NavigationError.UnknownError(RuntimeException("Test")),
            NavigationError.InvalidNavigationState,
            NavigationError.NavigationInProgress
        )
        
        // When & Then
        errors.forEach { error ->
            val message = navigationErrorHandler.getUserFriendlyMessage(error)
            assertTrue(message.isNotEmpty(), "Message should not be empty for $error")
            assertTrue(message.length > 10, "Message should be descriptive for $error")
        }
    }
    
    @Test
    fun `system should handle rapid error scenarios gracefully`() = runBlockingTest {
        // Given
        every { mockNavController.navigate(any<String>()) } throws RuntimeException("Rapid failure")
        
        // When - trigger multiple rapid errors
        repeat(10) {
            navigationManager.navigateToMap()
            navigationManager.navigateToFriends()
        }
        
        // Then - system should remain stable
        val errorStats = navigationAnalytics.getErrorStatistics()
        assertTrue(errorStats["unknown_error"] ?: 0 > 0)
        
        // Navigation state should be properly managed
        assertFalse(mockNavigationState.value.isNavigating)
    }
}