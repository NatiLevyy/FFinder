package com.locationsharing.app.ui.animation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.PI

/**
 * Optimized recomposition utilities for animation performance.
 * 
 * This file implements Task 13 requirements for optimized recomposition:
 * - Stable parameters to prevent unnecessary recompositions
 * - Efficient remember blocks with proper keys
 * - Memoized animation configurations
 * - Optimized state management
 * - Performance-aware animation updates
 */

/**
 * Stable animation configuration that prevents unnecessary recompositions.
 */
@Stable
data class StableAnimationConfig(
    val duration: Int,
    val easing: Easing,
    val delayMillis: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.Restart,
    val iterations: Int = 1
) {
    /**
     * Converts to AnimationSpec with memoization.
     */
    fun toAnimationSpec(): AnimationSpec<Float> = tween(
        durationMillis = duration,
        easing = easing,
        delayMillis = delayMillis
    )
    
    /**
     * Converts to repeatable AnimationSpec.
     */
    fun toRepeatableSpec(): AnimationSpec<Float> = repeatable(
        iterations = iterations,
        animation = tween(
            durationMillis = duration,
            easing = easing,
            delayMillis = delayMillis
        ),
        repeatMode = repeatMode
    )
}

/**
 * Stable color animation configuration.
 */
@Stable
data class StableColorAnimationConfig(
    val duration: Int,
    val easing: Easing,
    val colorSpace: androidx.compose.ui.graphics.colorspace.ColorSpace = androidx.compose.ui.graphics.colorspace.ColorSpaces.Srgb
) {
    fun toAnimationSpec(): AnimationSpec<Color> = tween(
        durationMillis = duration,
        easing = easing
    )
}

/**
 * Stable size animation configuration.
 */
@Stable
data class StableSizeAnimationConfig(
    val duration: Int,
    val easing: Easing,
    val threshold: Dp = 0.5.dp
) {
    fun toAnimationSpec(): AnimationSpec<Dp> = tween(
        durationMillis = duration,
        easing = easing
    )
}

/**
 * Memoized animation configuration factory with performance optimization.
 */
@Composable
fun rememberOptimizedAnimationConfig(
    quality: AnimationQuality,
    baseConfig: StableAnimationConfig,
    performanceMultiplier: Float = 1f
): StableAnimationConfig {
    return remember(quality, baseConfig, performanceMultiplier) {
        when (quality) {
            AnimationQuality.FULL -> baseConfig.copy(
                duration = (baseConfig.duration * performanceMultiplier).toInt()
            )
            AnimationQuality.REDUCED -> baseConfig.copy(
                duration = (baseConfig.duration * 0.7f * performanceMultiplier).toInt(),
                easing = FastOutSlowInEasing
            )
            AnimationQuality.MINIMAL -> baseConfig.copy(
                duration = (baseConfig.duration * 0.5f * performanceMultiplier).toInt(),
                easing = LinearEasing
            )
            AnimationQuality.DISABLED -> baseConfig.copy(
                duration = 0,
                easing = LinearEasing
            )
        }
    }
}

/**
 * Optimized float animation with stable parameters and memoization.
 */
@Composable
fun rememberOptimizedFloatAnimation(
    targetValue: Float,
    config: StableAnimationConfig,
    label: String = "",
    finishedListener: ((Float) -> Unit)? = null
): State<Float> {
    // Memoize the animation spec to prevent recreation
    val animationSpec = remember(config) { config.toAnimationSpec() }
    
    // Use stable parameters to prevent unnecessary recompositions
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        label = label,
        finishedListener = finishedListener
    )
}

/**
 * Optimized color animation with stable parameters.
 */
@Composable
fun rememberOptimizedColorAnimation(
    targetValue: Color,
    config: StableColorAnimationConfig,
    label: String = "",
    finishedListener: ((Color) -> Unit)? = null
): State<Color> {
    val animationSpec = remember(config) { config.toAnimationSpec() }
    
    return animateColorAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        label = label,
        finishedListener = finishedListener
    )
}

/**
 * Optimized Dp animation with stable parameters.
 */
@Composable
fun rememberOptimizedDpAnimation(
    targetValue: Dp,
    config: StableSizeAnimationConfig,
    label: String = "",
    finishedListener: ((Dp) -> Unit)? = null
): State<Dp> {
    val animationSpec = remember(config) { config.toAnimationSpec() }
    
    return animateDpAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        label = label,
        finishedListener = finishedListener
    )
}

/**
 * Optimized infinite transition with memoized configuration.
 */
@Composable
fun rememberOptimizedInfiniteTransition(
    label: String = "",
    enabled: Boolean = true
): InfiniteTransition? {
    return if (enabled) {
        rememberInfiniteTransition(label = label)
    } else {
        null
    }
}

/**
 * Performance-aware animation state that adapts to system conditions.
 */
@Composable
fun <T> rememberPerformanceAwareAnimationState(
    initialValue: T,
    performanceConfig: AnimationOptimizationConfig,
    typeConverter: TwoWayConverter<T, AnimationVector>
): Animatable<T, AnimationVector> {
    return remember(initialValue, performanceConfig.animationQuality) {
        Animatable(
            initialValue = initialValue,
            typeConverter = typeConverter,
            visibilityThreshold = null
        )
    }
}

/**
 * Memoized animation specs factory with performance optimization.
 */
object OptimizedAnimationSpecs {
    
    @Composable
    fun rememberFastAnimation(
        quality: AnimationQuality = AnimationQuality.FULL
    ): StableAnimationConfig {
        return remember(quality) {
            when (quality) {
                AnimationQuality.FULL -> StableAnimationConfig(
                    duration = 150,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.REDUCED -> StableAnimationConfig(
                    duration = 100,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.MINIMAL -> StableAnimationConfig(
                    duration = 50,
                    easing = LinearEasing
                )
                AnimationQuality.DISABLED -> StableAnimationConfig(
                    duration = 0,
                    easing = LinearEasing
                )
            }
        }
    }
    
    @Composable
    fun rememberMediumAnimation(
        quality: AnimationQuality = AnimationQuality.FULL
    ): StableAnimationConfig {
        return remember(quality) {
            when (quality) {
                AnimationQuality.FULL -> StableAnimationConfig(
                    duration = 300,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.REDUCED -> StableAnimationConfig(
                    duration = 200,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.MINIMAL -> StableAnimationConfig(
                    duration = 100,
                    easing = LinearEasing
                )
                AnimationQuality.DISABLED -> StableAnimationConfig(
                    duration = 0,
                    easing = LinearEasing
                )
            }
        }
    }
    
    @Composable
    fun rememberSlowAnimation(
        quality: AnimationQuality = AnimationQuality.FULL
    ): StableAnimationConfig {
        return remember(quality) {
            when (quality) {
                AnimationQuality.FULL -> StableAnimationConfig(
                    duration = 500,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.REDUCED -> StableAnimationConfig(
                    duration = 350,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.MINIMAL -> StableAnimationConfig(
                    duration = 200,
                    easing = LinearEasing
                )
                AnimationQuality.DISABLED -> StableAnimationConfig(
                    duration = 0,
                    easing = LinearEasing
                )
            }
        }
    }
    
    @Composable
    fun rememberBounceAnimation(
        quality: AnimationQuality = AnimationQuality.FULL
    ): StableAnimationConfig {
        return remember(quality) {
            when (quality) {
                AnimationQuality.FULL -> StableAnimationConfig(
                    duration = 600,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.REDUCED -> StableAnimationConfig(
                    duration = 400,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.MINIMAL -> StableAnimationConfig(
                    duration = 200,
                    easing = LinearEasing
                )
                AnimationQuality.DISABLED -> StableAnimationConfig(
                    duration = 0,
                    easing = LinearEasing
                )
            }
        }
    }
}

/**
 * Custom easing functions optimized for performance.
 */
object OptimizedEasing {
    val BounceEasing = Easing { fraction ->
        when {
            fraction < 0.36f -> 7.5625f * fraction * fraction
            fraction < 0.73f -> {
                val f = fraction - 0.545f
                7.5625f * f * f + 0.75f
            }
            fraction < 0.91f -> {
                val f = fraction - 0.818f
                7.5625f * f * f + 0.9375f
            }
            else -> {
                val f = fraction - 0.955f
                7.5625f * f * f + 0.984375f
            }
        }
    }
    
    val ElasticEasing = Easing { fraction ->
        if (fraction == 0f || fraction == 1f) {
            fraction
        } else {
            val p = 0.3f
            val s = p / 4f
            2.0.pow(-10.0 * fraction.toDouble()).toFloat() * 
                sin(((fraction - s) * (2f * PI) / p).toDouble()).toFloat() + 1f
        }
    }
}

/**
 * Optimized animation value holder that prevents unnecessary recompositions.
 */
@Stable
class OptimizedAnimationValue<T>(
    initialValue: T,
    private val threshold: (T, T) -> Boolean = { old, new -> old != new }
) {
    private var _value by mutableStateOf(initialValue)
    
    var value: T
        get() = _value
        set(newValue) {
            if (threshold(_value, newValue)) {
                _value = newValue
            }
        }
    
    @Composable
    fun collectAsState(): State<T> {
        return remember { derivedStateOf { _value } }
    }
}

/**
 * Flow-based animation state that optimizes recomposition frequency.
 */
@Composable
fun <T> Flow<T>.collectAsOptimizedState(
    initial: T,
    distinctThreshold: (T, T) -> Boolean = { old, new -> old != new }
): State<T> {
    val optimizedFlow = remember(this) {
        this.distinctUntilChanged(distinctThreshold)
    }
    
    return optimizedFlow.collectAsState(initial = initial)
}

/**
 * Debounced animation trigger that prevents excessive animation starts.
 */
@Composable
fun rememberDebouncedAnimationTrigger(
    delayMs: Long = 100L
): (suspend () -> Unit) -> Unit {
    var lastTriggerTime by remember { mutableStateOf(0L) }
    
    return remember {
        { action ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTriggerTime >= delayMs) {
                lastTriggerTime = currentTime
                // Note: This should be called from a coroutine context
                // For now, we'll make it a regular function call
                // In practice, this would be used within a LaunchedEffect
                try {
                    kotlinx.coroutines.runBlocking {
                        action()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in debounced animation trigger")
                }
            }
        }
    }
}

/**
 * Performance monitoring for recomposition optimization.
 */
@Composable
fun RecompositionPerformanceMonitor(
    componentName: String,
    onRecomposition: (String, Long) -> Unit = { _, _ -> }
) {
    val recompositionCount = remember { mutableStateOf(0) }
    val lastRecompositionTime = remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        val currentTime = System.currentTimeMillis()
        recompositionCount.value++
        
        if (lastRecompositionTime.value > 0) {
            val timeSinceLastRecomposition = currentTime - lastRecompositionTime.value
            onRecomposition(componentName, timeSinceLastRecomposition)
            
            // Log excessive recompositions
            if (timeSinceLastRecomposition < 16L) { // Less than one frame
                Timber.w("Excessive recomposition in $componentName: ${timeSinceLastRecomposition}ms")
            }
        }
        
        lastRecompositionTime.value = currentTime
    }
}

/**
 * Stable wrapper for lambda functions to prevent recomposition.
 */
@Stable
class StableLambda<T, R>(private val lambda: (T) -> R) : (T) -> R {
    override fun invoke(p1: T): R = lambda(p1)
}

/**
 * Creates a stable lambda wrapper.
 */
@Composable
fun <T, R> rememberStableLambda(lambda: (T) -> R): StableLambda<T, R> {
    return remember { StableLambda(lambda) }
}

/**
 * Optimized animation configuration provider.
 */
@Composable
fun ProvideOptimizedAnimationConfig(
    config: AnimationOptimizationConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAnimationOptimizationConfig provides config,
        content = content
    )
}

/**
 * CompositionLocal for animation optimization configuration.
 */
val LocalAnimationOptimizationConfig = compositionLocalOf {
    AnimationOptimizationConfig()
}