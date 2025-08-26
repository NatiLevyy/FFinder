package com.locationsharing.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.os.bundleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializable data class for persisting navigation state.
 */
@Serializable
private data class PersistedNavigationState(
    val currentScreen: String,
    val navigationHistory: List<String>,
    val screenStates: Map<String, String> = emptyMap()
)

/**
 * Implementation of NavigationStateTracker for managing navigation state.
 * Provides reactive state updates, maintains navigation history, and persists state across app restarts.
 */
@Singleton
class NavigationStateTrackerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NavigationStateTracker {
    
    companion object {
        private const val MAX_HISTORY_SIZE = 10
        private const val TAG = "NavigationStateTracker"
        private const val PREFS_NAME = "navigation_state_prefs"
        private const val KEY_NAVIGATION_STATE = "navigation_state"
        private const val KEY_SCREEN_STATES = "screen_states"
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val screenStates = mutableMapOf<Screen, Bundle>()
    
    private val _currentState = MutableStateFlow(loadPersistedState())
    
    override val currentState: StateFlow<NavigationState> = _currentState.asStateFlow()
    
    override fun updateCurrentScreen(screen: Screen) {
        val currentState = _currentState.value
        
        if (currentState.currentScreen == screen) {
            Timber.d(TAG, "Screen is already current: $screen")
            return
        }
        
        _currentState.value = currentState.copy(
            currentScreen = screen,
            canNavigateBack = screen != Screen.HOME || currentState.navigationHistory.isNotEmpty()
        )
        
        persistState()
        Timber.d(TAG, "Updated current screen to: $screen")
    }
    
    override fun recordNavigation(from: Screen, to: Screen) {
        val currentState = _currentState.value
        val updatedHistory = (currentState.navigationHistory + from)
            .takeLast(MAX_HISTORY_SIZE)
        
        _currentState.value = currentState.copy(
            navigationHistory = updatedHistory
        )
        
        persistState()
        Timber.d(TAG, "Recorded navigation from $from to $to")
    }
    
    override fun setNavigationInProgress(isNavigating: Boolean) {
        val currentState = _currentState.value
        
        if (currentState.isNavigating == isNavigating) {
            return
        }
        
        _currentState.value = currentState.copy(
            isNavigating = isNavigating
        )
        
        Timber.d(TAG, "Set navigation in progress: $isNavigating")
    }
    
    override fun clearHistory() {
        val currentState = _currentState.value
        
        _currentState.value = currentState.copy(
            navigationHistory = emptyList(),
            canNavigateBack = false
        )
        
        persistState()
        Timber.d(TAG, "Cleared navigation history")
    }
    
    override fun getPreviousScreen(): Screen? {
        val history = _currentState.value.navigationHistory
        return history.lastOrNull()
    }
    
    override fun saveScreenState(screen: Screen, state: Bundle) {
        screenStates[screen] = state
        persistScreenStates()
        Timber.d(TAG, "Saved state for screen: $screen")
    }
    
    override fun restoreScreenState(screen: Screen): Bundle? {
        return screenStates[screen]?.also {
            Timber.d(TAG, "Restored state for screen: $screen")
        }
    }
    
    override fun clearScreenState(screen: Screen) {
        screenStates.remove(screen)
        persistScreenStates()
        Timber.d(TAG, "Cleared state for screen: $screen")
    }
    
    override fun persistState() {
        val currentState = _currentState.value
        val persistedState = PersistedNavigationState(
            currentScreen = currentState.currentScreen.route,
            navigationHistory = currentState.navigationHistory.map { it.route }
        )
        
        try {
            val stateJson = json.encodeToString(persistedState)
            sharedPreferences.edit()
                .putString(KEY_NAVIGATION_STATE, stateJson)
                .apply()
            
            Timber.d(TAG, "Navigation state persisted successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist navigation state")
        }
    }
    
    override fun clearPersistedState() {
        sharedPreferences.edit()
            .remove(KEY_NAVIGATION_STATE)
            .remove(KEY_SCREEN_STATES)
            .apply()
        
        screenStates.clear()
        Timber.d(TAG, "Cleared all persisted navigation state")
    }
    
    /**
     * Load persisted navigation state from SharedPreferences.
     */
    private fun loadPersistedState(): NavigationState {
        return try {
            val stateJson = sharedPreferences.getString(KEY_NAVIGATION_STATE, null)
            if (stateJson != null) {
                val persistedState = json.decodeFromString<PersistedNavigationState>(stateJson)
                val currentScreen = Screen.fromRoute(persistedState.currentScreen) ?: Screen.HOME
                val history = persistedState.navigationHistory.mapNotNull { route ->
                    Screen.fromRoute(route)
                }
                
                loadPersistedScreenStates()
                
                NavigationState(
                    currentScreen = currentScreen,
                    canNavigateBack = currentScreen != Screen.HOME || history.isNotEmpty(),
                    navigationHistory = history,
                    isNavigating = false
                ).also {
                    Timber.d(TAG, "Loaded persisted navigation state: current=$currentScreen, history=${history.size}")
                }
            } else {
                getDefaultNavigationState()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load persisted navigation state, using default")
            getDefaultNavigationState()
        }
    }
    
    /**
     * Get the default navigation state.
     */
    private fun getDefaultNavigationState(): NavigationState {
        return NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
    }
    
    /**
     * Load persisted screen states from SharedPreferences.
     */
    private fun loadPersistedScreenStates() {
        try {
            val screenStatesJson = sharedPreferences.getString(KEY_SCREEN_STATES, null)
            if (screenStatesJson != null) {
                val persistedScreenStates = json.decodeFromString<Map<String, String>>(screenStatesJson)
                
                persistedScreenStates.forEach { (screenRoute, stateJson) ->
                    val screen = Screen.fromRoute(screenRoute)
                    if (screen != null) {
                        try {
                            // For simplicity, we'll store Bundle data as JSON strings
                            // In a real implementation, you might want to use a more sophisticated serialization
                            val bundle = Bundle()
                            bundle.putString("serialized_state", stateJson)
                            screenStates[screen] = bundle
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to deserialize state for screen: $screen")
                        }
                    }
                }
                
                Timber.d(TAG, "Loaded ${screenStates.size} persisted screen states")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load persisted screen states")
        }
    }
    
    /**
     * Persist screen states to SharedPreferences.
     */
    private fun persistScreenStates() {
        try {
            val screenStatesMap = screenStates.mapKeys { it.key.route }
                .mapValues { entry ->
                    // Serialize Bundle to JSON string
                    entry.value.getString("serialized_state") ?: ""
                }
            
            val screenStatesJson = json.encodeToString(screenStatesMap)
            sharedPreferences.edit()
                .putString(KEY_SCREEN_STATES, screenStatesJson)
                .apply()
            
            Timber.d(TAG, "Screen states persisted successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist screen states")
        }
    }
}