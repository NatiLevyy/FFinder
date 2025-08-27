package com.locationsharing.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.R
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.ui.components.LottieAsset
import com.locationsharing.app.ui.components.LiveShareButton
import com.locationsharing.app.ui.home.components.*
import com.locationsharing.app.ui.common.debounceClickable
import com.locationsharing.app.ui.home.components.AccessibilityUtils
import com.locationsharing.app.ui.theme.FFinderTheme

/**
 * Main HomeScreen composable that integrates all components with proper state management.
 * 
 * Enhanced with NavigationManager integration for proper navigation handling,
 * loading states, haptic feedback, and visual feedback. Features:
 * - ScrollableColumn layout with appropriate spacing and padding
 * - Integration of all home screen components (Hero, Map, CTA, Actions, Teaser)
 * - Enhanced navigation buttons with ResponsiveButton integration
 * - Proper lifecycle management for animations and map components
 * - Theme consistency across light and dark modes
 * - Responsive design for different screen sizes
 * - Comprehensive state management and event handling
 * 
 * @param onStartShare Callback invoked when user taps "Start Live Sharing"
 * @param onFriends Callback invoked when user taps "Friends" button
 * @param onSettings Callback invoked when user taps "Settings" button
 * @param navigationManager NavigationManager for handling navigation operations
 * @param modifier Modifier for customizing the screen's appearance
 * @param viewModel HomeScreenViewModel for state management (optional for testing)
 */
@Composable
fun HomeScreen(
    onStartShare: () -> Unit,
    navigationManager: NavigationManager? = null,
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = viewModel()
) {
    // Collect state from ViewModel
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // Responsive layout configuration
    ResponsiveLayout(
        onConfigurationChanged = { config ->
            viewModel.onEvent(HomeScreenEvent.ScreenConfigurationChanged(config.isNarrowScreen))
        }
    ) { layoutConfig ->
        
        // Premium gradient background with Lottie backdrop
        PremiumBackgroundGradient(modifier = modifier) {
            
            // Transparent TopAppBar
            TransparentTopAppBar(
                modifier = Modifier.zIndex(1f)
            )
            
            // Scrollable content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AccessibilityUtils.getResponsivePadding(
                            compactPadding = 16.dp,
                            mediumPadding = 20.dp,
                            expandedPadding = 24.dp
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    AccessibilityUtils.getResponsiveSpacing(
                        compactSpacing = 16.dp,
                        mediumSpacing = 20.dp,
                        expandedSpacing = 24.dp
                    )
                )
            ) {
                
                // Premium hero section with logo and slow pulse
                PremiumHeroSection(
                    animationsEnabled = state.animationsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Extra spacing for visual balance
                Spacer(modifier = Modifier.height(24.dp))
                
                // Lottie Animations Section (replaces Map Preview) - slightly smaller
                MapAnimationsSection(
                    animationsEnabled = state.animationsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(0.9f) // Reduce size by 10% to not compete with main button
                )
                
                // Primary Live Share Button with Label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    LiveShareButton(
                        isSharing = state.isLocationSharing,
                        waitingForFix = state.isWaitingForLocationFix,
                        onToggle = {
                            if (state.isLocationSharing) {
                                viewModel.stopLocationSharing()
                            } else {
                                viewModel.onEvent(HomeScreenEvent.StartSharing)
                                navigationManager?.navigateToMap(startSharing = true)
                                onStartShare()
                            }
                        },
                        modifier = Modifier.semantics { testTag = "ShareLocationBig" }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Share Location label
                    Text(
                        text = if (state.isLocationSharing) "Stop Sharing" else "Start Sharing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Friends & Settings Buttons (Filled Purple Buttons)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Friends button
                    Button(
                        onClick = {
                            viewModel.onEvent(HomeScreenEvent.NavigateToFriends)
                            navigationManager?.navigateToFriendsHub()
                        },
                        modifier = Modifier
                            .weight(0.42f)
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB791E0),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                            hoveredElevation = 6.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Friends",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Settings button
                    Button(
                        onClick = {
                            viewModel.onEvent(HomeScreenEvent.NavigateToSettings)
                            navigationManager?.navigateToSettings()
                        },
                        modifier = Modifier
                            .weight(0.42f)
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription = "Navigate to settings screen"
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB791E0),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                            hoveredElevation = 6.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // What's New banner removed
                
                // Bottom spacing for comfortable scrolling
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // What's New Dialog
    if (state.showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = {
                viewModel.onEvent(HomeScreenEvent.DismissWhatsNew)
            }
        )
    }
    
    // Handle animation preferences changes
    val animationsEnabled = AccessibilityUtils.shouldEnableAnimations()
    LaunchedEffect(animationsEnabled) {
        viewModel.onEvent(HomeScreenEvent.AnimationPreferencesChanged(animationsEnabled))
    }
}

/**
 * Stateless version of HomeScreen for testing and preview purposes.
 * 
 * @param state The current HomeScreenState
 * @param onEvent Callback for handling HomeScreenEvents
 * @param onStartShare Callback for start sharing action
 * @param onFriends Callback for friends navigation
 * @param onSettings Callback for settings navigation
 * @param navigationManager NavigationManager for handling navigation operations
 * @param modifier Modifier for customization
 */
@Composable
fun HomeScreenContent(
    state: HomeScreenState,
    onEvent: (HomeScreenEvent) -> Unit,
    onStartShare: () -> Unit,
    onStopShare: () -> Unit,
    onFriends: () -> Unit,
    onSettings: () -> Unit,
    navigationManager: NavigationManager? = null,
    modifier: Modifier = Modifier
) {
    // Responsive layout configuration
    ResponsiveLayout(
        onConfigurationChanged = { config ->
            onEvent(HomeScreenEvent.ScreenConfigurationChanged(config.isNarrowScreen))
        }
    ) { layoutConfig ->
        
        // Premium gradient background with Lottie backdrop
        PremiumBackgroundGradient(modifier = modifier) {
            
            // Transparent TopAppBar
            TransparentTopAppBar(
                modifier = Modifier.zIndex(1f)
            )
            
            // Scrollable content column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AccessibilityUtils.getResponsivePadding(
                            compactPadding = 16.dp,
                            mediumPadding = 20.dp,
                            expandedPadding = 24.dp
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    AccessibilityUtils.getResponsiveSpacing(
                        compactSpacing = 16.dp,
                        mediumSpacing = 20.dp,
                        expandedSpacing = 24.dp
                    )
                )
            ) {
                
                // Premium hero section with logo and slow pulse
                PremiumHeroSection(
                    animationsEnabled = state.animationsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Lottie Animations Section (replaces Map Preview)
                MapAnimationsSection(
                    animationsEnabled = state.animationsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Primary Live Share Button
                LiveShareButton(
                    isSharing = state.isLocationSharing,
                    waitingForFix = state.isWaitingForLocationFix,
                    onToggle = {
                        if (state.isLocationSharing) {
                            onStopShare()
                        } else {
                            onEvent(HomeScreenEvent.StartSharing)
                            onStartShare()
                        }
                    },
                    modifier = Modifier.padding(vertical = 32.dp)
                )
                
                // Friends & Settings Buttons (Material 3 OutlinedButton style)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Friends button
                    OutlinedButton(
                        onClick = {
                            onEvent(HomeScreenEvent.NavigateToFriends)
                            onFriends()
                        },
                        modifier = Modifier
                            .weight(0.42f)
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription = "Navigate to friends screen"
                            },
                        shape = RoundedCornerShape(24.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B4F8F)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Friends",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Settings button  
                    OutlinedButton(
                        onClick = {
                            onEvent(HomeScreenEvent.NavigateToSettings)
                            onSettings()
                        },
                        modifier = Modifier
                            .weight(0.42f)
                            .padding(vertical = 12.dp)
                            .semantics {
                                contentDescription = "Navigate to settings screen"
                            },
                        shape = RoundedCornerShape(24.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6B4F8F)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // What's New banner removed
                
                // Bottom spacing for comfortable scrolling
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // What's New Dialog
    if (state.showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = {
                onEvent(HomeScreenEvent.DismissWhatsNew)
            }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen - Light Theme")
@Composable
fun HomeScreenPreview() {
    FFinderTheme {
        HomeScreenContent(
            state = HomeScreenState(
                hasLocationPermission = true,
                mapPreviewLocation = LatLng(37.7749, -122.4194), // San Francisco
                animationsEnabled = true,
                isNarrowScreen = false,
                isLocationSharing = false,
                isWaitingForLocationFix = false
            ),
            onEvent = { /* Preview */ },
            onStartShare = { /* Preview */ },
            onStopShare = { /* Preview */ },
            onFriends = { /* Preview */ },
            onSettings = { /* Preview */ }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen - Dark Theme")
@Composable
fun HomeScreenDarkPreview() {
    FFinderTheme(darkTheme = true) {
        HomeScreenContent(
            state = HomeScreenState(
                hasLocationPermission = true,
                mapPreviewLocation = LatLng(37.7749, -122.4194),
                animationsEnabled = true,
                isNarrowScreen = false,
                isLocationSharing = false,
                isWaitingForLocationFix = false
            ),
            onEvent = { /* Preview */ },
            onStartShare = { /* Preview */ },
            onStopShare = { /* Preview */ },
            onFriends = { /* Preview */ },
            onSettings = { /* Preview */ }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen - No Location Permission")
@Composable
fun HomeScreenNoPermissionPreview() {
    FFinderTheme {
        HomeScreenContent(
            state = HomeScreenState(
                hasLocationPermission = false,
                mapPreviewLocation = null,
                animationsEnabled = true,
                isNarrowScreen = false,
                isLocationSharing = false,
                isWaitingForLocationFix = false
            ),
            onEvent = { /* Preview */ },
            onStartShare = { /* Preview */ },
            onStopShare = { /* Preview */ },
            onFriends = { /* Preview */ },
            onSettings = { /* Preview */ }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen - Narrow Screen")
@Composable
fun HomeScreenNarrowPreview() {
    FFinderTheme {
        HomeScreenContent(
            state = HomeScreenState(
                hasLocationPermission = true,
                mapPreviewLocation = LatLng(37.7749, -122.4194),
                animationsEnabled = true,
                isNarrowScreen = true,
                isLocationSharing = false,
                isWaitingForLocationFix = false
            ),
            onEvent = { /* Preview */ },
            onStartShare = { /* Preview */ },
            onStopShare = { /* Preview */ },
            onFriends = { /* Preview */ },
            onSettings = { /* Preview */ }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen - Animations Disabled")
@Composable
fun HomeScreenAccessibilityPreview() {
    FFinderTheme {
        HomeScreenContent(
            state = HomeScreenState(
                hasLocationPermission = true,
                mapPreviewLocation = LatLng(37.7749, -122.4194),
                animationsEnabled = false,
                isNarrowScreen = false,
                isLocationSharing = false,
                isWaitingForLocationFix = false
            ),
            onEvent = { /* Preview */ },
            onStartShare = { /* Preview */ },
            onStopShare = { /* Preview */ },
            onFriends = { /* Preview */ },
            onSettings = { /* Preview */ }
        )
    }
}

// Premium Components for the redesigned home screen

/**
 * Light gradient background (mint to white) with subtle hero motion
 */
@Composable
private fun PremiumBackgroundGradient(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val gradientTop = Color(0xFFE0F7EF)    // mint
    val gradientBottom = Color.White       // pure white
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTop, gradientBottom),
                    startY = 0f,          // mint at the very top
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Subtle hero motion background animation
        LottieAsset(
            resId = R.raw.travel_somewhere,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.15f),
            iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever,
            speed = 0.5f // Slow animation
        )
        
        content()
    }
}

/**
 * Transparent CenterAlignedTopAppBar with navigation when needed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransparentTopAppBar(
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { },
        navigationIcon = {
            // Only show back button when screen is not the start destination
            // This can be controlled by navigation state if needed
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
    )
}

/**
 * Premium hero section with logo and slow pulse animation
 */
@Composable
private fun PremiumHeroSection(
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Slow pulse animation for logo (scale 1.0 ↔ 1.05, 4s loop)
    val pulseScale by animateFloatAsState(
        targetValue = if (animationsEnabled) 1.05f else 1f,
        animationSpec = if (animationsEnabled) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 4000,
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(0)
        },
        label = "logo_pulse"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 24.dp, bottom = 16.dp)
    ) {
        // FFinder Logo with pulse (72dp height)
        Image(
            painter = painterResource(R.drawable.logo_full),
            contentDescription = "FFinder logo",
            modifier = Modifier
                .height(72.dp)
                .scale(pulseScale),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Premium subtitle
        Text(
            text = "Share your location\nConnect with friends",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 28.sp
        )
        
        // Additional spacer before MapPreview
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Premium secondary actions row with enhanced styling
 */
@Composable
private fun PremiumSecondaryActionsRow(
    onFriends: () -> Unit,
    onSettings: () -> Unit,
    navigationManager: NavigationManager?,
    friendsLoading: Boolean,
    settingsLoading: Boolean,
    friendsEnabled: Boolean,
    settingsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Friends button
        OutlinedButton(
            onClick = onFriends,
            enabled = friendsEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF6B4F8F)
            )
        ) {
            if (friendsLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Settings button
        OutlinedButton(
            onClick = onSettings,
            enabled = settingsEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF6B4F8F)
            )
        ) {
            if (settingsLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// What's New banner removed