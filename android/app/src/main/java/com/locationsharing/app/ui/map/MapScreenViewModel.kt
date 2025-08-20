package com.locationsharing.app.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import com.locationsharing.app.domain.usecase.ShareLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for MapScreen with proper state handling and lifecycle management
 * Implements requirements 2.2, 2.3, 7.1, 7.2, 7.3
 */
@HiltViewModel
class MapScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationService: EnhancedLocationService,
    private val friendsRepository: FriendsRepository,
    private val realTimeFriendsService: RealTimeFriendsService,
    private val getNearbyFriendsUseCase: GetNearbyFriendsUseCase,
    private val shareLocationUseCase: ShareLocationUseCase,
    private val locationSharingService: com.locationsharing.app.data.location.LocationSharingService,
    val navigationManager: com.locationsharing.app.navigation.NavigationManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(MapScreenState())
    val state: StateFlow<MapScreenState> = _state.asStateFlow()
    
    // Convenience StateFlows for UI components as required
    val isSharing: StateFlow<Boolean> = state.map { it.isLocationSharingActive }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    val userLocation: StateFlow<LatLng?> = state.map { it.currentLocation }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val nearbyFriends: StateFlow<List<com.locationsharing.app.domain.model.NearbyFriend>> = state.map { it.nearbyFriends }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private var locationUpdatesJob: Job? = null
    private var friendsUpdatesJob: Job? = null
    private var nearbyFriendsJob: Job? = null
    private var retryJob: Job? = null
    private var locationSharingJob: Job? = null
    
    init {
        initializeViewModel()
        observeLocationSharingState()
        startNearbyFriendsUpdates()
    }
    
    /**
     * Initialize the ViewModel and start necessary services
     */
    private fun initializeViewModel() {
        Timber.d("Initializing MapScreenViewModel")
        
        // Check initial location permission
        checkLocationPermission()
        
        // Set debug mode based on build config
        _state.value = _state.value.copy(isDebugMode = BuildConfig.DEBUG)
        
        // Start location updates if permission is granted
        if (_state.value.hasLocationPermission) {
            startLocationUpdates()
        }
        
        // Start friends updates
        startFriendsUpdates()
    }
    
    /**
     * Observe location sharing state and sync with UI state
     */
    private fun observeLocationSharingState() {
        locationSharingJob = viewModelScope.launch {
            locationSharingService.sharingState.collect { sharingState ->
                _state.value = _state.value.copy(
                    isLocationSharingActive = sharingState.status == com.locationsharing.app.data.location.LocationSharingStatus.ACTIVE,
                    locationSharingError = sharingState.error
                )
            }
        }
        
        // Observe notifications
        viewModelScope.launch {
            locationSharingService.notifications.collect { notification ->
                notification?.let {
                    // Handle notification display (could trigger snackbar, etc.)
                    Timber.d("Location sharing notification: ${it.message}")
                }
            }
        }
    }
    
    /**
     * Handle user events
     */
    fun onEvent(event: MapScreenEvent) {
        when (event) {
            // Navigation events
            is MapScreenEvent.OnBackPressed -> handleBackPressed()
            is MapScreenEvent.OnNearbyFriendsToggle -> handleNearbyFriendsToggle()
            is MapScreenEvent.OnSearchFriendsClick -> handleSearchFriendsClick()
            
            // Location events
            is MapScreenEvent.OnSelfLocationCenter -> handleSelfLocationCenter()
            is MapScreenEvent.OnLocationPermissionRequested -> handleLocationPermissionRequested()
            is MapScreenEvent.OnLocationPermissionGranted -> handleLocationPermissionGranted()
            is MapScreenEvent.OnLocationPermissionDenied -> handleLocationPermissionDenied()
            is MapScreenEvent.OnLocationUpdated -> handleLocationUpdated(event.location)
            
            // Location sharing events
            is MapScreenEvent.OnQuickShare -> handleQuickShare()
            is MapScreenEvent.OnStartLocationSharing -> handleStartLocationSharing()
            is MapScreenEvent.OnStopLocationSharing -> handleStopLocationSharing()
            is MapScreenEvent.OnStatusSheetDismiss -> handleStatusSheetDismiss()
            is MapScreenEvent.OnStatusSheetShow -> handleStatusSheetShow()
            
            // Friend interaction events
            is MapScreenEvent.OnFriendMarkerClick -> handleFriendMarkerClick(event.friendId)
            is MapScreenEvent.OnClusterClick -> handleClusterClick(event.friends)
            is MapScreenEvent.OnFriendSelectionClear -> handleFriendSelectionClear()
            is MapScreenEvent.OnFriendSearch -> handleFriendSearch(event.query)
            is MapScreenEvent.OnFriendSelectedFromSearch -> handleFriendSelectedFromSearch(event.friendId)
            
            // Map interaction events
            is MapScreenEvent.OnMapClick -> handleMapClick(event.location)
            is MapScreenEvent.OnMapLongClick -> handleMapLongClick(event.location)
            is MapScreenEvent.OnCameraMove -> handleCameraMove(event.center, event.zoom)
            
            // Debug events
            is MapScreenEvent.OnDebugAddFriends -> handleDebugAddFriends()
            is MapScreenEvent.OnDebugClearFriends -> handleDebugClearFriends()
            is MapScreenEvent.OnDebugToggleHighAccuracy -> handleDebugToggleHighAccuracy()
            
            // Error handling events
            is MapScreenEvent.OnRetry -> handleRetry()
            is MapScreenEvent.OnErrorDismiss -> handleErrorDismiss()
            
            // Drawer events
            is MapScreenEvent.OnDrawerOpen -> handleDrawerOpen()
            is MapScreenEvent.OnDrawerClose -> handleDrawerClose()
            is MapScreenEvent.OnDrawerDismiss -> handleDrawerDismiss()
            
            // Performance events
            is MapScreenEvent.OnEnableHighAccuracyMode -> handleEnableHighAccuracyMode()
            is MapScreenEvent.OnDisableHighAccuracyMode -> handleDisableHighAccuracyMode()
            is MapScreenEvent.OnBatteryLevelChanged -> handleBatteryLevelChanged(event.level)
            
            // Lifecycle events
            is MapScreenEvent.OnScreenResume -> handleScreenResume()
            is MapScreenEvent.OnScreenPause -> handleScreenPause()
            is MapScreenEvent.OnScreenDestroy -> handleScreenDestroy()
            
            // Settings events
            is MapScreenEvent.OnOpenSettings -> handleOpenSettings()
            is MapScreenEvent.OnRefreshData -> handleRefreshData()
            
            // Animation events
            is MapScreenEvent.OnAnimationComplete -> handleAnimationComplete()
            is MapScreenEvent.OnFABAnimationStart -> handleFABAnimationStart(event.fabType)
            is MapScreenEvent.OnFABAnimationEnd -> handleFABAnimationEnd(event.fabType)
        }
    }
    
    // Navigation event handlers
    private fun handleBackPressed() {
        Timber.d("Back button pressed")
        // Navigation will be handled by the composable
    }
    
    private fun handleNearbyFriendsToggle() {
        Timber.d("Nearby friends toggle pressed")
        _state.value = _state.value.copy(
            isNearbyDrawerOpen = !_state.value.isNearbyDrawerOpen
        )
    }
    
    private fun handleSearchFriendsClick() {
        Timber.d("Search friends button pressed")
        _state.value = _state.value.copy(shouldNavigateToSearchFriends = true)
    }
    
    fun onSearchFriendsNavigationHandled() {
        _state.value = _state.value.copy(shouldNavigateToSearchFriends = false)
    }
    
    // Location event handlers
    private fun handleSelfLocationCenter() {
        Timber.d("📍 Self location center pressed")
        centerOnUser()
    }
    
    /**
     * Center the map on user's current location
     * Implements requirement for Self-Location FAB functionality
     */
    fun centerOnUser() {
        Timber.d("📍 Center on user called")
        
        if (!_state.value.hasLocationPermission) {
            Timber.w("📍 Center on user: No location permission")
            handleLocationPermissionRequested()
            return
        }
        
        if (_state.value.currentLocation == null) {
            Timber.w("📍 Center on user: No current location available, triggering location update")
            _state.value = _state.value.copy(
                debugSnackbarMessage = "📍 Waiting for GPS...",
                isLocationLoading = true
            )
            // Trigger a single location update
            startLocationUpdates()
            return
        }
        
        // Update map center to current location with animation
        _state.value.currentLocation?.let { location ->
            Timber.d("📍 Center on user: Centering on ${location.latitude}, ${location.longitude}")
            _state.value = _state.value.copy(
                mapCenter = location,
                mapZoom = 16f,
                debugSnackbarMessage = "📍 Centered on your location!"
            )
        }
    }
    
    private fun handleLocationPermissionRequested() {
        Timber.d("Location permission requested")
        _state.value = _state.value.copy(
            isLocationPermissionRequested = true,
            locationError = null
        )
    }
    
    private fun handleLocationPermissionGranted() {
        Timber.d("Location permission granted")
        _state.value = _state.value.copy(
            hasLocationPermission = true,
            isLocationPermissionRequested = false,
            locationError = null
        )
        startLocationUpdates()
    }
    
    private fun handleLocationPermissionDenied() {
        Timber.d("Location permission denied")
        _state.value = _state.value.copy(
            hasLocationPermission = false,
            isLocationPermissionRequested = false,
            locationError = "Location permission is required to show your location on the map",
            isLocationLoading = false
        )
    }
    
    private fun handleLocationUpdated(location: LatLng) {
        Timber.d("Location updated: $location")
        _state.value = _state.value.copy(
            currentLocation = location,
            isLocationLoading = false,
            locationError = null,
            mapCenter = location // Update map center to new location
        )
    }
    
    // Location sharing event handlers
    private fun handleQuickShare() {
        Timber.d("📍 Quick share pressed - toggling live location sharing")
        toggleLiveSharing()
    }
    
    /**
     * Toggle live location sharing on/off
     * Implements requirement for Quick-Share FAB functionality
     */
    fun toggleLiveSharing() {
        Timber.d("📍 Toggle live sharing called")
        
        if (!_state.value.hasLocationPermission) {
            Timber.w("📍 Toggle live sharing: No location permission")
            handleLocationPermissionRequested()
            return
        }
        
        viewModelScope.launch {
            try {
                Timber.d("📍 Toggle live sharing: Calling locationSharingService.toggleLocationSharing()")
                val result = locationSharingService.toggleLocationSharing()
                
                if (result.isSuccess) {
                    val isActive = locationSharingService.isLocationSharingActive()
                    Timber.d("📍 Toggle live sharing: Success, sharing active: $isActive")
                    
                    _state.value = _state.value.copy(
                        isLocationSharingActive = isActive,
                        isStatusSheetVisible = isActive, // Auto-open sheet when sharing starts
                        debugSnackbarMessage = if (isActive) "📍 Live sharing started!" else "📍 Live sharing stopped!"
                    )
                    
                    if (isActive) {
                        // Start location updates for sharing
                        startLocationUpdates()
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "📍 Toggle live sharing: Failed")
                    _state.value = _state.value.copy(
                        locationError = "Failed to toggle location sharing: ${error?.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "📍 Toggle live sharing: Exception")
                _state.value = _state.value.copy(
                    locationError = "Failed to toggle location sharing: ${e.message}"
                )
            }
        }
    }
    
    private fun handleStartLocationSharing() {
        Timber.d("Starting location sharing")
        
        if (!_state.value.hasLocationPermission) {
            handleLocationPermissionRequested()
            return
        }
        
        viewModelScope.launch {
            val result = locationSharingService.startLocationSharing()
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isStatusSheetVisible = true,
                    locationSharingError = null
                )
                Timber.d("Location sharing started successfully")
            } else {
                val error = result.exceptionOrNull()
                Timber.e(error, "Failed to start location sharing")
                _state.value = _state.value.copy(
                    locationSharingError = error?.message ?: "Failed to start location sharing"
                )
            }
        }
    }
    
    private fun handleStopLocationSharing() {
        Timber.d("Stopping location sharing")
        
        viewModelScope.launch {
            val result = locationSharingService.stopLocationSharing()
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isStatusSheetVisible = false,
                    locationSharingError = null
                )
                Timber.d("Location sharing stopped successfully")
            } else {
                val error = result.exceptionOrNull()
                Timber.e(error, "Failed to stop location sharing")
                _state.value = _state.value.copy(
                    locationSharingError = error?.message ?: "Failed to stop location sharing"
                )
            }
        }
    }
    
    private fun handleStatusSheetShow() {
        Timber.d("Showing status sheet")
        _state.value = _state.value.copy(isStatusSheetVisible = true)
    }
    
    private fun handleStatusSheetDismiss() {
        Timber.d("Dismissing status sheet")
        _state.value = _state.value.copy(isStatusSheetVisible = false)
    }
    
    // Friend interaction event handlers
    private fun handleFriendMarkerClick(friendId: String) {
        Timber.d("Friend marker clicked: $friendId")
        _state.value = _state.value.copy(selectedFriendId = friendId)
        
        // Center map on selected friend
        val friend = _state.value.friends.find { it.id == friendId }
        friend?.getLatLng()?.let { location ->
            _state.value = _state.value.copy(
                mapCenter = location,
                mapZoom = MapScreenConstants.Map.CLOSE_ZOOM
            )
        }
    }
    
    private fun handleClusterClick(friends: List<Friend>) {
        Timber.d("Cluster clicked with ${friends.size} friends")
        
        // Focus camera on the cluster bounds to show all friends
        viewModelScope.launch {
            val locations = friends.mapNotNull { it.getLatLng() }
            if (locations.isNotEmpty()) {
                // Calculate bounds for all friends in cluster
                val minLat = locations.minOf { it.latitude }
                val maxLat = locations.maxOf { it.latitude }
                val minLng = locations.minOf { it.longitude }
                val maxLng = locations.maxOf { it.longitude }
                
                val centerLat = (minLat + maxLat) / 2
                val centerLng = (minLng + maxLng) / 2
                val center = LatLng(centerLat, centerLng)
                
                // Calculate appropriate zoom level to show all friends
                val latSpan = maxLat - minLat
                val lngSpan = maxLng - minLng
                val maxSpan = maxOf(latSpan, lngSpan)
                
                val zoomLevel = when {
                    maxSpan > 0.1 -> 10f
                    maxSpan > 0.05 -> 12f
                    maxSpan > 0.01 -> 14f
                    maxSpan > 0.005 -> 16f
                    else -> 18f
                }
                
                _state.value = _state.value.copy(
                    mapCenter = center,
                    mapZoom = zoomLevel
                )
            }
        }
    }
    
    private fun handleFriendSelectionClear() {
        Timber.d("Clearing friend selection")
        _state.value = _state.value.copy(selectedFriendId = null)
    }
    
    private fun handleFriendSearch(query: String) {
        Timber.d("Friend search: $query")
        // Search functionality will be implemented in later tasks
    }
    
    private fun handleFriendSelectedFromSearch(friendId: String) {
        Timber.d("Friend selected from search: $friendId")
        
        // Find the friend in the current friends list
        val friend = _state.value.friends.find { it.id == friendId }
        if (friend == null) {
            Timber.w("Friend not found for selection: $friendId")
            _state.value = _state.value.copy(generalError = "Friend not found")
            return
        }
        
        // Center map on selected friend with close zoom
        friend.getLatLng()?.let { location ->
            _state.value = _state.value.copy(
                mapCenter = location,
                mapZoom = MapScreenConstants.Map.CLOSE_ZOOM,
                selectedFriendId = friendId
            )
        } ?: run {
            Timber.w("Friend location not available: $friendId")
            _state.value = _state.value.copy(generalError = "Friend's location is not available")
        }
    }
    
    // Map interaction event handlers
    private fun handleMapClick(location: LatLng) {
        Timber.d("Map clicked at: $location")
        // Clear friend selection on map click
        handleFriendSelectionClear()
    }
    
    private fun handleMapLongClick(location: LatLng) {
        Timber.d("Map long clicked at: $location")
        // Long click functionality can be added later
    }
    
    private fun handleCameraMove(center: LatLng, zoom: Float) {
        _state.value = _state.value.copy(
            mapCenter = center,
            mapZoom = zoom
        )
    }
    
    // Debug event handlers
    private fun handleDebugAddFriends() {
        if (!BuildConfig.DEBUG) return
        
        Timber.d("Adding debug friends")
        addTestFriendsOnMap()
    }
    
    /**
     * Add test friends to the map for debugging purposes
     * Implements requirement 4.2 and 4.3
     */
    fun addTestFriendsOnMap() {
        if (!BuildConfig.DEBUG) return
        
        viewModelScope.launch {
            try {
                val debugFriends = createDebugFriends()
                val debugNearbyFriends = createDebugNearbyFriends()
                
                _state.value = _state.value.copy(
                    friends = _state.value.friends + debugFriends,
                    nearbyFriends = _state.value.nearbyFriends + debugNearbyFriends,
                    nearbyFriendsCount = _state.value.nearbyFriendsCount + debugFriends.size,
                    debugSnackbarMessage = "🧪 Debug: Added ${debugFriends.size} test friends to map!" // Requirement 4.3
                )
                Timber.d("Added ${debugFriends.size} debug friends and ${debugNearbyFriends.size} nearby friends")
            } catch (e: Exception) {
                Timber.e(e, "Failed to add debug friends")
                _state.value = _state.value.copy(
                    debugSnackbarMessage = "❌ Debug: Failed to add test friends"
                )
            }
        }
    }
    
    private fun handleDebugClearFriends() {
        if (!BuildConfig.DEBUG) return
        
        Timber.d("Clearing debug friends")
        _state.value = _state.value.copy(
            friends = emptyList(),
            nearbyFriendsCount = 0,
            selectedFriendId = null
        )
    }
    
    private fun handleDebugToggleHighAccuracy() {
        if (!BuildConfig.DEBUG) return
        
        Timber.d("Toggling high accuracy mode")
        val newMode = !_state.value.isHighAccuracyMode
        _state.value = _state.value.copy(isHighAccuracyMode = newMode)
        locationService.enableHighAccuracyMode(newMode)
    }
    
    // Error handling event handlers
    private fun handleRetry() {
        Timber.d("Retrying failed operations")
        
        if (!_state.value.canRetry) return
        
        _state.value = _state.value.copy(
            isRetrying = true,
            retryCount = _state.value.retryCount + 1
        )
        
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            delay(MapScreenConstants.States.RETRY_DELAY)
            
            try {
                // Retry location updates if needed
                if (_state.value.locationError != null && _state.value.hasLocationPermission) {
                    startLocationUpdates()
                }
                
                // Retry friends updates if needed
                if (_state.value.friendsError != null) {
                    startFriendsUpdates()
                }
                
                // Retry location sharing if needed
                if (_state.value.locationSharingError != null) {
                    locationSharingService.retryLocationSharing()
                }
                
                _state.value = _state.value.copy(isRetrying = false)
            } catch (e: Exception) {
                Timber.e(e, "Retry failed")
                _state.value = _state.value.copy(
                    isRetrying = false,
                    generalError = "Retry failed: ${e.message}"
                )
            }
        }
    }
    
    private fun handleErrorDismiss() {
        Timber.d("Dismissing errors")
        _state.value = _state.value.copy(
            locationError = null,
            friendsError = null,
            locationSharingError = null,
            generalError = null
        )
    }
    
    /**
     * Dismiss debug snackbar message
     */
    fun dismissDebugSnackbar() {
        _state.value = _state.value.copy(debugSnackbarMessage = null)
    }
    
    // Drawer event handlers
    private fun handleDrawerOpen() {
        Timber.d("Opening drawer")
        _state.value = _state.value.copy(isNearbyDrawerOpen = true)
    }
    
    private fun handleDrawerClose() {
        Timber.d("Closing drawer")
        _state.value = _state.value.copy(isNearbyDrawerOpen = false)
    }
    
    private fun handleDrawerDismiss() {
        Timber.d("Dismissing drawer")
        _state.value = _state.value.copy(isNearbyDrawerOpen = false)
    }
    
    // Performance event handlers
    private fun handleEnableHighAccuracyMode() {
        Timber.d("Enabling high accuracy mode")
        _state.value = _state.value.copy(isHighAccuracyMode = true)
        locationService.enableHighAccuracyMode(true)
    }
    
    private fun handleDisableHighAccuracyMode() {
        Timber.d("Disabling high accuracy mode")
        _state.value = _state.value.copy(isHighAccuracyMode = false)
        locationService.enableHighAccuracyMode(false)
    }
    
    private fun handleBatteryLevelChanged(level: Int) {
        _state.value = _state.value.copy(batteryLevel = level)
    }
    
    // Lifecycle event handlers
    private fun handleScreenResume() {
        Timber.d("Screen resumed")
        if (_state.value.hasLocationPermission && locationUpdatesJob?.isActive != true) {
            startLocationUpdates()
        }
    }
    
    private fun handleScreenPause() {
        Timber.d("Screen paused")
        // Keep location updates running for background sharing
    }
    
    private fun handleScreenDestroy() {
        Timber.d("Screen destroyed")
        stopAllUpdates()
    }
    
    // Settings event handlers
    private fun handleOpenSettings() {
        Timber.d("Opening settings")
        // Settings navigation will be handled by the composable
    }
    
    private fun handleRefreshData() {
        Timber.d("Refreshing data")
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLocationLoading = true,
                isFriendsLoading = true
            )
            
            // Restart location updates
            if (_state.value.hasLocationPermission) {
                startLocationUpdates()
            }
            
            // Restart friends updates
            startFriendsUpdates()
        }
    }
    
    // Animation event handlers
    private fun handleAnimationComplete() {
        // Animation completion handling
    }
    
    private fun handleFABAnimationStart(fabType: FABType) {
        Timber.d("FAB animation started: $fabType")
    }
    
    private fun handleFABAnimationEnd(fabType: FABType) {
        Timber.d("FAB animation ended: $fabType")
    }
    
    // Private helper methods
    
    /**
     * Check location permission status
     */
    private fun checkLocationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _state.value = _state.value.copy(hasLocationPermission = hasPermission)
        Timber.d("Location permission status: $hasPermission")
    }
    
    /**
     * Start location updates
     */
    private fun startLocationUpdates() {
        if (!_state.value.hasLocationPermission) {
            Timber.w("Cannot start location updates without permission")
            return
        }
        
        locationUpdatesJob?.cancel()
        locationUpdatesJob = locationService.getLocationUpdates()
            .onEach { locationUpdate ->
                handleLocationUpdated(locationUpdate.newLocation)
            }
            .catch { exception ->
                Timber.e(exception, "Location updates error")
                _state.value = _state.value.copy(
                    isLocationLoading = false,
                    locationError = "Failed to get location updates: ${exception.message}"
                )
            }
            .launchIn(viewModelScope)
        
        _state.value = _state.value.copy(isLocationLoading = true)
        Timber.d("Location updates started")
    }
    
    /**
     * Start friends updates with real-time synchronization
     */
    private fun startFriendsUpdates() {
        friendsUpdatesJob?.cancel()
        
        // Start real-time friends service
        realTimeFriendsService.startSync()
        
        friendsUpdatesJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isFriendsLoading = true)
                
                // Collect real-time friend updates with animations
                realTimeFriendsService.getFriendUpdatesWithAnimations()
                    .catch { exception ->
                        Timber.e(exception, "Real-time friends updates error")
                        _state.value = _state.value.copy(
                            isFriendsLoading = false,
                            friendsError = "Failed to load friends: ${exception.message}"
                        )
                    }
                    .collect { friendUpdates ->
                        // Process animated friend updates
                        val friends = friendUpdates.map { it.friend }
                        
                        _state.value = _state.value.copy(
                            friends = friends,
                            nearbyFriendsCount = friends.size,
                            isFriendsLoading = false,
                            friendsError = null
                        )
                        
                        Timber.d("Loaded ${friends.size} friends with real-time updates")
                        
                        // Log animation types for debugging
                        friendUpdates.forEach { update ->
                            Timber.d("Friend ${update.friend.name}: ${update.updateType} with ${update.animationType}")
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start real-time friends updates")
                _state.value = _state.value.copy(
                    isFriendsLoading = false,
                    friendsError = "Failed to load friends: ${e.message}"
                )
            }
        }
        
        Timber.d("Real-time friends updates started")
    }
    
    /**
     * Start nearby friends updates with distance calculation
     */
    private fun startNearbyFriendsUpdates() {
        nearbyFriendsJob?.cancel()
        
        nearbyFriendsJob = getNearbyFriendsUseCase()
            .catch { exception ->
                Timber.e(exception, "Nearby friends updates error")
                // Don't update state for nearby friends errors as it's secondary functionality
            }
            .onEach { nearbyFriendsList ->
                // GetNearbyFriendsUseCase already sorts by distance on background thread
                _state.value = _state.value.copy(
                    nearbyFriends = nearbyFriendsList,
                    nearbyFriendsCount = nearbyFriendsList.size
                )
                
                Timber.d("Updated ${nearbyFriendsList.size} nearby friends")
            }
            .launchIn(viewModelScope)
            
        Timber.d("Nearby friends updates started")
    }
    
    /**
     * Request a single location update
     */
    private fun requestLocationUpdate() {
        Timber.d("Requesting single location update")
        
        viewModelScope.launch {
            try {
                locationService.getCurrentLocation()
                    .catch { exception ->
                        Timber.e(exception, "Failed to get current location")
                        _state.value = _state.value.copy(
                            isLocationLoading = false,
                            locationError = "Failed to get current location: ${exception.message}"
                        )
                    }
                    .collect { location ->
                        if (location != null) {
                            handleLocationUpdated(location)
                        } else {
                            _state.value = _state.value.copy(
                                isLocationLoading = false,
                                locationError = "Unable to determine your current location. Please check that location services are enabled."
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Exception requesting location update")
                _state.value = _state.value.copy(
                    isLocationLoading = false,
                    locationError = "Location request failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Create debug friends for testing
     */
    private fun createDebugFriends(): List<Friend> {
        val baseLocation = _state.value.currentLocation ?: LatLng(37.7749, -122.4194)
        
        return listOf(
            createDebugFriend("debug_1", "Alice Johnson", baseLocation, 0.001, 0.001),
            createDebugFriend("debug_2", "Bob Smith", baseLocation, -0.001, 0.001),
            createDebugFriend("debug_3", "Carol Davis", baseLocation, 0.001, -0.001),
            createDebugFriend("debug_4", "David Wilson", baseLocation, -0.001, -0.001),
            createDebugFriend("debug_5", "Eve Brown", baseLocation, 0.002, 0.0)
        )
    }
    
    /**
     * Create a single debug friend
     */
    private fun createDebugFriend(
        id: String,
        name: String,
        baseLocation: LatLng,
        latOffset: Double,
        lngOffset: Double
    ): Friend {
        return Friend(
            id = id,
            userId = "debug_user_$id",
            name = name,
            email = "${name.lowercase().replace(" ", ".")}@example.com",
            avatarUrl = "",
            profileColor = "#${(0..5).random()}${(0..5).random()}${(0..5).random()}${(0..5).random()}${(0..5).random()}${(0..5).random()}",
            location = com.locationsharing.app.data.friends.FriendLocation(
                latitude = baseLocation.latitude + latOffset,
                longitude = baseLocation.longitude + lngOffset,
                accuracy = 10f,
                isMoving = (0..1).random() == 1,
                timestamp = java.util.Date()
            ),
            status = com.locationsharing.app.data.friends.FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
    
    /**
     * Create debug nearby friends for the drawer
     */
    private fun createDebugNearbyFriends(): List<com.locationsharing.app.domain.model.NearbyFriend> {
        val baseLocation = _state.value.currentLocation ?: LatLng(37.7749, -122.4194)
        val currentTime = System.currentTimeMillis()
        
        // Create diverse debug friends with different proximity buckets
        return listOf(
            // Very Close friend (< 300m) - will show enhanced halo
            com.locationsharing.app.domain.model.NearbyFriend(
                id = "nearby_1",
                displayName = "Alice Johnson (Very Close)",
                avatarUrl = null,
                distance = 150.0, // Very Close < 300m
                isOnline = true,
                lastUpdated = currentTime,
                latLng = LatLng(baseLocation.latitude + 0.001, baseLocation.longitude + 0.001),
                smartRankingScore = 0.15f // High priority (low score)
            ),
            // Another Very Close friend for testing halo animation
            com.locationsharing.app.domain.model.NearbyFriend(
                id = "nearby_2",
                displayName = "Bob Smith (Very Close)",
                avatarUrl = null,
                distance = 280.0, // Very Close < 300m
                isOnline = true,
                lastUpdated = currentTime - 30000, // 30 seconds ago
                latLng = LatLng(baseLocation.latitude - 0.001, baseLocation.longitude + 0.001),
                smartRankingScore = 0.18f // Slightly lower priority
            ),
            // Nearby friend (300m-2km)
            com.locationsharing.app.domain.model.NearbyFriend(
                id = "nearby_3",
                displayName = "Carol Davis (Nearby)",
                avatarUrl = null,
                distance = 890.0, // Nearby 300m-2km
                isOnline = false,
                lastUpdated = currentTime - 300000, // 5 minutes ago
                latLng = LatLng(baseLocation.latitude + 0.002, baseLocation.longitude - 0.001),
                smartRankingScore = 0.52f // Medium priority
            ),
            // Nearby friend (300m-2km) 
            com.locationsharing.app.domain.model.NearbyFriend(
                id = "nearby_4",
                displayName = "David Wilson (Nearby)",
                avatarUrl = null,
                distance = 1200.0, // Nearby 300m-2km
                isOnline = true,
                lastUpdated = currentTime - 120000, // 2 minutes ago
                latLng = LatLng(baseLocation.latitude - 0.002, baseLocation.longitude - 0.001),
                smartRankingScore = 0.35f // Good priority due to online status
            ),
            // In Town friend (2-10km)
            com.locationsharing.app.domain.model.NearbyFriend(
                id = "nearby_5",
                displayName = "Eve Brown (In Town)",
                avatarUrl = null,
                distance = 3500.0, // In Town 2-10km
                isOnline = true,
                lastUpdated = currentTime - 30000, // 30 seconds ago
                latLng = LatLng(baseLocation.latitude + 0.005, baseLocation.longitude),
                smartRankingScore = 0.28f // High priority due to recent activity and online
            )
        )
    }
    
    /**
     * Stop all updates and cleanup
     */
    private fun stopAllUpdates() {
        locationUpdatesJob?.cancel()
        friendsUpdatesJob?.cancel()
        nearbyFriendsJob?.cancel()
        retryJob?.cancel()
        locationSharingJob?.cancel()
        
        // Stop real-time friends service
        realTimeFriendsService.stopSync()
        
        Timber.d("All updates stopped and resources cleaned up")
    }
    
    /**
     * Cleanup resources when navigating away from map screen
     * Implements requirement 3.2 for proper cleanup
     */
    fun cleanupOnNavigationAway() {
        Timber.d("Cleaning up MapScreen resources on navigation away")
        
        try {
            // Stop location updates but keep sharing active if needed
            if (!_state.value.isLocationSharingActive) {
                locationUpdatesJob?.cancel()
            }
            
            // Clear UI state that's specific to map screen
            _state.value = _state.value.copy(
                selectedFriendId = null,
                isNearbyDrawerOpen = false,
                isStatusSheetVisible = false,
                debugSnackbarMessage = null
            )
            
            // Clear any pending animations or UI operations
            // This helps prevent memory leaks and improves performance
            
        } catch (e: Exception) {
            Timber.e(e, "Error during map screen cleanup")
            // Don't rethrow - cleanup should be resilient
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAllUpdates()
        Timber.d("MapScreenViewModel cleared")
    }
}