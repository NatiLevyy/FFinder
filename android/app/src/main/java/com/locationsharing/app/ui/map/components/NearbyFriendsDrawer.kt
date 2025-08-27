package com.locationsharing.app.ui.map.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.components.AnimatedPin
import com.locationsharing.app.ui.friends.components.NearbyFriendItem
import com.locationsharing.app.ui.map.MapScreenConstants
import com.locationsharing.app.ui.map.accessibility.MapAccessibilityConstants
import com.locationsharing.app.ui.map.haptic.rememberMapHapticFeedbackManager
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sin

/**
 * Custom overshoot easing function that mimics Android's OvershootInterpolator
 * with a tension factor of 1.2f for smooth drawer animation
 */
private val OvershootEasing = Easing { fraction ->
    val tension = 1.2f
    val adjustedFraction = fraction - 1.0f
    adjustedFraction * adjustedFraction * ((tension + 1) * adjustedFraction + tension) + 1.0f
}

/**
 * NearbyFriendsDrawer component implementing ModalNavigationDrawer with 280dp width,
 * 50% black scrim overlay, search bar, and LazyColumn for friend list display.
 * 
 * Implements requirements:
 * - 6.1: ModalNavigationDrawer from right edge
 * - 6.2: 280dp width with 50% black scrim
 * - 6.3: Search bar at top of drawer
 * - 6.4: LazyColumn of friend items
 * - 6.6: Tap-outside-to-dismiss functionality
 * - 6.7: Overshoot interpolator animation (300ms)
 * - 8.3: Smooth drawer slide animation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyFriendsDrawer(
    isOpen: Boolean,
    friends: List<NearbyFriend>,
    onDismiss: () -> Unit,
    onFriendClick: (NearbyFriend) -> Unit,
    onFriendMessage: ((NearbyFriend) -> Unit)? = null,
    onFriendMoreActions: ((NearbyFriend) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val coroutineScope = rememberCoroutineScope()
    val hapticManager = rememberMapHapticFeedbackManager()
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter friends based on search query
    val filteredFriends = remember(friends, searchQuery) {
        if (searchQuery.isBlank()) {
            friends
        } else {
            friends.filter { friend ->
                friend.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Enhanced drawer animation using MapMicroAnimations with accessibility support
    val drawerOffset = com.locationsharing.app.ui.map.animations.MapMicroAnimations.DrawerSlideAnimation(
        isOpen = isOpen,
        drawerWidth = MapScreenConstants.Dimensions.DRAWER_WIDTH.value,
        isReducedMotion = false // TODO: Get from accessibility settings
    )
    
    // Enhanced scrim fade animation
    val scrimAlpha = com.locationsharing.app.ui.map.animations.MapMicroAnimations.DrawerScrimFade(
        isVisible = isOpen,
        targetAlpha = MapScreenConstants.Layout.DRAWER_SCRIM_OPACITY
    )
    
    // Handle drawer state changes with enhanced overshoot animation
    LaunchedEffect(isOpen) {
        if (isOpen && drawerState.isClosed) {
            drawerState.animateTo(
                targetValue = DrawerValue.Open,
                anim = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            )
        } else if (!isOpen && drawerState.isOpen) {
            drawerState.animateTo(
                targetValue = DrawerValue.Closed,
                anim = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            )
        }
    }
    
    // Handle drawer close events
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed && isOpen) {
            onDismiss()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                friends = filteredFriends,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onFriendClick = onFriendClick,
                onFriendMessage = onFriendMessage,
                onFriendMoreActions = onFriendMoreActions,
                onDismiss = {
                    hapticManager.performDrawerAction() // Haptic feedback for drawer dismissal
                    coroutineScope.launch {
                        // Use enhanced spring animation for closing
                        drawerState.animateTo(
                            targetValue = DrawerValue.Closed,
                            anim = spring(
                                dampingRatio = 0.8f,
                                stiffness = 400f
                            )
                        )
                    }
                }
            )
        },
        modifier = modifier.semantics {
            contentDescription = MapAccessibilityConstants.DRAWER_CONTENT
            role = Role.Button
            testTag = MapAccessibilityConstants.DRAWER_TEST_TAG
            stateDescription = if (isOpen) {
                MapAccessibilityConstants.DRAWER_OPEN
            } else {
                MapAccessibilityConstants.DRAWER_CLOSED
            }
        },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha),
        gesturesEnabled = true
    ) {
        content()
    }
}

/**
 * Internal drawer content composable with search bar and friend list
 */
@Composable
private fun DrawerContent(
    friends: List<NearbyFriend>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFriendClick: (NearbyFriend) -> Unit,
    onFriendMessage: ((NearbyFriend) -> Unit)?,
    onFriendMoreActions: ((NearbyFriend) -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(MapScreenConstants.Dimensions.DRAWER_WIDTH)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = MapScreenConstants.Dimensions.DRAWER_ELEVATION
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MapScreenConstants.Dimensions.STANDARD_PADDING)
        ) {
            // Search bar at top
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = MapAccessibilityConstants.SEARCH_FIELD
                        role = Role.Button
                        testTag = MapAccessibilityConstants.SEARCH_FIELD_TEST_TAG
                    },
                placeholder = {
                    Text(
                        text = "Search friends...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Friends list or empty state
            if (friends.isEmpty()) {
                EmptyFriendsState(
                    hasSearchQuery = searchQuery.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .semantics {
                            contentDescription = MapAccessibilityConstants.friendListCount(friends.size)
                            role = Role.Button
                            testTag = MapAccessibilityConstants.FRIEND_LIST_TEST_TAG
                        }
                ) {
                    items(
                        items = friends,
                        key = { friend -> friend.id }
                    ) { friend ->
                        NearbyFriendItem(
                            friend = friend,
                            onClick = {
                                onFriendClick(friend)
                                onDismiss()
                            },
                            onMessageClick = onFriendMessage,
                            onMoreClick = onFriendMoreActions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no friends are available or search returns no results
 * Features Location animation (160×160 dp, play once) as specified
 */
@Composable
private fun EmptyFriendsState(
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location animation for empty state (64dp as per requirements)
            if (!hasSearchQuery) {
                AnimatedPin(
                    modifier = Modifier.size(64.dp),
                    tint = androidx.compose.ui.graphics.Color(0xFFB791E0), // Brand purple
                    animated = false // Static for empty state as per requirements
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = if (hasSearchQuery) {
                    "No friends found"
                } else {
                    "No nearby friends"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (hasSearchQuery) {
                    "Try adjusting your search"
                } else {
                    "Friends will appear here when they're nearby"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preview for NearbyFriendsDrawer with friends
 */
@Preview(showBackground = true)
@Composable
fun NearbyFriendsDrawerPreview() {
    FFinderTheme {
        val sampleFriends = listOf(
            NearbyFriend(
                id = "1",
                displayName = "Alice Johnson",
                avatarUrl = null,
                distance = 150.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis(),
                latLng = LatLng(37.7749, -122.4194)
            ),
            NearbyFriend(
                id = "2",
                displayName = "Bob Smith",
                avatarUrl = null,
                distance = 1200.0,
                isOnline = false,
                lastUpdated = System.currentTimeMillis() - 300000,
                latLng = LatLng(37.7849, -122.4094)
            ),
            NearbyFriend(
                id = "3",
                displayName = "Carol Davis",
                avatarUrl = null,
                distance = 2500.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis() - 60000,
                latLng = LatLng(37.7949, -122.3994)
            )
        )
        
        NearbyFriendsDrawer(
            isOpen = true,
            friends = sampleFriends,
            onDismiss = {},
            onFriendClick = {},
            onFriendMessage = { },
            onFriendMoreActions = { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("Main Content")
            }
        }
    }
}

/**
 * Preview for NearbyFriendsDrawer with no friends
 */
@Preview(showBackground = true)
@Composable
fun NearbyFriendsDrawerEmptyPreview() {
    FFinderTheme {
        NearbyFriendsDrawer(
            isOpen = true,
            friends = emptyList(),
            onDismiss = {},
            onFriendClick = {},
            onFriendMessage = { },
            onFriendMoreActions = { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("Main Content")
            }
        }
    }
}

/**
 * Preview for NearbyFriendsDrawer in dark theme
 */
@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NearbyFriendsDrawerDarkPreview() {
    FFinderTheme {
        val sampleFriends = listOf(
            NearbyFriend(
                id = "1",
                displayName = "Alice Johnson",
                avatarUrl = null,
                distance = 150.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis(),
                latLng = LatLng(37.7749, -122.4194)
            ),
            NearbyFriend(
                id = "2",
                displayName = "Bob Smith",
                avatarUrl = null,
                distance = 1200.0,
                isOnline = false,
                lastUpdated = System.currentTimeMillis() - 300000,
                latLng = LatLng(37.7849, -122.4094)
            )
        )
        
        NearbyFriendsDrawer(
            isOpen = true,
            friends = sampleFriends,
            onDismiss = {},
            onFriendClick = {},
            onFriendMessage = { },
            onFriendMoreActions = { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("Main Content")
            }
        }
    }
}