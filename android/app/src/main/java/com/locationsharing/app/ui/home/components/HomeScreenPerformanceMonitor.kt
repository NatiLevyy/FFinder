package com.locationsharing.app.ui.home.components

import android.view.Choreographer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.math.max

/**
 * Performance monitoring component for the FFinder Home Screen.
 * 
 * This component monitors animation performance to ensure 60fps target is maintained
 * during all home screen animations including:
 * - Logo fade-in and zoom animations
 * - Map preview pin bounce animation
 * - Camera auto-pan animation
 * - What's New teaser slide-up animation
 * 
 * Features:
 * - Real-time frame rate monitoring
 * - Performance degradation detection
 * - Automatic animation quality adjustment
 * - Memory usage tracking
 * - Performance metrics reporting
 * 
 * @param isEnabled Whether performance monitoring is active
 * @param onPerformanceIssue Callback when performance issues are detected
 * @param onFrameRateUpdate Callback with current frame rate information
 */
@Composable
fun HomeScreenPerformanceMonitor(
    isEnabled: Boolean = true,
    onPerformanceIssue: (PerformanceIssue) -> Unit = {},
    onFrameRateUpdate: (FrameRateInfo) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Performance monitoring state
    var performanceMonitor by remember { mutableStateOf<PerformanceMonitor?>(null) }
    
    // Initialize performance monitor when enabled
    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            performanceMonitor = PerformanceMonitor(
                onPerformanceIssue = onPerformanceIssue,
                onFrameRateUpdate = onFrameRateUpdate
            )
            performanceMonitor?.start()
        } else {
            performanceMonitor?.stop()
            performanceMonitor = null
        }
    }
    
    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isEnabled) {
                        performanceMonitor?.resume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    performanceMonitor?.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    performanceMonitor?.stop()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            performanceMonitor?.stop()
        }
    }
}

/**
 * Performance monitor implementation using Choreographer for frame rate tracking.
 */
private class PerformanceMonitor(
    private val onPerformanceIssue: (PerformanceIssue) -> Unit,
    private val onFrameRateUpdate: (FrameRateInfo) -> Unit
) {
    private var isRunning = false
    private var isPaused = false
    private val frameTimeHistory = mutableListOf<Long>()
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var droppedFrameCount = 0
    
    // Target frame time for 60fps (16.67ms)
    private val targetFrameTimeNanos = 16_670_000L
    
    // Performance thresholds
    private val maxFrameTimeNanos = 33_340_000L // 30fps threshold
    private val performanceCheckInterval = 60 // Check every 60 frames
    
    private val choreographerCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning || isPaused) return
            
            if (lastFrameTime != 0L) {
                val frameTime = frameTimeNanos - lastFrameTime
                recordFrameTime(frameTime)
                
                // Check for dropped frames
                if (frameTime > maxFrameTimeNanos) {
                    droppedFrameCount++
                    onPerformanceIssue(
                        PerformanceIssue.DroppedFrame(
                            frameTimeMs = frameTime / 1_000_000f,
                            targetFrameTimeMs = targetFrameTimeNanos / 1_000_000f
                        )
                    )
                }
                
                frameCount++
                
                // Periodic performance analysis
                if (frameCount % performanceCheckInterval == 0) {
                    analyzePerformance()
                }
            }
            
            lastFrameTime = frameTimeNanos
            
            if (isRunning && !isPaused) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }
    
    fun start() {
        if (!isRunning) {
            isRunning = true
            isPaused = false
            frameCount = 0
            droppedFrameCount = 0
            frameTimeHistory.clear()
            Choreographer.getInstance().postFrameCallback(choreographerCallback)
        }
    }
    
    fun stop() {
        isRunning = false
        isPaused = false
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
    }
    
    fun pause() {
        isPaused = true
    }
    
    fun resume() {
        if (isRunning) {
            isPaused = false
            Choreographer.getInstance().postFrameCallback(choreographerCallback)
        }
    }
    
    private fun recordFrameTime(frameTimeNanos: Long) {
        frameTimeHistory.add(frameTimeNanos)
        
        // Keep only recent frame times (last 2 seconds at 60fps = 120 frames)
        if (frameTimeHistory.size > 120) {
            frameTimeHistory.removeAt(0)
        }
    }
    
    private fun analyzePerformance() {
        if (frameTimeHistory.isEmpty()) return
        
        val averageFrameTime = frameTimeHistory.average()
        val maxFrameTime = frameTimeHistory.maxOrNull() ?: 0L
        val minFrameTime = frameTimeHistory.minOrNull() ?: 0L
        
        // Calculate frame rate
        val averageFps = if (averageFrameTime > 0) {
            1_000_000_000.0 / averageFrameTime
        } else {
            0.0
        }
        
        // Calculate frame time variance (jitter)
        val variance = frameTimeHistory.map { frameTime ->
            val diff = frameTime - averageFrameTime
            diff * diff
        }.average()
        val jitter = kotlin.math.sqrt(variance)
        
        // Calculate dropped frame percentage
        val droppedFramePercentage = if (frameCount > 0) {
            (droppedFrameCount.toFloat() / frameCount) * 100f
        } else {
            0f
        }
        
        val frameRateInfo = FrameRateInfo(
            averageFps = averageFps,
            minFps = if (maxFrameTime > 0) 1_000_000_000.0 / maxFrameTime else 0.0,
            maxFps = if (minFrameTime > 0) 1_000_000_000.0 / minFrameTime else 0.0,
            jitterMs = jitter / 1_000_000.0,
            droppedFramePercentage = droppedFramePercentage,
            totalFrames = frameCount,
            droppedFrames = droppedFrameCount
        )
        
        onFrameRateUpdate(frameRateInfo)
        
        // Check for performance issues
        when {
            averageFps < 45.0 -> {
                onPerformanceIssue(
                    PerformanceIssue.LowFrameRate(
                        currentFps = averageFps,
                        targetFps = 60.0
                    )
                )
            }
            
            jitter > 5_000_000 -> { // 5ms jitter threshold
                onPerformanceIssue(
                    PerformanceIssue.HighJitter(
                        jitterMs = jitter / 1_000_000.0,
                        thresholdMs = 5.0
                    )
                )
            }
            
            droppedFramePercentage > 10f -> {
                onPerformanceIssue(
                    PerformanceIssue.ExcessiveDroppedFrames(
                        droppedPercentage = droppedFramePercentage,
                        thresholdPercentage = 10f
                    )
                )
            }
        }
    }
}

/**
 * Data class containing frame rate and performance information.
 */
data class FrameRateInfo(
    val averageFps: Double,
    val minFps: Double,
    val maxFps: Double,
    val jitterMs: Double,
    val droppedFramePercentage: Float,
    val totalFrames: Int,
    val droppedFrames: Int
) {
    /**
     * Whether the current performance meets the 60fps target.
     */
    val meetsTarget: Boolean
        get() = averageFps >= 55.0 && droppedFramePercentage < 5f
    
    /**
     * Performance grade based on frame rate and stability.
     */
    val performanceGrade: PerformanceGrade
        get() = when {
            averageFps >= 58.0 && droppedFramePercentage < 2f -> PerformanceGrade.Excellent
            averageFps >= 55.0 && droppedFramePercentage < 5f -> PerformanceGrade.Good
            averageFps >= 45.0 && droppedFramePercentage < 10f -> PerformanceGrade.Fair
            averageFps >= 30.0 && droppedFramePercentage < 20f -> PerformanceGrade.Poor
            else -> PerformanceGrade.Critical
        }
}

/**
 * Enum representing performance quality grades.
 */
enum class PerformanceGrade {
    Excellent,
    Good,
    Fair,
    Poor,
    Critical
}

/**
 * Sealed class representing different types of performance issues.
 */
sealed class PerformanceIssue {
    data class DroppedFrame(
        val frameTimeMs: Float,
        val targetFrameTimeMs: Float
    ) : PerformanceIssue()
    
    data class LowFrameRate(
        val currentFps: Double,
        val targetFps: Double
    ) : PerformanceIssue()
    
    data class HighJitter(
        val jitterMs: Double,
        val thresholdMs: Double
    ) : PerformanceIssue()
    
    data class ExcessiveDroppedFrames(
        val droppedPercentage: Float,
        val thresholdPercentage: Float
    ) : PerformanceIssue()
    
    data class MemoryPressure(
        val currentMemoryMb: Long,
        val thresholdMemoryMb: Long
    ) : PerformanceIssue()
}

/**
 * Composable that provides performance-aware animation configuration.
 * 
 * This composable monitors performance and automatically adjusts animation
 * quality to maintain smooth user experience.
 * 
 * @param baseAnimationsEnabled Base animation preference
 * @return AnimationConfig with performance-adjusted settings
 */
@Composable
fun rememberPerformanceAwareAnimationConfig(
    baseAnimationsEnabled: Boolean = true
): AnimationConfig {
    var currentFrameRate by remember { mutableStateOf(60.0) }
    var performanceGrade by remember { mutableStateOf(PerformanceGrade.Excellent) }
    
    // Monitor performance
    HomeScreenPerformanceMonitor(
        isEnabled = baseAnimationsEnabled,
        onFrameRateUpdate = { frameRateInfo ->
            currentFrameRate = frameRateInfo.averageFps
            performanceGrade = frameRateInfo.performanceGrade
        }
    )
    
    return remember(baseAnimationsEnabled, performanceGrade) {
        AnimationConfig(
            enabled = baseAnimationsEnabled,
            quality = when (performanceGrade) {
                PerformanceGrade.Excellent, PerformanceGrade.Good -> AnimationQuality.High
                PerformanceGrade.Fair -> AnimationQuality.Medium
                PerformanceGrade.Poor -> AnimationQuality.Low
                PerformanceGrade.Critical -> AnimationQuality.Disabled
            },
            frameRate = currentFrameRate,
            performanceGrade = performanceGrade
        )
    }
}

/**
 * Data class representing animation configuration based on performance.
 */
data class AnimationConfig(
    val enabled: Boolean,
    val quality: AnimationQuality,
    val frameRate: Double,
    val performanceGrade: PerformanceGrade
) {
    /**
     * Whether complex animations should be enabled.
     */
    val enableComplexAnimations: Boolean
        get() = enabled && quality.ordinal <= AnimationQuality.Medium.ordinal
    
    /**
     * Whether simple animations should be enabled.
     */
    val enableSimpleAnimations: Boolean
        get() = enabled && quality.ordinal <= AnimationQuality.Low.ordinal
    
    /**
     * Animation duration multiplier based on performance.
     */
    val durationMultiplier: Float
        get() = when (quality) {
            AnimationQuality.High -> 1.0f
            AnimationQuality.Medium -> 0.8f
            AnimationQuality.Low -> 0.6f
            AnimationQuality.Disabled -> 0.0f
        }
}

/**
 * Enum representing animation quality levels.
 */
enum class AnimationQuality {
    High,
    Medium,
    Low,
    Disabled
}

/**
 * Utility object for performance monitoring and optimization.
 */
object PerformanceUtils {
    
    /**
     * Gets the recommended animation duration based on performance.
     */
    fun getOptimizedDuration(
        baseDurationMs: Int,
        animationConfig: AnimationConfig
    ): Int {
        return (baseDurationMs * animationConfig.durationMultiplier).toInt()
    }
    
    /**
     * Determines if an animation should be skipped based on performance.
     */
    fun shouldSkipAnimation(
        animationType: AnimationType,
        animationConfig: AnimationConfig
    ): Boolean {
        return when (animationType) {
            AnimationType.Essential -> !animationConfig.enableSimpleAnimations
            AnimationType.Enhanced -> !animationConfig.enableComplexAnimations
            AnimationType.Decorative -> animationConfig.quality.ordinal > AnimationQuality.High.ordinal
        }
    }
    
    /**
     * Gets performance-optimized easing curve.
     */
    fun getOptimizedEasing(animationConfig: AnimationConfig): androidx.compose.animation.core.Easing {
        return when (animationConfig.quality) {
            AnimationQuality.High -> androidx.compose.animation.core.EaseInOutCubic
            AnimationQuality.Medium -> androidx.compose.animation.core.EaseInOut
            AnimationQuality.Low -> androidx.compose.animation.core.LinearEasing
            AnimationQuality.Disabled -> androidx.compose.animation.core.LinearEasing
        }
    }
}

/**
 * Enum representing different types of animations by importance.
 */
enum class AnimationType {
    Essential,    // Critical for UX (e.g., loading indicators)
    Enhanced,     // Improves UX (e.g., transitions)
    Decorative    // Nice to have (e.g., ambient animations)
}