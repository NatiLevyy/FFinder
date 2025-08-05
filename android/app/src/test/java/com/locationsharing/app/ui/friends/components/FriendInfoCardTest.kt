package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
 * Tests for enhanced FriendInfoCard component
 */
@RunWith(AndroidJUnit4::class)
class FriendInfoCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val mockOnlineFriend = MockFriend(
        id = "1",
        name = "Alice Johnson",
        avatarUrl = "https://example.com/alice.jpg",
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
        name = "Bob Smith",
        isOnline = false,
        lastSeen = System.currentTimeMillis() - (30 * 60 * 1000) // 30 minutes ago
    )
    
    private val mockMovingFriend = mockOnlineFriend.copy(
        id = "3",
        name = "Charlie Brown",
        isMoving = true,
        movementSpeed = 0.0001f
    )
    
    @Test
    fun `friend info card should display when visible with friend data`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Online")
            .assertIsDisplayed()
    }
    
    @Test
    fun `friend info card should not display when not visible`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = false,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertDoesNotExist()
    }
    
    @Test
    fun `friend info card should not display when friend is null`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = null,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Alice Johnson")
            .assertDoesNotExist()
    }
    
    @Test
    fun `offline friend should show offline status and last seen info`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOfflineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Bob Smith")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Offline")
            .assertIsDisplayed()
        
        // Should show last seen information
        composeTestRule
            .onNodeWithText("Last seen:", substring = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun `moving friend should show activity indicator`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockMovingFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Charlie Brown")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Moving")
            .assertIsDisplayed()
    }
    
    @Test
    fun `message button should always be enabled`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOfflineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Message")
            .assertIsEnabled()
    }
    
    @Test
    fun `notify button should be disabled for offline friends`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOfflineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Notify")
            .assertIsNotEnabled()
    }
    
    @Test
    fun `notify button should be enabled for online friends`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Notify")
            .assertIsEnabled()
    }
    
    @Test
    fun `more button should always be enabled`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOfflineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("More")
            .assertIsEnabled()
    }
    
    @Test
    fun `close button should trigger onDismiss callback`() {
        var dismissCalled = false
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { dismissCalled = true },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Close friend info card")
            .performClick()
        
        assert(dismissCalled)
    }
    
    @Test
    fun `message button should trigger onMessageClick callback`() {
        var messageClickedId: String? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { friendId -> messageClickedId = friendId },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Message")
            .performClick()
        
        assert(messageClickedId == mockOnlineFriend.id)
    }
    
    @Test
    fun `notify button should trigger onNotifyClick callback when enabled`() {
        var notifyClickedId: String? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { friendId -> notifyClickedId = friendId },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Notify")
            .performClick()
        
        assert(notifyClickedId == mockOnlineFriend.id)
    }
    
    @Test
    fun `more button should trigger onMoreClick callback`() {
        var moreClickedId: String? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { friendId -> moreClickedId = friendId }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("More")
            .performClick()
        
        assert(moreClickedId == mockOnlineFriend.id)
    }
    
    @Test
    fun `friend color should be displayed in header`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Blue")
            .assertIsDisplayed()
    }
    
    @Test
    fun `accessibility content description should be present`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendInfoCard(
                    friend = mockOnlineFriend,
                    isVisible = true,
                    onDismiss = { },
                    onMessageClick = { },
                    onNotifyClick = { },
                    onMoreClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Friend info card for Alice Johnson. Swipe down to dismiss.")
            .assertIsDisplayed()
    }
}