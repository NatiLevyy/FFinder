package com.locationsharing.app.ui.home

/**
 * Sealed class representing all possible events that can occur on the FFinder Home Screen.
 * 
 * Enhanced with navigation state events for proper button responsiveness
 * and loading states. These events are used to communicate user interactions
 * and system events from the UI layer to the ViewModel for state management.
 */
sealed class HomeScreenEvent {
    
    /**
     * User tapped the primary "Start Live Sharing" button
     */
    object StartSharing : HomeScreenEvent()
    
    /**
     * User tapped the "Friends" navigation button
     */
    object NavigateToFriends : HomeScreenEvent()
    
    /**
     * User tapped the "Settings" navigation button
     */
    object NavigateToSettings : HomeScreenEvent()
    
    /**
     * User tapped the "What's New" teaser card
     */
    object ShowWhatsNew : HomeScreenEvent()
    
    /**
     * User dismissed the "What's New" dialog
     */
    object DismissWhatsNew : HomeScreenEvent()
    
    /**
     * Location permission was granted by the user
     */
    object LocationPermissionGranted : HomeScreenEvent()
    
    /**
     * Location permission was denied by the user
     */
    object LocationPermissionDenied : HomeScreenEvent()
    
    /**
     * Map preview failed to load
     */
    object MapLoadError : HomeScreenEvent()
    
    /**
     * Screen configuration changed (e.g., rotation, window size)
     */
    data class ScreenConfigurationChanged(val isNarrowScreen: Boolean) : HomeScreenEvent()
    
    /**
     * Animation preferences changed (accessibility setting)
     */
    data class AnimationPreferencesChanged(val animationsEnabled: Boolean) : HomeScreenEvent()
    
    /**
     * Navigation to map screen started
     */
    object NavigationToMapStarted : HomeScreenEvent()
    
    /**
     * Navigation to map screen completed
     */
    object NavigationToMapCompleted : HomeScreenEvent()
    
    /**
     * Navigation to friends screen started
     */
    object NavigationToFriendsStarted : HomeScreenEvent()
    
    /**
     * Navigation to friends screen completed
     */
    object NavigationToFriendsCompleted : HomeScreenEvent()
    
    /**
     * Navigation to settings screen started
     */
    object NavigationToSettingsStarted : HomeScreenEvent()
    
    /**
     * Navigation to settings screen completed
     */
    object NavigationToSettingsCompleted : HomeScreenEvent()
    
    /**
     * Navigation operation failed
     */
    data class NavigationFailed(val error: String) : HomeScreenEvent()
}