package com.locationsharing.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import com.locationsharing.app.R

/**
 * Reusable AnimatedPin composable that displays the new animated pin icon
 * across all location-related components in the app.
 * 
 * Features:
 * - Uses vector drawable for optimal performance  
 * - Smaller APK footprint than Lottie (≈2-4 KB vs 60 KB)
 * - Consistent branding with Material Design
 * - RTL mirroring support
 * - Customizable tint color
 * 
 * @param modifier Modifier for customization
 * @param tint Color tint to apply to the pin (default: brand purple #6B4F8F)
 * @param animated Whether to play the animation (currently uses static vector)
 */
@Composable
fun AnimatedPin(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF6B4F8F), // Brand purple as default
    animated: Boolean = true
) {
    // Use the brand pin vector (static for now, can be enhanced with animation later)
    val pinVector = ImageVector.vectorResource(R.drawable.ic_pin_path)
    val painter = rememberVectorPainter(pinVector)
    
    Image(
        painter = painter,
        contentDescription = "Location pin",
        colorFilter = if (tint != Color.Unspecified) {
            ColorFilter.tint(tint)
        } else null,
        modifier = modifier
    )
}

/**
 * Static version of the pin for cases where animation is not needed
 * or accessibility preferences require reduced motion.
 */
@Composable
fun StaticPin(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF6B4F8F)
) {
    AnimatedPin(
        modifier = modifier,
        tint = tint,
        animated = false
    )
}