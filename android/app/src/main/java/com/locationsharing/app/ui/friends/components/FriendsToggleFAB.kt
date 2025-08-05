package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R

/**
 * Friends Toggle FAB component for the Friends Nearby Panel.
 * 
 * Provides a floating action button that toggles between open/closed states
 * with smooth animations and proper accessibility support.
 * 
 * Features:
 * - 56dp visual size with 48dp minimum touch target (handled by Material3 FAB)
 * - Toggle animation between Group and Close icons
 * - Material 3 styling with primaryContainer colors
 * - Comprehensive accessibility descriptions
 * 
 * @param isOpen Whether the friends panel is currently open
 * @param onClick Callback when the FAB is clicked
 * @param modifier Modifier for styling and positioning
 */
@Composable
fun FriendsToggleFAB(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for icon rotation and transition
    val rotationAngle by animateFloatAsState(
        targetValue = if (isOpen) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "FAB rotation animation"
    )
    
    // Select appropriate icon based on state
    val icon: ImageVector = if (isOpen) Icons.Default.Close else Icons.Default.Group
    
    // Content description for accessibility
    val contentDesc = if (isOpen) {
        "Close friends nearby panel"
    } else {
        "Open friends nearby panel"
    }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp) // Visual size as specified
            .semantics {
                contentDescription = contentDesc
                role = Role.Button
            },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Content description is on the FAB itself
            modifier = Modifier
                .size(24.dp)
                .rotate(rotationAngle)
        )
    }
}