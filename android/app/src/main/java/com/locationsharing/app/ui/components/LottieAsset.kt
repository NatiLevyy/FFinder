package com.locationsharing.app.ui.components

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.*

/**
 * Lottie animation helper composable for FFinder app
 * 
 * Usage:
 * - GPS_Animation.json: ShareStatusSheet while acquiring GPS fix (120×120 dp, loop until fix)
 * - Travel_Somewhere.json: Hero backdrop on HomeScreen (match-parent, 0.3 speed)
 * - Location_animation.json: Empty-state in Nearby Friends drawer (160×160 dp, play once)
 */
@Composable
fun LottieAsset(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1f
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}