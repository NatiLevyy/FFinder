package com.locationsharing.app.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.locationsharing.app.R
import com.locationsharing.app.ui.theme.FFinderTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Interactive Map Preview Card component for the FFinder Home Screen.
 * 
 * Displays a mini Google Map with user location centering, animated pin marker,
 * auto-pan camera animation, and fallback UI for permission handling.
 * 
 * Features:
 * - 90% width and 160dp height with 16dp rounded corners
 * - Google Maps integration with user location centering
 * - ic_pin_finder marker at user location
 * - Auto-pan camera animation with 10-second loop
 * - Pin bounce animation every 3 seconds
 * - Fallback UI when location permission is not granted
 * 
 * @param location User's current location (null if permission not granted)
 * @param hasLocationPermission Whether location permission is granted
 * @param animationsEnabled Whether animations should be played (accessibility)
 * @param modifier Modifier for styling
 * @param onPermissionRequest Callback when user taps to request permission
 */
@Composable
fun MapPreviewCard(
    location: LatLng?,
    hasLocationPermission: Boolean,
    isLoading: Boolean = false,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onPermissionRequest: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(160.dp)
            .semantics {
                contentDescription = if (hasLocationPermission && location != null) {
                    "Map preview showing your current location with animated pin marker"
                } else {
                    "Map preview unavailable. Location permission required."
                }
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        when {
            isLoading -> {
                MapPreviewShimmer(animationsEnabled = animationsEnabled)
            }
            hasLocationPermission && location != null -> {
                MapPreviewContent(
                    location = location,
                    animationsEnabled = animationsEnabled
                )
            }
            else -> {
                MapPreviewFallback(
                    onPermissionRequest = onPermissionRequest
                )
            }
        }
    }
}

/**
 * Shimmer effect for map preview while loading
 */
@Composable
private fun MapPreviewShimmer(
    animationsEnabled: Boolean
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ), 
        label = "shimmer_translate"
    )
    
    val brush = if (animationsEnabled) {
        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(translateAnim - 200f, translateAnim - 200f),
            end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
        )
    } else {
        Brush.linearGradient(colors = listOf(shimmerColors.first(), shimmerColors.first()))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = brush)
            .clip(RoundedCornerShape(24.dp))
    )
}

/**
 * Map preview content with Google Maps integration and animations.
 */
@Composable
private fun MapPreviewContent(
    location: LatLng,
    animationsEnabled: Boolean
) {
    // Check system accessibility preferences
    val shouldAnimate = AccessibilityUtils.shouldEnableAnimations(animationsEnabled)
    
    // Camera position state for auto-pan animation
    var cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }
    
    // Pin bounce animation - animates every 3 seconds
    val pinBounceOffset by animateFloatAsState(
        targetValue = if (shouldAnimate) -8f else 0f,
        animationSpec = if (shouldAnimate) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 3000,
                    easing = EaseInOutBack
                ),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(0) // No animation when disabled
        },
        label = "pin_bounce"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            ),
            properties = MapProperties(
                isMyLocationEnabled = false // We'll use custom marker
            )
        ) {
            // Custom marker with ic_pin_finder
            Marker(
                state = MarkerState(position = location),
                title = "Your Location",
                snippet = "You are here"
            )
        }
        
        // Animated pin overlay for bounce effect
        Icon(
            painter = painterResource(id = R.drawable.ic_pin_finder_optimized),
            contentDescription = "Your location pin marker",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = pinBounceOffset.dp)
                .size(32.dp)
                .semantics {
                    contentDescription = "Location pin marker showing your current position on the map"
                },
            tint = MaterialTheme.colorScheme.primary
        )
    }
    
    // Auto-pan camera animation with 10-second loop
    LaunchedEffect(shouldAnimate, location) {
        if (shouldAnimate) {
            while (true) {
                delay(10000) // 10 second loop
                
                // Generate slight random offset for auto-pan effect
                val randomLatOffset = (Random.nextFloat() - 0.5) * 0.002 // ~200m radius
                val randomLngOffset = (Random.nextFloat() - 0.5) * 0.002
                val randomZoom = 14f + Random.nextFloat() * 2f // Zoom between 14-16
                
                val newPosition = CameraPosition.fromLatLngZoom(
                    LatLng(
                        location.latitude + randomLatOffset,
                        location.longitude + randomLngOffset
                    ),
                    randomZoom
                )
                
                // Animate camera to new position
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newCameraPosition(newPosition),
                    durationMs = 2000
                )
            }
        }
    }
}

/**
 * Fallback UI displayed when location permission is not granted.
 */
@Composable
private fun MapPreviewFallback(
    onPermissionRequest: () -> Unit
) {
    val hapticManager = rememberHapticFeedbackManager()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = "Map preview unavailable. Location permission is required to show your area on the map."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pin finder icon
            Icon(
                painter = painterResource(id = R.drawable.ic_pin_finder_vector),
                contentDescription = "Location preview unavailable",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title text
            Text(
                text = "Map Preview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description text
            Text(
                text = "Enable location to see your area",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Enable location button
            TextButton(
                onClick = {
                    hapticManager.performSecondaryAction()
                    onPermissionRequest()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Enable Location button. Tap to grant location permission and view map preview."
                    role = Role.Button
                }
            ) {
                Text(
                    text = "Enable Location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Enhanced map preview with comprehensive error handling.
 * 
 * This is a legacy wrapper that delegates to the new MapPreviewWithErrorHandling
 * component for backward compatibility.
 */
@Composable
fun MapPreviewWithLegacyErrorHandling(
    location: LatLng?,
    hasLocationPermission: Boolean,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onPermissionRequest: () -> Unit = {},
    onError: () -> Unit = {}
) {
    // Delegate to the new comprehensive error handling component
    com.locationsharing.app.ui.home.components.MapPreviewWithErrorHandling(
        location = location,
        hasLocationPermission = hasLocationPermission,
        animationsEnabled = animationsEnabled,
        modifier = modifier,
        onPermissionRequest = onPermissionRequest,
        onRetry = { /* Handle retry if needed */ },
        onError = { mapPreviewError ->
            // Convert MapPreviewError to legacy callback
            onError()
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MapPreviewCardWithLocationPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // With location permission and location
            MapPreviewCard(
                location = LatLng(37.7749, -122.4194), // San Francisco
                hasLocationPermission = true,
                animationsEnabled = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreviewCardFallbackPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Without location permission
            MapPreviewCard(
                location = null,
                hasLocationPermission = false,
                animationsEnabled = true,
                onPermissionRequest = { /* Handle permission request */ }
            )
        }
    }
}

/**
 * Enhanced Map Preview Card with ViewModel integration for location refresh.
 * Works on emulator by attempting to get last known location and retrying.
 */
@Composable
fun MapPreviewCardWithViewModel(
    vm: MapPreviewViewModel = hiltViewModel(),
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onPermissionRequest: () -> Unit = {}
) {
    val lastLocation by vm.lastLocation.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val hasLocationPermission by vm.hasLocationPermission.collectAsStateWithLifecycle()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when {
            isLoading -> {
                MapPreviewShimmer(animationsEnabled = animationsEnabled)
            }
            lastLocation != null -> {
                MapPreviewContent(
                    location = lastLocation!!,
                    animationsEnabled = animationsEnabled
                )
            }
            else -> {
                MapPreviewFallback(onPermissionRequest = {
                    onPermissionRequest()
                    vm.refreshPreview()
                })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreviewCardAccessibilityPreview() {
    FFinderTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // With animations disabled (accessibility)
            MapPreviewCard(
                location = LatLng(37.7749, -122.4194),
                hasLocationPermission = true,
                animationsEnabled = false
            )
        }
    }
}