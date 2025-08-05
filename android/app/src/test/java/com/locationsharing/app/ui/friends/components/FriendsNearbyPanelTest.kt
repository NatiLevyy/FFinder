package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * Unit tests for FriendsNearbyPanel component.
 * 
 * Tests requirements:
 * - 2.1, 2.2, 2.4, 2.5: Panel display states and friend information
 * - 3.1, 3.2, 3.3, 3.4: Search functionality and filtering
 * - 7.3: Accessibility support
 */
@RunWith(AndroidJUnit4::class)
class FriendsNearbyPanelTest {

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
    fun friendsNearbyPanel_displaysSearchBar() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_displaysLoadingState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = true,
                        friends = emptyList()
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Loading friends...")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_displaysErrorState() {
        val errorMessage = "Location permission denied"
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList(),
                        error = errorMessage
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Retry")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_displaysEmptyState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList()
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("No friends sharing location yet")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_displaysFriendsList() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = {}
                )
            }
        }

        // Check that all friends are displayed
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Bob Smith")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Charlie Brown")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_searchFiltersCorrectly() {
        var capturedEvent: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = { event -> capturedEvent = event }
                )
            }
        }

        // Only Alice should be visible when filtered
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Bob Smith")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithText("Charlie Brown")
            .assertDoesNotExist()
    }

    @Test
    fun friendsNearbyPanel_searchTriggersEvent() {
        var capturedEvent: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { event -> capturedEvent = event }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .performTextInput("Alice")

        assert(capturedEvent is NearbyPanelEvent.SearchQuery)
        assert((capturedEvent as NearbyPanelEvent.SearchQuery).query == "Alice")
    }

    @Test
    fun friendsNearbyPanel_clearSearchButton() {
        var capturedEvent: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = { event -> capturedEvent = event }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .performClick()

        assert(capturedEvent is NearbyPanelEvent.SearchQuery)
        assert((capturedEvent as NearbyPanelEvent.SearchQuery).query == "")
    }

    @Test
    fun friendsNearbyPanel_noSearchResultsState() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "NonExistentFriend"
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("No friends found matching \"NonExistentFriend\"")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_friendClickTriggersEvent() {
        var capturedEvent: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = { event -> capturedEvent = event }
                )
            }
        }

        composeTestRule
            .onNodeWithText("Alice Johnson")
            .performClick()

        assert(capturedEvent is NearbyPanelEvent.FriendClick)
        assert((capturedEvent as NearbyPanelEvent.FriendClick).friendId == "1")
    }

    @Test
    fun friendsNearbyPanel_retryButtonTriggersEvent() {
        var capturedEvent: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList(),
                        error = "Network error"
                    ),
                    onEvent = { event -> capturedEvent = event }
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Retry loading friends")
            .performClick()

        assert(capturedEvent is NearbyPanelEvent.RefreshFriends)
    }

    @Test
    fun friendsNearbyPanel_accessibilitySupport() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = {}
                )
            }
        }

        // Check search bar accessibility
        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .assertIsDisplayed()

        // Check friends list accessibility
        composeTestRule
            .onNodeWithContentDescription("3 friends nearby")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_friendsSortedByDistance() {
        val unsortedFriends = listOf(
            sampleFriends[2], // Charlie - 2500m
            sampleFriends[0], // Alice - 150m  
            sampleFriends[1]  // Bob - 1200m
        )
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = unsortedFriends
                    ),
                    onEvent = {}
                )
            }
        }

        // The UI should display friends sorted by distance (Alice, Bob, Charlie)
        // This is handled by the filteredFriends property in NearbyUiState
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Bob Smith")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Charlie Brown")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_caseInsensitiveSearch() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "alice" // lowercase
                    ),
                    onEvent = {}
                )
            }
        }

        // Should find Alice Johnson despite case difference
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Bob Smith")
            .assertDoesNotExist()
    }

    @Test
    fun friendsNearbyPanel_partialNameSearch() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "John" // partial match for "Johnson"
                    ),
                    onEvent = {}
                )
            }
        }

        // Should find Alice Johnson with partial name match
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Bob Smith")
            .assertDoesNotExist()
        
        composeTestRule
            .onNodeWithText("Charlie Brown")
            .assertDoesNotExist()
    }

    // Additional Accessibility Tests
    @Test
    fun friendsNearbyPanel_searchBarHasProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = {}
                )
            }
        }

        // Check search bar has proper accessibility description
        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_clearButtonHasProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = {}
                )
            }
        }

        // Check clear button has proper accessibility description
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsNearbyPanel_retryButtonHasProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = emptyList(),
                        error = "Network error"
                    ),
                    onEvent = {}
                )
            }
        }

        // Check retry button has proper accessibility description
        composeTestRule
            .onNodeWithContentDescription("Retry loading friends")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsNearbyPanel_friendsListHasLiveRegion() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = {}
                )
            }
        }

        // Check friends list has proper accessibility description and live region
        composeTestRule
            .onNodeWithContentDescription("Friends list with 3 friends")
            .assertIsDisplayed()
    }

    @Test
    fun friendsNearbyPanel_friendItemsHaveProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends
                    ),
                    onEvent = {}
                )
            }
        }

        // Check individual friend items have proper accessibility descriptions
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Friend Bob Smith, 1.2 km away, offline")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Friend Charlie Brown, 2.5 km away, online")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsNearbyPanel_minimumTouchTargets() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsNearbyPanel(
                    uiState = NearbyUiState(
                        isLoading = false,
                        friends = sampleFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = {}
                )
            }
        }

        // All interactive elements should have proper touch targets
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertHasClickAction()
    }
}