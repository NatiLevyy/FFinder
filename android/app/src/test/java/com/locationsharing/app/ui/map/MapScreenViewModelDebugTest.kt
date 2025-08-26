package com.locationsharing.app.ui.map

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.friends.RealTimeFriendsService
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.data.location.LocationSharingService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Test class for MapScreenViewModel debug functionality
 * Tests requirements 4.2, 4.3
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MapScreenViewModelDebugTest {
    
    @Mock
    private lateinit var locationService: EnhancedLocationService
    
    @Mock
    private lateinit var friendsRepository: FriendsRepository
    
    @Mock
    private lateinit var realTimeFriendsService: RealTimeFriendsService
    
    @Mock
    private lateinit var getNearbyFriendsUseCase: GetNearbyFriendsUseCase
    
    @Mock
    private lateinit var locationSharingService: LocationSharingService
    
    private lateinit var context: Context
    private lateinit var viewModel: MapScreenViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        viewModel = MapScreenViewModel(
            context = context,
            locationService = locationService,
            friendsRepository = friendsRepository,
            realTimeFriendsService = realTimeFriendsService,
            getNearbyFriendsUseCase = getNearbyFriendsUseCase,
            locationSharingService = locationSharingService
        )
    }
    
    @Test
    fun addTestFriendsOnMap_whenDebugBuild_addsTestFriends() = testScope.runTest {
        // Given - debug build and initial state
        val initialFriendsCount = viewModel.state.value.friends.size
        
        // When - adding test friends (requirement 4.2)
        viewModel.addTestFriendsOnMap()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - test friends should be added in debug builds
        if (BuildConfig.DEBUG) {
            val currentState = viewModel.state.value
            assertTrue("Friends should be added", currentState.friends.size > initialFriendsCount)
            assertEquals("Should add 5 debug friends", 5, currentState.friends.size - initialFriendsCount)
            assertNotNull("Debug snackbar message should be shown", currentState.debugSnackbarMessage)
            assertTrue("Snackbar should contain success message", 
                currentState.debugSnackbarMessage?.contains("Added") == true)
        }
    }
    
    @Test
    fun addTestFriendsOnMap_whenDebugBuild_showsConfirmationSnackbar() = testScope.runTest {
        // Given - debug build
        
        // When - adding test friends
        viewModel.addTestFriendsOnMap()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - confirmation snackbar should be shown (requirement 4.3)
        if (BuildConfig.DEBUG) {
            val currentState = viewModel.state.value
            assertNotNull("Debug snackbar message should be shown", currentState.debugSnackbarMessage)
            assertTrue("Snackbar should contain flask emoji", 
                currentState.debugSnackbarMessage?.contains("🧪") == true)
            assertTrue("Snackbar should contain 'Debug:' text", 
                currentState.debugSnackbarMessage?.contains("Debug:") == true)
            assertTrue("Snackbar should contain 'test friends' text", 
                currentState.debugSnackbarMessage?.contains("test friends") == true)
        }
    }
    
    @Test
    fun dismissDebugSnackbar_clearsSnackbarMessage() = testScope.runTest {
        // Given - debug snackbar is shown
        if (BuildConfig.DEBUG) {
            viewModel.addTestFriendsOnMap()
            testDispatcher.scheduler.advanceUntilIdle()
            assertNotNull("Snackbar should be shown", viewModel.state.value.debugSnackbarMessage)
            
            // When - dismissing snackbar
            viewModel.dismissDebugSnackbar()
            
            // Then - snackbar message should be cleared
            assertNull("Snackbar message should be cleared", viewModel.state.value.debugSnackbarMessage)
        }
    }
    
    @Test
    fun debugAddFriends_createsValidFriendData() = testScope.runTest {
        // Given - debug build and current location
        if (BuildConfig.DEBUG) {
            // Set a current location for the test
            val testLocation = LatLng(37.7749, -122.4194)
            
            // When - adding test friends
            viewModel.addTestFriendsOnMap()
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Then - created friends should have valid data
            val friends = viewModel.state.value.friends
            assertTrue("Should have friends", friends.isNotEmpty())
            
            friends.forEach { friend ->
                assertTrue("Friend should have valid ID", friend.id.startsWith("debug_"))
                assertTrue("Friend should have valid name", friend.name.isNotBlank())
                assertTrue("Friend should have valid email", friend.email.contains("@"))
                assertNotNull("Friend should have location", friend.location)
                assertNotNull("Friend should have status", friend.status)
                assertTrue("Friend should be online", friend.status.isOnline)
                assertTrue("Friend should have location sharing enabled", 
                    friend.status.isLocationSharingEnabled)
            }
        }
    }
    
    @Test
    fun debugEvent_onDebugAddFriends_triggersAddTestFriends() = testScope.runTest {
        // Given - debug build
        val initialFriendsCount = viewModel.state.value.friends.size
        
        // When - handling debug add friends event
        viewModel.onEvent(MapScreenEvent.OnDebugAddFriends)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - should add test friends
        if (BuildConfig.DEBUG) {
            val currentState = viewModel.state.value
            assertTrue("Friends should be added", currentState.friends.size > initialFriendsCount)
            assertNotNull("Debug snackbar should be shown", currentState.debugSnackbarMessage)
        }
    }
}