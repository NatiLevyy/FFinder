package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlin.math.roundToInt

/**
 * Enhanced animated bottom sheet card showing friend information and actions
 * Features: swipe-to-dismiss, haptic feedback, delightful micro-interactions
 */
@Composable
fun FriendInfoCard(
    friend: Friend?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onMessageClick: (String) -> Unit,
    onNotifyClick: (String) -> Unit,
    onMoreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Animate card appearance with enhanced spring
    val cardScale by animateFloatAsState(
        targetValue = if (isVisible && friend != null) 1f else 0.8f,
        animationSpec = FFinderAnimations.Springs.Bouncy,
        label = "card_scale"
    )
    
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible && friend != null) 1f else 0f,
        animationSpec = FFinderAnimations.Transitions.modalEnter(),
        label = "card_alpha"
    )
    
    // Drag-to-dismiss threshold
    val dismissThreshold = 150f
    val dragAlpha = 1f - (kotlin.math.abs(dragOffset) / dismissThreshold).coerceIn(0f, 0.7f)
    
    AnimatedVisibility(
        visible = isVisible && friend != null,
        enter = slideInVertically(
            initialOffsetY = { it }
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it }
        ) + fadeOut(),
        modifier = modifier
    ) {
        friend?.let { friendData ->
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
                    .padding(16.dp)
            ) {
                // Enhanced card with drag support
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(cardScale)
                        .alpha(cardAlpha * dragAlpha)
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { 
                                    isDragging = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (kotlin.math.abs(dragOffset) > dismissThreshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDismiss()
                                    }
                                    dragOffset = 0f
                                }
                            ) { _, dragAmount ->
                                dragOffset += dragAmount.y
                            }
                        }
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* Prevent click through */ }
                        .semantics {
                            contentDescription = "Friend info card for ${friendData.name}. Swipe down to dismiss."
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    tonalElevation = 8.dp
                ) {
                    Column {
                        // Drag handle
                        DragHandle(
                            isDragging = isDragging,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // Enhanced header with animations
                            EnhancedFriendInfoHeader(
                                friend = friendData,
                                onDismiss = onDismiss
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Enhanced status information
                            EnhancedFriendStatusInfo(friend = friendData)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Enhanced action buttons with micro-interactions
                            EnhancedFriendActionButtons(
                                friendId = friendData.id,
                                isOnline = friendData.isOnline(),
                                onMessageClick = onMessageClick,
                                onNotifyClick = onNotifyClick,
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Drag handle for bottom sheet
 */
@Composable
private fun DragHandle(
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val handleWidth by animateFloatAsState(
        targetValue = if (isDragging) 60f else 40f,
        animationSpec = FFinderAnimations.Springs.Standard,
        label = "handle_width"
    )
    
    val handleAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.8f else 0.4f,
        animationSpec = FFinderAnimations.MicroInteractions.hover(),
        label = "handle_alpha"
    )
    
    Box(
        modifier = modifier
            .padding(vertical = 12.dp)
            .width(handleWidth.dp)
            .height(4.dp)
            .alpha(handleAlpha)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(2.dp)
            )
    )
}

/**
 * Enhanced header with improved animations and accessibility
 */
@Composable
private fun EnhancedFriendInfoHeader(
    friend: Friend,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Avatar scale animation
    val avatarScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = FFinderAnimations.Springs.Bouncy,
        label = "avatar_scale"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Enhanced avatar with gradient ring and glow
        Box {
            // Glow effect for online friends
            if (friend.isOnline()) {
                val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.6f,
                    animationSpec = FFinderAnimations.Loading.breathing(),
                    label = "glow_alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .alpha(glowAlpha)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    friend.getDisplayColor().copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            // Main avatar container with gradient border
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(avatarScale)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                friend.getDisplayColor(),
                                friend.getDisplayColor().copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null, // Handled by parent semantics
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Enhanced online indicator with pulse
            if (friend.isOnline()) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = FFinderAnimations.Loading.pulse(),
                    label = "pulse_scale"
                )
                
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .scale(pulseScale)
                ) {
                    // Glow background
                    Box(
                        modifier = Modifier
                            .size(20.dp)
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
                            .size(16.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        // Enhanced name and status info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = friend.name,
                transitionSpec = {
                    fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                    fadeOut(FFinderAnimations.Transitions.screenExit())
                },
                label = "name_animation"
            ) { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            AnimatedContent(
                targetState = friend.getStatusText(),
                transitionSpec = {
                    fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                    fadeOut(FFinderAnimations.Transitions.screenExit())
                },
                label = "status_animation"
            ) { statusText ->
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Friend color indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            friend.getDisplayColor(),
                            CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Friend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Enhanced close button with haptic feedback
        IconButton(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss() 
            },
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    CircleShape
                )
                .semantics {
                    contentDescription = "Close friend info card"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Enhanced status information with rich visual indicators
 */
@Composable
private fun EnhancedFriendStatusInfo(friend: Friend) {
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(
                    label = "Status",
                    value = if (friend.isOnline()) "Online" else "Offline",
                    color = if (friend.isOnline()) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                    isAnimated = friend.isOnline()
                )
                
                if (friend.isMoving() && friend.isOnline()) {
                    StatusIndicator(
                        label = "Activity",
                        value = "Moving",
                        color = Color(0xFFFF9800),
                        isAnimated = true
                    )
                }
            }
            
            // Last seen information for offline friends
            if (!friend.isOnline()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Last seen: ${friend.getStatusText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Location accuracy indicator (mock)
            if (friend.isOnline()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "High accuracy location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Animated status indicator component
 */
@Composable
private fun StatusIndicator(
    label: String,
    value: String,
    color: Color,
    isAnimated: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_transition")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = if (isAnimated) {
            FFinderAnimations.Loading.pulse()
        } else {
            infiniteRepeatable(FFinderAnimations.Accessibility.reducedMotion())
        },
        label = "status_alpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(if (isAnimated) animatedAlpha else 1f)
                    .background(color, CircleShape)
                    .shadow(if (isAnimated) 4.dp else 0.dp, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Enhanced action buttons with delightful micro-interactions
 */
@Composable
private fun EnhancedFriendActionButtons(
    friendId: String,
    isOnline: Boolean,
    onMessageClick: (String) -> Unit,
    onNotifyClick: (String) -> Unit,
    onMoreClick: (String) -> Unit
) {
    Column {
        // Primary action button (Message)
        AnimatedActionButton(
            text = "Message",
            icon = Icons.Default.Message,
            onClick = { onMessageClick(friendId) },
            isPrimary = true,
            isEnabled = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Secondary action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedActionButton(
                text = "Notify",
                icon = Icons.Default.Notifications,
                onClick = { onNotifyClick(friendId) },
                isPrimary = false,
                isEnabled = isOnline,
                modifier = Modifier.weight(1f)
            )
            
            AnimatedActionButton(
                text = "More",
                icon = Icons.Default.MoreVert,
                onClick = { onMoreClick(friendId) },
                isPrimary = false,
                isEnabled = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Animated action button with haptic feedback and micro-interactions
 */
@Composable
private fun AnimatedActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    var isPressed by remember { mutableStateOf(false) }
    
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = FFinderAnimations.MicroInteractions.buttonPress(),
        label = "button_scale"
    )
    
    val buttonElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else if (isPrimary) 6f else 3f,
        animationSpec = FFinderAnimations.MicroInteractions.buttonPress(),
        label = "button_elevation"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    if (isPrimary) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier
                .scale(buttonScale)
                .shadow(buttonElevation.dp, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isPressed = true },
                        onDragEnd = { isPressed = false }
                    ) { _, _ -> }
                },
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            interactionSource = interactionSource,
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier
                .scale(buttonScale)
                .shadow(buttonElevation.dp, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isPressed = true },
                        onDragEnd = { isPressed = false }
                    ) { _, _ -> }
                },
            enabled = isEnabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(
                    colors = if (isEnabled) {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }
                )
            ),
            shape = RoundedCornerShape(16.dp),
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}