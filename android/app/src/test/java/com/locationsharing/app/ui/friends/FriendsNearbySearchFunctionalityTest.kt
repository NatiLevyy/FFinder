package com.locationsharing.app.ui.friends

import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.domain.model.NearbyFriend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive tests for search functionality with various query scenarios.
 * 
 * Tests requirements:
 * - 3.1: Real-time search filtering by friend display name
 * - 3.2: Search query clearing functionality
 * - 3.3: No friends found matching query state
 * - 3.4: Case-insensitive and partial name matching
 */
class FriendsNearbySearchFunctionalityTest {

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
        ),
        NearbyFriend(
            id = "4",
            displayName = "Alice Cooper",
            avatarUrl = null,
            distance = 800.0,
            isOnline = true,
            lastUpdated = System.currentTimeMillis() - 120000,
            latLng = LatLng(37.7649, -122.4294)
        ),
        NearbyFriend(
            id = "5",
            displayName = "David Johnson",
            avatarUrl = null,
            distance = 3000.0,
            isOnline = false,
            lastUpdated = System.currentTimeMillis() - 600000,
            latLng = LatLng(37.8049, -122.3894)
        )
    )

    @Test
    fun searchFunctionality_exactNameMatch() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Alice Johnson"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(1, filteredFriends.size)
        assertEquals("Alice Johnson", filteredFriends[0].displayName)
    }

    @Test
    fun searchFunctionality_partialNameMatch() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Alice"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(2, filteredFriends.size)
        assertTrue("Should contain Alice Johnson", 
            filteredFriends.any { it.displayName == "Alice Johnson" })
        assertTrue("Should contain Alice Cooper", 
            filteredFriends.any { it.displayName == "Alice Cooper" })
    }

    @Test
    fun searchFunctionality_lastNameMatch() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Johnson"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(2, filteredFriends.size)
        assertTrue("Should contain Alice Johnson", 
            filteredFriends.any { it.displayName == "Alice Johnson" })
        assertTrue("Should contain David Johnson", 
            filteredFriends.any { it.displayName == "David Johnson" })
    }

    @Test
    fun searchFunctionality_caseInsensitiveMatch() {
        val testCases = listOf(
            "alice",
            "ALICE", 
            "Alice",
            "aLiCe",
            "alice johnson",
            "ALICE JOHNSON"
        )

        testCases.forEach { query ->
            val uiState = NearbyUiState(
                isLoading = false,
                friends = sampleFriends,
                searchQuery = query
            )

            val filteredFriends = uiState.filteredFriends

            assertTrue("Query '$query' should find Alice Johnson", 
                filteredFriends.any { it.displayName == "Alice Johnson" })
        }
    }

    @Test
    fun searchFunctionality_partialWordMatch() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "John"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(2, filteredFriends.size)
        assertTrue("Should contain Alice Johnson", 
            filteredFriends.any { it.displayName == "Alice Johnson" })
        assertTrue("Should contain David Johnson", 
            filteredFriends.any { it.displayName == "David Johnson" })
    }

    @Test
    fun searchFunctionality_noMatches() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "NonExistentFriend"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(0, filteredFriends.size)
    }

    @Test
    fun searchFunctionality_emptyQuery() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = ""
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(sampleFriends.size, filteredFriends.size)
        assertEquals(sampleFriends, filteredFriends)
    }

    @Test
    fun searchFunctionality_whitespaceQuery() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "   "
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(sampleFriends.size, filteredFriends.size)
    }

    @Test
    fun searchFunctionality_specialCharacters() {
        val friendsWithSpecialChars = listOf(
            NearbyFriend(
                id = "1",
                displayName = "José García",
                avatarUrl = null,
                distance = 150.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis(),
                latLng = LatLng(37.7749, -122.4194)
            ),
            NearbyFriend(
                id = "2",
                displayName = "François Müller",
                avatarUrl = null,
                distance = 1200.0,
                isOnline = false,
                lastUpdated = System.currentTimeMillis() - 300000,
                latLng = LatLng(37.7849, -122.4094)
            ),
            NearbyFriend(
                id = "3",
                displayName = "李小明",
                avatarUrl = null,
                distance = 2500.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis() - 60000,
                latLng = LatLng(37.7949, -122.3994)
            )
        )

        val uiState = NearbyUiState(
            isLoading = false,
            friends = friendsWithSpecialChars,
            searchQuery = "José"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(1, filteredFriends.size)
        assertEquals("José García", filteredFriends[0].displayName)
    }

    @Test
    fun searchFunctionality_maintainsDistanceSorting() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Alice"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(2, filteredFriends.size)
        
        // Should be sorted by distance: Alice Johnson (150m), Alice Cooper (800m)
        assertEquals("Alice Johnson", filteredFriends[0].displayName)
        assertEquals(150.0, filteredFriends[0].distance, 0.1)
        
        assertEquals("Alice Cooper", filteredFriends[1].displayName)
        assertEquals(800.0, filteredFriends[1].distance, 0.1)
    }

    @Test
    fun searchFunctionality_multipleWordsQuery() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Alice Johnson"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(1, filteredFriends.size)
        assertEquals("Alice Johnson", filteredFriends[0].displayName)
    }

    @Test
    fun searchFunctionality_partialMultipleWordsQuery() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Alice John"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(1, filteredFriends.size)
        assertEquals("Alice Johnson", filteredFriends[0].displayName)
    }

    @Test
    fun searchFunctionality_reverseWordOrder() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "Johnson Alice"
        )

        val filteredFriends = uiState.filteredFriends

        // Should still find Alice Johnson even with reversed word order
        assertEquals(1, filteredFriends.size)
        assertEquals("Alice Johnson", filteredFriends[0].displayName)
    }

    @Test
    fun searchFunctionality_singleCharacterQuery() {
        val uiState = NearbyUiState(
            isLoading = false,
            friends = sampleFriends,
            searchQuery = "A"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(2, filteredFriends.size)
        assertTrue("Should contain Alice Johnson", 
            filteredFriends.any { it.displayName == "Alice Johnson" })
        assertTrue("Should contain Alice Cooper", 
            filteredFriends.any { it.displayName == "Alice Cooper" })
    }

    @Test
    fun searchFunctionality_numericQuery() {
        val friendsWithNumbers = listOf(
            NearbyFriend(
                id = "1",
                displayName = "User123",
                avatarUrl = null,
                distance = 150.0,
                isOnline = true,
                lastUpdated = System.currentTimeMillis(),
                latLng = LatLng(37.7749, -122.4194)
            ),
            NearbyFriend(
                id = "2",
                displayName = "Test456",
                avatarUrl = null,
                distance = 1200.0,
                isOnline = false,
                lastUpdated = System.currentTimeMillis() - 300000,
                latLng = LatLng(37.7849, -122.4094)
            )
        )

        val uiState = NearbyUiState(
            isLoading = false,
            friends = friendsWithNumbers,
            searchQuery = "123"
        )

        val filteredFriends = uiState.filteredFriends

        assertEquals(1, filteredFriends.size)
        assertEquals("User123", filteredFriends[0].displayName)
    }

    @Test
    fun searchFunctionality_performanceWithLargeFriendsList() {
        // Create a large list of friends for performance testing
        val largeFriendsList = (1..1000).map { index ->
            NearbyFriend(
                id = index.toString(),
                displayName = "Friend $index",
                avatarUrl = null,
                distance = (index * 10).toDouble(),
                isOnline = index % 2 == 0,
                lastUpdated = System.currentTimeMillis() - (index * 1000),
                latLng = LatLng(37.7749 + (index * 0.001), -122.4194 + (index * 0.001))
            )
        }

        val startTime = System.currentTimeMillis()
        
        val uiState = NearbyUiState(
            isLoading = false,
            friends = largeFriendsList,
            searchQuery = "Friend 5"
        )

        val filteredFriends = uiState.filteredFriends
        
        val endTime = System.currentTimeMillis()
        val searchTime = endTime - startTime

        // Search should complete quickly (under 100ms for 1000 friends)
        assertTrue("Search should complete quickly", searchTime < 100)
        
        // Should find all friends containing "Friend 5" (5, 50, 51, 52, ..., 59, 500, 501, etc.)
        assertTrue("Should find multiple matches", filteredFriends.size > 10)
        assertTrue("All results should contain 'Friend 5'", 
            filteredFriends.all { it.displayName.contains("Friend 5") })
    }

    @Test
    fun searchFunctionality_edgeCaseQueries() {
        val edgeCaseQueries = listOf(
            "!@#$%",
            "123456789",
            "   Alice   ",
            "\t\n",
            "Alice\nJohnson",
            "Alice\tJohnson"
        )

        edgeCaseQueries.forEach { query ->
            val uiState = NearbyUiState(
                isLoading = false,
                friends = sampleFriends,
                searchQuery = query
            )

            // Should not crash with edge case queries
            val filteredFriends = uiState.filteredFriends
            assertTrue("Should handle edge case query: '$query'", filteredFriends.size >= 0)
        }
    }
}