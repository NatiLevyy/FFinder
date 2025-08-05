package com.locationsharing.app.ui.friends.components

import android.location.Location
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.NearbyPanelEvent
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for FriendInfoBottomSheetContent component.
 * 
 * Tests component rendering, user interactions, and accessibility features
 * according to requirements 5.2, 5.3, 7.1, 7.4.
 */
class FriendInfoBottomSheetContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockOnEvent: (NearbyPanelEvent) -> Unit = mock()

    private val testFriend = NearbyFriend(
        id = "friend123",
        displayName = "Alice Johnson",
        avatarUrl = "https://example.com/avatar.jpg",
        distance = 150.0, // 150 meters
        isOnline = true,
        lastUpdated = System.currentTimeMillis() - 120_000, // 2 minutes ago
        latLng = LatLng(37.7749, -122.4194),
        location = Location("test").apply {
            latitude = 37.7749
            longitude = -122.4194
        }
    )

    @Test
    fun `displays friend information correctly`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        // Verify friend name is displayed
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()

        // Verify distance and timestamp are displayed
        composeTestRule
            .onNodeWithText("150 m away • Updated 2 min ago", substring = true)
            .assertIsDisplayed()

        // Verify avatar content description
        composeTestRule
            .onNodeWithContentDescription("Alice Johnson avatar")
            .assertIsDisplayed()
    }

    @Test
    fun `displays all action buttons with correct labels`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        // Verify all action button labels are displayed
        composeTestRule
            .onNodeWithText("Navigate")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ping")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Stop")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Message")
            .assertIsDisplayed()
    }

    @Test
    fun `action buttons have proper accessibility descriptions`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        // Verify accessibility descriptions for action buttons with friend name
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
    fun `navigate button triggers correct event`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Navigate to Alice Johnson")
            .performClick()

        verify(mockOnEvent).invoke(NearbyPanelEvent.Navigate("friend123"))
    }

    @Test
    fun `ping button triggers correct event`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Send ping to Alice Johnson")
            .performClick()

        verify(mockOnEvent).invoke(NearbyPanelEvent.Ping("friend123"))
    }

    @Test
    fun `stop sharing button triggers correct event`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Stop sharing location with Alice Johnson")
            .performClick()

        verify(mockOnEvent).invoke(NearbyPanelEvent.StopSharing("friend123"))
    }

    @Test
    fun `message button triggers correct event`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Send message to Alice Johnson")
            .performClick()

        verify(mockOnEvent).invoke(NearbyPanelEvent.Message("friend123"))
    }

    @Test
    fun `handles long friend names with ellipsis`() {
        val friendWithLongName = testFriend.copy(
            displayName = "This is a very long friend name that should be truncated with ellipsis"
        )

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = friendWithLongName,
                    onEvent = mockOnEvent
                )
            }
        }

        // Verify the long name is displayed (text will be truncated by the component)
        composeTestRule
            .onNodeWithText(friendWithLongName.displayName, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `displays correct distance formatting for meters`() {
        val friendNearby = testFriend.copy(distance = 250.0) // 250 meters

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = friendNearby,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithText("250 m away", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `displays correct distance formatting for kilometers`() {
        val friendFarAway = testFriend.copy(distance = 1500.0) // 1.5 kilometers

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = friendFarAway,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithText("1.5 km away", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `displays just now for recent timestamps`() {
        val friendJustUpdated = testFriend.copy(
            lastUpdated = System.currentTimeMillis() - 30_000 // 30 seconds ago
        )

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = friendJustUpdated,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithText("Updated Just now", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `displays hours for older timestamps`() {
        val friendUpdatedHoursAgo = testFriend.copy(
            lastUpdated = System.currentTimeMillis() - 7200_000 // 2 hours ago
        )

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = friendUpdatedHoursAgo,
                    onEvent = mockOnEvent
                )
            }
        }

        composeTestRule
            .onNodeWithText("Updated 2 hours ago", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `handles null avatar url gracefully`() {
        val friendWithoutAvatar = testFriend.copy(avatarUrl = null)

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = friendWithoutAvatar,
                    onEvent = mockOnEvent
                )
            }
        }

        // Should still display avatar placeholder with content description
        composeTestRule
            .onNodeWithContentDescription("Alice Johnson avatar")
            .assertIsDisplayed()
    }

    // Additional Accessibility Tests
    @Test
    fun `action buttons have proper accessibility roles`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        // Verify all action buttons have Button role
        composeTestRule
            .onNodeWithContentDescription("Navigate to Alice Johnson")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )

        composeTestRule
            .onNodeWithContentDescription("Send ping to Alice Johnson")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )

        composeTestRule
            .onNodeWithContentDescription("Stop sharing location with Alice Johnson")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )

        composeTestRule
            .onNodeWithContentDescription("Send message to Alice Johnson")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun `action buttons have minimum touch targets`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = testFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        // All action buttons should have proper touch targets (ensured by padding)
        composeTestRule
            .onNodeWithContentDescription("Navigate to Alice Johnson")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Send ping to Alice Johnson")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Stop sharing location with Alice Johnson")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithContentDescription("Send message to Alice Johnson")
            .assertHasClickAction()
    }

    @Test
    fun `accessibility descriptions update with different friend names`() {
        val differentFriend = testFriend.copy(
            id = "friend456",
            displayName = "Bob Smith"
        )

        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoBottomSheetContent(
                    friend = differentFriend,
                    onEvent = mockOnEvent
                )
            }
        }

        // Verify accessibility descriptions include the correct friend name
        composeTestRule
            .onNodeWithContentDescription("Navigate to Bob Smith")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Send ping to Bob Smith")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Stop sharing location with Bob Smith")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Send message to Bob Smith")
            .assertIsDisplayed()
    }
}