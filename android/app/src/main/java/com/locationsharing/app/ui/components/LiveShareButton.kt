package com.locationsharing.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.util.Log
import com.locationsharing.app.R
import com.locationsharing.app.ui.components.LottieAsset
import com.locationsharing.app.ui.theme.FFinderTheme
import com.airbnb.lottie.compose.LottieConstants

@Composable
fun LiveShareButton(
    isSharing: Boolean,
    waitingForFix: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // FORCE BRAND PURPLE - Debug logging
    Log.d("ColorProbe", "LiveShareButton FORCE PURPLE - isSharing=$isSharing")
    Log.d("ColorProbe", "LiveShareButton - Lottie animation enlarged successfully")
    
    // FORCE brand purple background regardless of sharing state
    val backgroundColor = Color(0xFFB791E0) // FORCE BRAND PURPLE
    
    // Click animation: scale 0.94 → overshoot 1.02 → settle 1.00
    val scale = remember { Animatable(1f) }
    
    // Content description for accessibility
    val contentDesc = if (isSharing) "Stop sharing location" else "Start live sharing"
    
    Box(
        modifier = modifier
            .size(144.dp)
            .background(Color(0xFFB791E0), CircleShape) // FORCE BRAND PURPLE
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 72.dp),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
            )
            .graphicsLayer { 
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        // Use location_icon.json Lottie animation for Start Sharing button
        LottieAsset(
            resId = R.raw.location_icon,
            modifier = Modifier.size(80.dp),
            iterations = if (waitingForFix || isSharing) LottieConstants.IterateForever else 1,
            speed = if (waitingForFix) 1.2f else 1f
        )
    }
    
    // Handle click animation
    LaunchedEffect(onToggle) {
        // This effect only triggers animation but doesn't call onToggle
    }
    
    // Click animation sequence: 0.94 → 1.02 → 1.00
    LaunchedEffect(isSharing, waitingForFix) {
        scale.animateTo(
            targetValue = 0.94f,
            animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.02f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        scale.animateTo(
            targetValue = 1.00f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LiveShareButtonPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            LiveShareButton(
                isSharing = false,
                waitingForFix = false,
                onToggle = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LiveShareButtonSharingPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            LiveShareButton(
                isSharing = true,
                waitingForFix = false,
                onToggle = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LiveShareButtonWaitingPreview() {
    FFinderTheme {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            LiveShareButton(
                isSharing = false,
                waitingForFix = true,
                onToggle = { }
            )
        }
    }
}