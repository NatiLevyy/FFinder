package com.locationsharing.app.ui.components.animations

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Reusable Lottie animation component for displaying JSON animations.
 * 
 * This component provides a standardized way to display Lottie animations
 * throughout the app with consistent behavior and performance optimizations.
 * 
 * @param resId Raw resource ID of the Lottie JSON file
 * @param modifier Modifier for styling and positioning
 * @param iterations Number of iterations (LottieConstants.IterateForever for infinite loop)
 * @param speed Animation playback speed (1f = normal speed, 0.5f = half speed, etc.)
 * @param isPlaying Whether the animation should be playing
 * @param restartOnPlay Whether to restart the animation when isPlaying becomes true
 */
@Composable
fun LottieAsset(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1f,
    isPlaying: Boolean = true,
    restartOnPlay: Boolean = true
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        isPlaying = isPlaying,
        restartOnPlay = restartOnPlay
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}