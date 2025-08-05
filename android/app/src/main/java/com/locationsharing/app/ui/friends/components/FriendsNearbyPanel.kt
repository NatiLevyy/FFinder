package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import com.google.android.gms.maps.model.LatLng

/**
 * A composable that displays the Friends Nearby Panel with search functionality.
 * 
 * Implements requirements:
 * - 2.1: Full-height side sheet with LazyColumn sorted by distance
 * - 2.2: Display friends with avatar, name, distance, and status
 * - 2.4: Empty state for no friends sharing location
 * - 2.5: Error state for location permission issues
 * - 3.1: SearchBar at the top for filtering friends
 * - 3.2: Real-time filtering by display name
 * - 3.3: Show all friends when search is cleared
 * - 3.4: No friends found message for empty search results
 * - 7.3: Search accessibility labels and hints
 * 
 * @param uiState The current UI state containing friends data and search query
 * @param onEvent Callback for handling user events
 * @param modifier Modifier for styling the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsNearbyPanel(
    uiState: NearbyUiState,
    onEvent: (NearbyPanelEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityManager = LocalAccessibilityManager.current
    
    // Announce friend count updates for screen readers
    LaunchedEffect(uiState.filteredFriends.size) {
        if (!uiState.isLoading && uiState.error == null) {
            // The announcement will be made through the live region in the LazyColumn
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar with accessibility support
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = { query -> onEvent(NearbyPanelEvent.SearchQuery(query)) },
            onSearch = { /* No action needed for real-time search */ },
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
                            contentDescription = null, // Content description is on the button
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        ) {
            // Empty content for SearchBar - we're not using the dropdown functionality
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on state
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
            
            uiState.filteredFriends.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                NoSearchResultsState(
                    searchQuery = uiState.searchQuery,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            uiState.filteredFriends.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                FriendsList(
                    friends = uiState.filteredFriends,
                    onFriendClick = { friendId -> onEvent(NearbyPanelEvent.FriendClick(friendId)) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Loading state composable
 */
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error state composable with retry functionality
 */
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
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

/**
 * Empty state when no friends are sharing location
 */
@Composable
private fun EmptyState(
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
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = "No friends sharing location",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.no_friends_sharing_location),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.invite_friends_to_share_location),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * No search results state
 */
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No search results",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = stringResource(R.string.no_friends_found_matching, searchQuery),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Friends list with lazy loading structure optimized for large friend lists (500+ items)
 * Implements performance optimizations for smooth scrolling
 */
@Composable
private fun FriendsList(
    friends: List<NearbyFriend>,
    onFriendClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Performance optimization: Use remember for stable references
    val stableFriends = remember(friends) { friends }
    val stableOnClick = remember(onFriendClick) { onFriendClick }
    
    // Performance optimization: Remember LazyListState for scroll position preservation
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }
    
    LazyColumn(
        state = listState,
        modifier = modifier.semantics {
            contentDescription = "Friends list with ${friends.size} friends"
            liveRegion = LiveRegionMode.Polite
        },
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = stableFriends,
            key = { friend -> friend.id },
            contentType = { "friend_item" } // Performance: Enable item recycling
        ) { friend ->
            // Performance optimization: Use stable key and content type
            NearbyFriendItem(
                friend = friend,
                onClick = { stableOnClick(friend.id) }
            )
        }
        
        // Performance indicator for large lists
        if (friends.size > 100) {
            item(
                key = "performance_info",
                contentType = "info_item"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Showing ${friends.size} friends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Placeholder for future pagination loading indicator
        // This structure prepares for lazy loading of 500+ friends
        /*
        if (hasMoreFriends) {
            item(
                key = "loading_indicator",
                contentType = "loading_item"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        */
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelPreview() {
    FFinderTheme {
        // Preview with friends
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = false,
                searchQuery = "",
                friends = listOf(
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
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelEmptyPreview() {
    FFinderTheme {
        // Preview with empty state
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = false,
                searchQuery = "",
                friends = emptyList()
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelLoadingPreview() {
    FFinderTheme {
        // Preview with loading state
        FriendsNearbyPanel(
            uiState = NearbyUiState(
                isLoading = true,
                searchQuery = "",
                friends = emptyList()
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsNearbyPanelErrorPreview() {
    FFinderTheme {
        // Preview with error state
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