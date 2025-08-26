package com.locationsharing.app.navigation

import androidx.navigation.NavController
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class NavigationIntegrationTest {
    
    private lateinit var navigationStateTracker: NavigationStateTrackerImpl
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    private lateinit var navigationManager: NavigationManagerImpl
    private lateinit var mockNavController: NavController
    
    @Before
    fun setUp() {
        navigationStateTracker = NavigationStateTrackerImpl()
        navigationErrorHandler = NavigationErrorHandler()
        navigationManager = NavigationManagerImpl(navigationStateTracker, navigationErrorHandler)
        mockNavController = mockk(relaxed = true)
        navigationManager.setNavController(mockNavController)
    }
    
    @Test
    fun `navigation system integration test`() = runTest {
        // Given - Initial state should be home
        val initialState = navigationStateTracker.currentState.first()
        assert(initialState.currentScreen == Screen.HOME)
        assert(!initialState.canNavigateBack)
        assert(initialState.navigationHistory.isEmpty())
        assert(!initialState.isNavigating)
        
        // When - Navigate to map
        navigationManager.navigateToMap()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then - Navigation should be recorded
        val mapState = navigationStateTracker.currentState.first()
        assert(mapState.currentScreen == Screen.MAP)
        assert(mapState.navigationHistory.contains(Screen.HOME))
        
        // Verify NavController was called
        verify { mockNavController.navigate(Screen.MAP.route) }
    }
    
    @Test
    fun `navigation error handling integration test`() = runTest {
        // Given - No NavController set
        val navigationManagerWithoutController = NavigationManagerImpl(
            navigationStateTracker, 
            navigationErrorHandler
        )
        
        // When - Try to navigate
        navigationManagerWithoutController.navigateToMap()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then - Should remain in initial state
        val state = navigationStateTracker.currentState.first()
        assert(state.currentScreen == Screen.HOME)
        assert(!state.isNavigating)
    }
    
    @Test
    fun `navigation state tracking integration test`() = runTest {
        // Given - Initial state
        val initialState = navigationStateTracker.currentState.first()
        assert(initialState.currentScreen == Screen.HOME)
        
        // When - Record navigation and update screen
        navigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP)
        navigationStateTracker.updateCurrentScreen(Screen.MAP)
        
        // Then - State should be updated
        val updatedState = navigationStateTracker.currentState.first()
        assert(updatedState.currentScreen == Screen.MAP)
        assert(updatedState.canNavigateBack)
        assert(updatedState.navigationHistory.contains(Screen.HOME))
        
        // When - Get previous screen
        val previousScreen = navigationStateTracker.getPreviousScreen()
        
        // Then - Should return home
        assert(previousScreen == Screen.HOME)
    }
    
    @Test
    fun `navigation error types test`() {
        // Given - Different error types
        val errors = listOf(
            NavigationError.NavigationTimeout,
            NavigationError.InvalidRoute,
            NavigationError.NavigationControllerNotFound,
            NavigationError.UnknownError(RuntimeException("Test")),
            NavigationError.InvalidNavigationState,
            NavigationError.NavigationInProgress
        )
        
        // When & Then - All errors should have user-friendly messages
        errors.forEach { error ->
            val message = navigationErrorHandler.getUserFriendlyMessage(error)
            assert(message.isNotBlank()) { "Error $error should have a user-friendly message" }
            
            // Should not throw exception when handling
            navigationErrorHandler.handleError(error)
        }
    }
    
    @Test
    fun `screen enum properties test`() {
        // Given - All screens
        val screens = Screen.values()
        
        // Then - Should have correct properties
        assert(screens.isNotEmpty())
        
        // Check specific screens
        assert(Screen.HOME.route == "home")
        assert(Screen.HOME.title == "Home")
        assert(Screen.MAP.route == "map")
        assert(Screen.MAP.title == "Map")
        assert(Screen.FRIENDS.route == "friends")
        assert(Screen.FRIENDS.title == "Friends")
        assert(Screen.SETTINGS.route == "settings")
        assert(Screen.SETTINGS.title == "Settings")
        
        // Routes should be unique
        val routes = screens.map { it.route }
        assert(routes.size == routes.toSet().size)
        
        // Titles should be unique
        val titles = screens.map { it.title }
        assert(titles.size == titles.toSet().size)
    }
}