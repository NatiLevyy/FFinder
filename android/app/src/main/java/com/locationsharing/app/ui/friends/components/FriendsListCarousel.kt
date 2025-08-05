package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced horizontal scrollable carousel of friends with comprehensive animations,
 * keyboard navigation, focus states, and accessibility support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendsListCarousel(
    friends: List<Friend>,
    selectedFriendId: String?,
    onFriendClick: (String) -> Unit,
    onInviteFriendsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    
    var focusedIndex by remember { mutableStateOf(-1) }
    var isKeyboardNavigating by remember { mutableStateOf(false) }
    
    // Staggered entry animation state
    var hasAppeared by remember { mutableStateOf(false) }
    
    LaunchedEffect(friends) {
        if (friends.isNotEmpty() && !hasAppeared) {
            hasAppeared = true
        }
    }
    
    // Auto-scroll to selected friend
    LaunchedEffect(selectedFriendId) {
        selectedFriendId?.let { id ->
            val index = friends.indexOfFirst { it.id == id }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }
    
    AnimatedVisibility(
        visible = friends.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn() + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(150)
        ) + fadeOut() + scaleOut(
            targetScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = "Friends list with ${friends.size} friends, ${friends.count { it.isOnline() }} online"
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Enhanced header with animations
                EnhancedCarouselHeader(
                    friendsCount = friends.size,
                    onlineCount = friends.count { it.isOnline() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enhanced friends list with keyboard navigation
                EnhancedFriendsList(
                    friends = friends,
                    selectedFriendId = selectedFriendId,
                    focusedIndex = focusedIndex,
                    isKeyboardNavigating = isKeyboardNavigating,
                    listState = listState,
                    hasAppeared = hasAppeared,
                    onFriendClick = { friendId ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFriendClick(friendId)
                    },
                    onFocusChanged = { index, focused ->
                        if (focused) {
                            focusedIndex = index
                            isKeyboardNavigating = true
                        }
                    },
                    onInviteFriendsClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onInviteFriendsClick()
                    }
                )
            }
        }
    }
}

/**
 * Enhanced carousel header with animated counters
 */
@Composable
private fun EnhancedCarouselHeader(
    friendsCount: Int,
    onlineCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = "Friends",
            transitionSpec = {
                fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                fadeOut(FFinderAnimations.Transitions.screenExit())
            },
            label = "header_title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Online count with pulsing indicator
            AnimatedOnlineCounter(
                count = onlineCount,
                isActive = onlineCount > 0
            )
        }
    }
}

/**
 * Animated online counter with pulsing indicator
 */
@Composable
private fun AnimatedOnlineCounter(
    count: Int,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "online_counter_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = if (isActive) {
            FFinderAnimations.Loading.pulse()
        } else {
            infiniteRepeatable(FFinderAnimations.Accessibility.reducedMotion())
        },
        label = "pulse_alpha"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isActive) pulseAlpha else 0.4f)
                .background(
                    color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                )
                .shadow(if (isActive) 4.dp else 0.dp, CircleShape)
        )
        
        AnimatedContent(
            targetState = "$count online",
            transitionSpec = {
                fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                fadeOut(FFinderAnimations.Transitions.screenExit())
            },
            label = "online_count"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isActive) {
                    Color(0xFF4CAF50)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Enhanced friends list with keyboard navigation and staggered animations
 */
@Composable
private fun EnhancedFriendsList(
    friends: List<Friend>,
    selectedFriendId: String?,
    focusedIndex: Int,
    isKeyboardNavigating: Boolean,
    listState: LazyListState,
    hasAppeared: Boolean,
    onFriendClick: (String) -> Unit,
    onFocusChanged: (Int, Boolean) -> Unit,
    onInviteFriendsClick: () -> Unit
) {
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, _ ->
                    // Handle swipe gestures for accessibility
                }
            }
    ) {
        itemsIndexed(friends) { index, friend ->
            EnhancedFriendCarouselItem(
                friend = friend,
                index = index,
                isSelected = friend.id == selectedFriendId,
                isFocused = focusedIndex == index && isKeyboardNavigating,
                hasAppeared = hasAppeared,
                onClick = { onFriendClick(friend.id) },
                onFocusChanged = { focused -> onFocusChanged(index, focused) }
            )
        }
        
        // Enhanced invite friends button
        item {
            EnhancedInviteFriendsButton(
                index = friends.size,
                isFocused = focusedIndex == friends.size && isKeyboardNavigating,
                hasAppeared = hasAppeared,
                onClick = onInviteFriendsClick,
                onFocusChanged = { focused -> onFocusChanged(friends.size, focused) }
            )
        }
    }
}

/**
 * Enhanced friend carousel item with comprehensive animations and accessibility
 */
@Composable
private fun EnhancedFriendCarouselItem(
    friend: Friend,
    index: Int,
    isSelected: Boolean,
    isFocused: Boolean,
    hasAppeared: Boolean,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Staggered entry animation with 150ms delay per item (Phase 2 requirement)
    var shouldAnimate by remember { mutableStateOf(false) }
    
    LaunchedEffect(hasAppeared) {
        if (hasAppeared) {
            delay(index * 150L) // Stagger by 150ms per item as per Phase 2 requirements
            shouldAnimate = true
        }
    }
    
    // Animation states
    val itemScale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.15f
            isFocused -> 1.1f
            else -> 1.0f
        },
        animationSpec = FFinderAnimations.Springs.Bouncy,
        label = "item_scale"
    )
    
    val itemAlpha by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = FFinderAnimations.Transitions.screenEnter(),
        label = "item_alpha"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = when {
            isSelected -> 12f
            isFocused -> 8f
            else -> 4f
        },
        animationSpec = FFinderAnimations.MicroInteractions.hover(),
        label = "shadow_elevation"
    )
    
    // Focus ring color
    val focusRingColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isFocused -> MaterialTheme.colorScheme.secondary
            else -> Color.Transparent
        },
        animationSpec = tween<Color>(300),
        label = "focus_ring_color"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(itemScale)
            .alpha(itemAlpha)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            }
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.Enter, Key.Spacebar -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "${friend.name}, ${if (friend.isOnline()) "online" else "offline"}" +
                        if (isSelected) ", selected" else ""
            }
    ) {
        Box {
            // Enhanced avatar with gradient ring and glow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(shadowElevation.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(friend.profileColor)),
                                Color(android.graphics.Color.parseColor(friend.profileColor)).copy(alpha = 0.8f)
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
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Enhanced online indicator with pulse
            if (friend.isOnline()) {
                val infiniteTransition = rememberInfiniteTransition(label = "online_pulse_transition")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = FFinderAnimations.Loading.pulse(),
                    label = "online_pulse_scale"
                )
                
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .scale(pulseScale)
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
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
            
            // Focus ring
            if (isFocused || isSelected) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 2.dp,
                            color = focusRingColor,
                            shape = CircleShape
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Enhanced friend name with status color
        AnimatedContent(
            targetState = friend.name.split(" ").firstOrNull() ?: friend.name,
            transitionSpec = {
                fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                fadeOut(FFinderAnimations.Transitions.screenExit())
            },
            label = "friend_name"
        ) { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    friend.isOnline() -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(68.dp)
            )
        }
    }
}

/**
 * Enhanced invite friends button with animations and accessibility
 */
@Composable
private fun EnhancedInviteFriendsButton(
    index: Int,
    isFocused: Boolean,
    hasAppeared: Boolean,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Staggered entry animation
    var shouldAnimate by remember { mutableStateOf(false) }
    
    LaunchedEffect(hasAppeared) {
        if (hasAppeared) {
            delay(index * 150L) // Stagger by 150ms per item as per Phase 2 requirements
            shouldAnimate = true
        }
    }
    
    // Animation states
    val buttonScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = FFinderAnimations.Springs.Standard,
        label = "button_scale"
    )
    
    val buttonAlpha by animateFloatAsState(
        targetValue = if (shouldAnimate) 1f else 0f,
        animationSpec = FFinderAnimations.Transitions.screenEnter(),
        label = "button_alpha"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (isFocused) 8f else 4f,
        animationSpec = FFinderAnimations.MicroInteractions.hover(),
        label = "button_shadow"
    )
    
    // Pulsing animation for invite button
    val infiniteTransition = rememberInfiniteTransition(label = "invite_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = FFinderAnimations.Loading.breathing(),
        label = "invite_pulse_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(buttonScale)
            .alpha(buttonAlpha)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            }
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.Enter, Key.Spacebar -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "Invite friends to FFinder"
            }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .shadow(shadowElevation.dp, CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null, // Handled by parent semantics
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Invite",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.width(68.dp)
        )
    }
}

/**
 * Enhanced empty state with delightful animations
 */
@Composable
fun EmptyFriendsState(
    onInviteFriendsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasAppeared by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(200) // Slight delay for smooth appearance
        hasAppeared = true
    }
    
    AnimatedVisibility(
        visible = hasAppeared,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = FFinderAnimations.Springs.Bouncy
        ) + fadeIn(
            animationSpec = FFinderAnimations.Transitions.screenEnter()
        ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics {
                    contentDescription = "No friends available. Invite friends to start sharing locations."
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated sleeping illustration
                AnimatedEmptyStateIllustration()
                
                Spacer(modifier = Modifier.height(20.dp))
                
                AnimatedContent(
                    targetState = "No friends online",
                    transitionSpec = {
                        fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                        fadeOut(FFinderAnimations.Transitions.screenExit())
                    },
                    label = "empty_title"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnimatedContent(
                    targetState = "Invite friends to start sharing locations and see them on your map",
                    transitionSpec = {
                        fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                        fadeOut(FFinderAnimations.Transitions.screenExit())
                    },
                    label = "empty_description"
                ) { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                EnhancedInviteFriendsButton(
                    index = 0,
                    isFocused = false,
                    hasAppeared = true,
                    onClick = onInviteFriendsClick,
                    onFocusChanged = { }
                )
            }
        }
    }
}

/**
 * Animated illustration for empty state
 */
@Composable
private fun AnimatedEmptyStateIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_illustration_transition")
    
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = FFinderAnimations.Loading.breathing(),
        label = "float_offset"
    )
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = FFinderAnimations.Loading.breathing(),
        label = "rotation_angle"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .offset(y = floatOffset.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "😴",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier
                .scale(1.2f)
        )
    }
}