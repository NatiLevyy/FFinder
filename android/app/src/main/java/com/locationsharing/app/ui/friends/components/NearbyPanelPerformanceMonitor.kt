package com.locationsharing.app.ui.friends.components

import android.util.Log
import com.locationsharing.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utility for the Friends Nearby Panel.
 * Tracks performance metrics and provides optimization insights.
 * 
 * Requirements:
 * - 6.6: Optimize LazyColumn performance for large friend lists (500+ items)
 * - 8.6: Add memory leak prevention for location updates
 */
@Singleton
open class NearbyPanelPerformanceMonitor @Inject constructor() {
    
    companion object {
        private const val TAG = "📍 NearbyPanel-Perf"
        private const val PERFORMANCE_THRESHOLD_MS = 100L
        private const val MEMORY_THRESHOLD_MB = 50L
    }
    
    private val performanceScope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Monitor distance calculation performance
     */
    open fun monitorDistanceCalculation(friendCount: Int, operation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val executionTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            if (executionTime > PERFORMANCE_THRESHOLD_MS) {
                Log.w(TAG, "Distance calculation for $friendCount friends took ${executionTime}ms (threshold: ${PERFORMANCE_THRESHOLD_MS}ms)")
            } else {
                Log.d(TAG, "Distance calculation for $friendCount friends: ${executionTime}ms")
            }
            
            // Log performance recommendations
            when {
                friendCount > 1000 -> {
                    Log.i(TAG, "Performance recommendation: Consider implementing pagination for ${friendCount} friends")
                }
                friendCount > 500 && executionTime > PERFORMANCE_THRESHOLD_MS -> {
                    Log.i(TAG, "Performance recommendation: Consider optimizing distance calculation for ${friendCount} friends")
                }
            }
        }
    }
    
    /**
     * Monitor LazyColumn scrolling performance
     */
    open fun monitorScrollPerformance(friendCount: Int, scrollOperation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val scrollTime = measureTimeMillis {
            scrollOperation()
        }
        
        performanceScope.launch {
            if (scrollTime > PERFORMANCE_THRESHOLD_MS) {
                Log.w(TAG, "LazyColumn scroll for $friendCount friends took ${scrollTime}ms (threshold: ${PERFORMANCE_THRESHOLD_MS}ms)")
            } else {
                Log.d(TAG, "LazyColumn scroll for $friendCount friends: ${scrollTime}ms")
            }
        }
    }
    
    /**
     * Monitor memory usage
     */
    open fun monitorMemoryUsage(operation: String) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            val runtime = Runtime.getRuntime()
            runtime.gc() // Force garbage collection for accurate measurement
            
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val usedMemoryMB = usedMemory / (1024 * 1024)
            
            Log.d(TAG, "Memory usage after $operation: ${usedMemoryMB}MB")
            
            if (usedMemoryMB > MEMORY_THRESHOLD_MB) {
                Log.w(TAG, "High memory usage detected: ${usedMemoryMB}MB (threshold: ${MEMORY_THRESHOLD_MB}MB)")
            }
        }
    }
    
    /**
     * Monitor search performance
     */
    open fun monitorSearchPerformance(friendCount: Int, query: String, searchOperation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val searchTime = measureTimeMillis {
            searchOperation()
        }
        
        performanceScope.launch {
            Log.d(TAG, "Search '$query' in $friendCount friends: ${searchTime}ms")
            
            if (searchTime > PERFORMANCE_THRESHOLD_MS) {
                Log.w(TAG, "Slow search performance: ${searchTime}ms for query '$query' in $friendCount friends")
                Log.i(TAG, "Performance recommendation: Consider implementing search indexing or debouncing")
            }
        }
    }
    
    /**
     * Monitor state preservation performance
     */
    open fun monitorStatePreservation(operation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val preservationTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            Log.d(TAG, "State preservation: ${preservationTime}ms")
            
            if (preservationTime > 50L) {
                Log.w(TAG, "Slow state preservation: ${preservationTime}ms")
            }
        }
    }
    
    /**
     * Log performance summary
     */
    open fun logPerformanceSummary(
        friendCount: Int,
        calculationTime: Long,
        renderTime: Long,
        memoryUsageMB: Long
    ) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            Log.i(TAG, "Performance Summary:")
            Log.i(TAG, "  Friends: $friendCount")
            Log.i(TAG, "  Calculation: ${calculationTime}ms")
            Log.i(TAG, "  Render: ${renderTime}ms")
            Log.i(TAG, "  Memory: ${memoryUsageMB}MB")
            
            // Overall performance assessment
            val totalTime = calculationTime + renderTime
            when {
                totalTime < 50L -> Log.i(TAG, "  Assessment: Excellent performance")
                totalTime < 100L -> Log.i(TAG, "  Assessment: Good performance")
                totalTime < 200L -> Log.i(TAG, "  Assessment: Acceptable performance")
                else -> Log.w(TAG, "  Assessment: Poor performance - optimization needed")
            }
        }
    }
    
    /**
     * Track throttling effectiveness
     */
    open fun trackThrottlingEffectiveness(
        movementDistance: Double,
        timeSinceLastCalculation: Long,
        wasThrottled: Boolean
    ) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            Log.d(TAG, "Throttling check: movement=${movementDistance}m, time=${timeSinceLastCalculation}ms, throttled=$wasThrottled")
            
            if (!wasThrottled && movementDistance < 20.0 && timeSinceLastCalculation < 10000L) {
                Log.w(TAG, "Throttling ineffective: calculation performed despite small movement/time")
            }
        }
    }
    
    /**
     * Monitor configuration change performance
     */
    open fun monitorConfigurationChange(operation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val configChangeTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            Log.d(TAG, "Configuration change handling: ${configChangeTime}ms")
            
            if (configChangeTime > 100L) {
                Log.w(TAG, "Slow configuration change handling: ${configChangeTime}ms")
                Log.i(TAG, "Performance recommendation: Optimize state preservation logic")
            }
        }
    }
}