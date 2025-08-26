package com.locationsharing.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * FFinder Animation Specifications
 * Defines consistent timing, easing, and motion design for the app
 */
object FFinderAnimations {
    // Animation Durations
    object Duration {
        val Instant = 0.milliseconds
        val Quick = 150.milliseconds
        val Standard = 300.milliseconds
        val Emphasized = 500.milliseconds
        val Extended = 800.milliseconds
        val Breathing = 2000.milliseconds
    }

    // Custom Easing Curves for FFinder Brand
    object Easing {
        // Standard Material Design easings
        val Standard = FastOutSlowInEasing
        val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val Decelerated = LinearOutSlowInEasing
        val Accelerated = FastOutLinearInEasing
        val Linear = LinearEasing

        // FFinder custom easings for brand personality
        val FFinderBounce = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
        val FFinderSmooth = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        val FFinderGentle = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
        val FFinderSharp = CubicBezierEasing(0.55f, 0.085f, 0.68f, 0.53f)
    }

    // Spring Configurations
    object Springs {
        val Gentle =
            spring<Float>(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            )

        val Standard =
            spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )

        val Bouncy =
            spring<Float>(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh,
            )

        val Snappy =
            spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh,
            )
    }

    // Micro-interaction Animations
    object MicroInteractions {
        // Button press feedback
        fun buttonPress() =
            tween<Float>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderSharp,
            )

        // Button release feedback
        fun buttonRelease() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderBounce,
            )

        // Hover state animation
        fun hover() =
            tween<Float>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderSmooth,
            )

        // Focus state animation
        fun focus() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderGentle,
            )

        // Ripple effect timing
        fun ripple() =
            tween<Float>(
                durationMillis = Duration.Emphasized.inWholeMilliseconds.toInt(),
                easing = Easing.Decelerated,
            )

        // Color transition animation
        fun colorTransition() =
            tween<androidx.compose.ui.graphics.Color>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderSmooth,
            )
    }

    // Screen Transition Animations
    object Transitions {
        // Screen enter animation
        fun screenEnter() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.Decelerated,
            )

        // Screen exit animation
        fun screenExit() =
            tween<Float>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.Accelerated,
            )

        // Modal enter animation
        fun modalEnter() =
            tween<Float>(
                durationMillis = Duration.Emphasized.inWholeMilliseconds.toInt(),
                easing = Easing.Decelerated,
            )

        // Modal exit animation
        fun modalExit() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.Accelerated,
            )

        // Shared element transition
        fun sharedElement() =
            tween<Float>(
                durationMillis = Duration.Emphasized.inWholeMilliseconds.toInt(),
                easing = Easing.Emphasized,
            )
    }

    // Loading State Animations
    object Loading {
        // Spinner rotation
        fun spinner() =
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = 1000,
                        easing = Easing.Linear,
                    ),
                repeatMode = RepeatMode.Restart,
            )

        // Pulse animation for active states
        fun pulse() =
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = Duration.Breathing.inWholeMilliseconds.toInt(),
                        easing = Easing.FFinderGentle,
                    ),
                repeatMode = RepeatMode.Reverse,
            )

        // Breathing animation for location sharing
        fun breathing() =
            infiniteRepeatable<Float>(
                animation =
                    keyframes {
                        durationMillis = Duration.Breathing.inWholeMilliseconds.toInt()
                        0.0f at 0 with Easing.FFinderSmooth
                        1.0f at (Duration.Breathing.inWholeMilliseconds / 2).toInt() with Easing.FFinderSmooth
                        0.0f at Duration.Breathing.inWholeMilliseconds.toInt() with Easing.FFinderSmooth
                    },
                repeatMode = RepeatMode.Restart,
            )

        // Shimmer effect for loading content
        fun shimmer() =
            infiniteRepeatable<Float>(
                animation =
                    tween(
                        durationMillis = 1200,
                        easing = Easing.Linear,
                    ),
                repeatMode = RepeatMode.Restart,
            )
    }

    // Map Animations
    object Map {
        // Marker appearance
        fun markerAppear() = Springs.Bouncy

        // Marker disappearance
        fun markerDisappear() =
            tween<Float>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.Accelerated,
            )

        // Marker movement
        fun markerMove() =
            tween<Float>(
                durationMillis = Duration.Extended.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderSmooth,
            )

        // Camera movement
        fun cameraMove() =
            tween<Float>(
                durationMillis = Duration.Emphasized.inWholeMilliseconds.toInt(),
                easing = Easing.Emphasized,
            )

        // Cluster formation
        fun clusterForm() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderGentle,
            )

        // Cluster break apart
        fun clusterBreak() = Springs.Standard
    }

    // Onboarding Animations
    object Onboarding {
        // Welcome screen logo animation
        fun logoReveal() = Springs.Bouncy

        // Step transition
        fun stepTransition() =
            tween<Float>(
                durationMillis = Duration.Emphasized.inWholeMilliseconds.toInt(),
                easing = Easing.Emphasized,
            )

        // Permission explanation illustration
        fun illustrationReveal() =
            tween<Float>(
                durationMillis = Duration.Extended.inWholeMilliseconds.toInt(),
                easing = Easing.Decelerated,
            )

        // Completion celebration
        fun celebration() = Springs.Bouncy
    }

    // Error State Animations
    object Error {
        // Error shake animation
        fun shake() =
            keyframes<Float> {
                durationMillis = 600
                0f at 0
                -10f at 100 with Easing.FFinderSharp
                10f at 200 with Easing.FFinderSharp
                -8f at 300 with Easing.FFinderSharp
                8f at 400 with Easing.FFinderSharp
                -4f at 500 with Easing.FFinderSharp
                0f at 600 with Easing.FFinderSmooth
            }

        // Error fade in
        fun errorAppear() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.Decelerated,
            )

        // Recovery animation
        fun recovery() = Springs.Gentle
    }

    // Accessibility Animations
    object Accessibility {
        // Reduced motion alternative
        fun reducedMotion() =
            tween<Float>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.Linear,
            )

        // High contrast state change
        fun highContrastTransition() =
            tween<Float>(
                durationMillis = Duration.Standard.inWholeMilliseconds.toInt(),
                easing = Easing.Standard,
            )

        // Focus indicator animation
        fun focusIndicator() =
            tween<Float>(
                durationMillis = Duration.Quick.inWholeMilliseconds.toInt(),
                easing = Easing.FFinderGentle,
            )
    }
}

/**
 * Animation state management for consistent timing
 */
enum class AnimationState {
    IDLE,
    ANIMATING,
    COMPLETED,
    CANCELLED,
}

/**
 * Animation performance monitoring
 */
data class AnimationPerformanceMetrics(
    val frameRate: Double,
    val droppedFrames: Int,
    val averageFrameTime: Duration,
    val memoryUsage: Long,
)
