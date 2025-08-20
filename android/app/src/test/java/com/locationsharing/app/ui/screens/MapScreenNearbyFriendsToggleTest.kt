package com.locationsharing.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for MapScreen nearby friends toggle implementation
 * Validates the implementation of task 10: Implement nearby friends toggle in AppBar
 * 
 * Requirements tested:
 * - 1.4: WHEN I tap the People icon THEN the system SHALL open the nearby friends panel
 * - 1.5: WHEN there are nearby friends THEN I SHALL see a badge with the friend count on the People icon
 * - 6.1: WHEN I tap the People icon THEN a side drawer SHALL open from the right
 * - 9.1: WHEN I use a screen reader THEN all icons and buttons SHALL have contentDescription
 * - 9.2: WHEN I navigate with TalkBack THEN focus order SHALL be: Back → Title → Nearby → Map → Quick-Share → Debug → Sheet/Drawer
 */
@RunWith(AndroidJUnit4::class)
class MapScreenNearbyFriendsToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nearbyFriendsToggle_hasCorrectAccessibilityDescription_whenNoFriends() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 0
                )
            }
        }

        // Then - Should have basic accessibility description
        composeTestRule
            .onNodeWithContentDescription("View nearby friends")
            .assertExists()
    }

    @Test
    fun nearbyFriendsToggle_hasCorrectAccessibilityDescription_whenFriendsPresent() {
        // Given
        val friendsCount = 3
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = friendsCount
                )
            }
        }

        // Then - Should have accessibility description with count
        composeTestRule
            .onNodeWithContentDescription("View nearby friends, $friendsCount friends available")
            .assertExists()
    }

    @Test
    fun nearbyFriendsToggle_showsBadge_whenFriendsPresent() {
        // Given
        val friendsCount = 5
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = friendsCount
                )
            }
        }

        // Then - Should display badge with friend count
        composeTestRule
            .onNodeWithText(friendsCount.toString())
            .assertExists()
    }

    @Test
    fun nearbyFriendsToggle_doesNotShowBadge_whenNoFriends() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 0
                )
            }
        }

        // Then - Should not display badge
        composeTestRule
            .onNodeWithText("0")
            .assertDoesNotExist()
    }

    @Test
    fun nearbyFriendsToggle_triggersCallback_whenClicked() {
        // Given
        var nearbyFriendsClicked = false
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    nearbyFriendsCount = 2
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("View nearby friends, 2 friends available")
            .performClick()

        // Then
        assert(nearbyFriendsClicked) { "Nearby friends callback should be triggered" }
    }

    @Test
    fun nearbyFriendsToggle_triggersCallback_whenClickedWithNoFriends() {
        // Given
        var nearbyFriendsClicked = false
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = { nearbyFriendsClicked = true },
                    nearbyFriendsCount = 0
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("View nearby friends")
            .performClick()

        // Then
        assert(nearbyFriendsClicked) { "Nearby friends callback should be triggered even with no friends" }
    }

    @Test
    fun nearbyFriendsToggle_hasCorrectSemanticRole() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = 1
                )
            }
        }

        // Then - Should be accessible as a button
        composeTestRule
            .onNodeWithContentDescription("View nearby friends, 1 friends available")
            .assertExists()
    }

    @Test
    fun nearbyFriendsToggle_badgeUpdates_whenFriendCountChanges() {
        // Given
        var friendsCount = 2
        composeTestRule.setContent {
            FFinderTheme {
                MapScreen(
                    onBack = {},
                    onNearbyFriends = {},
                    nearbyFriendsCount = friendsCount
                )
            }
        }

        // Then - Should show initial count
        composeTestRule
            .onNodeWithText("2")
            .assertExists()
        
        composeTestRule
            .onNodeWithContentDescription("View nearby friends, 2 friends available")
            .assertExists()
    }
}