package com.locationsharing.app.ui.map.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Current location marker with pulse animation
 * Implements requirements 2.2, 2.6, 7.4, 7.5 for current location display
 */
@Composable
fun CurrentLocationMarker(
    position: LatLng,
    isVisible: Boolean = true,
    accuracy: Float? = null,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val markerState = MarkerState(position = position)
    
    MarkerComposable(
        state = markerState,
        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
    ) {
        CurrentLocationMarkerContent(
            accuracy = accuracy,
            modifier = modifier
        )
    }
}

/**
 * Content of the current location marker with pin bounce animation
 * Implements requirement 8.1: Pin bounce animation on MapScreen (scale/translation loop)
 */
@Composable
private fun CurrentLocationMarkerContent(
    accuracy: Float? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Pin bounce animation using MapMicroAnimations with accessibility support
    val bounceScale = com.locationsharing.app.ui.map.animations.MapMicroAnimations.LocationMarkerPulse(
        isReducedMotion = false // TODO: Get from accessibility settings
    )
    
    // Translation bounce effect
    val infiniteTransition = rememberInfiniteTransition(label = "pin_bounce_translation")
    val bounceTranslationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000 // Every 3 seconds as specified
                0f at 0 with com.locationsharing.app.ui.theme.FFinderAnimations.Easing.FFinderSmooth
                -8f at 300 with com.locationsharing.app.ui.theme.FFinderAnimations.Easing.FFinderGentle
                0f at 600 with com.locationsharing.app.ui.theme.FFinderAnimations.Easing.FFinderSmooth
                0f at 3000 // Hold for remaining time
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce_translation"
    )
    
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Accuracy circle (if available)
        accuracy?.let { acc ->
            if (acc > 0) {
                AccuracyCircle(
                    accuracy = acc,
                    color = primaryColor.copy(alpha = 0.2f)
                )
            }
        }
        
        // Pin icon with bounce animation
        Icon(
            painter = painterResource(id = R.drawable.ic_pin_finder),
            contentDescription = "Your current location",
            modifier = Modifier
                .size(32.dp)
                .scale(bounceScale)
                .offset(y = bounceTranslationY.dp),
            tint = primaryColor
        )
    }
}

/**
 * Accuracy circle to show location precision
 */
@Composable
private fun AccuracyCircle(
    accuracy: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Convert accuracy (in meters) to a reasonable pixel size
    // This is a simplified conversion - in a real app you'd use map projection
    val radiusDp = with(density) {
        (accuracy.coerceIn(5f, 100f) * 2).dp
    }
    
    Canvas(
        modifier = modifier.size(radiusDp * 2)
    ) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2,
            style = Stroke(width = 2.dp.toPx())
        )
        
        drawCircle(
            color = color.copy(alpha = 0.1f),
            radius = size.minDimension / 2
        )
    }
}

/**
 * Static current location marker (without animation) for performance-critical scenarios
 */
@Composable
fun StaticCurrentLocationMarker(
    position: LatLng,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val markerState = MarkerState(position = position)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    MarkerComposable(
        state = markerState,
        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
    ) {
        Box(
            modifier = modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(20.dp)
            ) {
                // Outer ring
                drawCircle(
                    color = surfaceColor,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Inner dot
                drawCircle(
                    color = primaryColor,
                    radius = (size.minDimension / 2) - 2.dp.toPx()
                )
                
                // Center highlight
                drawCircle(
                    color = onPrimaryColor,
                    radius = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * Preview for current location marker
 */
@Preview(showBackground = true)
@Composable
fun CurrentLocationMarkerPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CurrentLocationMarkerContent(accuracy = 15f)
        }
    }
}

/**
 * Preview for static current location marker
 */
@Preview(showBackground = true)
@Composable
fun StaticCurrentLocationMarkerPreview() {
    FFinderTheme {
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
        val surfaceColor = MaterialTheme.colorScheme.surface
        
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(20.dp)
            ) {
                
                // Outer ring
                drawCircle(
                    color = surfaceColor,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Inner dot
                drawCircle(
                    color = primaryColor,
                    radius = (size.minDimension / 2) - 2.dp.toPx()
                )
                
                // Center highlight
                drawCircle(
                    color = onPrimaryColor,
                    radius = 2.dp.toPx()
                )
            }
        }
    }
}