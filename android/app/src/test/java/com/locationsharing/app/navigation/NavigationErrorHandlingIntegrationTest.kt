package com.locationsharing.app.navigation

import android.content.Context
import androidx.navigation.NavController
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for comprehensive navigation error handling scenarios.
 * Tests error recovery mechanisms, fallback navigation, and analytics tracking.
 */
@ExperimentalCoroutinesApi
class NavigationErrorHandlingIntegrationTest {
    
    @MockK
    private lateinit var mockNavController: NavController
    
    @MockK
    private lateinit var mockContext: Context
    
    @MockK
    private lateinit var mockNavigationAnalytics: NavigationAnalytics
    
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    private lateinit var testDispatcher: TestCoroutineDispatcher
    
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        testDispatcher = TestCoroutineDispatcher()
        navigationErrorHandler = NavigationErrorHandler(mockNavigationAnalytics)
    }
    
    @Test
    fun `handleError with NavigationTimeout should retry with exponential backoff`() = runBlockingTest {
        // Given
        val error = NavigationError.NavigationTimeout
        var retryCount = 0
        val onRetry = { retryCount++ }
        
        // When
        navigationErrorHandler.handleError(
            error = error,
            navController = mockNavController,
            onRetry = onRetry
        )
        
        // Advance time to trigger retry
        testDispatcher.advanceTimeBy(1000L)
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        assertEquals(1, retryCount)
    }
    
    @Test
    fun `handleError with NavigationTimeout should fallback to home after max retries`() = runBlockingTest {
        // Given
        val error = NavigationError.NavigationTimeout
        var retryCount = 0
        val onRetry = { retryCount++ }
        
        every { mockNavController.graph } returns mockk {
            every { startDestinationId } returns 1
        }
        every { mockNavController.popBackStack(any<Int>(), any()) } returns true
        
        // When - simulate multiple retries
        repeat(4) { // More than MAX_RETRY_ATTEMPTS (3)
            navigationErrorHandler.handleError(
                error = error,
                navController = mockNavController,
                onRetry = onRetry
            )
            testDispatcher.advanceTimeBy(2000L) // Advance enough time for retry
        }
        
        // Then
        verify { mockNavigationAnalytics.trackFallbackNavigation("home") }
        verify { mockNavController.popBackStack(1, false) }
    }
    
    @Test
    fun `handleError with InvalidRoute should immediately fallback to home`() {
        // Given
        val error = NavigationError.InvalidRoute
        
        every { mockNavController.graph } returns mockk {
            every { startDestinationId } returns 1
        }
        every { mockNavController.popBackStack(any<Int>(), any()) } returns true
        
        // When
        navigationErrorHandler.handleError(
            error = error,
            navController = mockNavController
        )
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        verify { mockNavigationAnalytics.trackFallbackNavigation("home") }
        verify { mockNavController.popBackStack(1, false) }
    }
    
    @Test
    fun `handleError with NavigationControllerNotFound should track critical error`() {
        // Given
        val error = NavigationError.NavigationControllerNotFound
        
        // When
        navigationErrorHandler.handleError(
            error = error,
            context = mockContext
        )
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        verify { mockNavigationAnalytics.trackCriticalNavigationError("controller_not_found") }
    }
    
    @Test
    fun `handleError with UnknownError should retry and then fallback`() = runBlockingTest {
        // Given
        val exception = RuntimeException("Test exception")
        val error = NavigationError.UnknownError(exception)
        var retryCount = 0
        val onRetry = { retryCount++ }
        
        every { mockNavController.graph } returns mockk {
            every { startDestinationId } returns 1
        }
        every { mockNavController.popBackStack(any<Int>(), any()) } returns true
        
        // When - simulate retries exceeding max attempts
        repeat(4) {
            navigationErrorHandler.handleError(
                error = error,
                navController = mockNavController,
                onRetry = onRetry
            )
            testDispatcher.advanceTimeBy(1000L)
        }
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        verify { mockNavigationAnalytics.trackFallbackNavigation("home") }
        assertTrue(retryCount >= 3) // Should have retried at least 3 times
    }
    
    @Test
    fun `handleError with InvalidNavigationState should reset to home`() {
        // Given
        val error = NavigationError.InvalidNavigationState
        
        every { mockNavController.graph } returns mockk {
            every { startDestinationId } returns 1
        }
        every { mockNavController.popBackStack(any<Int>(), any()) } returns true
        
        // When
        navigationErrorHandler.handleError(
            error = error,
            navController = mockNavController
        )
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        verify { mockNavigationAnalytics.trackFallbackNavigation("home") }
        verify { mockNavController.popBackStack(1, false) }
    }
    
    @Test
    fun `handleError with NavigationInProgress should queue retry`() = runBlockingTest {
        // Given
        val error = NavigationError.NavigationInProgress
        var retryCount = 0
        val onRetry = { retryCount++ }
        
        // When
        navigationErrorHandler.handleError(
            error = error,
            onRetry = onRetry
        )
        
        // Advance time to trigger queued retry
        testDispatcher.advanceTimeBy(500L)
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        assertEquals(1, retryCount)
    }
    
    @Test
    fun `fallback navigation failure should track critical error`() {
        // Given
        val error = NavigationError.InvalidRoute
        
        every { mockNavController.graph } returns mockk {
            every { startDestinationId } returns 1
        }
        every { mockNavController.popBackStack(any<Int>(), any()) } throws RuntimeException("Navigation failed")
        
        // When
        navigationErrorHandler.handleError(
            error = error,
            navController = mockNavController
        )
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error) }
        verify { mockNavigationAnalytics.trackCriticalNavigationError("fallback_failed") }
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate messages for all error types`() {
        // Given & When & Then
        val timeoutMessage = navigationErrorHandler.getUserFriendlyMessage(NavigationError.NavigationTimeout)
        assertEquals("Navigation is taking longer than expected. Please try again.", timeoutMessage)
        
        val invalidRouteMessage = navigationErrorHandler.getUserFriendlyMessage(NavigationError.InvalidRoute)
        assertEquals("The requested page is not available. Returning to home.", invalidRouteMessage)
        
        val controllerNotFoundMessage = navigationErrorHandler.getUserFriendlyMessage(NavigationError.NavigationControllerNotFound)
        assertEquals("Navigation system error. Please restart the app.", controllerNotFoundMessage)
        
        val unknownErrorMessage = navigationErrorHandler.getUserFriendlyMessage(NavigationError.UnknownError(RuntimeException()))
        assertEquals("An unexpected error occurred. Please try again.", unknownErrorMessage)
        
        val invalidStateMessage = navigationErrorHandler.getUserFriendlyMessage(NavigationError.InvalidNavigationState)
        assertEquals("Navigation state error. Resetting to home screen.", invalidStateMessage)
        
        val inProgressMessage = navigationErrorHandler.getUserFriendlyMessage(NavigationError.NavigationInProgress)
        assertEquals("Please wait for the current navigation to complete.", inProgressMessage)
    }
    
    @Test
    fun `error handling should work without optional parameters`() {
        // Given
        val error = NavigationError.NavigationTimeout
        
        // When - call with minimal parameters
        navigationErrorHandler.handleError(error)
        
        // Then - should not throw exception and should track error
        verify { mockNavigationAnalytics.trackNavigationError(error) }
    }
    
    @Test
    fun `multiple concurrent error handling should not interfere`() = runBlockingTest {
        // Given
        val error1 = NavigationError.NavigationTimeout
        val error2 = NavigationError.UnknownError(RuntimeException("Test"))
        var retry1Count = 0
        var retry2Count = 0
        
        // When
        navigationErrorHandler.handleError(error1, onRetry = { retry1Count++ })
        navigationErrorHandler.handleError(error2, onRetry = { retry2Count++ })
        
        testDispatcher.advanceTimeBy(1000L)
        
        // Then
        verify { mockNavigationAnalytics.trackNavigationError(error1) }
        verify { mockNavigationAnalytics.trackNavigationError(error2) }
        assertEquals(1, retry1Count)
        assertEquals(1, retry2Count)
    }
}