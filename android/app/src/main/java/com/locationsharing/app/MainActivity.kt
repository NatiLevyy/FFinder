package com.locationsharing.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.navigation.NavigationStateTracker
import com.locationsharing.app.navigation.Screen
import com.locationsharing.app.ui.screens.MapScreen
import com.locationsharing.app.ui.theme.FFinderTheme
import com.locationsharing.app.ui.screens.FriendsListScreen
import com.locationsharing.app.ui.home.HomeScreen
import com.locationsharing.app.ui.invite.InviteFriendsScreen
import com.locationsharing.app.ui.settings.SettingsScreen
import com.locationsharing.app.ui.friends.hub.FriendsHubScreen
import com.locationsharing.app.ui.search.GlobalFriendSearchScreen
import timber.log.Timber
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.locationsharing.app.data.auth.AuthManager

/**
 * Main activity for the FFinder application.
 * Uses enhanced NavigationManager for centralized navigation control with error handling.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var navigationManager: NavigationManager
    
    @Inject
    lateinit var navigationStateTracker: NavigationStateTracker
    
    @Inject
    lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Timber.d("MainActivity created with enhanced navigation")
        
        // Ensure user is authenticated on app startup
        lifecycleScope.launch {
            try {
                authManager.ensureSignedIn()
                Timber.d("User authenticated successfully on app startup")
            } catch (e: Exception) {
                Timber.e(e, "Failed to authenticate user on app startup")
                // App can still continue with limited functionality
            }
        }
        
        setContent {
            FFinderTheme {
                val navController = rememberNavController()
                val navigationState by navigationStateTracker.currentState.collectAsState()
                
                // Initialize NavigationManager with NavController
                LaunchedEffect(navController) {
                    navigationManager.setNavController(navController)
                    Timber.d("NavigationManager initialized with NavController")
                }
                
                // Enhanced NavHost with error handling
                NavHost(
                    navController = navController,
                    startDestination = Screen.HOME.route
                ) {
                    composable(Screen.HOME.route) {
                        HomeScreen(
                            onStartShare = {
                                Timber.d("HomeScreen: Start sharing triggered")
                                // Navigation is handled by the NavigationManager in the button components
                            },
                            navigationManager = navigationManager
                        )
                    }
                    
                    composable(
                        route = "${Screen.MAP.route}?startSharing={startSharing}&friendId={friendId}",
                        arguments = listOf(
                            navArgument("startSharing") {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                            navArgument("friendId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        // Update navigation state when entering map screen
                        LaunchedEffect(Unit) {
                            navigationStateTracker.updateCurrentScreen(Screen.MAP)
                        }
                        
                        val startSharing = backStackEntry.arguments?.getBoolean("startSharing") ?: false
                        val friendId = backStackEntry.arguments?.getString("friendId")
                        
                        MapScreen(
                            startSharing = startSharing,
                            friendIdToFocus = friendId,
                            onBackClick = {
                                Timber.d("MapScreen: Back navigation requested")
                                if (!navigationManager.navigateBack()) {
                                    // Fallback to home if back navigation fails
                                    navigationManager.navigateToHome()
                                }
                            },
                            onSearchFriendsClick = {
                                Timber.d("MapScreen: Search friends navigation requested")
                                navigationManager.navigateToSearchFriends()
                            }
                        )
                    }
                    
                    composable(Screen.FRIENDS.route) {
                        // Update navigation state when entering friends screen
                        LaunchedEffect(Unit) {
                            navigationStateTracker.updateCurrentScreen(Screen.FRIENDS)
                        }
                        
                        FriendsListScreen(
                            onBackClick = {
                                Timber.d("FriendsScreen: Back navigation requested")
                                if (!navigationManager.navigateBack()) {
                                    // Fallback to home if back navigation fails
                                    navigationManager.navigateToHome()
                                }
                            },
                            onFriendClick = { friend ->
                                Timber.d("FriendsScreen: Navigating to map for friend: ${friend.name}")
                                navigationManager.navigateToMap()
                            },
                            onInviteFriendsClick = {
                                Timber.d("FriendsScreen: Navigating to invite friends")
                                // For now, navigate to invite_friends route directly
                                // TODO: Add invite friends to NavigationManager when implemented
                                navController.navigate("invite_friends")
                            }
                        )
                    }
                    
                    composable(Screen.FRIENDS_HUB.route) {
                        FriendsHubScreen(
                            onNavigateToGlobalSearch = {
                                navigationManager.navigateToSearchFriends()
                            }
                        )
                    }

                    composable(Screen.SETTINGS.route) {
                        SettingsScreen()
                    }
                    
                    // Legacy route for invite friends - will be migrated to NavigationManager later
                    composable("invite_friends") {
                        InviteFriendsScreen(
                            onBackClick = {
                                Timber.d("InviteFriendsScreen: Back navigation requested")
                                if (!navigationManager.navigateBack()) {
                                    navigationManager.navigateToHome()
                                }
                            },
                            onEnableContactDiscovery = {
                                Timber.d("InviteFriendsScreen: Navigate to phone verification")
                                navController.navigate("phone_verification")
                            }
                        )
                    }
                    
                    // Phone verification for contact discovery (temporarily disabled for release build)
                    composable("phone_verification") {
                        // TODO: Re-enable PhoneVerificationScreen for full functionality
                        Text("Phone verification coming soon...")
                    }
                    
                    composable(Screen.SEARCH_FRIENDS.route) {
                        // Update navigation state when entering search friends screen
                        LaunchedEffect(Unit) {
                            navigationStateTracker.updateCurrentScreen(Screen.SEARCH_FRIENDS)
                        }
                        
                        GlobalFriendSearchScreen(
                            onBackClick = {
                                Timber.d("GlobalFriendSearchScreen: Back navigation requested")
                                if (!navigationManager.navigateBack()) {
                                    navigationManager.navigateToMap()
                                }
                            },
                            onFriendSelected = { friend ->
                                // Navigate back to map and zoom to friend
                                navigationManager.navigateToMapWithFriend(friend.id)
                            }
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Handle system back button with NavigationManager.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Timber.d("System back button pressed")
        
        if (!navigationManager.navigateBack()) {
            // If NavigationManager can't handle back navigation, use system default
            Timber.d("NavigationManager couldn't handle back navigation, using system default")
            super.onBackPressed()
        }
    }
}


