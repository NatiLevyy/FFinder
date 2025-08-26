package com.locationsharing.app.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.SideEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.locationsharing.app.ui.components.AnimatedPin
import com.locationsharing.app.ui.friends.components.FriendsNearbyPanel
import com.locationsharing.app.ui.friends.components.FriendsToggleFAB
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.components.SelfLocationFAB
import com.locationsharing.app.ui.map.MapScreenEvent
import com.locationsharing.app.ui.map.MapScreenState
import com.locationsharing.app.ui.map.MapScreenViewModel
import com.locationsharing.app.ui.map.components.DebugFAB
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.R
import com.locationsharing.app.domain.model.NearbyFriend
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * Interactive MapScreen with Google Maps, location sharing, and friend features
 * 
 * Features:
 * - Interactive Google Maps with user location
 * - Location permissions handling
 * - Self-Location FAB for centering on user location
 * - Quick-Share FAB for toggling live location sharing
 * - Friends nearby panel with drawer functionality
 * - Friend markers with clustering
 * - Real-time location updates
 * - GPS animation loop when waiting for location fix
 * - Map interactions (zoom, scroll, marker clicks)
 * - Accessibility support
 * 
 * @param startSharing Whether to start location sharing immediately
 * @param onBackClick Callback for back navigation
 * @param modifier Modifier for customization
 * @param viewModel MapScreenViewModel for state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    startSharing: Boolean = false,
    friendIdToFocus: String? = null,
    onBackClick: () -> Unit,
    onSearchFriendsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MapScreenViewModel = hiltViewModel(),
    onDebugAddFriends: (() -> Unit)? = null
) {
    // Collect state from ViewModel
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Handle back navigation: close drawer first, then navigate back
    BackHandler(enabled = true) {
        if (state.isNearbyDrawerOpen) {
            // Close drawer if open
            viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
        } else {
            // Navigate back to previous screen
            viewModel.cleanupOnNavigationAway()
            onBackClick()
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(MapScreenEvent.OnLocationPermissionGranted)
        } else {
            viewModel.onEvent(MapScreenEvent.OnLocationPermissionDenied)
        }
    }
    
    // Handle start sharing parameter
    LaunchedEffect(startSharing) {
        if (startSharing && !state.isLocationSharingActive) {
            viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        }
    }
    
    // Handle search friends navigation
    LaunchedEffect(state.shouldNavigateToSearchFriends) {
        if (state.shouldNavigateToSearchFriends) {
            onSearchFriendsClick()
            viewModel.onSearchFriendsNavigationHandled()
        }
    }
    
    // Handle friend focus from search
    LaunchedEffect(friendIdToFocus) {
        friendIdToFocus?.let { friendId ->
            viewModel.onEvent(MapScreenEvent.OnFriendSelectedFromSearch(friendId))
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Google Maps
        if (state.hasLocationPermission) {
            GoogleMapContent(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show permission request screen
            LocationPermissionScreen(
                onRequestPermission = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                shouldShowRationale = false, // For simplicity, can be improved later
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = if (state.isLocationSharingActive) "Live Sharing" else "Map",
                    fontWeight = FontWeight.Medium
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        viewModel.cleanupOnNavigationAway()
                        onBackClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        // Navigate to search friends screen
                        // This will be handled by the parent composable
                        viewModel.onEvent(MapScreenEvent.OnSearchFriendsClick)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search friends globally"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            modifier = Modifier.zIndex(1f)
        )
        
        // Self-Location FAB (bottom-right)
        SelfLocationFAB(
            onClick = {
                viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
            },
            isLoading = state.isLocationLoading,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
                .semantics {
                    contentDescription = "Center map on your location"
                }
        )
        
        // Quick-Share FAB (bottom-center)
        QuickShareFAB(
            isSharing = state.isLocationSharingActive,
            isWaitingForFix = state.isLocationLoading,
            onClick = {
                viewModel.onEvent(MapScreenEvent.OnQuickShare)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .semantics {
                    contentDescription = if (state.isLocationSharingActive) {
                        "Stop live location sharing"
                    } else {
                        "Start live location sharing"
                    }
                }
        )
        
        // Friends nearby toggle FAB (bottom-left)
        if (state.nearbyFriendsCount > 0) {
            FriendsToggleFAB(
                onClick = { viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle) },
                friendCount = state.nearbyFriendsCount,
                isExpanded = true,
                isPanelOpen = state.isNearbyDrawerOpen,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 100.dp)
                    .semantics {
                        contentDescription = "Toggle nearby friends panel"
                    }
            )
        }
        
        // Debug FAB (top-left, debug builds only)
        if (BuildConfig.DEBUG) {
            DebugFAB(
                onClick = {
                    onDebugAddFriends?.invoke() ?: viewModel.addTestFriendsOnMap()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 80.dp)
            )
        }
        
        // Enhanced Friends nearby panel with drawer and scrim
        if (state.nearbyFriendsCount > 0) {
            FriendsNearbyPanel(
                uiState = NearbyUiState(
                    friends = state.nearbyFriends,
                    isLoading = state.isFriendsLoading,
                    searchQuery = "",
                    error = state.friendsError
                ),
                onEvent = { event ->
                    when (event) {
                        is NearbyPanelEvent.FriendClick -> {
                            viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick(event.friendId))
                        }
                        is NearbyPanelEvent.TogglePanel -> {
                            viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
                        }
                        else -> {} // Handle other events as needed
                    }
                },
                isVisible = state.isNearbyDrawerOpen,
                onScrimClick = {
                    viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Debug snackbar for development
        if (state.debugSnackbarMessage != null) {
            LaunchedEffect(state.debugSnackbarMessage) {
                // Auto-dismiss after 3 seconds
                kotlinx.coroutines.delay(3000)
                viewModel.dismissDebugSnackbar()
            }
            
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                )
            ) {
                Text(
                    text = state.debugSnackbarMessage!!,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        // Error handling
        if (state.locationError != null || state.friendsError != null || state.generalError != null) {
            ErrorCard(
                error = state.locationError ?: state.friendsError ?: state.generalError ?: "",
                onRetry = {
                    viewModel.onEvent(MapScreenEvent.OnRetry)
                },
                onDismiss = {
                    viewModel.onEvent(MapScreenEvent.OnErrorDismiss)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Google Maps composable with markers and interactions
 */
@Composable
private fun GoogleMapContent(
    state: MapScreenState,
    onEvent: (MapScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            state.mapCenter ?: LatLng(37.7749, -122.4194), // Default to San Francisco
            state.mapZoom
        )
    }
    
    // Update camera when map center changes
    LaunchedEffect(state.mapCenter, state.mapZoom) {
        state.mapCenter?.let { center ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(center, state.mapZoom),
                500
            )
        }
    }
    
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = state.hasLocationPermission,
            isTrafficEnabled = false
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false, // We use our own FAB
            mapToolbarEnabled = false,
            compassEnabled = true,
            zoomControlsEnabled = false
        ),
        onMapClick = { latLng ->
            onEvent(MapScreenEvent.OnMapClick(latLng))
        },
        onMapLongClick = { latLng ->
            onEvent(MapScreenEvent.OnMapLongClick(latLng))
        }
    ) {
        // User location marker (if available)
        state.currentLocation?.let { location ->
            Marker(
                state = MarkerState(position = location),
                title = "Your Location",
                snippet = if (state.isLocationSharingActive) "Live sharing active" else "Your current location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            )
        }
        
        // Enhanced friend markers with pulse halo for Very Close friends
        state.nearbyFriends.forEach { nearbyFriend ->
            nearbyFriend.latLng.let { position ->
                // Create enhanced marker for Very Close friends (< 300m)
                val isVeryClose = nearbyFriend.proximityBucket == NearbyFriend.ProximityBucket.VERY_CLOSE
                
                if (isVeryClose) {
                    // Use custom composable marker for Very Close friends with enhanced halo
                    MarkerComposable(
                        keys = arrayOf(nearbyFriend.id),
                        state = MarkerState(position = position)
                    ) {
                        VeryCloseHaloMarker(
                            nearbyFriend = nearbyFriend,
                            onClick = {
                                onEvent(MapScreenEvent.OnFriendMarkerClick(nearbyFriend.id))
                            }
                        )
                    }
                } else {
                    // Standard marker for other friends
                    Marker(
                        state = MarkerState(position = position),
                        title = nearbyFriend.displayName,
                        snippet = "${nearbyFriend.formattedDistance} • ${if (nearbyFriend.isOnline) "Online" else "Last seen recently"}",
                        onClick = { marker ->
                            onEvent(MapScreenEvent.OnFriendMarkerClick(nearbyFriend.id))
                            true // Consume the click
                        },
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (nearbyFriend.isOnline) BitmapDescriptorFactory.HUE_GREEN 
                            else BitmapDescriptorFactory.HUE_ORANGE
                        )
                    )
                }
            }
        }
    }
}

/**
 * Quick-Share FAB with purple/green ring animation
 */
@Composable
private fun QuickShareFAB(
    isSharing: Boolean,
    isWaitingForFix: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSharing) {
        Color(0xFF4CAF50) // Green when sharing
    } else {
        Color(0xFF9C27B0) // Purple when not sharing
    }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(64.dp)
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        containerColor = backgroundColor
    ) {
        // Use AnimatedPin for all states - animated when waiting or sharing
        AnimatedPin(
            modifier = Modifier.size(32.dp),
            tint = Color.White, // White tint on colored FAB background
            animated = isWaitingForFix || isSharing
        )
    }
}

/**
 * Location permission request screen
 */
@Composable
private fun LocationPermissionScreen(
    onRequestPermission: () -> Unit,
    shouldShowRationale: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Location Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (shouldShowRationale) {
                "FFinder needs location permission to show your location on the map and enable location sharing with friends."
            } else {
                "To use the map features, please grant location permission."
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Grant Permission")
        }
    }
}

/**
 * Error card for displaying errors with retry option
 */
@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    FFinderTheme {
        MapScreen(
            startSharing = false,
            onBackClick = { },
            onSearchFriendsClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenSharingPreview() {
    FFinderTheme {
        MapScreen(
            startSharing = true,
            onBackClick = { },
            onSearchFriendsClick = { }
        )
    }
}

/**
 * Enhanced marker with pulse halo animation for Very Close friends (< 300m)
 */
@Composable
private fun VeryCloseHaloMarker(
    nearbyFriend: NearbyFriend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Enhanced pulse animation for Very Close friends
    val infiniteTransition = rememberInfiniteTransition(label = "very_close_pulse")
    
    // Large halo pulse
    val largePulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000)
        ),
        label = "large_pulse_scale"
    )
    
    val largePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000)
        ),
        label = "large_pulse_alpha"
    )
    
    // Medium halo pulse
    val mediumPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500)
        ),
        label = "medium_pulse_scale"
    )
    
    val mediumPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500)
        ),
        label = "medium_pulse_alpha"
    )
    
    Box(
        modifier = modifier
            .size(120.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Large outer halo (for Very Close friends)
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(largePulseScale)
                .alpha(largePulseAlpha * 0.4f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Medium halo ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(mediumPulseScale)
                .alpha(mediumPulseAlpha * 0.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Friend avatar container
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(nearbyFriend.avatarUrl ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = "${nearbyFriend.displayName} - Very close (${nearbyFriend.formattedDistance})",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        // Online status indicator
        if (nearbyFriend.isOnline) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = 18.dp, y = (-18).dp)
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
}