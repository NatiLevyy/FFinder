package com.locationsharing.app.ui.map

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendLocation
import com.locationsharing.app.data.friends.FriendStatus
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.location.LocationSharingService
import com.locationsharing.app.data.location.LocationSharingState
import com.locationsharing.app.data.location.LocationSharingStatus
import com.locationsharing.app.data.location.LocationUpdate
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Integration tests for location and map functionality
 * Tests the interaction between MapScreenViewModel, location services, and map components
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapLocationIntegrationTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var locationService: EnhancedLocationService

    @MockK
    private lateinit var friendsRepository: FriendsRepository

    @MockK
    private lateinit var realTimeFriendsService: RealTimeFriendsService

    @MockK
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase

    @MockK
    private lateinit var locationSharingService: LocationSharingService

    private lateinit var viewModel: MapScreenViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val sanFrancisco = LatLng(37.7749, -122.4194)
    private val newYork = LatLng(40.7128, -74.0060)
    private val london = LatLng(51.5074, -0.1278)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        mockkStatic(ContextCompat::class)
        setupDefaultMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultMocks() {
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_GRANTED

        every { locationService.stopLocationUpdates() } returns Unit
        every { locationService.enableHighAccuracyMode(any()) } returns Unit
        every { realTimeFriendsService.startSync() } returns Unit
        every { realTimeFriendsService.stopSync() } returns Unit

        every { locationSharingService.sharingState } returns flowOf(
            LocationSharingState(LocationSharingStatus.INACTIVE)
        )
        every { locationSharingService.notifications } returns flowOf(null)
        coEvery { locationSharingService.startLocationSharing() } returns Result.success(Unit)
        coEvery { locationSharingService.stopLocationSharing() } returns Result.success(Unit)
    }

    @Test
    fun `location updates should integrate with map centering`() = testScope.runTest {
        // Given - location service provides updates
        every { locationService.getLocationUpdates() } returns flow {
            emit(LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis()))
            delay(1000)
            emit(LocationUpdate(newYork, sanFrancisco, System.currentTimeMillis()))
            delay(1000)
            emit(LocationUpdate(london, newYork, System.currentTimeMillis()))
        }

        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        // When - create viewModel
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - should start with San Francisco
        assertEquals(sanFrancisco, viewModel.state.value.currentLocation)
        assertEquals(sanFrancisco, viewModel.state.value.mapCenter)

        // When - advance time to get New York update
        advanceTimeBy(1500)

        // Then - should update to New York
        assertEquals(newYork, viewModel.state.value.currentLocation)
        assertEquals(newYork, viewModel.state.value.mapCenter)

        // When - advance time to get London update
        advanceTimeBy(1500)

        // Then - should update to London
        assertEquals(london, viewModel.state.value.currentLocation)
        assertEquals(london, viewModel.state.value.mapCenter)
    }

    @Test
    fun `location permission flow should integrate with location updates`() = testScope.runTest {
        // Given - no location permission initially
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_DENIED

        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        // When - create viewModel
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - should not have location permission
        assertFalse(viewModel.state.value.hasLocationPermission)
        assertNull(viewModel.state.value.currentLocation)

        // When - grant permission
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_GRANTED

        viewModel.onEvent(MapScreenEvent.OnLocationPermissionGranted)
        advanceUntilIdle()

        // Then - should start location updates and get location
        assertTrue(viewModel.state.value.hasLocationPermission)
        assertEquals(sanFrancisco, viewModel.state.value.currentLocation)
        verify { locationService.getLocationUpdates() }
    }

    @Test
    fun `self location centering should integrate with current location`() = testScope.runTest {
        // Given - viewModel with current location
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // Manually set a different map center
        viewModel.onEvent(MapScreenEvent.OnCameraMove(newYork, 10f))
        advanceUntilIdle()

        assertEquals(newYork, viewModel.state.value.mapCenter)

        // When - center on self location
        viewModel.onEvent(MapScreenEvent.OnSelfLocationCenter)
        advanceUntilIdle()

        // Then - should center on current location with close zoom
        assertEquals(sanFrancisco, viewModel.state.value.mapCenter)
        assertEquals(MapScreenConstants.Map.CLOSE_ZOOM, viewModel.state.value.mapZoom)
    }

    @Test
    fun `location sharing integration should work with location updates`() = testScope.runTest {
        // Given - location updates and sharing service
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        // Mock sharing state changes
        every { locationSharingService.sharingState } returns flow {
            emit(LocationSharingState(LocationSharingStatus.INACTIVE))
            delay(500)
            emit(LocationSharingState(LocationSharingStatus.ACTIVE))
        }

        viewModel = createViewModel()
        advanceUntilIdle()

        // Initially inactive
        assertFalse(viewModel.state.value.isLocationSharingActive)

        // When - start location sharing
        viewModel.onEvent(MapScreenEvent.OnStartLocationSharing)
        advanceUntilIdle()

        // Then - should call service and update state
        coVerify { locationSharingService.startLocationSharing() }
        assertTrue(viewModel.state.value.isStatusSheetVisible)

        // When - advance time to get sharing state update
        advanceTimeBy(600)

        // Then - should reflect active sharing state
        assertTrue(viewModel.state.value.isLocationSharingActive)
    }

    @Test
    fun `friends integration should work with location updates`() = testScope.runTest {
        // Given - friends near current location
        val nearbyFriends = createTestFriends()
        
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(
            nearbyFriends.map { friend ->
                mockk {
                    every { this@mockk.friend } returns friend
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            }
        )

        // When - create viewModel
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - should have friends and location
        assertEquals(sanFrancisco, viewModel.state.value.currentLocation)
        assertEquals(3, viewModel.state.value.friends.size)
        assertEquals(3, viewModel.state.value.nearbyFriendsCount)

        // When - click on a friend marker
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend_1"))
        advanceUntilIdle()

        // Then - should select friend and center map on them
        assertEquals("friend_1", viewModel.state.value.selectedFriendId)
        val selectedFriend = viewModel.state.value.getSelectedFriend()
        assertNotNull(selectedFriend)
        assertEquals(selectedFriend!!.getLatLng(), viewModel.state.value.mapCenter)
    }

    @Test
    fun `map camera integration should work with location and friends`() = testScope.runTest {
        // Given - location and friends
        val friends = createTestFriends()
        
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(
            friends.map { friend ->
                mockk {
                    every { this@mockk.friend } returns friend
                    every { updateType } returns mockk()
                    every { animationType } returns mockk()
                }
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - move camera manually
        val customLocation = LatLng(35.0, -120.0)
        viewModel.onEvent(MapScreenEvent.OnCameraMove(customLocation, 12f))
        advanceUntilIdle()

        // Then - should update map state
        assertEquals(customLocation, viewModel.state.value.mapCenter)
        assertEquals(12f, viewModel.state.value.mapZoom)

        // When - click map (should clear friend selection)
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend_1"))
        advanceUntilIdle()
        assertEquals("friend_1", viewModel.state.value.selectedFriendId)

        viewModel.onEvent(MapScreenEvent.OnMapClick(customLocation))
        advanceUntilIdle()

        // Then - should clear selection
        assertNull(viewModel.state.value.selectedFriendId)
    }

    @Test
    fun `cluster click integration should calculate bounds correctly`() = testScope.runTest {
        // Given - friends in different locations
        val clusteredFriends = listOf(
            createTestFriend("cluster_1", "Friend 1", LatLng(37.7749, -122.4194)),
            createTestFriend("cluster_2", "Friend 2", LatLng(37.7850, -122.4094)),
            createTestFriend("cluster_3", "Friend 3", LatLng(37.7649, -122.4294))
        )

        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - click on cluster
        viewModel.onEvent(MapScreenEvent.OnClusterClick(clusteredFriends))
        advanceUntilIdle()

        // Then - should center on cluster bounds
        val state = viewModel.state.value
        
        // Should be centered between the friends
        assertTrue(state.mapCenter.latitude > 37.7649)
        assertTrue(state.mapCenter.latitude < 37.7850)
        assertTrue(state.mapCenter.longitude > -122.4294)
        assertTrue(state.mapCenter.longitude < -122.4094)
        
        // Should have appropriate zoom level
        assertTrue(state.mapZoom >= 10f)
        assertTrue(state.mapZoom <= 18f)
    }

    @Test
    fun `location error recovery integration should work`() = testScope.runTest {
        // Given - location service that fails then succeeds
        var shouldFail = true
        every { locationService.getLocationUpdates() } returns flow {
            if (shouldFail) {
                throw Exception("GPS unavailable")
            } else {
                emit(LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis()))
            }
        }

        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - should have location error
        assertNotNull(viewModel.state.value.locationError)
        assertTrue(viewModel.state.value.locationError!!.contains("GPS unavailable"))

        // When - fix the service and retry
        shouldFail = false
        viewModel.onEvent(MapScreenEvent.OnRetry)
        advanceUntilIdle()

        // Then - should recover and get location
        assertEquals(sanFrancisco, viewModel.state.value.currentLocation)
        assertNull(viewModel.state.value.locationError)
    }

    @Test
    fun `high accuracy mode integration should affect location service`() = testScope.runTest {
        // Given - normal accuracy location updates
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // Initially normal accuracy
        assertFalse(viewModel.state.value.isHighAccuracyMode)

        // When - enable high accuracy
        viewModel.onEvent(MapScreenEvent.OnEnableHighAccuracyMode)
        advanceUntilIdle()

        // Then - should update state and call service
        assertTrue(viewModel.state.value.isHighAccuracyMode)
        verify { locationService.enableHighAccuracyMode(true) }

        // When - disable high accuracy
        viewModel.onEvent(MapScreenEvent.OnDisableHighAccuracyMode)
        advanceUntilIdle()

        // Then - should update state and call service
        assertFalse(viewModel.state.value.isHighAccuracyMode)
        verify { locationService.enableHighAccuracyMode(false) }
    }

    @Test
    fun `lifecycle integration should manage location updates correctly`() = testScope.runTest {
        // Given - location service
        every { locationService.getLocationUpdates() } returns flowOf(
            LocationUpdate(sanFrancisco, sanFrancisco, System.currentTimeMillis())
        )
        every { locationService.getCurrentLocation() } returns flowOf(sanFrancisco)
        every { realTimeFriendsService.getFriendUpdatesWithAnimations() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // Should start location updates on init
        verify { locationService.getLocationUpdates() }

        // When - screen pauses (should keep updates for background sharing)
        viewModel.onEvent(MapScreenEvent.OnScreenPause)
        advanceUntilIdle()

        // When - screen resumes
        viewModel.onEvent(MapScreenEvent.OnScreenResume)
        advanceUntilIdle()

        // Then - should restart location updates
        verify(atLeast = 2) { locationService.getLocationUpdates() }
    }

    private fun createViewModel(): MapScreenViewModel {
        return MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
    }

    private fun createTestFriends(): List<Friend> {
        return listOf(
            createTestFriend("friend_1", "Alice", LatLng(37.7750, -122.4195)),
            createTestFriend("friend_2", "Bob", LatLng(37.7748, -122.4193)),
            createTestFriend("friend_3", "Carol", LatLng(37.7751, -122.4196))
        )
    }

    private fun createTestFriend(id: String, name: String, location: LatLng): Friend {
        return Friend(
            id = id,
            userId = "user_$id",
            name = name,
            email = "${name.lowercase()}@example.com",
            avatarUrl = "",
            profileColor = "#FF5722",
            location = FriendLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = 10f,
                isMoving = false,
                timestamp = Date()
            ),
            status = FriendStatus(
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isLocationSharingEnabled = true
            )
        )
    }
}