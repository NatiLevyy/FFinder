package com.locationsharing.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Quick Share FAB component for the MapScreen.
 * Uses ic_pin_finder vector (white pin) tinted with onPrimary, background = primary.
 * Size 56 dp, ripple effect tinted secondary, haptic feedback.
 */
@Composable
fun QuickShareFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    FloatingActionButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .size(56.dp)
            .semantics {
                contentDescription = "Quick Share. Tap to share your location instantly."
                role = Role.Button
            },
        shape = CircleShape,
        containerColor = Color(0xFFB791E0),
        contentColor = Color.Black,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 8.dp
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_pin_finder_vector),
            contentDescription = null, // Content description is on the FAB itself
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuickShareFABPreview() {
    FFinderTheme {
        QuickShareFAB(
            onClick = {}
        )
    }
}