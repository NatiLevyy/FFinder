package com.locationsharing.app.navigation

import android.os.Bundle
import kotlinx.coroutines.flow.StateFlow

/**
 * Data class representing the current navigation state.
 */
data class NavigationState(
    val currentScreen: Screen,
    val canNavigateBack: Boolean,
    val navigationHistory: List<Screen>,
    val isNavigating: Boolean
)



/**
 * Interface for tracking navigation state throughout the application.
 * Provides reactive state updates and navigation history management.
 */
interface NavigationStateTracker {
    /**
     * Current navigation state as a reactive flow.
     */
    val currentState: StateFlow<NavigationState>
    
    /**
     * Update the current screen in the navigation state.
     * @param screen The new current screen
     */
    fun updateCurrentScreen(screen: Screen)
    
    /**
     * Record a navigation event from one screen to another.
     * @param from The screen being navigated from
     * @param to The screen being navigated to
     */
    fun recordNavigation(from: Screen, to: Screen)
    
    /**
     * Set the navigation in progress state.
     * @param isNavigating Whether navigation is currently in progress
     */
    fun setNavigationInProgress(isNavigating: Boolean)
    
    /**
     * Clear the navigation history.
     */
    fun clearHistory()
    
    /**
     * Get the previous screen in the navigation history.
     * @return The previous screen or null if no history exists
     */
    fun getPreviousScreen(): Screen?
    
    /**
     * Save screen state for later restoration.
     * @param screen The screen to save state for
     * @param state The state bundle to save
     */
    fun saveScreenState(screen: Screen, state: Bundle)
    
    /**
     * Restore previously saved screen state.
     * @param screen The screen to restore state for
     * @return The restored state bundle or null if no state exists
     */
    fun restoreScreenState(screen: Screen): Bundle?
    
    /**
     * Clear saved state for a specific screen.
     * @param screen The screen to clear state for
     */
    fun clearScreenState(screen: Screen)
    
    /**
     * Persist current navigation state to storage.
     */
    fun persistState()
    
    /**
     * Clear all persisted navigation state.
     */
    fun clearPersistedState()
}