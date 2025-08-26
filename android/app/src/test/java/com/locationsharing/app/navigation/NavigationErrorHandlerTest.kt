package com.locationsharing.app.navigation

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NavigationErrorHandlerTest {
    
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    
    @Before
    fun setUp() {
        navigationErrorHandler = NavigationErrorHandler()
    }
    
    @Test
    fun `handleError should handle NavigationTimeout error`() {
        // Given
        val error = NavigationError.NavigationTimeout
        
        // When & Then - Should not throw exception
        navigationErrorHandler.handleError(error)
    }
    
    @Test
    fun `handleError should handle InvalidRoute error`() {
        // Given
        val error = NavigationError.InvalidRoute
        
        // When & Then - Should not throw exception
        navigationErrorHandler.handleError(error)
    }
    
    @Test
    fun `handleError should handle NavigationControllerNotFound error`() {
        // Given
        val error = NavigationError.NavigationControllerNotFound
        
        // When & Then - Should not throw exception
        navigationErrorHandler.handleError(error)
    }
    
    @Test
    fun `handleError should handle UnknownError with throwable`() {
        // Given
        val throwable = RuntimeException("Test exception")
        val error = NavigationError.UnknownError(throwable)
        
        // When & Then - Should not throw exception
        navigationErrorHandler.handleError(error)
    }
    
    @Test
    fun `handleError should handle InvalidNavigationState error`() {
        // Given
        val error = NavigationError.InvalidNavigationState
        
        // When & Then - Should not throw exception
        navigationErrorHandler.handleError(error)
    }
    
    @Test
    fun `handleError should handle NavigationInProgress error`() {
        // Given
        val error = NavigationError.NavigationInProgress
        
        // When & Then - Should not throw exception
        navigationErrorHandler.handleError(error)
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate message for NavigationTimeout`() {
        // Given
        val error = NavigationError.NavigationTimeout
        
        // When
        val message = navigationErrorHandler.getUserFriendlyMessage(error)
        
        // Then
        assert(message == "Navigation is taking longer than expected. Please try again.")
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate message for InvalidRoute`() {
        // Given
        val error = NavigationError.InvalidRoute
        
        // When
        val message = navigationErrorHandler.getUserFriendlyMessage(error)
        
        // Then
        assert(message == "The requested page is not available. Returning to home.")
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate message for NavigationControllerNotFound`() {
        // Given
        val error = NavigationError.NavigationControllerNotFound
        
        // When
        val message = navigationErrorHandler.getUserFriendlyMessage(error)
        
        // Then
        assert(message == "Navigation system error. Please restart the app.")
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate message for UnknownError`() {
        // Given
        val throwable = RuntimeException("Test exception")
        val error = NavigationError.UnknownError(throwable)
        
        // When
        val message = navigationErrorHandler.getUserFriendlyMessage(error)
        
        // Then
        assert(message == "An unexpected error occurred. Please try again.")
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate message for InvalidNavigationState`() {
        // Given
        val error = NavigationError.InvalidNavigationState
        
        // When
        val message = navigationErrorHandler.getUserFriendlyMessage(error)
        
        // Then
        assert(message == "Navigation state error. Resetting to home screen.")
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate message for NavigationInProgress`() {
        // Given
        val error = NavigationError.NavigationInProgress
        
        // When
        val message = navigationErrorHandler.getUserFriendlyMessage(error)
        
        // Then
        assert(message == "Please wait for the current navigation to complete.")
    }
    
    @Test
    fun `all error types should have user friendly messages`() {
        // Given
        val allErrors = listOf(
            NavigationError.NavigationTimeout,
            NavigationError.InvalidRoute,
            NavigationError.NavigationControllerNotFound,
            NavigationError.UnknownError(RuntimeException("Test")),
            NavigationError.InvalidNavigationState,
            NavigationError.NavigationInProgress
        )
        
        // When & Then
        allErrors.forEach { error ->
            val message = navigationErrorHandler.getUserFriendlyMessage(error)
            assert(message.isNotBlank()) { "Error $error should have a user-friendly message" }
        }
    }
}