package com.locationsharing.app.navigation.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.locationsharing.app.ui.components.button.ButtonResponseManager
import com.locationsharing.app.ui.components.button.ButtonState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized button response manager with performance enhancements.
 * Provides faster response times, better debouncing, and performance monitoring.
 */
@Singleton
class OptimizedButtonResponseManager @Inject constructor(
    private val performanceMonitor: NavigationPerformanceMonitor
) : ButtonResponseManager {
    
    companion object {
        private const val TAG = "OptimizedButtonResponseManager"
        private const val FAST_DEBOUNCE_DELAY_MS = 300L
        private const val STANDARD_DEBOUNCE_DELAY_MS = 500L
        private const val FEEDBACK_DURATION_MS = 150L
        private const val MAX_CONCURRENT_ACTIONS = 3
        private const val PERFORMANCE_THRESHOLD_MS = 100L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val buttonStates = ConcurrentHashMap<String, MutableStateFlow<ButtonState>>()
    private val actionMutex = Mutex()
    private val activeActions = mutableSetOf<String>()
    
    // Performance optimization: Pre-allocated state flows for common buttons
    private val commonButtonStates = mapOf(
        "home_button" to MutableStateFlow(ButtonState()),
        "map_button" to MutableStateFlow(ButtonState()),
        "friends_button" to MutableStateFlow(ButtonState()),
        "settings_button" to MutableStateFlow(ButtonState()),
        "back_button" to MutableStateFlow(ButtonState())
    )
    
    init {
        // Pre-populate common button states for faster access
        commonButtonStates.forEach { (buttonId, stateFlow) ->
            buttonStates[buttonId] = stateFlow
        }
    }
    
    override fun handleButtonClick(buttonId: String, action: () -> Unit) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                // Fast path for common buttons
                val isCommonButton = commonButtonStates.containsKey(buttonId)
                val debounceDelay = if (isCommonButton) FAST_DEBOUNCE_DELAY_MS else STANDARD_DEBOUNCE_DELAY_MS
                
                if (!canExecuteAction(buttonId, debounceDelay)) {
                    performanceMonitor.recordButtonResponseTime(buttonId, System.currentTimeMillis() - startTime)
                    return@launch
                }
                
                // Limit concurrent actions to prevent UI blocking
                actionMutex.withLock {
                    if (activeActions.size >= MAX_CONCURRENT_ACTIONS) {
                        Timber.w(TAG, "Too many concurrent actions, queuing button click: $buttonId")
                        // Could implement a queue here if needed
                        return@launch
                    }
                    activeActions.add(buttonId)
                }
                
                val currentState = getOrCreateButtonState(buttonId)
                val state = currentState.value
                
                // Update state with immediate feedback
                currentState.value = state
                    .withLastClickTime(System.currentTimeMillis())
                    .withFeedback(true)
                
                // Execute action with performance monitoring
                val actionStartTime = System.currentTimeMillis()
                try {
                    action()
                    val actionDuration = System.currentTimeMillis() - actionStartTime
                    
                    if (actionDuration > PERFORMANCE_THRESHOLD_MS) {
                        Timber.w(TAG, "Slow button action detected: $buttonId took ${actionDuration}ms")
                    }
                    
                    performanceMonitor.recordButtonResponseTime(buttonId, actionDuration)
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error executing button action: $buttonId")
                    // Update state to show error
                    currentState.value = currentState.value.withEnabled(false)
                    
                    // Re-enable after a short delay
                    delay(1000L)
                    currentState.value = currentState.value.withEnabled(true)
                }
                
                // Remove feedback after optimized duration
                delay(FEEDBACK_DURATION_MS)
                val updatedState = currentState.value
                currentState.value = updatedState.withFeedback(false)
                
            } finally {
                // Always clean up active actions
                actionMutex.withLock {
                    activeActions.remove(buttonId)
                }
                
                val totalTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordButtonResponseTime(buttonId, totalTime)
            }
        }
    }
    
    override fun showButtonFeedback(buttonId: String) {
        val currentState = getOrCreateButtonState(buttonId)
        currentState.value = currentState.value.withFeedback(true)
        
        // Optimized feedback removal
        scope.launch {
            delay(FEEDBACK_DURATION_MS)
            val updatedState = currentState.value
            if (updatedState.showFeedback) {
                currentState.value = updatedState.withFeedback(false)
            }
        }
    }
    
    override fun setButtonEnabled(buttonId: String, enabled: Boolean) {
        val currentState = getOrCreateButtonState(buttonId)
        val state = currentState.value
        
        if (state.isEnabled != enabled) {
            currentState.value = state.withEnabled(enabled)
            Timber.d(TAG, "Button $buttonId enabled state changed to: $enabled")
        }
    }
    
    override fun setButtonLoading(buttonId: String, loading: Boolean) {
        val currentState = getOrCreateButtonState(buttonId)
        val state = currentState.value
        
        if (state.isLoading != loading) {
            if (loading) {
                // Starting loading
                currentState.value = state
                    .withLoading(true)
                    .withLastClickTime(System.currentTimeMillis())
            } else {
                // Ending loading - track duration
                val loadingDuration = System.currentTimeMillis() - state.lastClickTime
                performanceMonitor.recordButtonResponseTime("${buttonId}_loading", loadingDuration)
                currentState.value = state.withLoading(false)
            }
        }
    }
    
    override fun getButtonState(buttonId: String): StateFlow<ButtonState> {
        return getOrCreateButtonState(buttonId).asStateFlow()
    }
    
    override fun clearAllStates() {
        // Clear non-common button states but preserve common ones
        val keysToRemove = buttonStates.keys.filter { !commonButtonStates.containsKey(it) }
        keysToRemove.forEach { buttonStates.remove(it) }
        
        // Reset common button states to default
        commonButtonStates.values.forEach { stateFlow ->
            stateFlow.value = ButtonState()
        }
        
        Timber.d(TAG, "Cleared ${keysToRemove.size} button states, preserved ${commonButtonStates.size} common states")
    }
    
    /**
     * Optimized composable for button state management.
     */
    @Composable
    fun OptimizedButtonState(
        buttonId: String,
        content: @Composable (ButtonState) -> Unit
    ) {
        val stateFlow = remember(buttonId) { getOrCreateButtonState(buttonId) }
        val state by stateFlow.collectAsState()
        
        // Preload state for better performance
        LaunchedEffect(buttonId) {
            if (!buttonStates.containsKey(buttonId)) {
                // Initialize state if not already present
                stateFlow.value = ButtonState()
            }
        }
        
        content(state)
    }
    
    /**
     * Batch update multiple button states for better performance.
     */
    fun batchUpdateButtonStates(updates: Map<String, ButtonState>) {
        updates.forEach { (buttonId, newState) ->
            val currentState = getOrCreateButtonState(buttonId)
            currentState.value = newState
        }
        
        Timber.d(TAG, "Batch updated ${updates.size} button states")
    }
    
    /**
     * Get performance statistics for button interactions.
     */
    fun getButtonPerformanceStats(): ButtonPerformanceStats {
        val totalButtons = buttonStates.size
        val activeButtons = buttonStates.count { it.value.value.isEnabled }
        val loadingButtons = buttonStates.count { it.value.value.isLoading }
        
        return ButtonPerformanceStats(
            totalButtons = totalButtons,
            activeButtons = activeButtons,
            loadingButtons = loadingButtons,
            commonButtonsPreloaded = commonButtonStates.size,
            activeActionsCount = activeActions.size
        )
    }
    
    /**
     * Optimize button states by cleaning up unused ones.
     */
    fun optimizeButtonStates() {
        scope.launch {
            val currentTime = System.currentTimeMillis()
            val staleThreshold = 5 * 60 * 1000L // 5 minutes
            
            val staleButtons = buttonStates.filter { (buttonId, stateFlow) ->
                !commonButtonStates.containsKey(buttonId) && 
                currentTime - stateFlow.value.lastClickTime > staleThreshold
            }
            
            staleButtons.keys.forEach { buttonId ->
                buttonStates.remove(buttonId)
            }
            
            if (staleButtons.isNotEmpty()) {
                Timber.d(TAG, "Optimized button states, removed ${staleButtons.size} stale buttons")
            }
        }
    }
    
    /**
     * Check if an action can be executed based on debouncing rules.
     */
    private fun canExecuteAction(buttonId: String, debounceDelay: Long): Boolean {
        val currentState = getOrCreateButtonState(buttonId)
        val state = currentState.value
        
        if (!state.canClick) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        return currentTime - state.lastClickTime >= debounceDelay
    }
    
    /**
     * Get or create button state with optimization for common buttons.
     */
    private fun getOrCreateButtonState(buttonId: String): MutableStateFlow<ButtonState> {
        return buttonStates.getOrPut(buttonId) {
            MutableStateFlow(ButtonState())
        }
    }
}

/**
 * Performance statistics for button interactions.
 */
data class ButtonPerformanceStats(
    val totalButtons: Int,
    val activeButtons: Int,
    val loadingButtons: Int,
    val commonButtonsPreloaded: Int,
    val activeActionsCount: Int
)

/**
 * Extension function to collect state as Compose state.
 */
@Composable
private fun <T> StateFlow<T>.collectAsState(): androidx.compose.runtime.State<T> {
    return this.collectAsState()
}