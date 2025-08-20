package com.locationsharing.app.ui.animation

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.view.Choreographer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * Comprehensive animation performance optimization system for FFinder.
 * 
 * This system implements Task 13 requirements:
 * - Proper animation lifecycle management with DisposableEffect
 * - Animation performance monitoring using Choreographer callbacks
 * - Optimized recomposition with stable parameters and remember blocks
 * - Reduced motion alternatives for accessibility compliance
 * - Memory usage monitoring for animation-heavy components
 * 
 * Features:
 * - Real-time performance monitoring
 * - Battery-aware animation scaling
 * - Thermal throttling detection
 * - Memory pressure monitoring
 * - Accessibility compliance
 * - Automatic quality adjustment
 */
@Composable
fun AnimationPerformanceOptimizer(
    isEnabled: Boolean = true,
    onPerformanceIssue: (AnimationPerformanceIssue) -> Unit = {},
    onPerformanceUpdate: (AnimationPerformanceMetrics) -> Unit = {},
    content: @Composable (AnimationOptimizationConfig) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Performance monitoring state
    var performanceMonitor by remember { mutableStateOf<AnimationPerformanceMonitor?>(null) }
    var optimizationConfig by remember { mutableStateOf(AnimationOptimizationConfig()) }
    
    // Initialize performance monitor
    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            performanceMonitor = AnimationPerformanceMonitor(
                context = context,
                onPerformanceIssue = onPerformanceIssue,
                onPerformanceUpdate = { metrics ->
                    onPerformanceUpdate(metrics)
                    optimizationConfig = optimizationConfig.copy(
                        currentFps = metrics.averageFps,
                        memoryUsageMb = metrics.memoryUsageMb,
                        batteryLevel = metrics.batteryLevel,
                        thermalState = metrics.thermalState,
                        isLowPowerMode = metrics.isLowPowerMode,
                        animationQuality = determineAnimationQuality(metrics)
                    )
                }
            )
            performanceMonitor?.start()
        } else {
            performanceMonitor?.stop()
            performanceMonitor = null
        }
    }
    
    // Lifecycle management with DisposableEffect
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
            performanceMonitor = null
        }
    }
    
    // Provide optimized configuration to content
    content(optimizationConfig)
}

/**
 * Animation performance monitor using Choreographer callbacks.
 * Implements comprehensive performance tracking and optimization.
 */
private class AnimationPerformanceMonitor(
    private val context: Context,
    private val onPerformanceIssue: (AnimationPerformanceIssue) -> Unit,
    private val onPerformanceUpdate: (AnimationPerformanceMetrics) -> Unit
) {
    private var isRunning = false
    private var isPaused = false
    
    // Frame tracking
    private val frameTimeHistory = mutableListOf<Long>()
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var droppedFrameCount = 0
    
    // Performance thresholds
    private val targetFrameTimeNanos = 16_670_000L // 60fps
    private val maxFrameTimeNanos = 33_340_000L // 30fps threshold
    private val performanceCheckInterval = 60 // Check every 60 frames
    
    // System services
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    // Memory monitoring
    private val runtime = Runtime.getRuntime()
    private var lastMemoryCheck = 0L
    private val memoryCheckInterval = 5000L // Check every 5 seconds
    
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
                        AnimationPerformanceIssue.DroppedFrame(
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
                
                // Memory monitoring
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMemoryCheck > memoryCheckInterval) {
                    checkMemoryUsage()
                    lastMemoryCheck = currentTime
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
            lastMemoryCheck = System.currentTimeMillis()
            Choreographer.getInstance().postFrameCallback(choreographerCallback)
            Timber.d("AnimationPerformanceMonitor started")
        }
    }
    
    fun stop() {
        isRunning = false
        isPaused = false
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
        Timber.d("AnimationPerformanceMonitor stopped")
    }
    
    fun pause() {
        isPaused = true
        Timber.d("AnimationPerformanceMonitor paused")
    }
    
    fun resume() {
        if (isRunning) {
            isPaused = false
            Choreographer.getInstance().postFrameCallback(choreographerCallback)
            Timber.d("AnimationPerformanceMonitor resumed")
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
        
        // Get system information
        val batteryLevel = getBatteryLevel()
        val isLowPowerMode = isLowPowerMode()
        val thermalState = getThermalState()
        val memoryUsage = getCurrentMemoryUsage()
        val isReducedMotionEnabled = isReducedMotionEnabled()
        
        val metrics = AnimationPerformanceMetrics(
            averageFps = averageFps,
            minFps = if (maxFrameTime > 0) 1_000_000_000.0 / maxFrameTime else 0.0,
            maxFps = if (minFrameTime > 0) 1_000_000_000.0 / minFrameTime else 0.0,
            jitterMs = jitter / 1_000_000.0,
            droppedFramePercentage = droppedFramePercentage,
            totalFrames = frameCount,
            droppedFrames = droppedFrameCount,
            memoryUsageMb = memoryUsage,
            batteryLevel = batteryLevel,
            isLowPowerMode = isLowPowerMode,
            thermalState = thermalState,
            isReducedMotionEnabled = isReducedMotionEnabled
        )
        
        onPerformanceUpdate(metrics)
        
        // Check for performance issues
        checkPerformanceIssues(metrics)
    }
    
    private fun checkPerformanceIssues(metrics: AnimationPerformanceMetrics) {
        when {
            metrics.averageFps < 45.0 -> {
                onPerformanceIssue(
                    AnimationPerformanceIssue.LowFrameRate(
                        currentFps = metrics.averageFps,
                        targetFps = 60.0
                    )
                )
            }
            
            metrics.jitterMs > 5.0 -> {
                onPerformanceIssue(
                    AnimationPerformanceIssue.HighJitter(
                        jitterMs = metrics.jitterMs,
                        thresholdMs = 5.0
                    )
                )
            }
            
            metrics.droppedFramePercentage > 10f -> {
                onPerformanceIssue(
                    AnimationPerformanceIssue.ExcessiveDroppedFrames(
                        droppedPercentage = metrics.droppedFramePercentage,
                        thresholdPercentage = 10f
                    )
                )
            }
            
            metrics.memoryUsageMb > 200L -> {
                onPerformanceIssue(
                    AnimationPerformanceIssue.MemoryPressure(
                        currentMemoryMb = metrics.memoryUsageMb,
                        thresholdMemoryMb = 200L
                    )
                )
            }
            
            metrics.thermalState >= ThermalState.THROTTLING -> {
                onPerformanceIssue(
                    AnimationPerformanceIssue.ThermalThrottling(
                        thermalState = metrics.thermalState
                    )
                )
            }
        }
    }
    
    private fun checkMemoryUsage() {
        val currentMemory = getCurrentMemoryUsage()
        if (currentMemory > 150L) { // 150MB threshold
            // Suggest garbage collection
            System.gc()
        }
    }
    
    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun isLowPowerMode(): Boolean {
        return powerManager.isPowerSaveMode
    }
    
    private fun getThermalState(): ThermalState {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalState.NONE
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.EMERGENCY
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.SHUTDOWN
                else -> ThermalState.THROTTLING
            }
        } else {
            ThermalState.NONE
        }
    }
    
    private fun getCurrentMemoryUsage(): Long {
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024) // Convert to MB
    }
    
    private fun isReducedMotionEnabled(): Boolean {
        return try {
            val animationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            animationScale == 0.0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Determines optimal animation quality based on performance metrics.
 */
private fun determineAnimationQuality(metrics: AnimationPerformanceMetrics): AnimationQuality {
    return when {
        metrics.isReducedMotionEnabled -> AnimationQuality.DISABLED
        metrics.isLowPowerMode -> AnimationQuality.MINIMAL
        metrics.batteryLevel < 20 -> AnimationQuality.MINIMAL
        metrics.thermalState >= ThermalState.SEVERE -> AnimationQuality.MINIMAL
        metrics.averageFps < 30.0 -> AnimationQuality.MINIMAL
        metrics.memoryUsageMb > 200L -> AnimationQuality.REDUCED
        metrics.batteryLevel < 50 -> AnimationQuality.REDUCED
        metrics.thermalState >= ThermalState.MODERATE -> AnimationQuality.REDUCED
        metrics.averageFps < 45.0 -> AnimationQuality.REDUCED
        metrics.droppedFramePercentage > 10f -> AnimationQuality.REDUCED
        else -> AnimationQuality.FULL
    }
}

/**
 * Configuration for animation optimization based on current performance.
 */
data class AnimationOptimizationConfig(
    val currentFps: Double = 60.0,
    val memoryUsageMb: Long = 0L,
    val batteryLevel: Int = 100,
    val thermalState: ThermalState = ThermalState.NONE,
    val isLowPowerMode: Boolean = false,
    val animationQuality: AnimationQuality = AnimationQuality.FULL
) {
    /**
     * Whether complex animations should be enabled.
     */
    val enableComplexAnimations: Boolean
        get() = animationQuality == AnimationQuality.FULL
    
    /**
     * Whether simple animations should be enabled.
     */
    val enableSimpleAnimations: Boolean
        get() = animationQuality != AnimationQuality.DISABLED
    
    /**
     * Animation duration multiplier based on performance.
     */
    val durationMultiplier: Float
        get() = when (animationQuality) {
            AnimationQuality.FULL -> 1.0f
            AnimationQuality.REDUCED -> 0.7f
            AnimationQuality.MINIMAL -> 0.5f
            AnimationQuality.DISABLED -> 0.0f
        }
    
    /**
     * Maximum concurrent animations allowed.
     */
    val maxConcurrentAnimations: Int
        get() = when (animationQuality) {
            AnimationQuality.FULL -> 8
            AnimationQuality.REDUCED -> 4
            AnimationQuality.MINIMAL -> 2
            AnimationQuality.DISABLED -> 0
        }
    
    /**
     * Whether to use hardware acceleration for animations.
     */
    val useHardwareAcceleration: Boolean
        get() = animationQuality != AnimationQuality.DISABLED && currentFps > 30.0
}

/**
 * Animation quality levels based on device performance.
 */
enum class AnimationQuality {
    FULL,       // All animations enabled with full quality
    REDUCED,    // Reduced animation complexity and frequency
    MINIMAL,    // Only essential animations
    DISABLED    // No animations (accessibility/performance)
}

/**
 * Thermal state enumeration.
 */
enum class ThermalState {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    THROTTLING
}

/**
 * Comprehensive animation performance metrics.
 */
data class AnimationPerformanceMetrics(
    val averageFps: Double,
    val minFps: Double,
    val maxFps: Double,
    val jitterMs: Double,
    val droppedFramePercentage: Float,
    val totalFrames: Int,
    val droppedFrames: Int,
    val memoryUsageMb: Long,
    val batteryLevel: Int,
    val isLowPowerMode: Boolean,
    val thermalState: ThermalState,
    val isReducedMotionEnabled: Boolean
) {
    /**
     * Overall performance grade.
     */
    val performanceGrade: PerformanceGrade
        get() = when {
            averageFps >= 58.0 && droppedFramePercentage < 2f && memoryUsageMb < 100L -> PerformanceGrade.EXCELLENT
            averageFps >= 55.0 && droppedFramePercentage < 5f && memoryUsageMb < 150L -> PerformanceGrade.GOOD
            averageFps >= 45.0 && droppedFramePercentage < 10f && memoryUsageMb < 200L -> PerformanceGrade.FAIR
            averageFps >= 30.0 && droppedFramePercentage < 20f -> PerformanceGrade.POOR
            else -> PerformanceGrade.CRITICAL
        }
}

/**
 * Performance grade enumeration.
 */
enum class PerformanceGrade {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}

/**
 * Animation performance issues.
 */
sealed class AnimationPerformanceIssue {
    data class DroppedFrame(
        val frameTimeMs: Float,
        val targetFrameTimeMs: Float
    ) : AnimationPerformanceIssue()
    
    data class LowFrameRate(
        val currentFps: Double,
        val targetFps: Double
    ) : AnimationPerformanceIssue()
    
    data class HighJitter(
        val jitterMs: Double,
        val thresholdMs: Double
    ) : AnimationPerformanceIssue()
    
    data class ExcessiveDroppedFrames(
        val droppedPercentage: Float,
        val thresholdPercentage: Float
    ) : AnimationPerformanceIssue()
    
    data class MemoryPressure(
        val currentMemoryMb: Long,
        val thresholdMemoryMb: Long
    ) : AnimationPerformanceIssue()
    
    data class ThermalThrottling(
        val thermalState: ThermalState
    ) : AnimationPerformanceIssue()
}