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
 * Performance monitoring utility specifically for the FriendsToggleFAB component.
 * 
 * Tracks performance metrics for:
 * - Badge rendering efficiency
 * - Animation resource cleanup
 * - Recomposition optimization
 * - Large friend list handling
 * - Button interaction responsiveness
 * 
 * Requirements: 5.4
 */
@Singleton
class FabPerformanceMonitor @Inject constructor() {
    
    companion object {
        private const val TAG = "🎯 FAB-Performance"
        private const val INTERACTION_THRESHOLD_MS = 100L
        private const val RENDER_THRESHOLD_MS = 50L
        private const val BADGE_UPDATE_THRESHOLD_MS = 16L // 60fps target
        private const val LARGE_FRIEND_LIST_THRESHOLD = 100
    }
    
    private val performanceScope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Monitor badge rendering performance for friend count updates
     */
    fun monitorBadgeRendering(friendCount: Int, operation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val renderTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            if (renderTime > BADGE_UPDATE_THRESHOLD_MS) {
                Log.w(TAG, "Slow badge rendering: ${renderTime}ms for $friendCount friends (threshold: ${BADGE_UPDATE_THRESHOLD_MS}ms)")
            } else {
                Log.d(TAG, "Badge rendering: ${renderTime}ms for $friendCount friends")
            }
            
            // Performance recommendations for badge rendering
            when {
                friendCount > 1000 && renderTime > BADGE_UPDATE_THRESHOLD_MS -> {
                    Log.i(TAG, "Performance recommendation: Consider badge update throttling for ${friendCount} friends")
                }
                renderTime > BADGE_UPDATE_THRESHOLD_MS * 2 -> {
                    Log.i(TAG, "Performance recommendation: Badge rendering optimization needed (${renderTime}ms)")
                }
            }
        }
    }
    
    /**
     * Monitor button interaction performance
     */
    fun monitorButtonInteraction(friendCount: Int, interactionType: String, operation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val interactionTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            if (interactionTime > INTERACTION_THRESHOLD_MS) {
                Log.w(TAG, "Slow $interactionType interaction: ${interactionTime}ms with $friendCount friends (threshold: ${INTERACTION_THRESHOLD_MS}ms)")
            } else {
                Log.d(TAG, "$interactionType interaction: ${interactionTime}ms with $friendCount friends")
            }
            
            // Track interaction performance trends
            if (friendCount > LARGE_FRIEND_LIST_THRESHOLD) {
                Log.i(TAG, "Large friend list interaction: $interactionType took ${interactionTime}ms for $friendCount friends")
            }
        }
    }
    
    /**
     * Monitor animation resource cleanup
     */
    fun monitorAnimationCleanup(animationType: String, operation: () -> Unit) {
        if (!BuildConfig.DEBUG) return
        
        val cleanupTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            Log.d(TAG, "Animation cleanup ($animationType): ${cleanupTime}ms")
            
            if (cleanupTime > 50L) {
                Log.w(TAG, "Slow animation cleanup: ${cleanupTime}ms for $animationType")
            }
        }
    }
    
    /**
     * Monitor recomposition efficiency
     */
    fun monitorRecomposition(
        friendCount: Int,
        isExpanded: Boolean,
        isPanelOpen: Boolean,
        recompositionCount: Int,
        operation: () -> Unit
    ) {
        if (!BuildConfig.DEBUG) return
        
        val recompositionTime = measureTimeMillis {
            operation()
        }
        
        performanceScope.launch {
            Log.d(TAG, "Recomposition: ${recompositionTime}ms (count: $recompositionCount, friends: $friendCount, expanded: $isExpanded, panel: $isPanelOpen)")
            
            // Analyze recomposition efficiency
            when {
                recompositionCount > 10 && recompositionTime > RENDER_THRESHOLD_MS -> {
                    Log.w(TAG, "Excessive recompositions: $recompositionCount in ${recompositionTime}ms")
                    Log.i(TAG, "Performance recommendation: Check stable state usage and derivedStateOf optimization")
                }
                recompositionTime > RENDER_THRESHOLD_MS * 2 -> {
                    Log.w(TAG, "Slow recomposition: ${recompositionTime}ms")
                }
            }
        }
    }
    
    /**
     * Monitor large friend list performance
     */
    fun monitorLargeFriendListPerformance(friendCount: Int, operation: String, executionTime: Long) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            if (friendCount > LARGE_FRIEND_LIST_THRESHOLD) {
                Log.i(TAG, "Large friend list performance: $operation took ${executionTime}ms for $friendCount friends")
                
                when {
                    friendCount > 1000 && executionTime > RENDER_THRESHOLD_MS -> {
                        Log.w(TAG, "Poor performance with large friend list: ${executionTime}ms for $friendCount friends")
                        Log.i(TAG, "Performance recommendation: Consider virtualization or pagination")
                    }
                    friendCount > 500 && executionTime > RENDER_THRESHOLD_MS / 2 -> {
                        Log.i(TAG, "Performance note: Moderate performance impact with $friendCount friends (${executionTime}ms)")
                    }
                }
            }
        }
    }
    
    /**
     * Monitor memory usage during FAB operations
     */
    fun monitorMemoryUsage(operation: String, friendCount: Int) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            val runtime = Runtime.getRuntime()
            runtime.gc() // Force garbage collection for accurate measurement
            
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val usedMemoryMB = usedMemory / (1024 * 1024)
            
            Log.d(TAG, "Memory usage after $operation: ${usedMemoryMB}MB (friends: $friendCount)")
            
            // Memory usage warnings
            when {
                usedMemoryMB > 100L -> {
                    Log.w(TAG, "High memory usage: ${usedMemoryMB}MB after $operation")
                }
                friendCount > LARGE_FRIEND_LIST_THRESHOLD && usedMemoryMB > 50L -> {
                    Log.i(TAG, "Memory usage with large friend list: ${usedMemoryMB}MB for $friendCount friends")
                }
            }
        }
    }
    
    /**
     * Log performance summary for FAB operations
     */
    fun logPerformanceSummary(
        friendCount: Int,
        badgeRenderTime: Long,
        interactionTime: Long,
        recompositionCount: Int,
        memoryUsageMB: Long
    ) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            Log.i(TAG, "FAB Performance Summary:")
            Log.i(TAG, "  Friends: $friendCount")
            Log.i(TAG, "  Badge render: ${badgeRenderTime}ms")
            Log.i(TAG, "  Interaction: ${interactionTime}ms")
            Log.i(TAG, "  Recompositions: $recompositionCount")
            Log.i(TAG, "  Memory: ${memoryUsageMB}MB")
            
            // Overall performance assessment
            val totalTime = badgeRenderTime + interactionTime
            when {
                totalTime < 50L && recompositionCount < 5 -> {
                    Log.i(TAG, "  Assessment: Excellent FAB performance")
                }
                totalTime < 100L && recompositionCount < 10 -> {
                    Log.i(TAG, "  Assessment: Good FAB performance")
                }
                totalTime < 200L && recompositionCount < 20 -> {
                    Log.i(TAG, "  Assessment: Acceptable FAB performance")
                }
                else -> {
                    Log.w(TAG, "  Assessment: Poor FAB performance - optimization needed")
                    Log.i(TAG, "  Recommendations:")
                    if (totalTime > 200L) Log.i(TAG, "    - Optimize rendering pipeline")
                    if (recompositionCount > 20) Log.i(TAG, "    - Review stable state usage")
                    if (friendCount > LARGE_FRIEND_LIST_THRESHOLD) Log.i(TAG, "    - Consider friend list optimization")
                }
            }
        }
    }
    
    /**
     * Track animation smoothness
     */
    fun trackAnimationSmoothness(animationType: String, frameCount: Int, totalTime: Long) {
        if (!BuildConfig.DEBUG) return
        
        performanceScope.launch {
            val fps = if (totalTime > 0) (frameCount * 1000) / totalTime else 0
            
            Log.d(TAG, "Animation smoothness ($animationType): ${fps}fps over ${totalTime}ms")
            
            when {
                fps < 30 -> {
                    Log.w(TAG, "Poor animation performance: ${fps}fps for $animationType")
                    Log.i(TAG, "Performance recommendation: Optimize animation or reduce complexity")
                }
                fps < 45 -> {
                    Log.i(TAG, "Moderate animation performance: ${fps}fps for $animationType")
                }
                else -> {
                    Log.d(TAG, "Good animation performance: ${fps}fps for $animationType")
                }
            }
        }
    }
}