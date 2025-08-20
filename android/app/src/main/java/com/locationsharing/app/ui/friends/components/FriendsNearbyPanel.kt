package com.locationsharing.app.ui.friends.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
import com.locationsharing.app.ui.friends.components.NearbyPanelPerformanceMonitor
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsNearbyPanel(
    uiState: NearbyUiState,
    onEvent: (NearbyPanelEvent) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onScrimClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = tween(durationMillis = 200)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = tween(durationMillis = 200)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onScrimClick?.invoke() }
            )
        }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(
                    durationMillis = 300,
                    easing = androidx.compose.animation.core.FastOutLinearInEasing
                )
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(
                    durationMillis = 300,
                    easing = androidx.compose.animation.core.FastOutLinearInEasing
                )
            )
        ) {
            EnhancedDrawerContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedDrawerContent(
    uiState: NearbyUiState,
    onEvent: (NearbyPanelEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityManager = LocalAccessibilityManager.current
    
    val filteredFriends = remember(uiState.friends, uiState.searchQuery) {
        uiState.filteredFriends
    }
    
    LaunchedEffect(filteredFriends.size) {
        if (!uiState.isLoading && uiState.error == null) {
            NearbyPanelLogger.logFriendListState(
                friends = filteredFriends,
                searchQuery = uiState.searchQuery
            )
        }
    }
    
    LaunchedEffect(Unit) {
        NearbyPanelLogger.logMemoryUsage("FriendListUpdate")
    }
    
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { query -> 
                    NearbyPanelLogger.measureUIUpdate(
                        "SearchQuery",
                        uiState.friends.size
                    ) {
                        onEvent(NearbyPanelEvent.SearchQuery(query))
                    }
                },
                onSearch = { /* No-op for now */ },
                active = false,
                onActiveChange = { /* Not using active state */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Search friends by name"
                    },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_friends_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onEvent(NearbyPanelEvent.SearchQuery("")) },
                            modifier = Modifier.semantics {
                                contentDescription = "Clear search"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            ) {
                // Empty content for SearchBar
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                uiState.isLoading -> {
                    LoadingState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error,
                        onRetry = { onEvent(NearbyPanelEvent.RefreshFriends) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                filteredFriends.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    NoSearchResultsState(
                        searchQuery = uiState.searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                filteredFriends.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        onInviteFriends = { onEvent(NearbyPanelEvent.InviteFriends) }
                    )
                }
                
                else -> {
                    FriendsList(
                        friends = filteredFriends,
                        onFriendClick = { friendId -> onEvent(NearbyPanelEvent.FriendClick(friendId)) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.loading_friends),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.semantics {
                    contentDescription = "Retry loading friends"
                }
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onInviteFriends: () -> Unit = {}
) {
    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.location_animation)).value
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Use Lottie animation from res/raw/location_animation.json
            LottieAnimation(
                composition = composition,
                modifier = Modifier.height(120.dp),
                iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever
            )
            
            Text(
                text = "No friends found within 10 km. Invite friends to appear here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextButton(
                onClick = onInviteFriends,
                modifier = Modifier.semantics {
                    contentDescription = "Invite friends to share location"
                }
            ) {
                Text("Invite friends")
            }
        }
    }
}

@Composable
private fun NoSearchResultsState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                 text = stringResource(R.string.no_friends_found_matching, searchQuery),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FriendsList(
    friends: List<NearbyFriend>,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Friends list with ${friends.size} friends"
            },
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = friends,
            key = { friend -> friend.id }
        ) { friend ->
            FriendItem(
                friend = friend,
                onClick = { onFriendClick(friend.id) }
            )
        }
    }
}

@Composable
private fun FriendItem(
    friend: NearbyFriend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                 contentDescription = "${friend.displayName}, ${friend.formattedDistance}"
             },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
             Text(
                 text = friend.displayName,
                 style = MaterialTheme.typography.bodyLarge,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
             )
             Text(
                 text = friend.formattedDistance,
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
             )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelPreview() {
    FFinderTheme {
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = false,
                searchQuery = "",
                friends = listOf(
                     NearbyFriend(
                         id = "1",
                         displayName = "Alice Johnson",
                         avatarUrl = null,
                         distance = 200.0,
                         isOnline = true,
                         lastUpdated = System.currentTimeMillis(),
                         latLng = LatLng(37.7749, -122.4194)
                     ),
                     NearbyFriend(
                         id = "2",
                         displayName = "Bob Smith",
                         avatarUrl = null,
                         distance = 500.0,
                         isOnline = false,
                         lastUpdated = System.currentTimeMillis(),
                         latLng = LatLng(37.7849, -122.4094)
                     )
                 ),
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelEmptyPreview() {
    FFinderTheme {
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = false,
                searchQuery = "",
                friends = emptyList(),
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelLoadingPreview() {
    FFinderTheme {
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = true,
                searchQuery = "",
                friends = emptyList(),
                error = null
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelErrorPreview() {
    FFinderTheme {
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = false,
                searchQuery = "",
                friends = emptyList(),
                error = "Location permission denied. Please enable location access to see nearby friends."
            ),
            onEvent = {}
        )
    }
}