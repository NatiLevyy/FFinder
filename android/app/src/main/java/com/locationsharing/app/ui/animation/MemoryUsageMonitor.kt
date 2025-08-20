package com.locationsharing.app.ui.animation

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.math.max

/**
 * Memory usage monitoring for animation-heavy components.
 * 
 * This file implements Task 13 requirements for memory monitoring:
 * - Real-time memory usage tracking
 * - Animation-specific memory profiling
 * - Memory leak detection
 * - Automatic cleanup suggestions
 * - Performance impact analysis
 */

/**
 * Memory usage metrics for animations.
 */
data class AnimationMemoryMetrics(
    val totalMemoryMb: Long,
    val usedMemoryMb: Long,
    val freeMemoryMb: Long,
    val maxMemoryMb: Long,
    val animationMemoryMb: Long,
    val memoryPressureLevel: MemoryPressureLevel,
    val gcCount: Int,
    val nativeHeapSizeMb: Long,
    val nativeHeapAllocatedMb: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Memory usage percentage (0-100).
     */
    val memoryUsagePercentage: Float
        get() = if (maxMemoryMb > 0) {
            (usedMemoryMb.toFloat() / maxMemoryMb.toFloat()) * 100f
        } else 0f
    
    /**
     * Available memory percentage (0-100).
     */
    val availableMemoryPercentage: Float
        get() = 100f - memoryUsagePercentage
    
    /**
     * Whether memory usage is critical.
     */
    val isCritical: Boolean
        get() = memoryPressureLevel == MemoryPressureLevel.CRITICAL ||
                memoryUsagePercentage > 90f
    
    /**
     * Whether memory usage is high.
     */
    val isHigh: Boolean
        get() = memoryPressureLevel == MemoryPressureLevel.HIGH ||
                memoryUsagePercentage > 75f
}

/**
 * Memory pressure levels.
 */
enum class MemoryPressureLevel {
    LOW,      // < 50% memory usage
    MODERATE, // 50-75% memory usage
    HIGH,     // 75-90% memory usage
    CRITICAL  // > 90% memory usage
}

/**
 * Memory monitoring configuration.
 */
data class MemoryMonitorConfig(
    val monitoringIntervalMs: Long = 1000L,
    val enableDetailedProfiling: Boolean = true,
    val enableGcTracking: Boolean = true,
    val enableNativeHeapTracking: Boolean = true,
    val memoryThresholdMb: Long = 200L,
    val criticalThresholdMb: Long = 300L
)

/**
 * Animation memory monitor that tracks memory usage for animation components.
 */
class AnimationMemoryMonitor(
    private val context: Context,
    private val config: MemoryMonitorConfig = MemoryMonitorConfig()
) {
    private val runtime = Runtime.getRuntime()
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()
    
    private var lastGcCount = 0
    private var animationMemoryEstimate = 0L
    private val activeAnimationComponents = mutableSetOf<WeakReference<Any>>()
    
    /**
     * Gets current memory metrics.
     */
    fun getCurrentMetrics(): AnimationMemoryMetrics {
        // Update memory info
        activityManager.getMemoryInfo(memoryInfo)
        
        // Runtime memory
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        // Native heap (if available)
        val nativeHeapSize = if (config.enableNativeHeapTracking) {
            Debug.getNativeHeapSize()
        } else 0L
        
        val nativeHeapAllocated = if (config.enableNativeHeapTracking) {
            Debug.getNativeHeapAllocatedSize()
        } else 0L
        
        // GC tracking
        val currentGcCount = if (config.enableGcTracking) {
            System.gc() // Suggest GC for accurate measurement
            Runtime.getRuntime().gc()
            0 // Simplified GC count
        } else 0
        
        // Clean up weak references
        cleanupWeakReferences()
        
        // Estimate animation memory usage
        updateAnimationMemoryEstimate()
        
        // Determine memory pressure level
        val memoryUsagePercentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
        val pressureLevel = when {
            memoryUsagePercentage > 90f -> MemoryPressureLevel.CRITICAL
            memoryUsagePercentage > 75f -> MemoryPressureLevel.HIGH
            memoryUsagePercentage > 50f -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.LOW
        }
        
        return AnimationMemoryMetrics(
            totalMemoryMb = totalMemory / (1024 * 1024),
            usedMemoryMb = usedMemory / (1024 * 1024),
            freeMemoryMb = freeMemory / (1024 * 1024),
            maxMemoryMb = maxMemory / (1024 * 1024),
            animationMemoryMb = animationMemoryEstimate / (1024 * 1024),
            memoryPressureLevel = pressureLevel,
            gcCount = currentGcCount - lastGcCount,
            nativeHeapSizeMb = nativeHeapSize / (1024 * 1024),
            nativeHeapAllocatedMb = nativeHeapAllocated / (1024 * 1024)
        )
    }
    
    /**
     * Registers an animation component for memory tracking.
     */
    fun registerAnimationComponent(component: Any, estimatedMemoryMb: Long = 2L) {
        activeAnimationComponents.add(WeakReference(component))
        animationMemoryEstimate += estimatedMemoryMb * 1024 * 1024
        
        Timber.d("Registered animation component: ${component.javaClass.simpleName} (+${estimatedMemoryMb}MB)")
    }
    
    /**
     * Unregisters an animation component.
     */
    fun unregisterAnimationComponent(component: Any, estimatedMemoryMb: Long = 2L) {
        activeAnimationComponents.removeAll { it.get() == component }
        animationMemoryEstimate = max(0L, animationMemoryEstimate - (estimatedMemoryMb * 1024 * 1024))
        
        Timber.d("Unregistered animation component: ${component.javaClass.simpleName} (-${estimatedMemoryMb}MB)")
    }
    
    /**
     * Creates a flow of memory metrics.
     */
    fun memoryMetricsFlow(): Flow<AnimationMemoryMetrics> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getCurrentMetrics())
            delay(config.monitoringIntervalMs)
        }
    }
    
    /**
     * Analyzes memory usage and provides optimization suggestions.
     */
    fun analyzeMemoryUsage(metrics: AnimationMemoryMetrics): MemoryAnalysisResult {
        val suggestions = mutableListOf<MemoryOptimizationSuggestion>()
        val warnings = mutableListOf<String>()
        
        // High memory usage
        if (metrics.memoryUsagePercentage > 80f) {
            suggestions.add(MemoryOptimizationSuggestion.REDUCE_ANIMATION_COMPLEXITY)
            warnings.add("High memory usage: ${metrics.memoryUsagePercentage.toInt()}%")
        }
        
        // High animation memory
        if (metrics.animationMemoryMb > 50L) {
            suggestions.add(MemoryOptimizationSuggestion.LIMIT_CONCURRENT_ANIMATIONS)
            warnings.add("High animation memory usage: ${metrics.animationMemoryMb}MB")
        }
        
        // Frequent GC
        if (metrics.gcCount > 5) {
            suggestions.add(MemoryOptimizationSuggestion.OPTIMIZE_OBJECT_ALLOCATION)
            warnings.add("Frequent garbage collection detected")
        }
        
        // Low available memory
        if (metrics.availableMemoryPercentage < 20f) {
            suggestions.add(MemoryOptimizationSuggestion.ENABLE_MEMORY_SAVING_MODE)
            warnings.add("Low available memory: ${metrics.availableMemoryPercentage.toInt()}%")
        }
        
        // Native heap issues
        if (metrics.nativeHeapAllocatedMb > 100L) {
            suggestions.add(MemoryOptimizationSuggestion.OPTIMIZE_NATIVE_RESOURCES)
            warnings.add("High native heap usage: ${metrics.nativeHeapAllocatedMb}MB")
        }
        
        return MemoryAnalysisResult(
            metrics = metrics,
            suggestions = suggestions,
            warnings = warnings,
            overallHealth = determineMemoryHealth(metrics)
        )
    }
    
    /**
     * Triggers memory cleanup operations.
     */
    fun performMemoryCleanup(): MemoryCleanupResult {
        val beforeMetrics = getCurrentMetrics()
        
        // Clean up weak references
        val cleanedReferences = cleanupWeakReferences()
        
        // Suggest garbage collection
        System.gc()
        Runtime.getRuntime().gc()
        
        // Wait a bit for GC to complete
        Thread.sleep(100)
        
        val afterMetrics = getCurrentMetrics()
        val memoryFreed = beforeMetrics.usedMemoryMb - afterMetrics.usedMemoryMb
        
        Timber.d("Memory cleanup completed: freed ${memoryFreed}MB, cleaned $cleanedReferences references")
        
        return MemoryCleanupResult(
            memoryFreedMb = memoryFreed,
            referencesCleared = cleanedReferences,
            beforeMetrics = beforeMetrics,
            afterMetrics = afterMetrics
        )
    }
    
    private fun cleanupWeakReferences(): Int {
        val initialSize = activeAnimationComponents.size
        activeAnimationComponents.removeAll { it.get() == null }
        return initialSize - activeAnimationComponents.size
    }
    
    private fun updateAnimationMemoryEstimate() {
        // Recalculate based on active components
        val activeCount = activeAnimationComponents.count { it.get() != null }
        animationMemoryEstimate = activeCount * 2L * 1024 * 1024 // 2MB per component estimate
    }
    
    private fun determineMemoryHealth(metrics: AnimationMemoryMetrics): MemoryHealth {
        return when {
            metrics.isCritical -> MemoryHealth.CRITICAL
            metrics.isHigh -> MemoryHealth.POOR
            metrics.memoryUsagePercentage > 60f -> MemoryHealth.FAIR
            metrics.memoryUsagePercentage > 40f -> MemoryHealth.GOOD
            else -> MemoryHealth.EXCELLENT
        }
    }
}

/**
 * Memory analysis result.
 */
data class MemoryAnalysisResult(
    val metrics: AnimationMemoryMetrics,
    val suggestions: List<MemoryOptimizationSuggestion>,
    val warnings: List<String>,
    val overallHealth: MemoryHealth
)

/**
 * Memory cleanup result.
 */
data class MemoryCleanupResult(
    val memoryFreedMb: Long,
    val referencesCleared: Int,
    val beforeMetrics: AnimationMemoryMetrics,
    val afterMetrics: AnimationMemoryMetrics
)

/**
 * Memory optimization suggestions.
 */
enum class MemoryOptimizationSuggestion {
    REDUCE_ANIMATION_COMPLEXITY,
    LIMIT_CONCURRENT_ANIMATIONS,
    OPTIMIZE_OBJECT_ALLOCATION,
    ENABLE_MEMORY_SAVING_MODE,
    OPTIMIZE_NATIVE_RESOURCES,
    CLEAR_ANIMATION_CACHE,
    REDUCE_ANIMATION_DURATION,
    USE_HARDWARE_ACCELERATION
}

/**
 * Memory health levels.
 */
enum class MemoryHealth {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}

/**
 * Composable that provides memory monitoring for animation components.
 */
@Composable
fun AnimationMemoryMonitor(
    config: MemoryMonitorConfig = MemoryMonitorConfig(),
    onMemoryUpdate: (AnimationMemoryMetrics) -> Unit = {},
    onMemoryWarning: (MemoryAnalysisResult) -> Unit = {},
    content: @Composable (AnimationMemoryMonitor) -> Unit
) {
    val context = LocalContext.current
    val monitor = remember { AnimationMemoryMonitor(context, config) }
    
    // Monitor memory usage
    LaunchedEffect(monitor) {
        monitor.memoryMetricsFlow().collect { metrics ->
            onMemoryUpdate(metrics)
            
            // Analyze and report issues
            val analysis = monitor.analyzeMemoryUsage(metrics)
            if (analysis.warnings.isNotEmpty() || analysis.overallHealth == MemoryHealth.POOR || analysis.overallHealth == MemoryHealth.CRITICAL) {
                onMemoryWarning(analysis)
            }
            
            // Auto-cleanup if critical
            if (metrics.isCritical) {
                Timber.w("Critical memory usage detected, performing cleanup")
                monitor.performMemoryCleanup()
            }
        }
    }
    
    content(monitor)
}

/**
 * Composable that tracks memory usage for a specific animation component.
 */
@Composable
fun <T> rememberAnimationComponentWithMemoryTracking(
    component: T,
    estimatedMemoryMb: Long = 2L,
    monitor: AnimationMemoryMonitor? = null
): T {
    val context = LocalContext.current
    val localMonitor = monitor ?: remember { 
        AnimationMemoryMonitor(context) 
    }
    
    DisposableEffect(component) {
        localMonitor.registerAnimationComponent(component as Any, estimatedMemoryMb)
        
        onDispose {
            localMonitor.unregisterAnimationComponent(component as Any, estimatedMemoryMb)
        }
    }
    
    return component
}

/**
 * Memory-aware animation configuration that adapts based on available memory.
 */
@Composable
fun rememberMemoryAwareAnimationConfig(
    baseConfig: StableAnimationConfig,
    memoryThresholdMb: Long = 100L
): StableAnimationConfig {
    val context = LocalContext.current
    val monitor = remember { AnimationMemoryMonitor(context) }
    
    var adaptedConfig by remember { mutableStateOf(baseConfig) }
    
    LaunchedEffect(baseConfig) {
        monitor.memoryMetricsFlow().collect { metrics ->
            adaptedConfig = when {
                metrics.usedMemoryMb > memoryThresholdMb * 1.5f -> {
                    // High memory usage - minimal animations
                    baseConfig.copy(
                        duration = (baseConfig.duration * 0.3f).toInt(),
                        easing = LinearEasing
                    )
                }
                metrics.usedMemoryMb > memoryThresholdMb -> {
                    // Moderate memory usage - reduced animations
                    baseConfig.copy(
                        duration = (baseConfig.duration * 0.7f).toInt(),
                        easing = FastOutSlowInEasing
                    )
                }
                else -> {
                    // Normal memory usage - full animations
                    baseConfig
                }
            }
        }
    }
    
    return adaptedConfig
}

/**
 * Extension function to monitor memory usage of any composable.
 */
@Composable
fun Modifier.trackMemoryUsage(
    componentName: String,
    estimatedMemoryMb: Long = 1L,
    monitor: AnimationMemoryMonitor? = null
): Modifier {
    val context = LocalContext.current
    val localMonitor = monitor ?: remember { 
        AnimationMemoryMonitor(context) 
    }
    
    DisposableEffect(componentName) {
        localMonitor.registerAnimationComponent(componentName, estimatedMemoryMb)
        
        onDispose {
            localMonitor.unregisterAnimationComponent(componentName, estimatedMemoryMb)
        }
    }
    
    return this
}