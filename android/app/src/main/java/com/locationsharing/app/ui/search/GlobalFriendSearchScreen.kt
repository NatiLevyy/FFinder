package com.locationsharing.app.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.locationsharing.app.R
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.theme.FFinderExtendedColors
import com.locationsharing.app.ui.theme.FFinderGradientTop
import kotlin.math.roundToInt

/**
 * Global Friend Search Screen
 * Provides search functionality to find friends globally without radius limits
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalFriendSearchScreen(
    onBackClick: () -> Unit,
    onFriendSelected: (Friend) -> Unit,
    viewModel: GlobalFriendSearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    
    var isSearchBarActive by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Show FAB when user scrolls deep into results
    val showBackToMapFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 5
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Friends") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (showBackToMapFab) {
                FloatingActionButton(
                    onClick = onBackClick,
                    containerColor = FFinderGradientTop,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Map")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onSearch = { isSearchBarActive = false },
                active = isSearchBarActive,
                onActiveChange = { isSearchBarActive = it },
                placeholder = { Text("Search for friends...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize()
            ) {
                // Search suggestions could go here if needed
            }
            
            // Search Results with AnimatedContent
            AnimatedContent(
                targetState = getSearchState(searchResults, isSearching, searchQuery),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.fillMaxSize(),
                label = "search_content"
            ) { state ->
                when (state) {
                    SearchState.EMPTY -> EmptySearchState()
                    SearchState.LOADING -> LoadingState()
                    SearchState.NO_RESULTS -> NoResultsState(searchQuery)
                    SearchState.RESULTS -> SearchResultsList(
                        searchResults = searchResults,
                        listState = listState,
                        onFriendSelected = onFriendSelected
                    )
                }
            }
        }
    }
}

/**
 * Determine the current search state
 */
private fun getSearchState(
    searchResults: LazyPagingItems<Friend>,
    isSearching: Boolean,
    searchQuery: String
): SearchState {
    return when {
        searchQuery.isEmpty() -> SearchState.EMPTY
        isSearching || searchResults.loadState.refresh is LoadState.Loading -> SearchState.LOADING
        searchResults.itemCount == 0 && searchResults.loadState.refresh is LoadState.NotLoading -> SearchState.NO_RESULTS
        else -> SearchState.RESULTS
    }
}

/**
 * Search state enum
 */
private enum class SearchState {
    EMPTY, LOADING, NO_RESULTS, RESULTS
}

/**
 * Empty state when no search query is entered
 */
@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search for friends globally",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a name to find friends anywhere",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Loading state with spinner
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = FFinderGradientTop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * No results state with Lottie animation
 */
@Composable
private fun NoResultsState(searchQuery: String) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.location_animation))
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No friends found",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No results for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Search results list
 */
@Composable
private fun SearchResultsList(
    searchResults: LazyPagingItems<Friend>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onFriendSelected: (Friend) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = searchResults.itemCount,
            key = { index -> searchResults[index]?.id ?: index }
        ) { index ->
            val friend = searchResults[index]
            if (friend != null) {
                FriendSearchResultItem(
                    friend = friend,
                    onClick = { onFriendSelected(friend) }
                )
            }
        }
        
        // Loading indicator for pagination
        if (searchResults.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = FFinderGradientTop,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual friend search result item
 */
@Composable
private fun FriendSearchResultItem(
    friend: Friend,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(friend.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${friend.name} avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(FFinderGradientTop.copy(alpha = 0.1f)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Friend info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Distance calculation (placeholder for now)
                val distance = calculateDistance(friend)
                Text(
                    text = "$distance km away",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Calculate distance to friend (placeholder implementation)
 */
private fun calculateDistance(friend: Friend): Int {
    // TODO: Implement actual distance calculation using user's current location
    // For now, return a random distance for demo purposes
    return (50..500).random()
}