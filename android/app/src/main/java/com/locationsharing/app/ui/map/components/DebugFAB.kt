package com.locationsharing.app.ui.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.R
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.map.haptic.rememberMapHapticFeedbackManager
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Debug FAB component for adding test friends to the map
 * Only visible in debug builds according to requirements 4.1, 4.2, 4.4, 4.5
 */
@Composable
fun DebugFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Only render in debug builds (requirement 4.4)
    if (!BuildConfig.DEBUG) return
    
    val hapticManager = rememberMapHapticFeedbackManager()
    var isPressed by remember { mutableStateOf(false) }
    
    // Scale animation for press feedback (requirement 4.5)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) MapScreenConstants.Animations.FAB_PRESSED_SCALE else MapScreenConstants.Animations.FAB_NORMAL_SCALE,
        animationSpec = tween(durationMillis = MapScreenConstants.Animations.QUICK_DURATION),
        label = "debug_fab_scale"
    )
    
    FloatingActionButton(
        onClick = {
            if (enabled) {
                // Enhanced haptic feedback for secondary FAB action (requirement 4.5)
                hapticManager.performSecondaryFABAction()
                onClick()
            }
        },
        modifier = modifier
            .size(MapScreenConstants.Dimensions.FAB_SIZE)
            .scale(scale)
            .semantics {
                contentDescription = MapScreenConstants.Accessibility.DEBUG_FAB_DESC
                role = Role.Button
                testTag = "debug_fab"
                stateDescription = "Debug mode active"
                if (!enabled) {
                    disabled()
                }
            },
        containerColor = MaterialTheme.colorScheme.secondary, // Purple color (requirement 4.1)
        contentColor = MaterialTheme.colorScheme.onSecondary
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_flask), // Flask icon (requirement 4.1)
            contentDescription = null, // Description is on the FAB
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Preview for DebugFAB component
 * Only shows in debug builds
 */
@Preview(showBackground = true)
@Composable
fun DebugFABPreview() {
    FFinderTheme {
        if (BuildConfig.DEBUG) {
            DebugFAB(
                onClick = {}
            )
        }
    }
}

/**
 * Preview for DebugFAB component in disabled state
 */
@Preview(showBackground = true)
@Composable
fun DebugFABDisabledPreview() {
    FFinderTheme {
        if (BuildConfig.DEBUG) {
            DebugFAB(
                onClick = {},
                enabled = false
            )
        }
    }
}