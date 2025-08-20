package com.locationsharing.app.ui.animation

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import timber.log.Timber

/**
 * Reduced motion alternatives for accessibility compliance.
 * 
 * This file implements Task 13 requirements for reduced motion support:
 * - Respect system animation settings
 * - Provide non-animated alternatives
 * - Maintain functionality without motion
 * - Accessibility-compliant state changes
 * - Screen reader friendly implementations
 */

/**
 * Accessibility configuration for animations.
 */
@Stable
data class AccessibilityAnimationConfig(
    val respectReducedMotion: Boolean = true,
    val useAlternativeIndicators: Boolean = true,
    val provideFeedback: Boolean = true,
    val maintainTiming: Boolean = false
)

/**
 * Checks if reduced motion is enabled in system settings.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    
    return remember {
        try {
            val animationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            animationScale == 0.0f
        } catch (e: Exception) {
            Timber.w(e, "Failed to check animation scale setting")
            false
        }
    }
}

/**
 * Accessibility-aware animation wrapper that provides reduced motion alternatives.
 */
@Composable
fun AccessibleAnimation(
    enabled: Boolean = true,
    config: AccessibilityAnimationConfig = AccessibilityAnimationConfig(),
    reducedMotionContent: @Composable () -> Unit,
    animatedContent: @Composable () -> Unit
) {
    val isReducedMotion = isReducedMotionEnabled()
    
    if (enabled && !isReducedMotion) {
        animatedContent()
    } else {
        reducedMotionContent()
    }
}

/**
 * Accessible fade transition that respects reduced motion settings.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccessibleFadeTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val isReducedMotion = isReducedMotionEnabled()
    
    if (isReducedMotion) {
        // Instant visibility change without animation
        if (visible) {
            Box(
                modifier = modifier.semantics {
                    stateDescription = if (visible) "Visible" else "Hidden"
                }
            ) {
                // For reduced motion, use AnimatedVisibility with instant transition
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                    label = label,
                    content = content
                )
            }
        }
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            label = label,
            content = content
        )
    }
}

/**
 * Accessible slide transition with reduced motion alternative.
 */
@Composable
fun AccessibleSlideTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    slideDirection: SlideDirection = SlideDirection.Up,
    label: String = "",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val isReducedMotion = isReducedMotionEnabled()
    
    if (isReducedMotion) {
        // Use fade instead of slide for reduced motion
        AccessibleFadeTransition(
            visible = visible,
            modifier = modifier,
            label = label,
            content = content
        )
    } else {
        val (enter, exit) = when (slideDirection) {
            SlideDirection.Up -> slideInVertically { it } to slideOutVertically { it }
            SlideDirection.Down -> slideInVertically { -it } to slideOutVertically { -it }
            SlideDirection.Left -> slideInHorizontally { it } to slideOutHorizontally { it }
            SlideDirection.Right -> slideInHorizontally { -it } to slideOutHorizontally { -it }
        }
        
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = enter + fadeIn(),
            exit = exit + fadeOut(),
            label = label,
            content = content
        )
    }
}

/**
 * Accessible scale transition with reduced motion alternative.
 */
@Composable
fun AccessibleScaleTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val isReducedMotion = isReducedMotionEnabled()
    
    if (isReducedMotion) {
        // Use simple fade for reduced motion
        AccessibleFadeTransition(
            visible = visible,
            modifier = modifier,
            label = label,
            content = content
        )
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(),
            label = label,
            content = content
        )
    }
}

/**
 * Accessible animated float value that respects reduced motion.
 */
@Composable
fun animateAccessibleFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = tween(),
    label: String = "",
    finishedListener: ((Float) -> Unit)? = null
): State<Float> {
    val isReducedMotion = isReducedMotionEnabled()
    
    return if (isReducedMotion) {
        // Return target value immediately without animation
        remember(targetValue) {
            mutableStateOf(targetValue).apply {
                finishedListener?.invoke(targetValue)
            }
        }
    } else {
        animateFloatAsState(
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = label,
            finishedListener = finishedListener
        )
    }
}

/**
 * Accessible animated color value that respects reduced motion.
 */
@Composable
fun animateAccessibleColorAsState(
    targetValue: Color,
    animationSpec: AnimationSpec<Color> = tween(),
    label: String = "",
    finishedListener: ((Color) -> Unit)? = null
): State<Color> {
    val isReducedMotion = isReducedMotionEnabled()
    
    return if (isReducedMotion) {
        // Return target color immediately without animation
        remember(targetValue) {
            mutableStateOf(targetValue).apply {
                finishedListener?.invoke(targetValue)
            }
        }
    } else {
        animateColorAsState(
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = label,
            finishedListener = finishedListener
        )
    }
}

/**
 * Accessible animated Dp value that respects reduced motion.
 */
@Composable
fun animateAccessibleDpAsState(
    targetValue: Dp,
    animationSpec: AnimationSpec<Dp> = tween(),
    label: String = "",
    finishedListener: ((Dp) -> Unit)? = null
): State<Dp> {
    val isReducedMotion = isReducedMotionEnabled()
    
    return if (isReducedMotion) {
        // Return target value immediately without animation
        remember(targetValue) {
            mutableStateOf(targetValue).apply {
                finishedListener?.invoke(targetValue)
            }
        }
    } else {
        animateDpAsState(
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = label,
            finishedListener = finishedListener
        )
    }
}

/**
 * Accessible infinite transition that can be disabled for reduced motion.
 */
@Composable
fun rememberAccessibleInfiniteTransition(
    label: String = ""
): InfiniteTransition? {
    val isReducedMotion = isReducedMotionEnabled()
    
    return if (isReducedMotion) {
        null // No infinite animations for reduced motion
    } else {
        rememberInfiniteTransition(label = label)
    }
}

/**
 * Alternative visual indicators for reduced motion scenarios.
 */
object ReducedMotionIndicators {
    
    /**
     * Static indicator for loading states instead of spinning animation.
     */
    @Composable
    fun LoadingIndicator(
        isLoading: Boolean,
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colorScheme.primary
    ) {
        val isReducedMotion = isReducedMotionEnabled()
        
        if (isReducedMotion) {
            // Static dots indicator
            if (isLoading) {
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                    }
                }
            }
        } else {
            // Animated circular progress indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = modifier,
                    color = color
                )
            }
        }
    }
    
    /**
     * State change indicator that uses color/text instead of animation.
     */
    @Composable
    fun StateChangeIndicator(
        state: String,
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colorScheme.primary
    ) {
        val isReducedMotion = isReducedMotionEnabled()
        
        if (isReducedMotion) {
            // Text-based state indicator
            Text(
                text = state,
                modifier = modifier.semantics {
                    stateDescription = "Current state: $state"
                },
                color = color,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            // Could include subtle animations here
            Text(
                text = state,
                modifier = modifier,
                color = color,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    /**
     * Progress indicator that uses static bars instead of animated progress.
     */
    @Composable
    fun ProgressIndicator(
        progress: Float,
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colorScheme.primary
    ) {
        val isReducedMotion = isReducedMotionEnabled()
        
        if (isReducedMotion) {
            // Static progress bar with immediate updates
            LinearProgressIndicator(
                progress = progress,
                modifier = modifier,
                color = color
            )
        } else {
            // Animated progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 300)
            )
            
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = modifier,
                color = color
            )
        }
    }
}

/**
 * Slide direction enumeration for accessible transitions.
 */
enum class SlideDirection {
    Up, Down, Left, Right
}

/**
 * Accessibility-aware animation configuration provider.
 */
@Composable
fun ProvideAccessibleAnimationConfig(
    config: AccessibilityAnimationConfig = AccessibilityAnimationConfig(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAccessibilityAnimationConfig provides config,
        content = content
    )
}

/**
 * CompositionLocal for accessibility animation configuration.
 */
val LocalAccessibilityAnimationConfig = compositionLocalOf {
    AccessibilityAnimationConfig()
}

/**
 * Utility function to create accessible animation specs.
 */
fun <T> createAccessibleAnimationSpec(
    normalSpec: AnimationSpec<T>,
    reducedMotionSpec: AnimationSpec<T>? = null,
    context: Context
): AnimationSpec<T> {
    val isReducedMotion = try {
        val animationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        animationScale == 0.0f
    } catch (e: Exception) {
        false
    }
    
    return if (isReducedMotion) {
        reducedMotionSpec ?: snap() // Instant transition
    } else {
        normalSpec
    }
}

/**
 * Extension function to make any AnimationSpec accessible.
 */
fun <T> AnimationSpec<T>.makeAccessible(context: Context): AnimationSpec<T> {
    return createAccessibleAnimationSpec(
        normalSpec = this,
        reducedMotionSpec = snap(),
        context = context
    )
}

/**
 * Accessible crossfade that respects reduced motion settings.
 */
@Composable
fun <T> AccessibleCrossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    label: String = "AccessibleCrossfade",
    content: @Composable (T) -> Unit
) {
    val isReducedMotion = isReducedMotionEnabled()
    
    if (isReducedMotion) {
        // Instant state change without crossfade
        content(targetState)
    } else {
        Crossfade(
            targetState = targetState,
            modifier = modifier,
            animationSpec = animationSpec,
            label = label,
            content = content
        )
    }
}

/**
 * Accessible animated content that provides alternatives for reduced motion.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AccessibleAnimatedContent(
    targetState: Any?,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<Any?>.() -> ContentTransform = {
        fadeIn() with fadeOut()
    },
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "AccessibleAnimatedContent",
    content: @Composable AnimatedContentScope.(targetState: Any?) -> Unit
) {
    val isReducedMotion = isReducedMotionEnabled()
    
    if (isReducedMotion) {
        // Static content without transitions
        Box(
            modifier = modifier,
            contentAlignment = contentAlignment
        ) {
            // For reduced motion, use AnimatedContent with instant transition
                AnimatedContent(
                    targetState = targetState,
                    modifier = Modifier,
                    transitionSpec = { ContentTransform(EnterTransition.None, ExitTransition.None) },
                    contentAlignment = Alignment.TopStart,
                    label = label,
                    content = content
                )
        }
    } else {
        AnimatedContent(
            targetState = targetState,
            modifier = modifier,
            transitionSpec = transitionSpec,
            contentAlignment = contentAlignment,
            label = label,
            content = content
        )
    }
}