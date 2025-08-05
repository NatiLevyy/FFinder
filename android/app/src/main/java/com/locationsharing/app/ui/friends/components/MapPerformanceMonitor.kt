package com.locationsharing.app.ui.friends.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring component for map operations
 * Tracks frame rates, memory usage, and rendering performance
 */
@Composable
fun MapPerformanceMonitor(
    friendsCount: Int,
    onlineFriendsCount: Int,
    isEnabled: Boolean = true
) {
    var frameCount by remember { mutableStateOf(0) }
    var lastFrameTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var averageFps by remember { mutableStateOf(60f) }
    
    LaunchedEffect(isEnabled) {
        if (!isEnabled) return@LaunchedEffect
        
        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = currentTime - lastFrameTime
            
            if (deltaTime > 0) {
                val currentFps = 1000f / deltaTime
                averageFps = (averageFps * 0.9f + currentFps * 0.1f) // Smooth average
                
                frameCount++
                
                // Log performance metrics every 5 seconds
                if (frameCount % 300 == 0) { // ~5 seconds at 60 FPS
                    logPerformanceMetrics(
                        fps = averageFps,
                        friendsCount = friendsCount,
                        onlineFriendsCount = onlineFriendsCount
                    )
                }
                
                // Warn if FPS drops significantly
                if (averageFps < 45f && frameCount > 60) {
                    Timber.w("Map performance degraded: ${averageFps.toInt()} FPS with $friendsCount friends")
                }
            }
            
            lastFrameTime = currentTime
            delay(16) // ~60 FPS monitoring
        }
    }
}

/**
 * Log performance metrics for monitoring
 */
private fun logPerformanceMetrics(
    fps: Float,
    friendsCount: Int,
    onlineFriendsCount: Int
) {
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
    val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
    val memoryUsage = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt()
    
    Timber.d(
        "Map Performance - FPS: ${fps.toInt()}, " +
        "Friends: $friendsCount (${onlineFriendsCount} online), " +
        "Memory: ${usedMemory}MB/${maxMemory}MB (${memoryUsage}%)"
    )
    
    // Alert if memory usage is high
    if (memoryUsage > 80) {
        Timber.w("High memory usage detected: ${memoryUsage}%")
    }
}

/**
 * Measure and log the performance of a map operation
 */
inline fun <T> measureMapOperation(
    operationName: String,
    operation: () -> T
): T {
    val result: T
    val executionTime = measureTimeMillis {
        result = operation()
    }
    
    if (executionTime > 16) { // Longer than one frame at 60 FPS
        Timber.w("Slow map operation: $operationName took ${executionTime}ms")
    } else {
        Timber.d("Map operation: $operationName completed in ${executionTime}ms")
    }
    
    return result
}