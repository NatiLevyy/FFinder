package com.locationsharing.app.ui.components.button

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ButtonResponseManager that handles button interactions,
 * debouncing, and state management.
 */
@Singleton
class ButtonResponseManagerImpl @Inject constructor(
    private val buttonAnalytics: com.locationsharing.app.navigation.ButtonAnalytics
) : ButtonResponseManager {
    
    companion object {
        private const val DEBOUNCE_DELAY_MS = 500L
        private const val FEEDBACK_DURATION_MS = 200L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val buttonStates = mutableMapOf<String, MutableStateFlow<ButtonState>>()
    
    override fun handleButtonClick(buttonId: String, action: () -> Unit) {
        val currentState = getOrCreateButtonState(buttonId)
        val state = currentState.value
        
        // Check if button can be clicked and debounce
        if (!state.canClick) {
            buttonAnalytics.trackButtonInteractionError(buttonId, "button_disabled")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - state.lastClickTime < DEBOUNCE_DELAY_MS) {
            // Track double-click prevention
            buttonAnalytics.trackButtonDoubleClickPrevention(buttonId, 1)
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        // Update state with new click time and show feedback
        currentState.value = state
            .withLastClickTime(currentTime)
            .withFeedback(true)
        
        // Execute the action
        try {
            action()
            val responseTime = System.currentTimeMillis() - startTime
            buttonAnalytics.trackButtonClick(buttonId, "navigation", responseTime)
        } catch (e: Exception) {
            // Log error and track analytics
            android.util.Log.e("ButtonResponseManager", "Error executing button action", e)
            buttonAnalytics.trackButtonInteractionError(buttonId, "action_failed")
        }
        
        // Remove feedback after duration
        scope.launch {
            delay(FEEDBACK_DURATION_MS)
            val updatedState = currentState.value
            currentState.value = updatedState.withFeedback(false)
        }
    }
    
    override fun showButtonFeedback(buttonId: String) {
        val currentState = getOrCreateButtonState(buttonId)
        currentState.value = currentState.value.withFeedback(true)
        
        // Remove feedback after duration
        scope.launch {
            delay(FEEDBACK_DURATION_MS)
            val updatedState = currentState.value
            currentState.value = updatedState.withFeedback(false)
        }
    }
    
    override fun setButtonEnabled(buttonId: String, enabled: Boolean) {
        val currentState = getOrCreateButtonState(buttonId)
        currentState.value = currentState.value.withEnabled(enabled)
    }
    
    override fun setButtonLoading(buttonId: String, loading: Boolean) {
        val currentState = getOrCreateButtonState(buttonId)
        val state = currentState.value
        
        if (loading && !state.isLoading) {
            // Starting loading - record start time
            currentState.value = state.withLoading(loading).withLastClickTime(System.currentTimeMillis())
        } else if (!loading && state.isLoading) {
            // Ending loading - track duration
            val loadingDuration = System.currentTimeMillis() - state.lastClickTime
            buttonAnalytics.trackButtonLoadingDuration(buttonId, loadingDuration)
            currentState.value = state.withLoading(loading)
        } else {
            currentState.value = state.withLoading(loading)
        }
    }
    
    override fun getButtonState(buttonId: String): StateFlow<ButtonState> {
        return getOrCreateButtonState(buttonId).asStateFlow()
    }
    
    override fun clearAllStates() {
        buttonStates.clear()
    }
    
    private fun getOrCreateButtonState(buttonId: String): MutableStateFlow<ButtonState> {
        return buttonStates.getOrPut(buttonId) {
            MutableStateFlow(ButtonState())
        }
    }
}