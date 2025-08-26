package com.locationsharing.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Animation lifecycle manager that provides proper cleanup and memory management.
 * 
 * This manager implements Task 13 requirements for animation lifecycle management:
 * - Proper DisposableEffect usage for cleanup
 * - Memory leak prevention
 * - Animation state tracking
 * - Automatic cleanup on lifecycle events
 * - Performance monitoring integration
 */
class AnimationLifecycleManager {
    private val activeAnimations = ConcurrentHashMap<String, AnimationState>()
    private val animationCounter = AtomicInteger(0)
    private var isDisposed = false
    
    /**
     * Registers a new animation with automatic lifecycle management.
     */
    fun registerAnimation(
        animationId: String = generateAnimationId(),
        onComplete: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ): String {
        if (isDisposed) {
            Timber.w("Attempting to register animation on disposed manager")
            return animationId
        }
        
        val animationState = AnimationState(
            id = animationId,
            startTime = System.currentTimeMillis(),
            onComplete = onComplete,
            onCancel = onCancel
        )
        
        activeAnimations[animationId] = animationState
        Timber.d("Registered animation: $animationId (total: ${activeAnimations.size})")
        
        return animationId
    }
    
    /**
     * Unregisters an animation and performs cleanup.
     */
    fun unregisterAnimation(animationId: String, completed: Boolean = true) {
        val animationState = activeAnimations.remove(animationId)
        if (animationState != null) {
            if (completed) {
                animationState.onComplete?.invoke()
            } else {
                animationState.onCancel?.invoke()
            }
            
            // Cancel any associated coroutines
            animationState.coroutineJob?.cancel()
            
            Timber.d("Unregistered animation: $animationId (remaining: ${activeAnimations.size})")
        }
    }
    
    /**
     * Associates a coroutine job with an animation for proper cleanup.
     */
    fun associateJob(animationId: String, job: Job) {
        activeAnimations[animationId]?.coroutineJob = job
    }
    
    /**
     * Gets the current number of active animations.
     */
    fun getActiveAnimationCount(): Int = activeAnimations.size
    
    /**
     * Gets information about all active animations.
     */
    fun getActiveAnimations(): List<AnimationInfo> {
        val currentTime = System.currentTimeMillis()
        return activeAnimations.values.map { state ->
            AnimationInfo(
                id = state.id,
                durationMs = currentTime - state.startTime,
                isLongRunning = (currentTime - state.startTime) > 5000L
            )
        }
    }
    
    /**
     * Cancels all active animations and cleans up resources.
     */
    fun cancelAllAnimations() {
        val animationsToCancel = activeAnimations.keys.toList()
        animationsToCancel.forEach { animationId ->
            unregisterAnimation(animationId, completed = false)
        }
        Timber.d("Cancelled ${animationsToCancel.size} animations")
    }
    
    /**
     * Disposes the manager and cleans up all resources.
     */
    fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            cancelAllAnimations()
            activeAnimations.clear()
            Timber.d("AnimationLifecycleManager disposed")
        }
    }
    
    /**
     * Handles lifecycle events for proper animation management.
     */
    fun onLifecycleEvent(event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                // Optionally pause long-running animations
                val longRunningAnimations = getActiveAnimations().filter { it.isLongRunning }
                if (longRunningAnimations.isNotEmpty()) {
                    Timber.d("Found ${longRunningAnimations.size} long-running animations on pause")
                }
            }
            Lifecycle.Event.ON_STOP -> {
                // Cancel non-essential animations to save resources
                val currentTime = System.currentTimeMillis()
                val animationsToCancel = activeAnimations.filter { (_, state) ->
                    (currentTime - state.startTime) > 10000L // Cancel animations running > 10s
                }.keys.toList()
                
                animationsToCancel.forEach { animationId ->
                    unregisterAnimation(animationId, completed = false)
                }
                
                if (animationsToCancel.isNotEmpty()) {
                    Timber.d("Cancelled ${animationsToCancel.size} long-running animations on stop")
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                dispose()
            }
            else -> {}
        }
    }
    
    private fun generateAnimationId(): String {
        return "anim_${animationCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    private data class AnimationState(
        val id: String,
        val startTime: Long,
        val onComplete: (() -> Unit)?,
        val onCancel: (() -> Unit)?,
        var coroutineJob: Job? = null
    )
}

/**
 * Information about an active animation.
 */
data class AnimationInfo(
    val id: String,
    val durationMs: Long,
    val isLongRunning: Boolean
)

/**
 * Composable that provides animation lifecycle management with automatic cleanup.
 */
@Composable
fun rememberAnimationLifecycleManager(): AnimationLifecycleManager {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val manager = remember { AnimationLifecycleManager() }
    
    // Lifecycle management with DisposableEffect
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            manager.onLifecycleEvent(event)
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.dispose()
        }
    }
    
    return manager
}

/**
 * Composable that manages a single animation with automatic lifecycle cleanup.
 */
@Composable
fun <T> rememberManagedAnimation(
    targetValue: T,
    animationSpec: AnimationSpec<T>,
    label: String = "",
    finishedListener: ((T) -> Unit)? = null,
    lifecycleManager: AnimationLifecycleManager = rememberAnimationLifecycleManager()
): State<T> {
    val animationState = animateFloatAsState(
        targetValue = if (targetValue is Float) targetValue else 0f,
        animationSpec = animationSpec as AnimationSpec<Float>,
        label = label,
        finishedListener = { finishedListener?.invoke(targetValue) }
    ) as State<T>
    
    // Register animation with lifecycle manager
    LaunchedEffect(targetValue) {
        val animationId = lifecycleManager.registerAnimation(
            onComplete = { finishedListener?.invoke(targetValue) }
        )
        
        // Associate this coroutine with the animation
        lifecycleManager.associateJob(animationId, coroutineContext[Job]!!)
    }
    
    return animationState
}

/**
 * Composable that manages an infinite animation with automatic lifecycle cleanup.
 */
@Composable
fun rememberManagedInfiniteTransition(
    label: String = "",
    lifecycleManager: AnimationLifecycleManager = rememberAnimationLifecycleManager()
): InfiniteTransition {
    val infiniteTransition = rememberInfiniteTransition(label = label)
    
    // Register infinite animation with lifecycle manager
    LaunchedEffect(Unit) {
        val animationId = lifecycleManager.registerAnimation(
            animationId = "infinite_$label",
            onCancel = {
                // Infinite animations don't naturally complete, so we handle cancellation
                Timber.d("Infinite animation cancelled: $label")
            }
        )
        
        // Associate this coroutine with the animation
        lifecycleManager.associateJob(animationId, coroutineContext[Job]!!)
    }
    
    return infiniteTransition
}

/**
 * Extension function to create a managed animatable with automatic cleanup.
 */
@Composable
fun <T, V : AnimationVector> Animatable<T, V>.managedBy(
    lifecycleManager: AnimationLifecycleManager = rememberAnimationLifecycleManager(),
    animationId: String = "animatable_${hashCode()}"
): Animatable<T, V> {
    LaunchedEffect(this) {
        val registeredId = lifecycleManager.registerAnimation(
            animationId = animationId,
            onCancel = {
                // Stop any running animations
                launch { stop() }
            }
        )
        
        lifecycleManager.associateJob(registeredId, coroutineContext[Job]!!)
    }
    
    return this
}

/**
 * Composable that provides animation performance monitoring integration.
 */
@Composable
fun AnimationPerformanceMonitor(
    lifecycleManager: AnimationLifecycleManager = rememberAnimationLifecycleManager(),
    onPerformanceUpdate: (AnimationPerformanceData) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        // Monitor animation performance periodically
        while (true) {
            kotlinx.coroutines.delay(1000L) // Check every second
            
            val activeAnimations = lifecycleManager.getActiveAnimations()
            val performanceData = AnimationPerformanceData(
                activeAnimationCount = activeAnimations.size,
                longRunningAnimationCount = activeAnimations.count { it.isLongRunning },
                averageAnimationDuration = if (activeAnimations.isNotEmpty()) {
                    activeAnimations.map { it.durationMs }.average()
                } else 0.0,
                memoryEstimateMb = activeAnimations.size * 2L // Rough estimate: 2MB per animation
            )
            
            onPerformanceUpdate(performanceData)
            
            // Log performance warnings
            if (performanceData.activeAnimationCount > 10) {
                Timber.w("High animation count: ${performanceData.activeAnimationCount}")
            }
            
            if (performanceData.longRunningAnimationCount > 3) {
                Timber.w("Multiple long-running animations: ${performanceData.longRunningAnimationCount}")
            }
        }
    }
}

/**
 * Performance data for animation monitoring.
 */
data class AnimationPerformanceData(
    val activeAnimationCount: Int,
    val longRunningAnimationCount: Int,
    val averageAnimationDuration: Double,
    val memoryEstimateMb: Long
)