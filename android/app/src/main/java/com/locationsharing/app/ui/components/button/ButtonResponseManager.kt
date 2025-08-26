package com.locationsharing.app.ui.components.button

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing button response behavior including debouncing,
 * state management, and feedback mechanisms.
 */
interface ButtonResponseManager {
    
    /**
     * Handles button click with debouncing mechanism to prevent double-clicks.
     * 
     * @param buttonId Unique identifier for the button
     * @param action The action to execute when button is clicked
     */
    fun handleButtonClick(buttonId: String, action: () -> Unit)
    
    /**
     * Shows visual feedback for button interaction.
     * 
     * @param buttonId Unique identifier for the button
     */
    fun showButtonFeedback(buttonId: String)
    
    /**
     * Sets the enabled state of a button.
     * 
     * @param buttonId Unique identifier for the button
     * @param enabled Whether the button should be enabled
     */
    fun setButtonEnabled(buttonId: String, enabled: Boolean)
    
    /**
     * Sets the loading state of a button.
     * 
     * @param buttonId Unique identifier for the button
     * @param loading Whether the button should show loading state
     */
    fun setButtonLoading(buttonId: String, loading: Boolean)
    
    /**
     * Gets the current state of a button.
     * 
     * @param buttonId Unique identifier for the button
     * @return StateFlow of the button's current state
     */
    fun getButtonState(buttonId: String): StateFlow<ButtonState>
    
    /**
     * Clears all button states and resets the manager.
     */
    fun clearAllStates()
}