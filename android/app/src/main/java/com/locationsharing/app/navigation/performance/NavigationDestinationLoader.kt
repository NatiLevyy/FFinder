package com.locationsharing.app.navigation.performance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages lazy loading of navigation destinations to improve performance.
 * Preloads destinations based on user navigation patterns and caches composables.
 */
@Singleton
class NavigationDestinationLoader @Inject constructor(
    private val navigationCache: NavigationCache,
    private val performanceMonitor: NavigationPerformanceMonitor
) {
    
    companion object {
        private const val TAG = "NavigationDestinationLoader"
        private const val PRELOAD_DELAY_MS = 100L
        private const val MAX_CACHED_DESTINATIONS = 5
    }
    
    private val loadedDestinations = mutableSetOf<String>()
    private val preloadQueue = mutableListOf<String>()
    
    /**
     * Lazy composable wrapper that loads destinations on demand.
     */
    @Composable
    fun LazyDestination(
        route: String,
        backStackEntry: NavBackStackEntry,
        content: @Composable (NavBackStackEntry) -> Unit
    ) {
        var isLoaded by remember(route) { mutableStateOf(false) }
        var isLoading by remember(route) { mutableStateOf(false) }
        
        LaunchedEffect(route) {
            if (!isLoaded && !isLoading) {
                isLoading = true
                val startTime = System.currentTimeMillis()
                
                try {
                    // Check if destination is cached
                    if (navigationCache.isDestinationCached(route)) {
                        Timber.d(TAG, "Loading cached destination: $route")
                        isLoaded = true
                    } else {
                        // Simulate loading time for heavy destinations
                        delay(PRELOAD_DELAY_MS)
                        
                        // Mark as loaded and cache
                        loadedDestinations.add(route)
                        navigationCache.cacheDestination(route)
                        isLoaded = true
                        
                        Timber.d(TAG, "Loaded destination: $route")
                    }
                    
                    val loadTime = System.currentTimeMillis() - startTime
                    performanceMonitor.recordDestinationLoadTime(route, loadTime)
                    
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load destination: $route")
                    performanceMonitor.recordDestinationLoadError(route, e)
                } finally {
                    isLoading = false
                }
            }
        }
        
        if (isLoaded) {
            content(backStackEntry)
        } else {
            // Show loading placeholder
            LoadingPlaceholder(route = route)
        }
    }
    
    /**
     * Preload destinations based on navigation patterns.
     */
    suspend fun preloadDestinations(currentRoute: String, navigationHistory: List<String>) {
        val destinationsToPreload = getPredictedDestinations(currentRoute, navigationHistory)
        
        destinationsToPreload.forEach { route ->
            if (!loadedDestinations.contains(route) && !preloadQueue.contains(route)) {
                preloadQueue.add(route)
                preloadDestination(route)
            }
        }
    }
    
    /**
     * Preload a specific destination in the background.
     */
    private suspend fun preloadDestination(route: String) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Simulate preloading (in real implementation, this would prepare the destination)
            delay(50L)
            
            navigationCache.cacheDestination(route)
            loadedDestinations.add(route)
            preloadQueue.remove(route)
            
            val preloadTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordDestinationPreloadTime(route, preloadTime)
            
            Timber.d(TAG, "Preloaded destination: $route in ${preloadTime}ms")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to preload destination: $route")
            preloadQueue.remove(route)
            performanceMonitor.recordDestinationLoadError(route, e)
        }
    }
    
    /**
     * Predict which destinations the user is likely to navigate to next.
     */
    private fun getPredictedDestinations(currentRoute: String, history: List<String>): List<String> {
        val predictions = mutableListOf<String>()
        
        // Common navigation patterns
        when (currentRoute) {
            "home" -> {
                predictions.addAll(listOf("map", "friends"))
            }
            "map" -> {
                predictions.addAll(listOf("home", "friends"))
            }
            "friends" -> {
                predictions.addAll(listOf("map", "settings"))
            }
            "settings" -> {
                predictions.add("home")
            }
        }
        
        // Add frequently visited destinations from history
        val frequentDestinations = history
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(2)
            .map { it.first }
        
        predictions.addAll(frequentDestinations)
        
        return predictions.distinct().take(3)
    }
    
    /**
     * Clear loaded destinations to free memory.
     */
    fun clearLoadedDestinations() {
        val destinationsToKeep = loadedDestinations.take(MAX_CACHED_DESTINATIONS)
        loadedDestinations.clear()
        loadedDestinations.addAll(destinationsToKeep)
        
        // Clear cache in a coroutine scope
        CoroutineScope(Dispatchers.IO).launch {
            navigationCache.clearOldCache()
        }
        
        Timber.d(TAG, "Cleared loaded destinations, kept ${destinationsToKeep.size}")
    }
    
    /**
     * Get loading statistics for monitoring.
     */
    fun getLoadingStats(): NavigationLoadingStats {
        return NavigationLoadingStats(
            loadedDestinations = loadedDestinations.size,
            queuedDestinations = preloadQueue.size,
            cachedDestinations = navigationCache.getCacheSize()
        )
    }
}

/**
 * Loading placeholder composable shown while destinations are loading.
 */
@Composable
private fun LoadingPlaceholder(route: String) {
    // Simple loading indicator - in real implementation, this could be more sophisticated
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Statistics about navigation loading performance.
 */
data class NavigationLoadingStats(
    val loadedDestinations: Int,
    val queuedDestinations: Int,
    val cachedDestinations: Int
)