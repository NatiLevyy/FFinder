package com.locationsharing.app.navigation.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the navigation performance dashboard.
 * Manages performance metrics, recommendations, and optimization actions.
 */
@HiltViewModel
class NavigationPerformanceDashboardViewModel @Inject constructor(
    private val performanceMonitor: NavigationPerformanceMonitor,
    private val navigationCache: NavigationCache,
    private val optimizedButtonManager: OptimizedButtonResponseManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "NavigationPerformanceDashboardViewModel"
    }
    
    // Performance metrics from the monitor
    val performanceMetrics: StateFlow<NavigationPerformanceMetrics> = performanceMonitor.performanceMetrics
    
    // Recommendations state
    private val _recommendations = MutableStateFlow<List<PerformanceRecommendation>>(emptyList())
    val recommendations: StateFlow<List<PerformanceRecommendation>> = _recommendations.asStateFlow()
    
    // Cache statistics state
    private val _cacheStats = MutableStateFlow<NavigationCacheStats?>(null)
    val cacheStats: StateFlow<NavigationCacheStats?> = _cacheStats.asStateFlow()
    
    // Button statistics state
    private val _buttonStats = MutableStateFlow<ButtonPerformanceStats?>(null)
    val buttonStats: StateFlow<ButtonPerformanceStats?> = _buttonStats.asStateFlow()
    
    init {
        loadInitialData()
        startPeriodicUpdates()
    }
    
    /**
     * Load initial performance data.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                updateRecommendations()
                updateCacheStats()
                updateButtonStats()
                
                Timber.d(TAG, "Initial performance data loaded")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load initial performance data")
            }
        }
    }
    
    /**
     * Start periodic updates of performance data.
     */
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            try {
                // Update every 30 seconds
                kotlinx.coroutines.delay(30_000L)
                
                while (true) {
                    updateRecommendations()
                    updateCacheStats()
                    updateButtonStats()
                    
                    kotlinx.coroutines.delay(30_000L)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in periodic updates")
            }
        }
    }
    
    /**
     * Update performance recommendations.
     */
    private suspend fun updateRecommendations() {
        try {
            val newRecommendations = performanceMonitor.getPerformanceRecommendations()
            _recommendations.value = newRecommendations
            
            Timber.d(TAG, "Updated recommendations: ${newRecommendations.size} items")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update recommendations")
        }
    }
    
    /**
     * Update cache statistics.
     */
    private suspend fun updateCacheStats() {
        try {
            val stats = navigationCache.getCacheStats()
            _cacheStats.value = stats
            
            Timber.d(TAG, "Updated cache stats: ${stats.totalDestinations} destinations, ${stats.totalStates} states")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update cache stats")
        }
    }
    
    /**
     * Update button statistics.
     */
    private suspend fun updateButtonStats() {
        try {
            val stats = optimizedButtonManager.getButtonPerformanceStats()
            _buttonStats.value = stats
            
            Timber.d(TAG, "Updated button stats: ${stats.totalButtons} total, ${stats.activeButtons} active")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update button stats")
        }
    }
    
    /**
     * Reset all performance metrics.
     */
    fun resetMetrics() {
        viewModelScope.launch {
            try {
                performanceMonitor.resetMetrics()
                
                // Update UI immediately
                updateRecommendations()
                updateCacheStats()
                updateButtonStats()
                
                Timber.i(TAG, "Performance metrics reset")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reset metrics")
            }
        }
    }
    
    /**
     * Optimize navigation cache.
     */
    fun optimizeCache() {
        viewModelScope.launch {
            try {
                navigationCache.clearOldCache()
                
                // Update cache stats immediately
                updateCacheStats()
                updateRecommendations()
                
                Timber.i(TAG, "Cache optimized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to optimize cache")
            }
        }
    }
    
    /**
     * Optimize button states.
     */
    fun optimizeButtons() {
        viewModelScope.launch {
            try {
                optimizedButtonManager.optimizeButtonStates()
                
                // Update button stats immediately
                updateButtonStats()
                updateRecommendations()
                
                Timber.i(TAG, "Button states optimized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to optimize button states")
            }
        }
    }
    
    /**
     * Get detailed performance summary.
     */
    fun getDetailedSummary(): NavigationPerformanceSummary {
        return performanceMonitor.getPerformanceSummary()
    }
    
    /**
     * Get performance statistics for a specific route.
     */
    fun getRouteStats(route: String): RoutePerformanceStats {
        return performanceMonitor.getRoutePerformanceStats(route)
    }
    
    /**
     * Force refresh all performance data.
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                updateRecommendations()
                updateCacheStats()
                updateButtonStats()
                
                Timber.d(TAG, "Performance data refreshed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh performance data")
            }
        }
    }
    
    /**
     * Export performance data for analysis.
     */
    fun exportPerformanceData(): PerformanceExportData {
        val summary = performanceMonitor.getPerformanceSummary()
        val cacheStats = _cacheStats.value
        val buttonStats = _buttonStats.value
        val recommendations = _recommendations.value
        
        return PerformanceExportData(
            summary = summary,
            cacheStats = cacheStats,
            buttonStats = buttonStats,
            recommendations = recommendations,
            exportTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if performance is degraded and needs attention.
     */
    fun isPerformanceDegraded(): Boolean {
        val metrics = performanceMetrics.value
        val recommendations = _recommendations.value
        
        return metrics.performanceGrade == PerformanceGrade.POOR ||
                recommendations.any { it.priority == Priority.CRITICAL } ||
                metrics.averageNavigationTime > 1000 ||
                metrics.totalErrors > metrics.totalNavigations * 0.1
    }
    
    /**
     * Get performance health score (0-100).
     */
    fun getPerformanceHealthScore(): Int {
        val metrics = performanceMetrics.value
        val recommendations = _recommendations.value
        
        var score = when (metrics.performanceGrade) {
            PerformanceGrade.EXCELLENT -> 100
            PerformanceGrade.GOOD -> 80
            PerformanceGrade.FAIR -> 60
            PerformanceGrade.POOR -> 40
        }
        
        // Deduct points for critical recommendations
        val criticalRecommendations = recommendations.count { it.priority == Priority.CRITICAL }
        val highRecommendations = recommendations.count { it.priority == Priority.HIGH }
        
        score -= (criticalRecommendations * 20)
        score -= (highRecommendations * 10)
        
        return maxOf(0, score)
    }
}

/**
 * Data class for exporting performance data.
 */
data class PerformanceExportData(
    val summary: NavigationPerformanceSummary,
    val cacheStats: NavigationCacheStats?,
    val buttonStats: ButtonPerformanceStats?,
    val recommendations: List<PerformanceRecommendation>,
    val exportTimestamp: Long
)