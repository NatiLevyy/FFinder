package com.locationsharing.app.navigation

import androidx.navigation.NavController

/**
 * Interface for managing navigation throughout the application.
 * Provides centralized navigation control with error handling and state management.
 */
interface NavigationManager {
    /**
     * Navigate to the home screen.
     */
    fun navigateToHome()
    
    /**
     * Navigate to the map screen.
     */
    fun navigateToMap()
    
    /**
     * Navigate to the map screen with startSharing parameter.
     * @param startSharing Whether to start location sharing immediately
     */
    fun navigateToMap(startSharing: Boolean)
    
    /**
     * Navigate to the map screen and focus on a specific friend.
     * @param friendId The ID of the friend to focus on
     */
    fun navigateToMapWithFriend(friendId: String)
    
    /**
     * Navigate to the friends screen.
     */
    fun navigateToFriends()

    /**
     * Navigate to the friends hub screen.
     */
    fun navigateToFriendsHub()
    
    /**
     * Navigate to the settings screen.
     */
    fun navigateToSettings()
    
    /**
     * Navigate to the invite friends screen.
     */
    fun navigateToInviteFriends()
    
    /**
     * Navigate to the search friends screen.
     */
    fun navigateToSearchFriends()
    
    /**
     * Handle back navigation.
     * @return true if navigation was handled, false if should use system back
     */
    fun navigateBack(): Boolean
    
    /**
     * Handle navigation errors with appropriate recovery mechanisms.
     * @param error The navigation error that occurred
     */
    fun handleNavigationError(error: NavigationError)
    
    /**
     * Set the navigation controller for this manager.
     * @param navController The NavController to use for navigation
     */
    fun setNavController(navController: NavController)
}