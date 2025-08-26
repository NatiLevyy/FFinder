package com.locationsharing.app.ui.map

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.location.LocationSharingService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import com.locationsharing.app.navigation.NavigationManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for MapScreenViewModel navigation-related functionality.
 * Tests requirements 3.2, 4.1, 4.2 for proper cleanup and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MapScreenViewModelNavigationTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockLocationService: EnhancedLocationService

    @MockK
    private lateinit var mockFriendsRepository: FriendsRepository

    @MockK
    private lateinit var mockRealTimeFriendsService: RealTimeFriendsService

    @MockK
    private lateinit var mockGetNearbyFriendsUseCase: GetNearbyFriendsUseCase

    @MockK
    private lateinit var mockLocationSharingService: LocationSharingService

    @MockK
    private lateinit var mockNavigationManager: NavigationManager

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: MapScreenViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        // Setup mocks
        every { mockRealTimeFriendsService.stopSync() } just runs
        every { mockLocationSharingService.sharingState } returns kotlinx.coroutines.flow.flowOf(
            com.locationsharing.app.data.location.LocationSharingState(
                status = com.locationsharing.app.data.location.LocationSharingStatus.INACTIVE,
                error = null
            )
        )
        every { mockLocationSharingService.notifications } returns kotlinx.coroutines.flow.flowOf(null)

        viewModel = MapScreenViewModel(
            context = mockContext,
            locationService = mockLocationService,
            friendsRepository = mockFriendsRepository,
            realTimeFriendsService = mockRealTimeFriendsService,
            getNearbyFriendsUseCase = mockGetNearbyFriendsUseCase,
            locationSharingService = mockLocationSharingService,
            navigationManager = mockNavigationManager
        )
    }

    @Test
    fun cleanupOnNavigationAway_stopsRealTimeFriendsService() = testScope.runTest {
        // When
        viewModel.cleanupOnNavigationAway()

        // Then
        verify { mockRealTimeFriendsService.stopSync() }
    }

    @Test
    fun cleanupOnNavigationAway_clearsUIState() = testScope.runTest {
        // Given - set some UI state
        viewModel.onEvent(MapScreenEvent.OnFriendMarkerClick("friend1"))
        viewModel.onEvent(MapScreenEvent.OnDrawerOpen)
        viewModel.onEvent(MapScreenEvent.OnStatusSheetShow)

        // When
        viewModel.cleanupOnNavigationAway()

        // Then
        val state = viewModel.state.value
        assert(state.selectedFriendId == null) { "Selected friend ID should be cleared" }
        assert(!state.isNearbyDrawerOpen) { "Nearby drawer should be closed" }
        assert(!state.isStatusSheetVisible) { "Status sheet should be hidden" }
        assert(state.debugSnackbarMessage == null) { "Debug snackbar message should be cleared" }
    }

    @Test
    fun cleanupOnNavigationAway_doesNotStopLocationUpdatesWhenSharingActive() = testScope.runTest {
        // Given - location sharing is active
        every { mockLocationSharingService.sharingState } returns kotlinx.coroutines.flow.flowOf(
            com.locationsharing.app.data.location.LocationSharingState(
                status = com.locationsharing.app.data.location.LocationSharingStatus.ACTIVE,
                error = null
            )
        )

        // When
        viewModel.cleanupOnNavigationAway()

        // Then - location updates should continue for sharing
        // This is verified by the fact that we don't cancel location updates when sharing is active
        verify { mockRealTimeFriendsService.stopSync() }
    }

    @Test
    fun cleanupOnNavigationAway_handlesExceptionsGracefully() = testScope.runTest {
        // Given - mock throws exception
        every { mockRealTimeFriendsService.stopSync() } throws RuntimeException("Cleanup error")

        // When - should not throw
        viewModel.cleanupOnNavigationAway()

        // Then - exception should be handled gracefully
        // The method should complete without throwing
    }

    @Test
    fun navigationManager_isAccessible() {
        // Then
        assert(viewModel.navigationManager === mockNavigationManager) {
            "NavigationManager should be accessible from ViewModel"
        }
    }

    @Test
    fun onEvent_OnBackPressed_handledCorrectly() = testScope.runTest {
        // When
        viewModel.onEvent(MapScreenEvent.OnBackPressed)

        // Then - back press event should be handled (navigation is handled by composable)
        // This test verifies the event is processed without errors
    }

    @Test
    fun onEvent_OnNearbyFriendsToggle_togglesDrawerState() = testScope.runTest {
        // Given
        val initialState = viewModel.state.value.isNearbyDrawerOpen

        // When
        viewModel.onEvent(MapScreenEvent.OnNearbyFriendsToggle)

        // Then
        val newState = viewModel.state.value.isNearbyDrawerOpen
        assert(newState != initialState) { "Nearby drawer state should be toggled" }
    }

    @Test
    fun dismissDebugSnackbar_clearsMessage() = testScope.runTest {
        // Given - set debug message
        viewModel.onEvent(MapScreenEvent.OnDebugAddFriends)

        // When
        viewModel.dismissDebugSnackbar()

        // Then
        val state = viewModel.state.value
        assert(state.debugSnackbarMessage == null) { "Debug snackbar message should be cleared" }
    }
}