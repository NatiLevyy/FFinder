package com.locationsharing.app.ui.friends.logging

import android.util.Log
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.domain.model.NearbyFriend
import kotlin.system.measureTimeMillis

/**
 * Centralized logging utility for the Friends Nearby Panel feature.
 * 
 * Provides comprehensive logging with "📍 NearbyPanel" tag prefix for easy filtering.
 * All debug logging is guarded with BuildConfig.DEBUG to prevent logging in release builds.
 */
object NearbyPanelLogger {
    
    private const val TAG_PREFIX = "📍 NearbyPanel"
    
    // Performance monitoring thresholds
    private const val DISTANCE_CALCULATION_THRESHOLD_MS = 100L
    private const val UI_UPDATE_THRESHOLD_MS = 50L
    
    /**
     * Logs distance calculation updates with friend count.
     */
    fun logDistanceUpdate(friendCount: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_PREFIX, "Distance updated for $friendCount friends")
        }
    }
    
    /**
     * Logs friend interaction events with context.
     */
    fun logFriendInteraction(action: String, friendId: String, friendName: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX:Interaction", "$action - Friend: $friendName (ID: $friendId)")
        }
    }
    
    /**
     * Logs search query changes with result count.
     */
    fun logSearchQuery(query: String, resultCount: Int) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX:Search", "Query: '$query' -> $resultCount results")
        }
    }
    
    /**
     * Logs panel state changes.
     */
    fun logPanelStateChange(isOpen: Boolean, friendCount: Int) {
        if (BuildConfig.DEBUG) {
            val state = if (isOpen) "OPENED" else "CLOSED"
            Log.d("$TAG_PREFIX:State", "Panel $state with $friendCount friends")
        }
    }
    
    /**
     * Logs location permission status changes.
     */
    fun logLocationPermissionStatus(hasPermission: Boolean, friendsVisible: Int) {
        if (BuildConfig.DEBUG) {
            val status = if (hasPermission) "GRANTED" else "DENIED"
            Log.d("$TAG_PREFIX:Permission", "Location permission $status - $friendsVisible friends visible")
        }
    }
    
    /**
     * Logs error events with proper context for troubleshooting.
     */
    fun logError(operation: String, error: Throwable, context: Map<String, Any> = emptyMap()) {
        val contextStr = if (context.isNotEmpty()) {
            context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else {
            "No additional context"
        }
        
        Log.e("$TAG_PREFIX:Error", "Operation: $operation failed - Context: [$contextStr]", error)
    }
    
    /**
     * Logs warning events with context.
     */
    fun logWarning(operation: String, message: String, context: Map<String, Any> = emptyMap()) {
        val contextStr = if (context.isNotEmpty()) {
            context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else {
            "No additional context"
        }
        
        Log.w("$TAG_PREFIX:Warning", "Operation: $operation - Message: $message - Context: [$contextStr]")
    }
    
    /**
     * Measures and logs performance of distance calculation operations.
     */
    fun <T> measureDistanceCalculation(
        operation: String,
        friendCount: Int,
        block: () -> T
    ): T {
        val result: T
        val timeMs = measureTimeMillis {
            result = block()
        }
        
        if (BuildConfig.DEBUG) {
            val performanceLevel = when {
                timeMs > DISTANCE_CALCULATION_THRESHOLD_MS -> "SLOW"
                timeMs > DISTANCE_CALCULATION_THRESHOLD_MS / 2 -> "MODERATE"
                else -> "FAST"
            }
            
            Log.d(
                "$TAG_PREFIX:Performance", 
                "$operation completed in ${timeMs}ms for $friendCount friends - Performance: $performanceLevel"
            )
            
            if (timeMs > DISTANCE_CALCULATION_THRESHOLD_MS) {
                Log.w(
                    "$TAG_PREFIX:Performance", 
                    "Distance calculation took ${timeMs}ms (threshold: ${DISTANCE_CALCULATION_THRESHOLD_MS}ms) - Consider optimization"
                )
            }
        }
        
        return result
    }
    
    /**
     * Measures and logs UI update performance.
     */
    fun <T> measureUIUpdate(
        operation: String,
        itemCount: Int,
        block: () -> T
    ): T {
        val result: T
        val timeMs = measureTimeMillis {
            result = block()
        }
        
        if (BuildConfig.DEBUG) {
            Log.d(
                "$TAG_PREFIX:UI", 
                "$operation UI update completed in ${timeMs}ms for $itemCount items"
            )
            
            if (timeMs > UI_UPDATE_THRESHOLD_MS) {
                Log.w(
                    "$TAG_PREFIX:UI", 
                    "UI update took ${timeMs}ms (threshold: ${UI_UPDATE_THRESHOLD_MS}ms) - Consider optimization"
                )
            }
        }
        
        return result
    }
    
    /**
     * Logs detailed friend list state for debugging.
     */
    fun logFriendListState(friends: List<NearbyFriend>, searchQuery: String = "") {
        if (BuildConfig.DEBUG) {
            val queryInfo = if (searchQuery.isNotEmpty()) " (filtered by '$searchQuery')" else ""
            Log.d("$TAG_PREFIX:State", "Friend list state$queryInfo:")
            
            friends.forEachIndexed { index, friend ->
                Log.d(
                    "$TAG_PREFIX:State", 
                    "  [$index] ${friend.displayName} - ${friend.formattedDistance} - ${if (friend.isOnline) "Online" else "Offline"}"
                )
            }
            
            if (friends.isEmpty()) {
                Log.d("$TAG_PREFIX:State", "  No friends in list")
            }
        }
    }
    
    /**
     * Logs repository operation results.
     */
    fun logRepositoryOperation(operation: String, success: Boolean, friendId: String? = null) {
        if (BuildConfig.DEBUG) {
            val status = if (success) "SUCCESS" else "FAILURE"
            val friendInfo = friendId?.let { " for friend $it" } ?: ""
            Log.d("$TAG_PREFIX:Repository", "$operation $status$friendInfo")
        }
    }
    
    /**
     * Logs use case execution with timing.
     */
    fun <T> measureUseCaseExecution(
        useCaseName: String,
        parameters: Map<String, Any> = emptyMap(),
        block: () -> T
    ): T {
        if (BuildConfig.DEBUG) {
            val paramStr = if (parameters.isNotEmpty()) {
                parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }
            } else {
                "No parameters"
            }
            Log.d("$TAG_PREFIX:UseCase", "$useCaseName started - Parameters: [$paramStr]")
        }
        
        val result: T
        val timeMs = measureTimeMillis {
            result = block()
        }
        
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX:UseCase", "$useCaseName completed in ${timeMs}ms")
        }
        
        return result
    }
    
    /**
     * Debug utility to log memory usage during operations.
     */
    fun logMemoryUsage(operation: String) {
        if (BuildConfig.DEBUG) {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsagePercent = (usedMemory * 100 / maxMemory)
            
            Log.d(
                "$TAG_PREFIX:Memory", 
                "$operation - Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB ($memoryUsagePercent%)"
            )
            
            if (memoryUsagePercent > 80) {
                Log.w("$TAG_PREFIX:Memory", "High memory usage detected: $memoryUsagePercent%")
            }
        }
    }
}