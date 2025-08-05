package com.locationsharing.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.friends.FriendsListViewModel
import com.locationsharing.app.ui.theme.FFinderAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time friends list screen with Firebase integration
 * Shows live status updates and smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    onBackClick: () -> Unit,
    onFriendClick: (Friend) -> Unit,
    onInviteFriendsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Handle error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = "Friends",
                        transitionSpec = {
                            fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                            fadeOut(FFinderAnimations.Transitions.screenExit())
                        },
                        label = "title_animation"
                    ) { title ->
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshFriends() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh friends"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onInviteFriendsClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Invite friends",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.friends.isEmpty() -> {
                    EmptyFriendsState(
                        onInviteFriendsClick = onInviteFriendsClick,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    FriendsListContent(
                        friends = uiState.friends,
                        onlineFriends = uiState.onlineFriends,
                        onFriendClick = onFriendClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Loading state with FFinder branded shimmer effect
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFF6B35),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Loading friends...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Main friends list content with real-time updates
 */
@Composable
private fun FriendsListContent(
    friends: List<Friend>,
    onlineFriends: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Online friends section
        if (onlineFriends.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Online",
                    count = onlineFriends.size,
                    isOnline = true
                )
            }
            
            itemsIndexed(onlineFriends) { index, friend ->
                FriendListItem(
                    friend = friend,
                    index = index,
                    isOnline = true,
                    onClick = { onFriendClick(friend) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Offline friends section
        val offlineFriends = friends.filter { !it.isOnline() }
        if (offlineFriends.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Offline",
                    count = offlineFriends.size,
                    isOnline = false
                )
            }
            
            itemsIndexed(offlineFriends) { index, friend ->
                FriendListItem(
                    friend = friend,
                    index = index,
                    isOnline = false,
                    onClick = { onFriendClick(friend) }
                )
            }
        }
    }
}

/**
 * Section header with animated counter
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Animated online indicator
        if (isOnline) {
            val infiniteTransition = rememberInfiniteTransition(label = "online_indicator")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000)
                ),
                label = "pulse_alpha"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulseAlpha)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    )
                    .shadow(4.dp, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Animated count
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                fadeIn(FFinderAnimations.Transitions.screenEnter()) togetherWith
                fadeOut(FFinderAnimations.Transitions.screenExit())
            },
            label = "count_animation"
        ) { animatedCount ->
            Text(
                text = "($animatedCount)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual friend list item with staggered animations
 */
@Composable
private fun FriendListItem(
    friend: Friend,
    index: Int,
    isOnline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAppeared by remember { mutableStateOf(false) }
    
    // Staggered entrance animation
    LaunchedEffect(Unit) {
        delay(index * 150L) // 150ms delay between items
        hasAppeared = true
    }
    
    val itemScale by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0.9f,
        animationSpec = FFinderAnimations.Springs.Bouncy,
        label = "item_scale"
    )
    
    val itemAlpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = FFinderAnimations.Transitions.screenEnter(),
        label = "item_alpha"
    )
    
    AnimatedVisibility(
        visible = hasAppeared,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(300)
        ) + fadeIn() + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(itemScale)
                .alpha(itemAlpha)
                .clickable { onClick() }
                .semantics {
                    contentDescription = "${friend.name}, ${if (isOnline) "online" else "offline"}, ${friend.getStatusText()}"
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Friend avatar with status indicator
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(friend.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(friend.profileColor)),
                                        Color(android.graphics.Color.parseColor(friend.profileColor)).copy(alpha = 0.8f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Online status indicator
                    if (isOnline) {
                        val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000)
                            ),
                            label = "pulse_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .scale(pulseScale)
                        ) {
                            // Glow effect
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
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
                                    .size(12.dp)
                                    .align(Alignment.Center)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Friend info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
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
                            color = if (isOnline) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                // Location sharing indicator
                if (friend.status.isLocationSharingEnabled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "📍",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state with branded animation and invite CTA
 */
@Composable
private fun EmptyFriendsState(
    onInviteFriendsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasAppeared by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300) // Slight delay for smooth appearance
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated floating elements
            AnimatedFloatingElements()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "No Friends Yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Invite friends to start sharing locations and see them on your map",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            FloatingActionButton(
                onClick = onInviteFriendsClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Invite friends",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Animated floating elements for empty state
 */
@Composable
private fun AnimatedFloatingElements() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_elements")
    
    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000)
        ),
        label = "float_offset_1"
    )
    
    val floatOffset2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500)
        ),
        label = "float_offset_2"
    )
    
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating map pin
        Text(
            text = "📍",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier
                .offset(y = floatOffset1.dp)
                .alpha(0.7f)
        )
        
        // Floating person icon
        Text(
            text = "👥",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier
                .offset(x = 30.dp, y = floatOffset2.dp)
                .alpha(0.5f)
        )
    }
}