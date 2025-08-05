package com.locationsharing.app.domain.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for NearbyFriend data class, focusing on distance formatting
 * as specified in requirements 1.1, 4.5, 4.6, 6.5
 */
@RunWith(RobolectricTestRunner::class)
class NearbyFriendTest {

    @Test
    fun `formattedDistance should show meters for distances under 1000m`() {
        // Test various distances under 1000m
        val testCases = listOf(
            0.0 to "0 m",
            1.0 to "1 m",
            50.5 to "51 m", // Should round to nearest integer
            150.0 to "150 m",
            500.7 to "501 m", // Should round to nearest integer
            999.0 to "999 m",
            999.9 to "1000 m" // Should round to 1000m, not 1.0 km
        )

        testCases.forEach { (distance, expected) ->
            val nearbyFriend = createNearbyFriend(distance = distance)
            assertEquals(
                "Distance $distance should format as $expected",
                expected,
                nearbyFriend.formattedDistance
            )
        }
    }

    @Test
    fun `formattedDistance should show kilometers for distances 1000m and above`() {
        // Test various distances 1000m and above
        val testCases = listOf(
            1000.0 to "1.0 km",
            1050.0 to "1.1 km", // Should round to 1 decimal place
            1500.0 to "1.5 km",
            2000.0 to "2.0 km",
            2500.5 to "2.5 km", // Should round to 1 decimal place
            10000.0 to "10.0 km",
            15750.0 to "15.8 km", // Should round to 1 decimal place
            100000.0 to "100.0 km"
        )

        testCases.forEach { (distance, expected) ->
            val nearbyFriend = createNearbyFriend(distance = distance)
            assertEquals(
                "Distance $distance should format as $expected",
                expected,
                nearbyFriend.formattedDistance
            )
        }
    }

    @Test
    fun `calculateDistance should return correct distance between two locations`() {
        // Create two locations with known distance
        val userLocation = Location("test").apply {
            latitude = 0.0
            longitude = 0.0
        }
        
        val friendLocation = Location("test").apply {
            latitude = 0.001 // Approximately 111 meters north
            longitude = 0.0
        }

        val distance = NearbyFriend.calculateDistance(userLocation, friendLocation)
        
        // Distance should be approximately 111 meters (1 degree latitude ≈ 111km)
        assertEquals(111.0, distance, 1.0) // Allow 1 meter tolerance
    }

    @Test
    fun `calculateDistance should handle same location`() {
        val location = Location("test").apply {
            latitude = 37.7749
            longitude = -122.4194
        }

        val distance = NearbyFriend.calculateDistance(location, location)
        assertEquals(0.0, distance, 0.1) // Should be 0 with small tolerance
    }

    @Test
    fun `fromFriend should create NearbyFriend with calculated distance`() {
        val userLocation = Location("user").apply {
            latitude = 37.7749
            longitude = -122.4194
        }

        val friend = Friend(
            id = "friend1",
            name = "John Doe",
            avatarUrl = "https://example.com/avatar.jpg",
            location = FriendLocation(
                latitude = 37.7849, // ~1.1km north
                longitude = -122.4194
            ),
            status = FriendStatus(isOnline = true, lastSeen = System.currentTimeMillis())
        )

        val nearbyFriend = NearbyFriend.fromFriend(friend, userLocation)

        assertNotNull(nearbyFriend)
        assertEquals("friend1", nearbyFriend!!.id)
        assertEquals("John Doe", nearbyFriend.displayName)
        assertEquals("https://example.com/avatar.jpg", nearbyFriend.avatarUrl)
        assertEquals(true, nearbyFriend.isOnline)
        
        // Distance should be approximately 1111 meters
        assertEquals(1111.0, nearbyFriend.distance, 50.0) // Allow 50m tolerance
        assertEquals("1.1 km", nearbyFriend.formattedDistance)
    }

    @Test
    fun `fromFriend should return null when friend has no location`() {
        val userLocation = Location("user").apply {
            latitude = 37.7749
            longitude = -122.4194
        }

        val friend = Friend(
            id = "friend1",
            name = "John Doe",
            location = null // No location
        )

        val nearbyFriend = NearbyFriend.fromFriend(friend, userLocation)
        assertNull(nearbyFriend)
    }

    @Test
    fun `fromFriend should handle null user location`() {
        val friend = Friend(
            id = "friend1",
            name = "John Doe",
            location = FriendLocation(
                latitude = 37.7849,
                longitude = -122.4194
            )
        )

        val nearbyFriend = NearbyFriend.fromFriend(friend, null)

        assertNotNull(nearbyFriend)
        assertEquals(Double.MAX_VALUE, nearbyFriend!!.distance, 0.0)
    }

    @Test
    fun `fromFriend should handle empty avatar URL`() {
        val userLocation = Location("user").apply {
            latitude = 37.7749
            longitude = -122.4194
        }

        val friend = Friend(
            id = "friend1",
            name = "John Doe",
            avatarUrl = "", // Empty avatar URL
            location = FriendLocation(
                latitude = 37.7749,
                longitude = -122.4194
            )
        )

        val nearbyFriend = NearbyFriend.fromFriend(friend, userLocation)

        assertNotNull(nearbyFriend)
        assertNull(nearbyFriend!!.avatarUrl) // Should be null for empty string
    }

    @Test
    fun `distance formatting edge cases`() {
        // Test edge cases around the 1000m boundary
        val edgeCases = listOf(
            999.4 to "999 m", // Should round down
            999.5 to "1000 m", // Should round up but still show meters
            999.9 to "1000 m", // Should round up but still show meters
            1000.0 to "1.0 km", // Exactly 1000m should show as km
            1000.1 to "1.0 km"  // Just over 1000m should show as km
        )

        edgeCases.forEach { (distance, expected) ->
            val nearbyFriend = createNearbyFriend(distance = distance)
            assertEquals(
                "Edge case: distance $distance should format as $expected",
                expected,
                nearbyFriend.formattedDistance
            )
        }
    }

    private fun createNearbyFriend(
        id: String = "test_friend",
        displayName: String = "Test Friend",
        avatarUrl: String? = null,
        distance: Double = 100.0,
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