package com.locationsharing.app.navigation

import androidx.navigation.NavController
import com.locationsharing.app.ui.components.feedback.FeedbackType
import com.locationsharing.app.ui.components.feedback.HapticFeedbackManager
import com.locationsharing.app.ui.components.feedback.VisualFeedbackManager
import com.locationsharing.app.ui.components.feedback.triggerNavigationError
import com.locationsharing.app.ui.components.feedback.triggerNavigationStart
import com.locationsharing.app.ui.components.feedback.triggerNavigationSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NavigationManager providing centralized navigation control.
 * Handles navigation operations with error recovery, state management, and performance optimizations.
 */
@Singleton
class NavigationManagerImpl @Inject constructor(
    private val navigationStateTracker: NavigationStateTracker,
    private val navigationErrorHandler: NavigationErrorHandler,
    private val visualFeedbackManager: VisualFeedbackManager,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val navigationAnalytics: NavigationAnalytics,
    private val performanceMonitor: com.locationsharing.app.navigation.performance.NavigationPerformanceMonitor,
    private val navigationCache: com.locationsharing.app.navigation.performance.NavigationCache,
    private val destinationLoader: com.locationsharing.app.navigation.performance.NavigationDestinationLoader
) : NavigationManager {
    
    private var navController: NavController? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        private const val NAVIGATION_TIMEOUT_MS = 5000L
        private const val TAG = "NavigationManagerImpl"
    }
    
    override fun navigateToHome() {
        performNavigation(Screen.HOME, "button") {
            navController?.navigate(Screen.HOME.route) {
                popUpTo(Screen.HOME.route) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }
    
    override fun navigateToMap() {
        performNavigation(Screen.MAP, "button") {
            navController?.navigate(Screen.MAP.route)
        }
    }
    
    override fun navigateToMap(startSharing: Boolean) {
        performNavigation(Screen.MAP, "button") {
            val route = if (startSharing) {
                "${Screen.MAP.route}?startSharing=true"
            } else {
                Screen.MAP.route
            }
            navController?.navigate(route)
        }
    }
    
    override fun navigateToMapWithFriend(friendId: String) {
        performNavigation(Screen.MAP, "friend_selection") {
            val route = "${Screen.MAP.route}?friendId=$friendId"
            navController?.navigate(route)
        }
    }
    
    override fun navigateToFriends() {
        performNavigation(Screen.FRIENDS, "button") {
            navController?.navigate(Screen.FRIENDS.route)
        }
    }

    override fun navigateToFriendsHub() {
        performNavigation(Screen.FRIENDS_HUB, "button") {
            navController?.navigate(Screen.FRIENDS_HUB.route)
        }
    }
    
    override fun navigateToSettings() {
        performNavigation(Screen.SETTINGS, "button") {
            navController?.navigate(Screen.SETTINGS.route)
        }
    }
    
    override fun navigateToInviteFriends() {
        performNavigation(Screen.INVITE_FRIENDS, "button") {
            navController?.navigate(Screen.INVITE_FRIENDS.route)
        }
    }
    
    override fun navigateToSearchFriends() {
        performNavigation(Screen.SEARCH_FRIENDS, "button") {
            navController?.navigate(Screen.SEARCH_FRIENDS.route)
        }
    }
    
    override fun navigateBack(): Boolean {
        return try {
            val currentState = navigationStateTracker.currentState.value
            
            if (!currentState.canNavigateBack) {
                return false
            }
            
            if (currentState.isNavigating) {
                Timber.w(TAG, "Navigation already in progress, ignoring back navigation")
                navigationAnalytics.trackNavigationError(NavigationError.NavigationInProgress)
                return false
            }
            
            val startTime = System.currentTimeMillis()
            navigationStateTracker.setNavigationInProgress(true)
            
            val success = navController?.popBackStack() ?: false
            
            if (success) {
                val previousScreen = navigationStateTracker.getPreviousScreen()
                if (previousScreen != null) {
                    navigationStateTracker.updateCurrentScreen(previousScreen)
                    val duration = System.currentTimeMillis() - startTime
                    navigationAnalytics.trackSuccessfulNavigation(
                        fromRoute = currentState.currentScreen.route,
                        toRoute = previousScreen.route,
                        duration = duration
                    )
                    navigationAnalytics.trackUserJourney(previousScreen.route, "back_button")
                }
            } else {
                // Fallback to home if back navigation fails
                navigationAnalytics.trackFallbackNavigation("home")
                navigateToHome()
            }
            
            navigationStateTracker.setNavigationInProgress(false)
            success
        } catch (e: Exception) {
            Timber.e(e, "Error during back navigation")
            handleNavigationError(NavigationError.UnknownError(e))
            false
        }
    }
    
    override fun handleNavigationError(error: NavigationError) {
        Timber.e(TAG, "Navigation error occurred: $error")
        navigationStateTracker.setNavigationInProgress(false)
        
        // Track the error in analytics
        navigationAnalytics.trackNavigationError(error)
        
        // Track critical errors that need immediate attention
        when (error) {
            is NavigationError.NavigationControllerNotFound -> {
                navigationAnalytics.trackCriticalNavigationError("controller_not_found")
            }
            is NavigationError.InvalidNavigationState -> {
                navigationAnalytics.trackCriticalNavigationError("invalid_state")
            }
            else -> { /* Non-critical errors */ }
        }
        
        // Trigger error feedback
        coroutineScope.launch {
            hapticFeedbackManager.triggerNavigationError()
            visualFeedbackManager.triggerFeedback(
                type = FeedbackType.NAVIGATION_ERROR,
                duration = 500L
            )
        }
        
        // Provide retry callback for recoverable errors
        val retryCallback = when (error) {
            is NavigationError.NavigationTimeout,
            is NavigationError.UnknownError -> {
                // For timeout and unknown errors, we can retry the last navigation
                { retryLastNavigation() }
            }
            else -> null
        }
        
        navigationErrorHandler.handleError(
            error = error,
            navController = navController,
            onRetry = retryCallback
        )
    }
    
    /**
     * Retry the last navigation operation.
     */
    private fun retryLastNavigation() {
        val currentState = navigationStateTracker.currentState.value
        val lastRoute = currentState.navigationHistory.lastOrNull()
        
        lastRoute?.let { screen ->
            Timber.d(TAG, "Retrying navigation to $screen")
            
            // Track retry attempt
            val errorType = when {
                currentState.navigationHistory.isEmpty() -> "unknown"
                else -> "timeout"
            }
            navigationAnalytics.trackNavigationRetry(errorType, 1)
            
            when (screen) {
                Screen.HOME -> navigateToHome()
                Screen.MAP -> navigateToMap()
                Screen.FRIENDS -> navigateToFriends()
                Screen.SETTINGS -> navigateToSettings()
                Screen.INVITE_FRIENDS -> {
                    // Navigate to invite friends - for now use legacy route
                    navController?.navigate("invite_friends")
                }
                Screen.SEARCH_FRIENDS -> navigateToSearchFriends()
                Screen.FRIENDS_HUB -> navigateToFriendsHub()
            }
        }
    }
    
    override fun setNavController(navController: NavController) {
        this.navController = navController
        Timber.d(TAG, "NavController set successfully")
    }
    
    /**
     * Perform navigation with error handling, timeout protection, visual feedback, and performance optimizations.
     */
    private fun performNavigation(targetScreen: Screen, source: String, navigationAction: () -> Unit) {
        coroutineScope.launch {
            try {
                val currentState = navigationStateTracker.currentState.value
                
                if (currentState.isNavigating) {
                    Timber.w(TAG, "Navigation already in progress, ignoring navigation to $targetScreen")
                    performanceMonitor.recordNavigationTime(
                        currentState.currentScreen.route, 
                        targetScreen.route, 
                        0L // Blocked navigation
                    )
                    return@launch
                }
                
                if (navController == null) {
                    handleNavigationError(NavigationError.NavigationControllerNotFound)
                    return@launch
                }
                
                // Check if destination is cached for faster navigation
                val isCached = navigationCache.isDestinationCached(targetScreen.route)
                if (isCached) {
                    Timber.d(TAG, "Using cached destination for faster navigation: $targetScreen")
                }
                
                // Trigger navigation start feedback
                hapticFeedbackManager.triggerNavigationStart()
                visualFeedbackManager.triggerFeedback(
                    type = FeedbackType.NAVIGATION_START,
                    duration = 300L
                )
                
                navigationStateTracker.setNavigationInProgress(true)
                val startTime = System.currentTimeMillis()
                
                withTimeout(NAVIGATION_TIMEOUT_MS) {
                    // Cache current screen state before navigation
                    val currentScreenState = mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "source" to source
                    )
                    navigationCache.cacheNavigationState(currentState.currentScreen.route, currentScreenState)
                    
                    navigationStateTracker.recordNavigation(currentState.currentScreen, targetScreen)
                    navigationAction()
                    navigationStateTracker.updateCurrentScreen(targetScreen)
                    
                    // Cache the destination after successful navigation
                    if (!isCached) {
                        navigationCache.cacheDestination(targetScreen.route)
                    }
                }
                
                val navigationDuration = System.currentTimeMillis() - startTime
                navigationStateTracker.setNavigationInProgress(false)
                
                // Record performance metrics
                performanceMonitor.recordNavigationTime(
                    currentState.currentScreen.route,
                    targetScreen.route,
                    navigationDuration
                )
                
                // Track successful navigation
                navigationAnalytics.trackSuccessfulNavigation(
                    fromRoute = currentState.currentScreen.route,
                    toRoute = targetScreen.route,
                    duration = navigationDuration
                )
                
                // Track user journey
                navigationAnalytics.trackUserJourney(targetScreen.route, source)
                
                // Preload likely next destinations
                destinationLoader.preloadDestinations(
                    targetScreen.route,
                    currentState.navigationHistory.map { it.route }
                )
                
                // Trigger navigation success feedback
                hapticFeedbackManager.triggerNavigationSuccess()
                visualFeedbackManager.triggerFeedback(
                    type = FeedbackType.NAVIGATION_SUCCESS,
                    duration = 200L
                )
                
                Timber.d(TAG, "Successfully navigated to $targetScreen in ${navigationDuration}ms (cached: $isCached)")
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                handleNavigationError(NavigationError.NavigationTimeout)
            } catch (e: Exception) {
                handleNavigationError(NavigationError.UnknownError(e))
            }
        }
    }
}