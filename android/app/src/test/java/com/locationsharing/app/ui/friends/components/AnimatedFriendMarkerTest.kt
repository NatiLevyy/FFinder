package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.FriendColor
import com.locationsharing.app.data.friends.MockFriend
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for AnimatedFriendMarker component
 */
@RunWith(AndroidJUnit4::class)
class AnimatedFriendMarkerTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val mockOnlineFriend = MockFriend(
        id = "1",
        name = "John Doe",
        avatarUrl = "https://example.com/avatar.jpg",
        color = FriendColor.BLUE,
        location = LatLng(37.7749, -122.4194),
        isOnline = true,
        lastSeen = System.currentTimeMillis(),
        isMoving = false,
        movementSpeed = 0f,
        movementDirection = 0f
    )
    
    private val mockOfflineFriend = mockOnlineFriend.copy(
        id = "2",
        name = "Jane Smith",
        isOnline = false,
        lastSeen = System.currentTimeMillis() - 60000 // 1 minute ago
    )
    
    private val mockMovingFriend = mockOnlineFriend.copy(
        id = "3",
        name = "Bob Wilson",
        isMoving = true,
        movementSpeed = 0.0001f
    )
    
    @Test
    fun `online friend marker should display with proper accessibility description`() {
        composeTestRule.setContent {
            FFinderTheme {
                AnimatedFriendMarker(
                    friend = mockOnlineFriend,
                    isSelected = false,
                    onClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("John Doe is online")
            .assertIsDisplayed()
    }
    
    @Test
    fun `offline friend marker should display with proper accessibility description`() {
        composeTestRule.setContent {
            FFinderTheme {
                AnimatedFriendMarker(
                    friend = mockOfflineFriend,
                    isSelected = false,
                    onClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Jane Smith is offline")
            .assertIsDisplayed()
    }
    
    @Test
    fun `moving friend marker should display with movement indicator`() {
        composeTestRule.setContent {
            FFinderTheme {
                AnimatedFriendMarker(
                    friend = mockMovingFriend,
                    isSelected = false,
                    showMovementTrail = true,
                    onClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Bob Wilson is online and moving")
            .assertIsDisplayed()
    }
    
    @Test
    fun `marker click should trigger onClick callback`() {
        var clickCount = 0
        
        composeTestRule.setContent {
            FFinderTheme {
                AnimatedFriendMarker(
                    friend = mockOnlineFriend,
                    isSelected = false,
                    onClick = { clickCount++ }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("John Doe is online")
            .performClick()
        
        assert(clickCount == 1)
    }
    
    @Test
    fun `selected marker should have different visual state`() {
        composeTestRule.setContent {
            FFinderTheme {
                AnimatedFriendMarker(
                    friend = mockOnlineFriend,
                    isSelected = true,
                    onClick = { }
                )
            }
        }
        
        // Marker should still be displayed when selected
        composeTestRule
            .onNodeWithContentDescription("John Doe is online")
            .assertIsDisplayed()
    }
    
    @Test
    fun `simple friend marker should display with accessibility description`() {
        composeTestRule.setContent {
            FFinderTheme {
                SimpleFriendMarker(
                    friend = mockOnlineFriend,
                    onClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("John Doe - online")
            .assertIsDisplayed()
    }
    
    @Test
    fun `simple offline friend marker should display correctly`() {
        composeTestRule.setContent {
            FFinderTheme {
                SimpleFriendMarker(
                    friend = mockOfflineFriend,
                    onClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Jane Smith - offline")
            .assertIsDisplayed()
    }
    
    @Test
    fun `marker with appearance animation should eventually display`() {
        composeTestRule.setContent {
            FFinderTheme {
                AnimatedFriendMarker(
                    friend = mockOnlineFriend,
                    isSelected = false,
                    showAppearAnimation = true,
                    onClick = { }
                )
            }
        }
        
        // Wait for animation to complete
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithContentDescription("John Doe is online")
            .assertIsDisplayed()
    }
}