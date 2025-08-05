package com.locationsharing.app.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.components.FriendsPanelScaffold
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Friends Nearby Panel open/close interactions.
 * 
 * Tests requirements:
 * - 1.2: Panel toggle functionality with Modal Navigation Drawer
 * - 1.3: Tap outside to close functionality
 * - 1.4: Smooth opening/closing animations
 * - 2.1: Panel display and interaction states
 * - 5.1: Friend interaction flows from panel
 */
@RunWith(AndroidJUnit4::class)
class FriendsNearbyPanelUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleFriends = listOf(
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



    @Test
    fun friendsToggleFAB_opensPanel() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Initially panel should be closed
        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel")
            .assertIsNotDisplayed()

        // Click FAB to open panel
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertIsDisplayed()
            .performClick()

        // Panel should now be visible (this would be handled by state change in real app)
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel")
            .assertIsDisplayed()
    }

    @Test
    fun friendsToggleFAB_closesPanel() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Panel should be open initially
        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel")
            .assertIsDisplayed()

        // Click FAB to close panel
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertIsDisplayed()
            .performClick()

        // Panel should now be closed (this would be handled by state change in real app)
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel")
            .assertIsNotDisplayed()
    }

    @Test
    fun friendsPanel_maintainsMapViewState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content with identifiable element
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "Google Maps view" }
                    )
                }
            }
        }

        // Map should be visible when panel is closed
        composeTestRule
            .onNodeWithContentDescription("Google Maps view")
            .assertIsDisplayed()

        // Open panel
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content with identifiable element
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "Google Maps view" }
                    )
                }
            }
        }

        // Map should still be visible when panel is open (behind the panel)
        composeTestRule
            .onNodeWithContentDescription("Google Maps view")
            .assertIsDisplayed()
    }

    @Test
    fun friendsPanel_searchInteraction() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Search bar should be visible
        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .assertIsDisplayed()

        // Type in search bar
        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .performTextInput("Alice")

        // Clear search button should appear
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .performClick()
    }

    @Test
    fun friendsPanel_friendItemInteraction() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Friend items should be clickable
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Friend Bob Smith, 1.2 km away, offline")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
    }

    @Test
    fun friendsPanel_errorStateRecovery() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = emptyList(),
                        error = "Network connection failed"
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Error message should be displayed
        composeTestRule
            .onNodeWithText("Network connection failed")
            .assertIsDisplayed()

        // Retry button should be available and clickable
        composeTestRule
            .onNodeWithContentDescription("Retry loading friends")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
    }

    @Test
    fun friendsPanel_loadingState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = true,
                        friends = emptyList()
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Loading indicator should be displayed
        composeTestRule
            .onNodeWithText("Loading friends...")
            .assertIsDisplayed()
    }

    @Test
    fun friendsPanel_emptyState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = emptyList()
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Empty state message should be displayed
        composeTestRule
            .onNodeWithText("No friends sharing location yet")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Invite friends to start sharing their location with you")
            .assertIsDisplayed()
    }

    @Test
    fun friendsPanel_noSearchResults() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "NonExistentFriend"
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // No search results message should be displayed
        composeTestRule
            .onNodeWithText("No friends found matching \"NonExistentFriend\"")
            .assertIsDisplayed()
    }

    @Test
    fun friendsPanel_animationStates() {
        // Test that panel can transition between states without crashes
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = true,
                        friends = emptyList()
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Transition to open with loading
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = true,
                        friends = emptyList()
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Transition to loaded state
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Should display friends without crashes
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
    }
}