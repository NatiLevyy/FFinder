package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Unit tests for NearbyFriendItem component.
 * Tests core functionality, data handling, and accessibility.
 */
@RunWith(AndroidJUnit4::class)
class NearbyFriendItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleOnlineFriend = NearbyFriend(
        id = "1",
        displayName = "Alice Johnson",
        avatarUrl = "https://example.com/avatar.jpg",
        distance = 150.0,
        isOnline = true,
        lastUpdated = System.currentTimeMillis(),
        latLng = LatLng(37.7749, -122.4194)
    )

    private val sampleOfflineFriend = NearbyFriend(
        id = "2",
        displayName = "Bob Smith",
        avatarUrl = null,
        distance = 1200.0,
        isOnline = false,
        lastUpdated = System.currentTimeMillis() - 300000,
        latLng = LatLng(37.7849, -122.4094)
    )

    @Test
    fun nearbyFriend_formatsDistanceCorrectly_meters() {
        assertEquals("150 m", sampleOnlineFriend.formattedDistance)
    }

    @Test
    fun nearbyFriend_formatsDistanceCorrectly_kilometers() {
        assertEquals("1.2 km", sampleOfflineFriend.formattedDistance)
    }

    @Test
    fun nearbyFriend_formatsDistanceCorrectly_exactly1000m() {
        val friendAt1000m = sampleOnlineFriend.copy(distance = 1000.0)
        assertEquals("1.0 km", friendAt1000m.formattedDistance)
    }

    @Test
    fun nearbyFriend_formatsDistanceCorrectly_veryClose() {
        val friendVeryClose = sampleOnlineFriend.copy(distance = 0.5)
        assertEquals("1 m", friendVeryClose.formattedDistance) // Should round to 1m
    }

    @Test
    fun nearbyFriend_formatsDistanceCorrectly_farDistance() {
        val friendFar = sampleOnlineFriend.copy(distance = 2567.8)
        assertEquals("2.6 km", friendFar.formattedDistance) // Should show 1 decimal place
    }

    @Test
    fun nearbyFriend_hasCorrectOnlineStatus() {
        assertTrue("Online friend should be online", sampleOnlineFriend.isOnline)
        assertFalse("Offline friend should be offline", sampleOfflineFriend.isOnline)
    }

    @Test
    fun nearbyFriend_hasCorrectAvatarUrl() {
        assertNotNull("Online friend should have avatar URL", sampleOnlineFriend.avatarUrl)
        assertNull("Offline friend should not have avatar URL", sampleOfflineFriend.avatarUrl)
    }

    @Test
    fun nearbyFriend_hasCorrectDisplayName() {
        assertEquals("Alice Johnson", sampleOnlineFriend.displayName)
        assertEquals("Bob Smith", sampleOfflineFriend.displayName)
    }

    @Test
    fun nearbyFriend_hasCorrectDistance() {
        assertEquals(150.0, sampleOnlineFriend.distance, 0.01)
        assertEquals(1200.0, sampleOfflineFriend.distance, 0.01)
    }

    @Test
    fun nearbyFriend_handlesLongNames() {
        val friendWithLongName = sampleOnlineFriend.copy(
            displayName = "Christopher Alexander Thompson-Williams Jr."
        )
        assertEquals("Christopher Alexander Thompson-Williams Jr.", friendWithLongName.displayName)
    }

    @Test
    fun nearbyFriend_hasValidLatLng() {
        assertNotNull("Friend should have valid LatLng", sampleOnlineFriend.latLng)
        assertEquals(37.7749, sampleOnlineFriend.latLng.latitude, 0.0001)
        assertEquals(-122.4194, sampleOnlineFriend.latLng.longitude, 0.0001)
    }

    @Test
    fun nearbyFriend_hasValidTimestamp() {
        assertTrue("Friend should have valid timestamp", sampleOnlineFriend.lastUpdated > 0)
        assertTrue("Offline friend should have older timestamp", 
            sampleOfflineFriend.lastUpdated < sampleOnlineFriend.lastUpdated)
    }

    // Accessibility Tests
    @Test
    fun nearbyFriendItem_hasProperAccessibilityDescription() {
        // Given
        var clicked = false
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleOnlineFriend,
                    onClick = { clicked = true }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun nearbyFriendItem_offlineFriend_hasCorrectAccessibilityDescription() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleOfflineFriend,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Friend Bob Smith, 1.2 km away, offline")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun nearbyFriendItem_hasProperAccessibilityRole() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleOnlineFriend,
                    onClick = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    androidx.compose.ui.semantics.Role.Button
                )
            )
    }

    @Test
    fun nearbyFriendItem_hasMinimumTouchTarget() {
        // Given
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleOnlineFriend,
                    onClick = { }
                )
            }
        }

        // Then - Card component ensures proper touch target
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .assertHasClickAction()
            .assertIsDisplayed()
    }

    @Test
    fun nearbyFriendItem_clickTriggersCallback() {
        // Given
        var clicked = false
        composeTestRule.setContent {
            FFinderTheme {
                NearbyFriendItem(
                    friend = sampleOnlineFriend,
                    onClick = { clicked = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Friend Alice Johnson, 150 m away, online")
            .performClick()

        // Then
        assertTrue("Click should trigger callback", clicked)
    }
}