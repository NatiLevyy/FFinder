package com.locationsharing.app.ui.friends.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.ui.theme.FFinderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for enhanced FriendsListCarousel component
 */
@RunWith(AndroidJUnit4::class)
class FriendsListCarouselTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val mockFriends = listOf(
        Friend(
            id = "1",
            name = "Alice Johnson",
            email = "alice@example.com",
            avatarUrl = "https://example.com/alice.jpg",
            profileColor = "#2196F3",
            location = FriendLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                timestamp = System.currentTimeMillis(),
                accuracy = 10.0f
            ),
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharing = true
            ),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        Friend(
            id = "2",
            name = "Bob Smith",
            email = "bob@example.com",
            avatarUrl = "https://example.com/bob.jpg",
            profileColor = "#4CAF50",
            location = FriendLocation(
                latitude = 37.7849,
                longitude = -122.4094,
                timestamp = System.currentTimeMillis(),
                accuracy = 15.0f
            ),
            status = FriendStatus(
                isOnline = false,
                lastSeen = System.currentTimeMillis() - 60000,
                isLocationSharing = false
            ),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        Friend(
            id = "3",
            name = "Charlie Brown",
            email = "charlie@example.com",
            avatarUrl = "https://example.com/charlie.jpg",
            profileColor = "#9C27B0",
            location = FriendLocation(
                latitude = 37.7649,
                longitude = -122.4294,
                timestamp = System.currentTimeMillis(),
                accuracy = 8.0f
            ),
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharing = true
            ),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )
    
    @Test
    fun `friends list carousel should display when friends are available`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Friends")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Alice")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Bob")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Charlie")
            .assertIsDisplayed()
    }
    
    @Test
    fun `friends list carousel should not display when no friends available`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = emptyList(),
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Friends")
            .assertDoesNotExist()
    }
    
    @Test
    fun `online counter should show correct count`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        // 2 out of 3 friends are online
        composeTestRule
            .onNodeWithText("2 online")
            .assertIsDisplayed()
    }
    
    @Test
    fun `friend click should trigger callback with correct id`() {
        var clickedFriendId: String? = null
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = null,
                    onFriendClick = { friendId -> clickedFriendId = friendId },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Alice Johnson, online")
            .performClick()
        
        assert(clickedFriendId == "1")
    }
    
    @Test
    fun `invite friends button should trigger callback`() {
        var inviteClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { inviteClicked = true }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Invite friends to FFinder")
            .performClick()
        
        assert(inviteClicked)
    }
    
    @Test
    fun `selected friend should have different visual state`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = "1", // Alice is selected
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Alice Johnson, online, selected")
            .assertIsDisplayed()
    }
    
    @Test
    fun `offline friend should have correct accessibility description`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Bob Smith, offline")
            .assertIsDisplayed()
    }
    
    @Test
    fun `carousel should have proper accessibility description`() {
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = mockFriends,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Friends list with 3 friends, 2 online")
            .assertIsDisplayed()
    }
    
    @Test
    fun `empty friends state should display when no friends`() {
        composeTestRule.setContent {
            FFinderTheme {
                EmptyFriendsState(
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("No friends online")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Invite friends to start sharing locations and see them on your map")
            .assertIsDisplayed()
    }
    
    @Test
    fun `empty state invite button should trigger callback`() {
        var inviteClicked = false
        
        composeTestRule.setContent {
            FFinderTheme {
                EmptyFriendsState(
                    onInviteFriendsClick = { inviteClicked = true }
                )
            }
        }
        
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithContentDescription("Invite friends to FFinder")
            .performClick()
        
        assert(inviteClicked)
    }
    
    @Test
    fun `empty state should have proper accessibility description`() {
        composeTestRule.setContent {
            FFinderTheme {
                EmptyFriendsState(
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithContentDescription("No friends available. Invite friends to start sharing locations.")
            .assertIsDisplayed()
    }
    
    @Test
    fun `carousel should handle single friend correctly`() {
        val singleFriend = listOf(mockFriends.first())
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = singleFriend,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("1 online")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Alice")
            .assertIsDisplayed()
    }
    
    @Test
    fun `carousel should handle all offline friends correctly`() {
        val offlineFriends = mockFriends.map { 
            it.copy(status = it.status.copy(isOnline = false)) 
        }
        
        composeTestRule.setContent {
            FFinderTheme {
                FriendsListCarousel(
                    friends = offlineFriends,
                    selectedFriendId = null,
                    onFriendClick = { },
                    onInviteFriendsClick = { }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("0 online")
            .assertIsDisplayed()
    }
}