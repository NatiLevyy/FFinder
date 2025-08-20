package com.locationsharing.app.navigation

import android.content.Context
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for navigation errors with user-friendly recovery mechanisms.
 * Provides comprehensive error handling, fallback navigation, and analytics.
 */
@Singleton
class NavigationErrorHandler @Inject constructor(
    private val navigationAnalytics: NavigationAnalytics
) {
    
    companion object {
        private const val TAG = "NavigationErrorHandler"
        private const val RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private val errorHandlingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val retryAttempts = mutableMapOf<String, Int>()
    
    /**
     * Handle navigation errors with comprehensive recovery mechanisms.
     * @param error The navigation error that occurred
     * @param navController Optional navigation controller for fallback navigation
     * @param context Optional context for showing user messages
     * @param onRetry Optional callback for retry operations
     */
    fun handleError(
        error: NavigationError,
        navController: NavController? = null,
        context: Context? = null,
        onRetry: (() -> Unit)? = null
    ) {
        // Log error for debugging and analytics
        logError(error)
        
        // Track error in analytics
        navigationAnalytics.trackNavigationError(error)
        
        when (error) {
            is NavigationError.NavigationTimeout -> {
                handleTimeoutError(error, navController, onRetry)
            }
            
            is NavigationError.InvalidRoute -> {
                handleInvalidRouteError(error, navController)
            }
            
            is NavigationError.NavigationControllerNotFound -> {
                handleControllerNotFoundError(error, context)
            }
            
            is NavigationError.UnknownError -> {
                handleUnknownError(error, navController, onRetry)
            }
            
            is NavigationError.InvalidNavigationState -> {
                handleInvalidStateError(error, navController)
            }
            
            is NavigationError.NavigationInProgress -> {
                handleNavigationInProgressError(error, onRetry)
            }
        }
    }
    
    /**
     * Handle navigation timeout errors with retry mechanism.
     */
    private fun handleTimeoutError(
        error: NavigationError.NavigationTimeout,
        navController: NavController?,
        onRetry: (() -> Unit)?
    ) {
        Timber.e(TAG, "Navigation timeout occurred")
        
        val retryKey = "timeout_${System.currentTimeMillis()}"
        val currentAttempts = retryAttempts.getOrDefault(retryKey, 0)
        
        if (currentAttempts < MAX_RETRY_ATTEMPTS && onRetry != null) {
            retryAttempts[retryKey] = currentAttempts + 1
            
            errorHandlingScope.launch {
                delay(RETRY_DELAY_MS * (currentAttempts + 1)) // Exponential backoff
                Timber.d(TAG, "Retrying navigation after timeout (attempt ${currentAttempts + 1})")
                onRetry()
            }
        } else {
            // Max retries reached, fallback to home
            fallbackToHome(navController)
            retryAttempts.remove(retryKey)
        }
    }
    
    /**
     * Handle invalid route errors with fallback to home.
     */
    private fun handleInvalidRouteError(
        error: NavigationError.InvalidRoute,
        navController: NavController?
    ) {
        Timber.e(TAG, "Invalid route navigation attempted")
        fallbackToHome(navController)
    }
    
    /**
     * Handle navigation controller not found errors.
     */
    private fun handleControllerNotFoundError(
        error: NavigationError.NavigationControllerNotFound,
        context: Context?
    ) {
        Timber.e(TAG, "Navigation controller not found - critical error")
        
        // This is a critical error that might require app restart
        // For now, we log it and track it in analytics
        navigationAnalytics.trackCriticalNavigationError("controller_not_found")
        
        // Could potentially trigger activity recreation here if context is available
        context?.let {
            // Implementation would depend on how we want to handle this critical error
            Timber.w(TAG, "Context available for potential activity restart")
        }
    }
    
    /**
     * Handle unknown errors with retry mechanism.
     */
    private fun handleUnknownError(
        error: NavigationError.UnknownError,
        navController: NavController?,
        onRetry: (() -> Unit)?
    ) {
        Timber.e(error.throwable, "Unknown navigation error occurred")
        
        val retryKey = "unknown_${error.throwable.javaClass.simpleName}"
        val currentAttempts = retryAttempts.getOrDefault(retryKey, 0)
        
        if (currentAttempts < MAX_RETRY_ATTEMPTS && onRetry != null) {
            retryAttempts[retryKey] = currentAttempts + 1
            
            errorHandlingScope.launch {
                delay(RETRY_DELAY_MS)
                Timber.d(TAG, "Retrying navigation after unknown error (attempt ${currentAttempts + 1})")
                onRetry()
            }
        } else {
            // Max retries reached, fallback to home
            fallbackToHome(navController)
            retryAttempts.remove(retryKey)
        }
    }
    
    /**
     * Handle invalid navigation state errors.
     */
    private fun handleInvalidStateError(
        error: NavigationError.InvalidNavigationState,
        navController: NavController?
    ) {
        Timber.e(TAG, "Invalid navigation state detected - resetting to home")
        fallbackToHome(navController)
    }
    
    /**
     * Handle navigation in progress errors with queuing.
     */
    private fun handleNavigationInProgressError(
        error: NavigationError.NavigationInProgress,
        onRetry: (() -> Unit)?
    ) {
        Timber.w(TAG, "Navigation attempted while another navigation is in progress")
        
        // Queue the navigation for retry after a short delay
        onRetry?.let { retry ->
            errorHandlingScope.launch {
                delay(500L) // Short delay to allow current navigation to complete
                Timber.d(TAG, "Retrying queued navigation")
                retry()
            }
        }
    }
    
    /**
     * Fallback navigation to home screen.
     */
    private fun fallbackToHome(navController: NavController?) {
        navController?.let { controller ->
            try {
                Timber.d(TAG, "Executing fallback navigation to home")
                
                // Clear back stack and navigate to home
                controller.popBackStack(controller.graph.startDestinationId, false)
                
                navigationAnalytics.trackFallbackNavigation("home")
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute fallback navigation to home")
                navigationAnalytics.trackCriticalNavigationError("fallback_failed")
            }
        } ?: run {
            Timber.w(TAG, "Cannot execute fallback navigation - no NavController available")
        }
    }
    
    /**
     * Get user-friendly error message for the given navigation error.
     * @param error The navigation error
     * @return User-friendly error message
     */
    fun getUserFriendlyMessage(error: NavigationError): String {
        return when (error) {
            is NavigationError.NavigationTimeout -> 
                "Navigation is taking longer than expected. Please try again."
            
            is NavigationError.InvalidRoute -> 
                "The requested page is not available. Returning to home."
            
            is NavigationError.NavigationControllerNotFound -> 
                "Navigation system error. Please restart the app."
            
            is NavigationError.UnknownError -> 
                "An unexpected error occurred. Please try again."
            
            is NavigationError.InvalidNavigationState -> 
                "Navigation state error. Resetting to home screen."
            
            is NavigationError.NavigationInProgress -> 
                "Please wait for the current navigation to complete."
        }
    }
    
    /**
     * Log navigation error with appropriate level.
     */
    private fun logError(error: NavigationError) {
        when (error) {
            is NavigationError.NavigationTimeout -> {
                Timber.e(TAG, "Navigation timeout occurred")
            }
            is NavigationError.InvalidRoute -> {
                Timber.e(TAG, "Invalid route navigation attempted")
            }
            is NavigationError.NavigationControllerNotFound -> {
                Timber.e(TAG, "Navigation controller not found - critical error")
            }
            is NavigationError.UnknownError -> {
                Timber.e(error.throwable, "Unknown navigation error: ${error.throwable.message}")
            }
            is NavigationError.InvalidNavigationState -> {
                Timber.e(TAG, "Invalid navigation state detected")
            }
            is NavigationError.NavigationInProgress -> {
                Timber.w(TAG, "Navigation attempted while another navigation is in progress")
            }
        }
    }
}