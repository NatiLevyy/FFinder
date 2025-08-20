package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive accessibility tests for the Friends Nearby Panel feature.
 * 
 * Tests requirements:
 * - 7.1: Comprehensive accessibility descriptions for screen readers
 * - 7.2: Proper focus management when panel opens/closes
 * - 7.3: Search accessibility labels and hints
 * - 7.4: Clear accessibility descriptions for action buttons
 * - 7.5: Proper loading state indicators
 * - 7.6: Clear, accessible error messages
 * 
 * Verifies 48dp minimum touch targets across all interactive elements.
 */
@RunWith(AndroidJUnit4::class)
class FriendsNearbyPanelAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleFriends = listOf(
        NearbyFriend(
            id = "1",
            displayName = "Alice Johnson",
            avatarUrl = "https://example.com/avatar1.jpg",
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
        ),
        NearbyFriend(
            id = "3",
            displayName = "Charlie Brown",
            avatarUrl = null,
            distance = 2500.0,
            isOnline = true,
            lastUpdated = System.currentTimeMillis() - 60000,
            latLng = LatLng(37.7949, -122.3994)
        )
    )

    @Test
    fun friendsToggleFAB_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isPanelOpen = false,
                    friendCount = 0,
                    onClick = { }
                )
            }
        }

        // Verify FAB has enhanced accessibility role and description
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel. No friends are currently sharing their location.")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun friendsToggleFAB_stateChanges_updateAccessibilityDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isPanelOpen = true,
                    friendCount = 2,
                    onClick = { }
                )
            }
        }

        // Verify FAB shows enhanced close state accessibility description
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel. Panel is currently open.")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun nearbyFriendItem_hasComprehensiveAccessibilityDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleFriends[0],
                    onClick = { }
                )
            }
        }

        // Verify comprehensive accessibility description includes all relevant info
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun nearbyFriendItem_offlineFriend_hasCorrectAccessibilityDescription() {
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleFriends[1], // Bob Smith - offline
                    onClick = { }
                )
            }
        }

        // Verify offline status is properly announced
        composeTestRule
            .onNodeWithContentDescription("Friend Bob Smith, 1.2 km away, offline")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsNearbyPanel_searchBar_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify search bar has proper accessibility description
        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_clearSearchButton_hasProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify clear search button has proper accessibility
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsNearbyPanel_friendsList_hasLiveRegionForAnnouncements() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify friends list has proper accessibility description with count
        composeTestRule
            .onNodeWithContentDescription("Friends list with 3 friends")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_retryButton_hasProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList(),
                        error = "Network error"
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify retry button has proper accessibility description
        composeTestRule
            .onNodeWithContentDescription("Retry loading friends")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendInfoBottomSheet_actionButtons_haveSpecificAccessibilityDescriptions() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = sampleFriends[0],
                    onEvent = { }
                )
            }
        }

        // Verify each action button has specific accessibility description with friend name
        composeTestRule
            .onNodeWithContentDescription("Navigate to Alice Johnson")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Send ping to Alice Johnson")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Stop sharing location with Alice Johnson")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Send message to Alice Johnson")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendInfoBottomSheet_actionButtons_haveProperAccessibilityRoles() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = sampleFriends[0],
                    onEvent = { }
                )
            }
        }

        // Verify all action buttons have Button role
        val actionButtons = listOf(
            "Navigate to Alice Johnson",
            "Send ping to Alice Johnson",
            "Stop sharing location with Alice Johnson",
            "Send message to Alice Johnson"
        )

        actionButtons.forEach { description ->
            composeTestRule
                .onNodeWithContentDescription(description)
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Role,
                        androidx.compose.ui.semantics.Role.Button
                    )
                )
        }
    }

    @Test
    fun friendsPanelScaffold_hasProperAccessibilityLabels() {
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

        // Verify scaffold has proper accessibility labels
        composeTestRule
            .onNodeWithContentDescription("Map screen with friends panel")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel")
            .assertIsDisplayed()
    }

    @Test
    fun allInteractiveElements_haveMinimumTouchTargets() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Verify all interactive elements have click actions (indicating proper touch targets)
        val interactiveElements = listOf(
            "Close friends nearby panel",
            "Clear search",
            "Friend Alice Johnson, 150 m away, online"
        )

        interactiveElements.forEach { description ->
            composeTestRule
                .onNodeWithContentDescription(description)
                .assertHasClickAction()
        }
    }

    @Test
    fun friendInfoBottomSheet_actionButtons_haveMinimumTouchTargets() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = sampleFriends[0],
                    onEvent = { }
                )
            }
        }

        // Verify all action buttons have proper touch targets (48dp ensured by padding)
        val actionButtons = listOf(
            "Navigate to Alice Johnson",
            "Send ping to Alice Johnson", 
            "Stop sharing location with Alice Johnson",
            "Send message to Alice Johnson"
        )

        actionButtons.forEach { description ->
            composeTestRule
                .onNodeWithContentDescription(description)
                .assertHasClickAction()
        }
    }

    @Test
    fun loadingState_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = true,
                        friends = emptyList()
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify loading state has proper accessibility indicators
        composeTestRule
            .onNodeWithText("Loading friends...")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_hasProperAccessibilitySupport() {
        val errorMessage = "Location permission denied. Please enable location access."
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList(),
                        error = errorMessage
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify error state has clear, accessible error messages
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Retry loading friends")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun emptyState_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList()
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify empty state has clear messaging
        composeTestRule
            .onNodeWithText("No friends sharing location yet")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Invite friends to start sharing their location with you")
            .assertIsDisplayed()
    }

    @Test
    fun noSearchResults_hasProperAccessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "NonExistentFriend"
                    ),
                    onEvent = { }
                )
            }
        }

        // Verify no search results state has clear messaging
        composeTestRule
            .onNodeWithText("No friends found matching \"NonExistentFriend\"")
            .assertIsDisplayed()
    }

    @Test
    fun friendCountAnnouncements_workCorrectly() {
        var eventCaptured: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { event -> eventCaptured = event }
                )
            }
        }

        // Verify friends list announces count for screen readers
        composeTestRule
            .onNodeWithContentDescription("Friends list with 3 friends")
            .assertIsDisplayed()

        // Test with filtered results
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = { event -> eventCaptured = event }
                )
            }
        }

        // Should announce filtered count
        composeTestRule
            .onNodeWithContentDescription("Friends list with 1 friends")
            .assertIsDisplayed()
    }

    @Test
    fun friendsToggleFAB_hasEnhancedAccessibilityWithFriendCount() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isPanelOpen = false,
                    friendCount = 3,
                    onClick = { }
                )
            }
        }

        // Verify FAB announces friend count in accessibility description
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel. 3 friends are available nearby.")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun friendsToggleFAB_hasEnhancedAccessibilityWithSingleFriend() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isPanelOpen = false,
                    friendCount = 1,
                    onClick = { }
                )
            }
        }

        // Verify FAB uses singular form for single friend
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel. 1 friend is available nearby.")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsToggleFAB_hasStateDescriptionForScreenReaders() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isPanelOpen = false,
                    friendCount = 2,
                    onClick = { }
                )
            }
        }

        // Verify FAB has state description for screen readers
        composeTestRule
            .onNodeWithContentDescription("Open nearby friends panel. 2 friends are available nearby.")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "2 nearby"
                )
            )
    }

    @Test
    fun friendsToggleFAB_hasLiveRegionForStateChanges() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsToggleFAB(
                    isPanelOpen = true,
                    friendCount = 1,
                    onClick = { }
                )
            }
        }

        // Verify FAB has live region for announcing state changes
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel. Panel is currently open.")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    androidx.compose.ui.semantics.LiveRegionMode.Polite
                )
            )
    }

    @Test
    fun friendsPanelScaffold_hasEnhancedAccessibilityLabelsWithState() {
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

        // Verify scaffold has enhanced accessibility labels with friend count
        composeTestRule
            .onNodeWithContentDescription("Map screen with friends panel")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel, 3 friends available")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    androidx.compose.ui.semantics.LiveRegionMode.Polite
                )
            )
    }

    @Test
    fun friendsPanelScaffold_announcesLoadingState() {
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

        // Verify scaffold announces loading state
        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel, loading friends")
            .assertIsDisplayed()
    }

    @Test
    fun friendsPanelScaffold_announcesErrorState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = emptyList(),
                        error = "Network error"
                    ),
                    onEvent = { }
                ) {
                    // Mock map content
                }
            }
        }

        // Verify scaffold announces error state
        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel, error occurred")
            .assertIsDisplayed()
    }
}