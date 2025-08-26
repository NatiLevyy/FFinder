package com.locationsharing.app.navigation

/**
 * Sealed class representing different types of navigation errors.
 */
sealed class NavigationError(val message: String) {
    
    /**
     * Navigation operation timed out.
     */
    object NavigationTimeout : NavigationError("Navigation operation timed out")
    
    /**
     * Invalid route provided for navigation.
     */
    data class InvalidRoute(val route: String) : NavigationError("Invalid route: $route")
    
    /**
     * Navigation controller not found or not initialized.
     */
    object NavigationControllerNotFound : NavigationError("Navigation controller not found")
    
    /**
     * Navigation is already in progress.
     */
    object NavigationInProgress : NavigationError("Navigation already in progress")
    
    /**
     * Invalid navigation state.
     */
    data class InvalidNavigationState(val state: String) : NavigationError("Invalid navigation state: $state")
    
    /**
     * Unknown navigation error.
     */
    data class UnknownError(val throwable: Throwable) : NavigationError("Unknown navigation error: ${throwable.message}")
}