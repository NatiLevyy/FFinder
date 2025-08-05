package com.locationsharing.app.ui.friends

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for NearbyUiState and NearbyPanelEvent classes
 */
@RunWith(RobolectricTestRunner::class)
class NearbyPanelStateTest {

    @Test
    fun `filteredFriends should return all friends sorted by distance when search query is empty`() {
        val friends = listOf(
            createNearbyFriend("friend3", "Charlie", distance = 300.0),
            createNearbyFriend("friend1", "Alice", distance = 100.0),
            createNearbyFriend("friend2", "Bob", distance = 200.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            searchQuery = ""
        )

        val filteredFriends = uiState.filteredFriends
        assertEquals(3, filteredFriends.size)
        assertEquals("Alice", filteredFriends[0].displayName) // Closest
        assertEquals("Bob", filteredFriends[1].displayName)
        assertEquals("Charlie", filteredFriends[2].displayName) // Farthest
    }

    @Test
    fun `filteredFriends should return all friends sorted by distance when search query is blank`() {
        val friends = listOf(
            createNearbyFriend("friend3", "Charlie", distance = 300.0),
            createNearbyFriend("friend1", "Alice", distance = 100.0),
            createNearbyFriend("friend2", "Bob", distance = 200.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            searchQuery = "   " // Blank with spaces
        )

        val filteredFriends = uiState.filteredFriends
        assertEquals(3, filteredFriends.size)
        assertEquals("Alice", filteredFriends[0].displayName) // Closest
    }

    @Test
    fun `filteredFriends should filter by display name case insensitive`() {
        val friends = listOf(
            createNearbyFriend("friend1", "Alice Johnson", distance = 100.0),
            createNearbyFriend("friend2", "Bob Smith", distance = 200.0),
            createNearbyFriend("friend3", "Charlie Brown", distance = 300.0),
            createNearbyFriend("friend4", "Alice Cooper", distance = 400.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            searchQuery = "alice"
        )

        val filteredFriends = uiState.filteredFriends
        assertEquals(2, filteredFriends.size)
        assertEquals("Alice Johnson", filteredFriends[0].displayName) // Closer Alice
        assertEquals("Alice Cooper", filteredFriends[1].displayName) // Farther Alice
    }

    @Test
    fun `filteredFriends should filter by partial name match`() {
        val friends = listOf(
            createNearbyFriend("friend1", "John Smith", distance = 100.0),
            createNearbyFriend("friend2", "Jane Johnson", distance = 200.0),
            createNearbyFriend("friend3", "Bob Jones", distance = 300.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            searchQuery = "jo"
        )

        val filteredFriends = uiState.filteredFriends
        assertEquals(3, filteredFriends.size) // John, Johnson, Jones all match
        assertEquals("John Smith", filteredFriends[0].displayName) // Closest
        assertEquals("Jane Johnson", filteredFriends[1].displayName)
        assertEquals("Bob Jones", filteredFriends[2].displayName) // Farthest
    }

    @Test
    fun `filteredFriends should return empty list when no matches found`() {
        val friends = listOf(
            createNearbyFriend("friend1", "Alice", distance = 100.0),
            createNearbyFriend("friend2", "Bob", distance = 200.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            searchQuery = "xyz"
        )

        val filteredFriends = uiState.filteredFriends
        assertTrue(filteredFriends.isEmpty())
    }

    @Test
    fun `selectedFriend should return correct friend when selectedFriendId is set`() {
        val friends = listOf(
            createNearbyFriend("friend1", "Alice", distance = 100.0),
            createNearbyFriend("friend2", "Bob", distance = 200.0),
            createNearbyFriend("friend3", "Charlie", distance = 300.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            selectedFriendId = "friend2"
        )

        val selectedFriend = uiState.selectedFriend
        assertEquals("Bob", selectedFriend?.displayName)
        assertEquals("friend2", selectedFriend?.id)
    }

    @Test
    fun `selectedFriend should return null when selectedFriendId is null`() {
        val friends = listOf(
            createNearbyFriend("friend1", "Alice", distance = 100.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            selectedFriendId = null
        )

        assertNull(uiState.selectedFriend)
    }

    @Test
    fun `selectedFriend should return null when selectedFriendId not found in friends list`() {
        val friends = listOf(
            createNearbyFriend("friend1", "Alice", distance = 100.0)
        )

        val uiState = NearbyUiState(
            friends = friends,
            selectedFriendId = "nonexistent"
        )

        assertNull(uiState.selectedFriend)
    }

    @Test
    fun `default NearbyUiState should have correct initial values`() {
        val uiState = NearbyUiState()

        assertEquals(false, uiState.isPanelOpen)
        assertEquals(true, uiState.isLoading)
        assertEquals("", uiState.searchQuery)
        assertTrue(uiState.friends.isEmpty())
        assertNull(uiState.error)
        assertNull(uiState.userLocation)
        assertNull(uiState.selectedFriendId)
    }

    @Test
    fun `NearbyPanelEvent sealed class should have all expected event types`() {
        // Test that all event types can be instantiated
        val events = listOf(
            NearbyPanelEvent.TogglePanel,
            NearbyPanelEvent.SearchQuery("test"),
            NearbyPanelEvent.FriendClick("friend1"),
            NearbyPanelEvent.Navigate("friend1"),
            NearbyPanelEvent.Ping("friend1"),
            NearbyPanelEvent.StopSharing("friend1"),
            NearbyPanelEvent.Message("friend1"),
            NearbyPanelEvent.DismissBottomSheet,
            NearbyPanelEvent.RefreshFriends,
            NearbyPanelEvent.ClearError
        )

        // Verify all events are instances of NearbyPanelEvent
        events.forEach { event ->
            assertTrue("$event should be instance of NearbyPanelEvent", 
                event is NearbyPanelEvent)
        }
    }

    @Test
    fun `NearbyPanelEvent data classes should hold correct data`() {
        val searchEvent = NearbyPanelEvent.SearchQuery("test query")
        assertEquals("test query", searchEvent.query)

        val friendClickEvent = NearbyPanelEvent.FriendClick("friend123")
        assertEquals("friend123", friendClickEvent.friendId)

        val navigateEvent = NearbyPanelEvent.Navigate("friend456")
        assertEquals("friend456", navigateEvent.friendId)

        val pingEvent = NearbyPanelEvent.Ping("friend789")
        assertEquals("friend789", pingEvent.friendId)

        val stopSharingEvent = NearbyPanelEvent.StopSharing("friend101")
        assertEquals("friend101", stopSharingEvent.friendId)

        val messageEvent = NearbyPanelEvent.Message("friend202")
        assertEquals("friend202", messageEvent.friendId)
    }

    private fun createNearbyFriend(
        id: String,
        displayName: String,
        distance: Double = 100.0,
        avatarUrl: String? = null,
        isOnline: Boolean = true,
        lastUpdated: Long = System.currentTimeMillis(),
        latLng: LatLng = LatLng(0.0, 0.0),
        location: Location? = null
    ): NearbyFriend {
        return NearbyFriend(
            id = id,
            displayName = displayName,
            avatarUrl = avatarUrl,
            distance = distance,
            isOnline = isOnline,
            lastUpdated = lastUpdated,
            latLng = latLng,
            location = location
        )
    }
}