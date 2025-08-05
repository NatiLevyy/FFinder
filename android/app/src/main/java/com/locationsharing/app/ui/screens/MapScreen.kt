package com.locationsharing.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.BottomSheetScaffold
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.MapStyleOptions
import com.locationsharing.app.R
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.ui.friends.FriendsMapViewModel
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.components.AnimatedFriendMarker
import com.locationsharing.app.ui.friends.components.EmptyFriendsState
import com.locationsharing.app.ui.friends.components.EnhancedEmptyMapState
import com.locationsharing.app.ui.friends.components.EnhancedMapMarkerManager
import com.locationsharing.app.ui.friends.components.FriendInfoBottomSheetContent
import com.locationsharing.app.ui.friends.components.FriendInfoCard
import com.locationsharing.app.ui.friends.components.FriendsPanelScaffold
import com.locationsharing.app.ui.friends.components.MapPerformanceMonitor
import com.locationsharing.app.ui.friends.components.FriendsListCarousel
import com.locationsharing.app.ui.friends.components.MapTransitionController
import com.locationsharing.app.ui.friends.components.CoordinatedMarkerAnimation
import com.locationsharing.app.ui.navigation.FFinderScreenTransitions
import com.locationsharing.app.ui.navigation.BreathingAnimation
import com.locationsharing.app.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Enhanced MapScreen with integrated Friends Nearby Panel
 * 
 * Task 11: Integrate components into MapScreen
 * Requirements: 1.1, 1.2, 5.1, 5.2
 * 
 * Features:
 * - Replaced existing MapScreen content with FriendsPanelScaffold
 * - Wired up ViewModel state and event handlers
 * - Added conditional rendering of FriendInfoBottomSheet
 * - Ensured proper state management between map and panel interactions
 * - Tested panel opening/closing with map interaction preservation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Friends map functionality
    val friendsViewModel: FriendsMapViewModel = hiltViewModel()
    val friendsUiState by friendsViewModel.uiState.collectAsState()
    val locationUpdates by friendsViewModel.locationUpdates.collectAsState()
    val selectedFriend by friendsViewModel.selectedFriend.collectAsState()
    
    // Nearby panel state
    val nearbyUiState by friendsViewModel.nearbyUiState.collectAsState()
    val nearbyFriends by friendsViewModel.nearbyFriends.collectAsState()
    
    // Enhanced map transition controller and marker manager
    val mapTransitionController = remember { MapTransitionController() }
    val markerManager = remember { EnhancedMapMarkerManager() }
    
    // Bottom sheet state for friend info
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    // Default location (San Francisco) if location is not available
    val defaultLocation = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }
    
    // Check location permission
    hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            Timber.d("Location permission granted in MapScreen")
        } else {
            Timber.w("Location permission denied in MapScreen")
            locationError = "Location permission is required to show your current location"
        }
    }
    
    // Function to get current location
    suspend fun getCurrentLocation(): LatLng? {
        return try {
            if (!hasLocationPermission) {
                locationError = "Location permission not granted"
                return null
            }
            
            val location: Location = fusedLocationClient.lastLocation.await()
                ?: throw Exception("Unable to get current location")
            
            LatLng(location.latitude, location.longitude)
        } catch (e: Exception) {
            Timber.e(e, "Error getting current location")
            locationError = "Failed to get current location: ${e.message}"
            null
        }
    }
    
    // Get location on first load
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            isLoading = true
            locationError = null
            
            val location = getCurrentLocation()
            if (location != null) {
                currentLocation = location
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 16f),
                    durationMs = 1000
                )
            }
            isLoading = false
        } else {
            isLoading = false
        }
    }
    
    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    // Wrap the entire screen with FriendsPanelScaffold
    FriendsPanelScaffold(
        uiState = nearbyUiState.copy(friends = nearbyFriends),
        onEvent = { event ->
            when (event) {
                is NearbyPanelEvent.Navigate -> {
                    friendsViewModel.navigateToFriend(event.friendId) { intent ->
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to open navigation: ${e.message}")
                            }
                        }
                    }
                }
                is NearbyPanelEvent.Message -> {
                    friendsViewModel.messageFriend(event.friendId) { intent ->
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to open messaging: ${e.message}")
                            }
                        }
                    }
                }
                is NearbyPanelEvent.FriendClick -> {
                    friendsViewModel.focusOnFriend(event.friendId) { latLng, zoom ->
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latLng, zoom),
                                durationMs = 1200
                            )
                        }
                    }
                }
                else -> friendsViewModel.onNearbyPanelEvent(event)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        // Main map content with conditional bottom sheet
        if (nearbyUiState.selectedFriend != null) {
            BottomSheetScaffold(
                scaffoldState = bottomSheetScaffoldState,
                sheetContent = {
                    nearbyUiState.selectedFriend?.let { friend ->
                        FriendInfoBottomSheetContent(
                            friend = friend,
                            onEvent = { event ->
                                when (event) {
                                    is NearbyPanelEvent.Navigate -> {
                                        friendsViewModel.navigateToFriend(event.friendId) { intent ->
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to open navigation: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                    is NearbyPanelEvent.Message -> {
                                        friendsViewModel.messageFriend(event.friendId) { intent ->
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to open messaging: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                    else -> friendsViewModel.onNearbyPanelEvent(event)
                                }
                            }
                        )
                    }
                },
                sheetPeekHeight = 0.dp,
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                MapContent(
                    paddingValues = paddingValues,
                    friendsUiState = friendsUiState,
                    selectedFriend = selectedFriend,
                    locationUpdates = locationUpdates,
                    cameraPositionState = cameraPositionState,
                    currentLocation = currentLocation,
                    isLoading = isLoading,
                    hasLocationPermission = hasLocationPermission,
                    locationError = locationError,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onBackClick,
                    friendsViewModel = friendsViewModel,
                    mapTransitionController = mapTransitionController,
                    markerManager = markerManager,
                    getCurrentLocation = ::getCurrentLocation,
                    scope = scope,
                    context = context
                )
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                "Your Location",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back to home"
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
                    if (hasLocationPermission && currentLocation != null) {
                        // Enhanced FAB with breathing animation for location sharing
                        BreathingAnimation(isActive = true) { breathingScale ->
                            FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val location = getCurrentLocation()
                                        if (location != null) {
                                            currentLocation = location
                                            // Enhanced camera animation with smooth transition
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(location, 16f),
                                                durationMs = 1200
                                            )
                                            snackbarHostState.showSnackbar("Location updated!")
                                        }
                                        isLoading = false
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .scale(breathingScale)
                                    .shadow(
                                        elevation = (8 + (breathingScale - 1f) * 4).dp,
                                        shape = CircleShape,
                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Center on my location",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                MapContent(
                    paddingValues = paddingValues,
                    friendsUiState = friendsUiState,
                    selectedFriend = selectedFriend,
                    locationUpdates = locationUpdates,
                    cameraPositionState = cameraPositionState,
                    currentLocation = currentLocation,
                    isLoading = isLoading,
                    hasLocationPermission = hasLocationPermission,
                    locationError = locationError,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onBackClick,
                    friendsViewModel = friendsViewModel,
                    mapTransitionController = mapTransitionController,
                    markerManager = markerManager,
                    getCurrentLocation = ::getCurrentLocation,
                    scope = scope,
                    context = context
                )
            }
        }
    }
    
    // Show error snackbar for location errors
    LaunchedEffect(locationError) {
        locationError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }
    
    // Show error snackbar for friends errors
    LaunchedEffect(friendsUiState.error) {
        friendsUiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            friendsViewModel.clearError()
        }
    }
    
    // Show error snackbar for nearby panel errors
    LaunchedEffect(nearbyUiState.error) {
        nearbyUiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            friendsViewModel.onNearbyPanelEvent(NearbyPanelEvent.ClearError)
        }
    }
    
    // Show feedback messages from nearby panel
    LaunchedEffect(nearbyUiState.feedbackMessage) {
        nearbyUiState.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            friendsViewModel.onNearbyPanelEvent(NearbyPanelEvent.ClearFeedback)
        }
    }
}

/**
 * Extracted map content composable for better organization and reusability
 */
@Composable
private fun MapContent(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    friendsUiState: com.locationsharing.app.ui.friends.FriendsMapUiState,
    selectedFriend: Friend?,
    locationUpdates: List<com.locationsharing.app.data.friends.FriendUpdateWithAnimation>,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    currentLocation: LatLng?,
    isLoading: Boolean,
    hasLocationPermission: Boolean,
    locationError: String?,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    friendsViewModel: FriendsMapViewModel,
    mapTransitionController: MapTransitionController,
    markerManager: EnhancedMapMarkerManager,
    getCurrentLocation: suspend () -> LatLng?,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Friends list carousel at the top
        if (friendsUiState.hasAnyFriends) {
            FriendsListCarousel(
                friends = friendsUiState.friends,
                selectedFriendId = friendsUiState.selectedFriend?.id,
                onFriendClick = { friendId ->
                    friendsViewModel.selectFriend(friendId)
                    // Enhanced camera transition to selected friend
                    friendsViewModel.getFriendById(friendId)?.let { friend ->
                        scope.launch {
                            mapTransitionController.focusOnFriend(
                                friend = friend,
                                cameraPositionState = cameraPositionState,
                                zoomLevel = 16f,
                                duration = 1200
                            )
                        }
                    }
                },
                onInviteFriendsClick = {
                    // TODO: Navigate to invite friends screen
                    scope.launch {
                        snackbarHostState.showSnackbar("Invite friends feature coming soon!")
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        } else {
            // Enhanced empty state with delightful animations
            EnhancedEmptyMapState(
                onInviteFriendsClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Invite friends feature coming soon!")
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
        
        // Enhanced Google Map with FFinder branding
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = hasLocationPermission,
                mapStyleOptions = com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                    context, 
                    com.locationsharing.app.R.raw.map_style
                )
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = true,
                zoomGesturesEnabled = true,
                mapToolbarEnabled = false,
                indoorLevelPickerEnabled = false
            )
        ) {
            // Current location marker
            currentLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "You are here",
                    snippet = "Your current location"
                )
            }
            
            // Enhanced markers with clustering and smooth animations
            markerManager.RenderEnhancedMarkers(
                friends = friendsUiState.friends,
                selectedFriendId = friendsUiState.selectedFriend?.id,
                cameraPositionState = cameraPositionState,
                onMarkerClick = { friendId ->
                    Timber.d("📍 MapScreen - Marker clicked for friend ID: $friendId")
                    
                    val friend = friendsViewModel.getFriendById(friendId)
                    if (friend != null) {
                        Timber.d("📍 Found friend: ${friend.name}")
                        friendsViewModel.selectFriend(friendId)
                        
                        scope.launch {
                            try {
                                markerManager.focusOnFriend(
                                    friend = friend,
                                    cameraPositionState = cameraPositionState,
                                    zoomLevel = 17f,
                                    duration = 1400
                                )
                                Timber.d("📍 Camera focused on ${friend.name}")
                            } catch (e: Exception) {
                                Timber.e(e, "📍 Error focusing camera on friend")
                            }
                        }
                    } else {
                        Timber.w("📍 Friend not found for ID: $friendId")
                    }
                },
                onClusterClick = { clusteredFriends ->
                    scope.launch {
                        markerManager.focusOnFriends(
                            friends = clusteredFriends,
                            cameraPositionState = cameraPositionState,
                            padding = 150
                        )
                        snackbarHostState.showSnackbar("${clusteredFriends.size} friends in this area")
                    }
                }
            )
        }
        
        // Loading indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Getting your location...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Error message
        locationError?.let { error ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // Location info card
        AnimatedVisibility(
            visible = currentLocation != null && !isLoading,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Location Sharing Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your friends can see your location",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    currentLocation?.let { location ->
                        Text(
                            text = "Lat: ${String.format("%.6f", location.latitude)}, " +
                                   "Lng: ${String.format("%.6f", location.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
        
        // Enhanced friend info card at the bottom
        FriendInfoCard(
            friend = selectedFriend,
            isVisible = selectedFriend != null,
            onDismiss = { friendsViewModel.clearFriendSelection() },
            onMessageClick = { friendId ->
                scope.launch {
                    snackbarHostState.showSnackbar("Message feature coming soon!")
                }
            },
            onNotifyClick = { friendId ->
                scope.launch {
                    snackbarHostState.showSnackbar("Notification feature coming soon!")
                }
            },
            onMoreClick = { friendId ->
                scope.launch {
                    snackbarHostState.showSnackbar("More options coming soon!")
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}