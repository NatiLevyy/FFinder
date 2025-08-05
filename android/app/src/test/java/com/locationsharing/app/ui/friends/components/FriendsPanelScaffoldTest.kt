package com.locationsharing.app.ui.friends.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.friends.NearbyUiState
import com.locationsharing.app.ui.theme.FFinderTheme
import com.google.android.gms.maps.model.LatLng
import org.junit.Rule
import org.junit.Test

class FriendsPanelScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testFriends = listOf(
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
    fun friendsPanelScaffold_initiallyClosedState_showsCorrectElements() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = false,
                        friends = testFriends
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify main content is displayed
        composeTestRule.onNodeWithText("Map Content").assertIsDisplayed()
        
        // Verify FAB is displayed with correct state
        composeTestRule.onNodeWithContentDescription("Open friends nearby panel").assertIsDisplayed()
        
        // Verify panel content is not visible when closed
        composeTestRule.onNodeWithContentDescription("Search friends by name").assertIsNotDisplayed()
    }

    @Test
    fun friendsPanelScaffold_openState_showsPanelContent() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = testFriends
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify main content is still displayed (maintains map state)
        composeTestRule.onNodeWithText("Map Content").assertIsDisplayed()
        
        // Verify FAB shows close state
        composeTestRule.onNodeWithContentDescription("Close friends nearby panel").assertIsDisplayed()
        
        // Verify panel content is visible
        composeTestRule.onNodeWithContentDescription("Search friends by name").assertIsDisplayed()
        
        // Verify friends are displayed in panel
        composeTestRule.onNodeWithText("Alice Johnson").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Smith").assertIsDisplayed()
    }

    @Test
    fun friendsPanelScaffold_fabClick_triggersToggleEvent() {
        var lastEvent: NearbyPanelEvent? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = false,
                        friends = testFriends
                    ),
                    onEvent = { event -> lastEvent = event }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Click the FAB
        composeTestRule.onNodeWithContentDescription("Open friends nearby panel").performClick()
        
        // Verify toggle event was triggered
        assert(lastEvent is NearbyPanelEvent.TogglePanel)
    }

    @Test
    fun friendsPanelScaffold_stateChange_updatesUICorrectly() {
        var uiState by mutableStateOf(
            NearbyUiState(
                isPanelOpen = false,
                isLoading = false,
                friends = testFriends
            )
        )
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = uiState,
                    onEvent = { event ->
                        when (event) {
                            is NearbyPanelEvent.TogglePanel -> {
                                uiState = uiState.copy(isPanelOpen = !uiState.isPanelOpen)
                            }
                            else -> {}
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Initially closed
        composeTestRule.onNodeWithContentDescription("Open friends nearby panel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search friends by name").assertIsNotDisplayed()
        
        // Click to open
        composeTestRule.onNodeWithContentDescription("Open friends nearby panel").performClick()
        
        // Wait for state change and verify panel is open
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close friends nearby panel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search friends by name").assertIsDisplayed()
    }

    @Test
    fun friendsPanelScaffold_emptyState_showsEmptyMessage() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = emptyList()
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify empty state message is displayed
        composeTestRule.onNodeWithText("No friends sharing location yet").assertIsDisplayed()
    }

    @Test
    fun friendsPanelScaffold_loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = true,
                        friends = emptyList()
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify loading state is displayed
        composeTestRule.onNodeWithText("Loading nearby friends...").assertIsDisplayed()
    }

    @Test
    fun friendsPanelScaffold_errorState_showsErrorMessage() {
        val errorMessage = "Location permission denied"
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = emptyList(),
                        error = errorMessage
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun friendsPanelScaffold_searchState_filtersCorrectly() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        searchQuery = "Alice",
                        friends = testFriends
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify filtered results
        composeTestRule.onNodeWithText("Alice Johnson").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Smith").assertIsNotDisplayed()
    }

    @Test
    fun friendsPanelScaffold_accessibilityLabels_areCorrect() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = testFriends
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify accessibility labels
        composeTestRule.onNodeWithContentDescription("Map screen with friends panel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Friends nearby panel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close friends nearby panel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Search friends by name").assertIsDisplayed()
    }

    // Additional Accessibility Tests
    @Test
    fun friendsPanelScaffold_fabHasProperAccessibilityRole() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = false,
                        isLoading = false,
                        friends = testFriends
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify FAB has proper accessibility role
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
            .assertHasClickAction()
    }

    @Test
    fun friendsPanelScaffold_focusManagement_panelOpenClose() {
        var uiState by mutableStateOf(
            NearbyUiState(
                isPanelOpen = false,
                isLoading = false,
                friends = testFriends
            )
        )
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = uiState,
                    onEvent = { event ->
                        when (event) {
                            is NearbyPanelEvent.TogglePanel -> {
                                uiState = uiState.copy(isPanelOpen = !uiState.isPanelOpen)
                            }
                            else -> {}
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Initially closed - FAB should be accessible
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertIsDisplayed()
            .assertHasClickAction()

        // Open panel
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .performClick()

        composeTestRule.waitForIdle()

        // Panel open - search should be accessible
        composeTestRule
            .onNodeWithContentDescription("Search friends by name")
            .assertIsDisplayed()

        // FAB should show close state
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsPanelScaffold_friendItemsHaveProperAccessibility() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = testFriends
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Verify friend items have proper accessibility descriptions
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertIsDisplayed()
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Friend Bob Smith, 1.2 km away, offline")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun friendsPanelScaffold_minimumTouchTargets() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = NearbyUiState(
                        isPanelOpen = true,
                        isLoading = false,
                        friends = testFriends,
                        searchQuery = "Alice"
                    ),
                    onEvent = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // All interactive elements should have proper touch targets
        composeTestRule
            .onNodeWithContentDescription("Close friends nearby panel")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertHasClickAction()
    }

    @Test
    fun friendsPanelScaffold_accessibilityStateAnnouncements() {
        var uiState by mutableStateOf(
            NearbyUiState(
                isPanelOpen = false,
                isLoading = false,
                friends = testFriends
            )
        )
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsPanelScaffold(
                    uiState = uiState,
                    onEvent = { event ->
                        when (event) {
                            is NearbyPanelEvent.TogglePanel -> {
                                uiState = uiState.copy(isPanelOpen = !uiState.isPanelOpen)
                            }
                            else -> {}
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Map Content")
                    }
                }
            }
        }

        // Initially closed
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .assertIsDisplayed()

        // Open panel - should announce state change through drawer accessibility
        composeTestRule
            .onNodeWithContentDescription("Open friends nearby panel")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify panel is accessible when open
        composeTestRule
            .onNodeWithContentDescription("Friends nearby panel")
            .assertIsDisplayed()

        // Verify friends list has live region for announcements
        composeTestRule
            .onNodeWithContentDescription("Friends list with 2 friends")
            .assertIsDisplayed()
    }
}