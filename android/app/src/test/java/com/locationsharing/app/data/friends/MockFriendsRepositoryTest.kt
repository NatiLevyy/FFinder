package com.locationsharing.app.data.friends

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MockFriendsRepository
 */
class MockFriendsRepositoryTest {
    
    private lateinit var repository: MockFriendsRepository
    
    @Before
    fun setUp() {
        repository = MockFriendsRepository()
    }
    
    @Test
    fun `repository should generate 7 mock friends`() = runTest {
        val friends = repository.friends.first()
        assertEquals(7, friends.size)
    }
    
    @Test
    fun `friends should have diverse names and colors`() = runTest {
        val friends = repository.friends.first()
        
        // Check that all friends have different names
        val names = friends.map { it.name }
        assertEquals(names.size, names.distinct().size)
        
        // Check that all friends have different colors
        val colors = friends.map { it.color }
        assertEquals(colors.size, colors.distinct().size)
    }
    
    @Test
    fun `some friends should be online and some offline`() = runTest {
        val friends = repository.friends.first()
        
        val onlineFriends = friends.filter { it.isOnline }
        val offlineFriends = friends.filter { !it.isOnline }
        
        assertTrue("Should have online friends", onlineFriends.isNotEmpty())
        assertTrue("Should have offline friends", offlineFriends.isNotEmpty())
    }
    
    @Test
    fun `moving friends should have movement speed greater than 0`() = runTest {
        val friends = repository.friends.first()
        
        friends.filter { it.isMoving }.forEach { friend ->
            assertTrue("Moving friend should have speed > 0", friend.movementSpeed > 0f)
            assertTrue("Movement direction should be valid", friend.movementDirection in 0f..360f)
        }
    }
    
    @Test
    fun `selectFriend should update selectedFriend flow`() = runTest {
        val friends = repository.friends.first()
        val firstFriend = friends.first()
        
        repository.selectFriend(firstFriend.id)
        
        val selectedFriend = repository.selectedFriend.first()
        assertNotNull(selectedFriend)
        assertEquals(firstFriend.id, selectedFriend?.id)
    }
    
    @Test
    fun `clearSelection should set selectedFriend to null`() = runTest {
        val friends = repository.friends.first()
        val firstFriend = friends.first()
        
        // First select a friend
        repository.selectFriend(firstFriend.id)
        assertNotNull(repository.selectedFriend.first())
        
        // Then clear selection
        repository.clearSelection()
        assertEquals(null, repository.selectedFriend.first())
    }
    
    @Test
    fun `toggleFriendOnlineStatus should change friend status`() = runTest {
        val friends = repository.friends.first()
        val firstFriend = friends.first()
        val originalStatus = firstFriend.isOnline
        
        repository.toggleFriendOnlineStatus(firstFriend.id)
        
        val updatedFriends = repository.friends.first()
        val updatedFriend = updatedFriends.find { it.id == firstFriend.id }
        
        assertNotNull(updatedFriend)
        assertEquals(!originalStatus, updatedFriend?.isOnline)
    }
    
    @Test
    fun `getOnlineFriends should return only online friends`() = runTest {
        val onlineFriends = repository.getOnlineFriends().first()
        
        assertTrue("All returned friends should be online", 
            onlineFriends.all { it.isOnline })
    }
    
    @Test
    fun `getFriendById should return correct friend`() = runTest {
        val friends = repository.friends.first()
        val firstFriend = friends.first()
        
        val foundFriend = repository.getFriendById(firstFriend.id)
        
        assertNotNull(foundFriend)
        assertEquals(firstFriend.id, foundFriend?.id)
        assertEquals(firstFriend.name, foundFriend?.name)
    }
    
    @Test
    fun `getFriendById should return null for invalid id`() = runTest {
        val foundFriend = repository.getFriendById("invalid-id")
        assertEquals(null, foundFriend)
    }
}