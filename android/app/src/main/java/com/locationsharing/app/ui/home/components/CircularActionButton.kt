package com.locationsharing.app.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.ui.components.AnimatedPin
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.ui.theme.FFinderPrimary

/**
 * Circular action button for starting/stopping live sharing
 * 
 * Features:
 * - GPS animation while waiting for location
 * - Smooth scale animation on press
 * - Haptic feedback on interaction
 * - Color change based on sharing state
 * - Full accessibility support
 */
@Suppress("DEPRECATION")
@Composable
fun CircularActionButton(
    isSharing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // Colors based on sharing state
    val bgColor = if (isSharing) FFinderPrimary /* green */ else Color(0xFFB791E0) /* brand purple */
    
    // Scale animation for press feedback
    val scale = remember { Animatable(1f) }
    
    // GPS animation state - animate when not sharing (waiting for GPS fix)
    val isWaitingForGPS = !isSharing
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(128.dp)
            .clip(CircleShape)
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.6f))
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 64.dp),
                role = Role.Button,
                enabled = enabled,
                onClick = {
                    // Haptic feedback
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    // Scale animation
                    onClick()
                }
            )
            .semantics {
                contentDescription = if (isSharing) "Stop Live Sharing" else "Start Live Sharing"
                role = Role.Button
                testTag = "circular_action_button"
                stateDescription = if (isSharing) "Currently sharing location" else "Ready to start sharing"
            }
    ) {
        // Use AnimatedPin for all states
        AnimatedPin(
            modifier = Modifier.size(if (isSharing) 48.dp else 64.dp),
            tint = if (isSharing) Color.White else Color.Black,
            animated = isWaitingForGPS  // Animate when waiting for GPS fix, static when sharing
        )
    }
    
    // Handle click animation
    LaunchedEffect(isSharing) {
        if (isSharing) {
            // Scale down and back up when starting to share
            scale.animateTo(
                targetValue = 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
}

/**
 * Preview variant with different states
 */
@Preview(showBackground = true, name = "Circular Button - Not Sharing")
@Composable
fun CircularActionButtonNotSharingPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularActionButton(
                isSharing = false,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Circular Button - Sharing")
@Composable
fun CircularActionButtonSharingPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularActionButton(
                isSharing = true,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Circular Button - Disabled")
@Composable
fun CircularActionButtonDisabledPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularActionButton(
                isSharing = false,
                onClick = { },
                enabled = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Circular Button - Dark Theme")
@Composable
fun CircularActionButtonDarkPreview() {
    FFinderTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularActionButton(
                isSharing = false,
                onClick = { }
            )
        }
    }
}