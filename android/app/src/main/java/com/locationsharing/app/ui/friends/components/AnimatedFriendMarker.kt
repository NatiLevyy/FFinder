package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay

/**
 * Enhanced animated friend marker for map display
 * Features: bounce-in animation, pulsing online indicator, selection state, shadow effects,
 * movement trails, branded animations, and accessibility support
 */
@Composable
fun AnimatedFriendMarker(
    friend: Friend,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAppearAnimation: Boolean = false,
    showMovementTrail: Boolean = false
) {
    val context = LocalContext.current
    
    // Appearance animation state
    var hasAppeared by remember { mutableStateOf(!showAppearAnimation) }
    
    LaunchedEffect(showAppearAnimation) {
        if (showAppearAnimation && !hasAppeared) {
            delay(100) // Slight delay for staggered appearance
            hasAppeared = true
        }
    }
    
    // Core animation states
    val markerScale by animateFloatAsState(
        targetValue = if (isSelected) 1.3f else 1.0f,
        animationSpec = FFinderAnimations.Springs.Bouncy,
        label = "marker_scale"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (isSelected) 16f else 8f,
        animationSpec = FFinderAnimations.MicroInteractions.hover(),
        label = "shadow_elevation"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.White
        },
        animationSpec = tween<Color>(300),
        label = "border_color"
    )
    
    // Enhanced infinite animations
    val infiniteTransition = rememberInfiniteTransition(label = "marker_infinite_transition")
    
    // Pulsing ring animation for online friends
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = if (friend.isOnline()) {
            FFinderAnimations.Loading.breathing()
        } else {
            infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing))
        },
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = if (friend.isOnline()) {
            FFinderAnimations.Loading.breathing()
        } else {
            infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing))
        },
        label = "pulse_alpha"
    )
    
    // Rotating ring animation for selected state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = if (isSelected) {
            infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing))
        },
        label = "rotation_angle"
    )
    
    // Movement indicator animation
    val movingPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = if (friend.isMoving() && friend.isOnline()) {
            FFinderAnimations.Loading.pulse()
        } else {
            infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing))
        },
        label = "moving_pulse"
    )
    
    AnimatedVisibility(
        visible = hasAppeared,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = FFinderAnimations.Springs.Bouncy
        ) + fadeIn(
            animationSpec = FFinderAnimations.Transitions.screenEnter()
        ) + slideInVertically(
            initialOffsetY = { -it * 2 }, // Fly in from above
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        exit = scaleOut(
            targetScale = 0.3f,
            animationSpec = FFinderAnimations.Map.markerDisappear()
        ) + fadeOut(
            animationSpec = FFinderAnimations.Transitions.screenExit()
        ) + slideOutVertically(
            targetOffsetY = { it * 2 }, // Fly out downward
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        Box(
            modifier = modifier
                .size(64.dp)
                .scale(markerScale)
                .clickable { onClick() }
                .semantics {
                    contentDescription = "${friend.name} is ${if (friend.isOnline()) "online" else "offline"}" +
                            if (friend.isMoving()) " and moving" else ""
                },
            contentAlignment = Alignment.Center
        ) {
            // Movement trail effect
            if (showMovementTrail && friend.isMoving() && friend.isOnline()) {
                MovementTrail(
                    color = friend.getDisplayColor(),
                    modifier = Modifier.size(80.dp)
                )
            }
            
            // Multi-layered pulsing rings for online friends
            if (friend.isOnline()) {
                // Outer pulse ring
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha * 0.6f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.parseColor(friend.profileColor)).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Inner pulse ring
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale * 0.8f)
                        .alpha(pulseAlpha)
                        .background(
                            color = Color(android.graphics.Color.parseColor(friend.profileColor))
                                .copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
            
            // Selection ring with rotation
            if (isSelected) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(rotationAngle)
                ) {
                    drawSelectionRing(
                        color = primaryColor,
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            
            // Enhanced drop shadow with blur effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = 3.dp)
                    .shadow(
                        elevation = shadowElevation.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .background(
                        color = Color.Transparent,
                        shape = CircleShape
                    )
            )
            
            // Main avatar container with gradient border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                borderColor,
                                borderColor.copy(alpha = 0.7f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Friend avatar with crossfade animation
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null, // Handled by parent semantics
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Enhanced online status indicator with glow
            if (friend.isOnline()) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = 18.dp, y = (-18).dp)
                ) {
                    // Glow effect
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50).copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Main indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.Center)
                            .background(
                                color = Color(0xFF4CAF50),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }
            
            // Enhanced moving indicator with trail effect
            if (friend.isMoving() && friend.isOnline()) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(movingPulse)
                        .offset(x = (-18).dp, y = 18.dp)
                ) {
                    // Glow effect for movement
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF9800).copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Movement indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.Center)
                            .background(
                                color = Color(0xFFFF9800),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Movement trail effect for moving friends
 */
@Composable
private fun MovementTrail(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trail_transition")
    
    val trailRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trail_rotation"
    )
    
    val trailAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = FFinderAnimations.Loading.breathing(),
        label = "trail_alpha"
    )
    
    Canvas(
        modifier = modifier.rotate(trailRotation)
    ) {
        drawMovementTrail(
            color = color.copy(alpha = trailAlpha),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Draw selection ring with dashed pattern
 */
private fun DrawScope.drawSelectionRing(
    color: Color,
    strokeWidth: Float
) {
    val radius = size.minDimension / 2 - strokeWidth / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // Draw dashed circle
    val dashLength = 10f
    val gapLength = 8f
    val circumference = 2 * Math.PI * radius
    val totalDashAndGap = dashLength + gapLength
    val numberOfDashes = (circumference / totalDashAndGap).toInt()
    
    for (i in 0 until numberOfDashes) {
        val startAngle = (i * totalDashAndGap / radius * 180 / Math.PI).toFloat()
        val sweepAngle = (dashLength / radius * 180 / Math.PI).toFloat()
        
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }
}

/**
 * Draw movement trail effect
 */
private fun DrawScope.drawMovementTrail(
    color: Color,
    strokeWidth: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - strokeWidth
    
    // Draw multiple concentric circles with decreasing opacity
    for (i in 1..3) {
        val currentRadius = radius * (0.3f + i * 0.2f)
        val currentAlpha = color.alpha * (0.8f - i * 0.2f)
        
        drawCircle(
            color = color.copy(alpha = currentAlpha),
            radius = currentRadius,
            center = center,
            style = Stroke(width = strokeWidth * (1.5f - i * 0.3f))
        )
    }
}

/**
 * Simplified friend marker for clustering or distant view
 */
@Composable
fun SimpleFriendMarker(
    friend: Friend,
    size: Int = 32,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAnimation: Boolean = true
) {
    val context = LocalContext.current
    
    val markerScale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = if (showAnimation) {
            FFinderAnimations.Springs.Standard
        } else {
            tween(durationMillis = 1000, easing = LinearEasing)
        },
        label = "simple_marker_scale"
    )
    
    // Subtle pulse for online friends
    val infiniteTransition = rememberInfiniteTransition(label = "simple_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = if (friend.isOnline() && showAnimation) {
            FFinderAnimations.Loading.pulse()
        } else {
            infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing))
        },
        label = "simple_pulse_scale"
    )
    
    Box(
        modifier = modifier
            .size(size.dp)
            .scale(markerScale)
            .clickable { onClick() }
            .semantics {
                contentDescription = "${friend.name} - ${if (friend.isOnline()) "online" else "offline"}"
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle glow for online friends
        if (friend.isOnline() && showAnimation) {
            Box(
                modifier = Modifier
                    .size((size + 4).dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                friend.getDisplayColor().copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        // Main marker
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            friend.getDisplayColor(),
                            friend.getDisplayColor().copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = Color.White,
                    shape = CircleShape
                )
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(friend.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null, // Handled by parent semantics
                modifier = Modifier
                    .size((size - 4).dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        // Enhanced online indicator
        if (friend.isOnline()) {
            Box(
                modifier = Modifier
                    .size((size / 4).dp)
                    .offset(x = (size / 3).dp, y = (-size / 3).dp)
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size((size / 4).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Main indicator
                Box(
                    modifier = Modifier
                        .size((size / 5).dp)
                        .align(Alignment.Center)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
