package com.locationsharing.app.ui.home.components

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * WhatsNewTeaser composable that displays a sliding card animation from bottom
 * showcasing new features with a rocket emoji and descriptive text.
 * 
 * This component implements:
 * - Sliding card animation from bottom using animateIntAsState with EaseOutBack easing
 * - 🚀 emoji and "New: Nearby Friends panel & Quick Share!" text
 * - Card styling with 16dp rounded corners, 4dp elevation, and 95% opacity surface color
 * - Tap handling to open modal dialog
 * - Accessibility support with proper content descriptions
 * 
 * @param onTap Callback invoked when the teaser card is tapped
 * @param modifier Modifier for customizing the component's appearance and behavior
 * @param isVisible Whether the teaser should be visible and animated
 */
@Composable
fun WhatsNewTeaser(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    animationsEnabled: Boolean = true
) {
    val hapticManager = rememberHapticFeedbackManager()
    
    // Check system accessibility preferences
    val shouldAnimate = AccessibilityUtils.shouldEnableAnimations(animationsEnabled)
    
    // Slide-up animation using animateIntAsState with EaseOutBack easing
    val slideOffset by animateIntAsState(
        targetValue = if (isVisible) 0 else 100,
        animationSpec = if (shouldAnimate) {
            tween(
                durationMillis = 800,
                easing = EaseOutBack
            )
        } else {
            tween(0) // No animation when disabled
        },
        label = "whats_new_slide_animation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = slideOffset.dp)
            .clickable { 
                hapticManager.performCardInteraction()
                onTap() 
            }
            .semantics {
                contentDescription = "What's New announcement. Tap to learn about new features: Nearby Friends panel and Quick Share functionality."
                role = Role.Button
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Rocket emoji
            Text(
                text = "🚀",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            // Feature announcement text
            Text(
                text = "New: Nearby Friends panel & Quick Share!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * WhatsNewDialog composable that displays a modal dialog with detailed information
 * about new features when the teaser card is tapped.
 * 
 * @param onDismiss Callback invoked when the dialog should be dismissed
 * @param modifier Modifier for customizing the dialog's appearance
 */
@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚀",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "What's New in FFinder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "We've added exciting new features to make finding and sharing with friends even easier:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Nearby Friends panel feature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "👥",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    Column {
                        Text(
                            text = "Nearby Friends Panel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "See which friends are close by with real-time distance updates and quick actions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Quick Share feature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    Column {
                        Text(
                            text = "Quick Share",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Share your location instantly with one tap, no need to navigate through menus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Got it!",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun WhatsNewTeaserPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WhatsNewTeaser(
                onTap = {},
                isVisible = true
            )
            
            WhatsNewTeaser(
                onTap = {},
                isVisible = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WhatsNewDialogPreview() {
    FFinderTheme {
        WhatsNewDialog(
            onDismiss = {}
        )
    }
}