package com.locationsharing.app.ui.components.button

/**
 * Data class representing the state of a button in the UI.
 * 
 * @property isEnabled Whether the button is enabled for interaction
 * @property isLoading Whether the button is in a loading state
 * @property showFeedback Whether visual feedback should be shown
 * @property lastClickTime Timestamp of the last click for debouncing
 */
data class ButtonState(
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val showFeedback: Boolean = false,
    val lastClickTime: Long = 0L
) {
    
    /**
     * Checks if the button can be clicked based on enabled state and loading state.
     */
    val canClick: Boolean
        get() = isEnabled && !isLoading
    
    /**
     * Creates a copy of this state with updated enabled status.
     */
    fun withEnabled(enabled: Boolean): ButtonState = copy(isEnabled = enabled)
    
    /**
     * Creates a copy of this state with updated loading status.
     */
    fun withLoading(loading: Boolean): ButtonState = copy(isLoading = loading)
    
    /**
     * Creates a copy of this state with updated feedback status.
     */
    fun withFeedback(showFeedback: Boolean): ButtonState = copy(showFeedback = showFeedback)
    
    /**
     * Creates a copy of this state with updated last click time.
     */
    fun withLastClickTime(time: Long): ButtonState = copy(lastClickTime = time)
}