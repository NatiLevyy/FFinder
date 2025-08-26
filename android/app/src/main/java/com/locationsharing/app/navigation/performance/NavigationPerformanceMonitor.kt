package com.locationsharing.app.navigation.performance

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors navigation performance metrics and provides optimization insights.
 * Tracks navigation timing, cache performance, and user interaction patterns.
 */
@Singleton
class NavigationPerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "NavigationPerformanceMonitor"
        private const val PREFS_NAME = "navigation_performance_prefs"
        private const val KEY_PERFORMANCE_DATA = "performance_data"
        private const val SLOW_NAVIGATION_THRESHOLD_MS = 1000L
        private const val PERFORMANCE_WINDOW_SIZE = 100
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Performance metrics
    private val navigationTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val destinationLoadTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val buttonResponseTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    
    // Real-time performance state
    private val _performanceMetrics = MutableStateFlow(NavigationPerformanceMetrics())
    val performanceMetrics: StateFlow<NavigationPerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    init {
        loadPersistedMetrics()
    }
    
    /**
     * Record navigation timing.
     */
    fun recordNavigationTime(fromRoute: String, toRoute: String, durationMs: Long) {
        val key = "$fromRoute->$toRoute"
        navigationTimes.getOrPut(key) { mutableListOf() }.apply {
            add(durationMs)
            if (size > PERFORMANCE_WINDOW_SIZE) {
                removeAt(0)
            }
        }
        
        if (durationMs > SLOW_NAVIGATION_THRESHOLD_MS) {
            Timber.w(TAG, "Slow navigation detected: $key took ${durationMs}ms")
            recordSlowNavigation(key, durationMs)
        }
        
        updatePerformanceMetrics()
        persistMetrics()
        
        Timber.d(TAG, "Recorded navigation time: $key = ${durationMs}ms")
    }
    
    /**
     * Record destination load time.
     */
    fun recordDestinationLoadTime(route: String, loadTimeMs: Long) {
        destinationLoadTimes.getOrPut(route) { mutableListOf() }.apply {
            add(loadTimeMs)
            if (size > PERFORMANCE_WINDOW_SIZE) {
                removeAt(0)
            }
        }
        
        updatePerformanceMetrics()
        Timber.d(TAG, "Recorded destination load time: $route = ${loadTimeMs}ms")
    }
    
    /**
     * Record destination preload time.
     */
    fun recordDestinationPreloadTime(route: String, preloadTimeMs: Long) {
        val key = "${route}_preload"
        destinationLoadTimes.getOrPut(key) { mutableListOf() }.apply {
            add(preloadTimeMs)
            if (size > PERFORMANCE_WINDOW_SIZE) {
                removeAt(0)
            }
        }
        
        Timber.d(TAG, "Recorded destination preload time: $route = ${preloadTimeMs}ms")
    }
    
    /**
     * Record destination load error.
     */
    fun recordDestinationLoadError(route: String, error: Throwable) {
        val errorKey = "destination_load_error_$route"
        errorCounts.getOrPut(errorKey) { AtomicLong(0) }.incrementAndGet()
        
        updatePerformanceMetrics()
        Timber.e(TAG, "Destination load error for $route: ${error.message}")
    }
    
    /**
     * Record button response time.
     */
    fun recordButtonResponseTime(buttonId: String, responseTimeMs: Long) {
        buttonResponseTimes.getOrPut(buttonId) { mutableListOf() }.apply {
            add(responseTimeMs)
            if (size > PERFORMANCE_WINDOW_SIZE) {
                removeAt(0)
            }
        }
        
        updatePerformanceMetrics()
        Timber.d(TAG, "Recorded button response time: $buttonId = ${responseTimeMs}ms")
    }
    
    /**
     * Record cache hit.
     */
    fun recordCacheHit(cacheType: String, key: String) {
        cacheHits.incrementAndGet()
        updatePerformanceMetrics()
        Timber.v(TAG, "Cache hit: $cacheType - $key")
    }
    
    /**
     * Record cache miss.
     */
    fun recordCacheMiss(cacheType: String, key: String) {
        cacheMisses.incrementAndGet()
        updatePerformanceMetrics()
        Timber.v(TAG, "Cache miss: $cacheType - $key")
    }
    
    /**
     * Record cache expiry.
     */
    fun recordCacheExpiry(cacheType: String, key: String) {
        val errorKey = "cache_expiry_$cacheType"
        errorCounts.getOrPut(errorKey) { AtomicLong(0) }.incrementAndGet()
        
        updatePerformanceMetrics()
        Timber.d(TAG, "Cache expiry: $cacheType - $key")
    }
    
    /**
     * Record cache operation.
     */
    fun recordCacheOperation(operation: String, details: String) {
        Timber.d(TAG, "Cache operation: $operation - $details")
    }
    
    /**
     * Record cache error.
     */
    fun recordCacheError(errorType: String, details: String) {
        val errorKey = "cache_error_$errorType"
        errorCounts.getOrPut(errorKey) { AtomicLong(0) }.incrementAndGet()
        
        updatePerformanceMetrics()
        Timber.e(TAG, "Cache error: $errorType - $details")
    }
    
    /**
     * Record slow navigation for analysis.
     */
    private fun recordSlowNavigation(navigationKey: String, durationMs: Long) {
        val errorKey = "slow_navigation"
        errorCounts.getOrPut(errorKey) { AtomicLong(0) }.incrementAndGet()
        
        // Log detailed information for slow navigations
        Timber.w(TAG, "SLOW NAVIGATION ALERT: $navigationKey took ${durationMs}ms (threshold: ${SLOW_NAVIGATION_THRESHOLD_MS}ms)")
    }
    
    /**
     * Get performance statistics for a specific route.
     */
    fun getRoutePerformanceStats(route: String): RoutePerformanceStats {
        val navigationStats = navigationTimes.filterKeys { it.contains(route) }
            .values.flatten()
        
        val loadStats = destinationLoadTimes[route] ?: emptyList()
        
        return RoutePerformanceStats(
            route = route,
            averageNavigationTime = navigationStats.average().takeIf { !it.isNaN() } ?: 0.0,
            maxNavigationTime = navigationStats.maxOrNull() ?: 0L,
            minNavigationTime = navigationStats.minOrNull() ?: 0L,
            averageLoadTime = loadStats.average().takeIf { !it.isNaN() } ?: 0.0,
            maxLoadTime = loadStats.maxOrNull() ?: 0L,
            navigationCount = navigationStats.size,
            loadCount = loadStats.size
        )
    }
    
    /**
     * Get overall performance summary.
     */
    fun getPerformanceSummary(): NavigationPerformanceSummary {
        val allNavigationTimes = navigationTimes.values.flatten()
        val allLoadTimes = destinationLoadTimes.values.flatten()
        val allButtonTimes = buttonResponseTimes.values.flatten()
        
        val cacheHitRate = if (cacheHits.get() + cacheMisses.get() > 0) {
            cacheHits.get().toDouble() / (cacheHits.get() + cacheMisses.get())
        } else 0.0
        
        val totalErrors = errorCounts.values.sumOf { it.get() }
        
        return NavigationPerformanceSummary(
            averageNavigationTime = allNavigationTimes.average().takeIf { !it.isNaN() } ?: 0.0,
            averageLoadTime = allLoadTimes.average().takeIf { !it.isNaN() } ?: 0.0,
            averageButtonResponseTime = allButtonTimes.average().takeIf { !it.isNaN() } ?: 0.0,
            cacheHitRate = cacheHitRate,
            totalNavigations = allNavigationTimes.size,
            totalErrors = totalErrors,
            slowNavigationCount = errorCounts["slow_navigation"]?.get() ?: 0L,
            performanceGrade = calculatePerformanceGrade(allNavigationTimes, cacheHitRate, totalErrors)
        )
    }
    
    /**
     * Get performance recommendations based on collected metrics.
     */
    fun getPerformanceRecommendations(): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()
        val summary = getPerformanceSummary()
        
        // Check navigation performance
        if (summary.averageNavigationTime > 500) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.NAVIGATION_OPTIMIZATION,
                    priority = if (summary.averageNavigationTime > 1000) Priority.HIGH else Priority.MEDIUM,
                    description = "Average navigation time is ${summary.averageNavigationTime.toInt()}ms. Consider optimizing navigation transitions.",
                    actionable = "Enable navigation preloading and optimize screen initialization."
                )
            )
        }
        
        // Check cache performance
        if (summary.cacheHitRate < 0.7) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.CACHE_OPTIMIZATION,
                    priority = Priority.MEDIUM,
                    description = "Cache hit rate is ${(summary.cacheHitRate * 100).toInt()}%. Consider improving cache strategy.",
                    actionable = "Increase cache size or improve cache prediction algorithms."
                )
            )
        }
        
        // Check button responsiveness
        if (summary.averageButtonResponseTime > 200) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.BUTTON_OPTIMIZATION,
                    priority = Priority.HIGH,
                    description = "Button response time is ${summary.averageButtonResponseTime.toInt()}ms. Users expect immediate feedback.",
                    actionable = "Optimize button debouncing and reduce action execution time."
                )
            )
        }
        
        // Check error rate
        if (summary.totalErrors > summary.totalNavigations * 0.05) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.ERROR_REDUCTION,
                    priority = Priority.HIGH,
                    description = "Error rate is ${(summary.totalErrors.toDouble() / summary.totalNavigations * 100).toInt()}%. This affects user experience.",
                    actionable = "Investigate and fix common navigation errors."
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Reset all performance metrics.
     */
    fun resetMetrics() {
        navigationTimes.clear()
        destinationLoadTimes.clear()
        buttonResponseTimes.clear()
        errorCounts.clear()
        cacheHits.set(0)
        cacheMisses.set(0)
        
        updatePerformanceMetrics()
        clearPersistedMetrics()
        
        Timber.i(TAG, "Performance metrics reset")
    }
    
    /**
     * Update the real-time performance metrics state.
     */
    private fun updatePerformanceMetrics() {
        scope.launch {
            val summary = getPerformanceSummary()
            _performanceMetrics.value = NavigationPerformanceMetrics(
                averageNavigationTime = summary.averageNavigationTime,
                cacheHitRate = summary.cacheHitRate,
                totalNavigations = summary.totalNavigations,
                totalErrors = summary.totalErrors,
                performanceGrade = summary.performanceGrade,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Calculate performance grade based on metrics.
     */
    private fun calculatePerformanceGrade(
        navigationTimes: List<Long>,
        cacheHitRate: Double,
        totalErrors: Long
    ): PerformanceGrade {
        val avgTime = navigationTimes.average().takeIf { !it.isNaN() } ?: 0.0
        val errorRate = if (navigationTimes.isNotEmpty()) {
            totalErrors.toDouble() / navigationTimes.size
        } else 0.0
        
        return when {
            avgTime < 300 && cacheHitRate > 0.8 && errorRate < 0.02 -> PerformanceGrade.EXCELLENT
            avgTime < 500 && cacheHitRate > 0.7 && errorRate < 0.05 -> PerformanceGrade.GOOD
            avgTime < 800 && cacheHitRate > 0.5 && errorRate < 0.1 -> PerformanceGrade.FAIR
            else -> PerformanceGrade.POOR
        }
    }
    
    /**
     * Persist performance metrics to SharedPreferences.
     */
    private fun persistMetrics() {
        scope.launch {
            try {
                val performanceData = PerformanceData(
                    navigationTimes = navigationTimes.mapValues { it.value.toList() },
                    destinationLoadTimes = destinationLoadTimes.mapValues { it.value.toList() },
                    buttonResponseTimes = buttonResponseTimes.mapValues { it.value.toList() },
                    cacheHits = cacheHits.get(),
                    cacheMisses = cacheMisses.get(),
                    errorCounts = errorCounts.mapValues { it.value.get() },
                    timestamp = System.currentTimeMillis()
                )
                
                val dataJson = json.encodeToString(performanceData)
                sharedPreferences.edit()
                    .putString(KEY_PERFORMANCE_DATA, dataJson)
                    .apply()
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist performance metrics")
            }
        }
    }
    
    /**
     * Load persisted performance metrics.
     */
    private fun loadPersistedMetrics() {
        scope.launch {
            try {
                val dataJson = sharedPreferences.getString(KEY_PERFORMANCE_DATA, null)
                if (dataJson != null) {
                    val performanceData = json.decodeFromString<PerformanceData>(dataJson)
                    
                    // Restore navigation times
                    performanceData.navigationTimes.forEach { (key, times) ->
                        navigationTimes[key] = times.toMutableList()
                    }
                    
                    // Restore load times
                    performanceData.destinationLoadTimes.forEach { (key, times) ->
                        destinationLoadTimes[key] = times.toMutableList()
                    }
                    
                    // Restore button times
                    performanceData.buttonResponseTimes.forEach { (key, times) ->
                        buttonResponseTimes[key] = times.toMutableList()
                    }
                    
                    // Restore cache metrics
                    cacheHits.set(performanceData.cacheHits)
                    cacheMisses.set(performanceData.cacheMisses)
                    
                    // Restore error counts
                    performanceData.errorCounts.forEach { (key, count) ->
                        errorCounts[key] = AtomicLong(count)
                    }
                    
                    updatePerformanceMetrics()
                    Timber.d(TAG, "Loaded persisted performance metrics")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load persisted performance metrics")
            }
        }
    }
    
    /**
     * Clear persisted performance metrics.
     */
    private fun clearPersistedMetrics() {
        sharedPreferences.edit()
            .remove(KEY_PERFORMANCE_DATA)
            .apply()
    }
}

/**
 * Container for persisted performance data.
 */
@Serializable
private data class PerformanceData(
    val navigationTimes: Map<String, List<Long>>,
    val destinationLoadTimes: Map<String, List<Long>>,
    val buttonResponseTimes: Map<String, List<Long>>,
    val cacheHits: Long,
    val cacheMisses: Long,
    val errorCounts: Map<String, Long>,
    val timestamp: Long
)

/**
 * Real-time performance metrics.
 */
data class NavigationPerformanceMetrics(
    val averageNavigationTime: Double = 0.0,
    val cacheHitRate: Double = 0.0,
    val totalNavigations: Int = 0,
    val totalErrors: Long = 0L,
    val performanceGrade: PerformanceGrade = PerformanceGrade.GOOD,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Performance statistics for a specific route.
 */
data class RoutePerformanceStats(
    val route: String,
    val averageNavigationTime: Double,
    val maxNavigationTime: Long,
    val minNavigationTime: Long,
    val averageLoadTime: Double,
    val maxLoadTime: Long,
    val navigationCount: Int,
    val loadCount: Int
)

/**
 * Overall performance summary.
 */
data class NavigationPerformanceSummary(
    val averageNavigationTime: Double,
    val averageLoadTime: Double,
    val averageButtonResponseTime: Double,
    val cacheHitRate: Double,
    val totalNavigations: Int,
    val totalErrors: Long,
    val slowNavigationCount: Long,
    val performanceGrade: PerformanceGrade
)

/**
 * Performance recommendation.
 */
data class PerformanceRecommendation(
    val type: RecommendationType,
    val priority: Priority,
    val description: String,
    val actionable: String
)

/**
 * Types of performance recommendations.
 */
enum class RecommendationType {
    NAVIGATION_OPTIMIZATION,
    CACHE_OPTIMIZATION,
    BUTTON_OPTIMIZATION,
    ERROR_REDUCTION,
    MEMORY_OPTIMIZATION
}

/**
 * Priority levels for recommendations.
 */
enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Performance grade levels.
 */
enum class PerformanceGrade {
    EXCELLENT, GOOD, FAIR, POOR
}