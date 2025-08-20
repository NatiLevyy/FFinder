package com.locationsharing.app.ui.map.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.locationsharing.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle manager for MapScreen to optimize resource usage and prevent memory leaks
 * 
 * Features:
 * - Proper lifecycle management for location updates
 * - Memory leak prevention
 * - Resource cleanup on lifecycle events
 * - Background/foreground optimization
 */
@Singleton
class MapLifecycleManager @Inject constructor() {
    
    companion object {
        private const val TAG = "MapLifecycleManager"
        private const val BACKGROUND_UPDATE_INTERVAL = 30000L // 30 seconds
        private const val FOREGROUND_UPDATE_INTERVAL = 5000L  // 5 seconds
    }
    
    private var lifecycleScope: CoroutineScope? = null
    private var locationUpdateJob: Job? = null
    private var friendsUpdateJob: Job? = null
    private var performanceMonitoringJob: Job? = null
    
    private var isInForeground = true
    private var isLocationUpdatesActive = false
    private var isFriendsUpdatesActive = false
    
    // Lifecycle callbacks
    private var onResumeCallback: (() -> Unit)? = null
    private var onPauseCallback: (() -> Unit)? = null
    private var onStopCallback: (() -> Unit)? = null
    private var onDestroyCallback: (() -> Unit)? = null
    
    /**
     * Initialize lifecycle management
     */
    fun initialize() {
        lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Lifecycle manager initialized")
        }
    }
    
    /**
     * Start location updates with lifecycle awareness
     */
    fun startLocationUpdates(updateCallback: suspend () -> Unit) {
        if (isLocationUpdatesActive) return
        
        isLocationUpdatesActive = true
        locationUpdateJob?.cancel()
        
        locationUpdateJob = lifecycleScope?.launch {
            while (isLocationUpdatesActive) {
                try {
                    updateCallback()
                    
                    val interval = if (isInForeground) {
                        FOREGROUND_UPDATE_INTERVAL
                    } else {
                        BACKGROUND_UPDATE_INTERVAL
                    }
                    
                    delay(interval)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Timber.e(e, "$TAG: Error in location updates")
                    }
                    delay(5000) // Wait before retrying
                }
            }
        }
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Location updates started")
        }
    }
    
    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        isLocationUpdatesActive = false
        locationUpdateJob?.cancel()
        locationUpdateJob = null
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Location updates stopped")
        }
    }
    
    /**
     * Start friends updates with lifecycle awareness
     */
    fun startFriendsUpdates(updateCallback: suspend () -> Unit) {
        if (isFriendsUpdatesActive) return
        
        isFriendsUpdatesActive = true
        friendsUpdateJob?.cancel()
        
        friendsUpdateJob = lifecycleScope?.launch {
            while (isFriendsUpdatesActive) {
                try {
                    updateCallback()
                    
                    val interval = if (isInForeground) {
                        FOREGROUND_UPDATE_INTERVAL
                    } else {
                        BACKGROUND_UPDATE_INTERVAL * 2 // Less frequent in background
                    }
                    
                    delay(interval)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Timber.e(e, "$TAG: Error in friends updates")
                    }
                    delay(10000) // Wait before retrying
                }
            }
        }
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Friends updates started")
        }
    }
    
    /**
     * Stop friends updates
     */
    fun stopFriendsUpdates() {
        isFriendsUpdatesActive = false
        friendsUpdateJob?.cancel()
        friendsUpdateJob = null
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Friends updates stopped")
        }
    }
    
    /**
     * Start performance monitoring
     */
    fun startPerformanceMonitoring(monitoringCallback: suspend () -> Unit) {
        performanceMonitoringJob?.cancel()
        
        performanceMonitoringJob = lifecycleScope?.launch {
            while (true) {
                try {
                    if (isInForeground) {
                        monitoringCallback()
                    }
                    delay(1000) // Monitor every second when in foreground
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Timber.e(e, "$TAG: Error in performance monitoring")
                    }
                    delay(5000)
                }
            }
        }
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Performance monitoring started")
        }
    }
    
    /**
     * Handle lifecycle resume event
     */
    fun onResume() {
        isInForeground = true
        onResumeCallback?.invoke()
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: App resumed - switching to foreground mode")
        }
    }
    
    /**
     * Handle lifecycle pause event
     */
    fun onPause() {
        isInForeground = false
        onPauseCallback?.invoke()
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: App paused - switching to background mode")
        }
    }
    
    /**
     * Handle lifecycle stop event
     */
    fun onStop() {
        onStopCallback?.invoke()
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: App stopped")
        }
    }
    
    /**
     * Handle lifecycle destroy event
     */
    fun onDestroy() {
        cleanup()
        onDestroyCallback?.invoke()
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: App destroyed - cleaning up resources")
        }
    }
    
    /**
     * Clean up all resources
     */
    fun cleanup() {
        stopLocationUpdates()
        stopFriendsUpdates()
        
        performanceMonitoringJob?.cancel()
        performanceMonitoringJob = null
        
        lifecycleScope?.cancel()
        lifecycleScope = null
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: All resources cleaned up")
        }
    }
    
    /**
     * Set lifecycle callbacks
     */
    fun setLifecycleCallbacks(
        onResume: (() -> Unit)? = null,
        onPause: (() -> Unit)? = null,
        onStop: (() -> Unit)? = null,
        onDestroy: (() -> Unit)? = null
    ) {
        onResumeCallback = onResume
        onPauseCallback = onPause
        onStopCallback = onStop
        onDestroyCallback = onDestroy
    }
    
    /**
     * Check if app is in foreground
     */
    fun isInForeground(): Boolean = isInForeground
    
    /**
     * Check if location updates are active
     */
    fun isLocationUpdatesActive(): Boolean = isLocationUpdatesActive
    
    /**
     * Check if friends updates are active
     */
    fun isFriendsUpdatesActive(): Boolean = isFriendsUpdatesActive
    
    /**
     * Get current update interval based on foreground state
     */
    fun getCurrentUpdateInterval(): Long {
        return if (isInForeground) {
            FOREGROUND_UPDATE_INTERVAL
        } else {
            BACKGROUND_UPDATE_INTERVAL
        }
    }
}

/**
 * Composable for lifecycle-aware map management
 */
@Composable
fun MapLifecycleEffect(
    lifecycleManager: MapLifecycleManager,
    onLocationUpdate: suspend () -> Unit = {},
    onFriendsUpdate: suspend () -> Unit = {},
    onPerformanceMonitoring: suspend () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialized by remember { mutableStateOf(false) }
    
    // Initialize lifecycle manager
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            lifecycleManager.initialize()
            isInitialized = true
        }
    }
    
    // Set up lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    if (BuildConfig.DEBUG) {
                        Timber.d("MapLifecycleEffect: ON_CREATE")
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (BuildConfig.DEBUG) {
                        Timber.d("MapLifecycleEffect: ON_START")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    lifecycleManager.onResume()
                    lifecycleManager.startLocationUpdates(onLocationUpdate)
                    lifecycleManager.startFriendsUpdates(onFriendsUpdate)
                    lifecycleManager.startPerformanceMonitoring(onPerformanceMonitoring)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    lifecycleManager.onPause()
                }
                Lifecycle.Event.ON_STOP -> {
                    lifecycleManager.onStop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    lifecycleManager.onDestroy()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            lifecycleManager.cleanup()
        }
    }
}