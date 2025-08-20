package com.locationsharing.app.navigation

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NavigationModelsTest {
    
    @Test
    fun `Screen enum should have correct routes and titles`() {
        // Then
        assert(Screen.HOME.route == "home")
        assert(Screen.HOME.title == "Home")
        
        assert(Screen.MAP.route == "map")
        assert(Screen.MAP.title == "Map")
        
        assert(Screen.FRIENDS.route == "friends")
        assert(Screen.FRIENDS.title == "Friends")
        
        assert(Screen.SETTINGS.route == "settings")
        assert(Screen.SETTINGS.title == "Settings")
    }
    
    @Test
    fun `NavigationState should be created with correct default values`() {
        // Given
        val navigationState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        // Then
        assert(navigationState.currentScreen == Screen.HOME)
        assert(!navigationState.canNavigateBack)
        assert(navigationState.navigationHistory.isEmpty())
        assert(!navigationState.isNavigating)
    }
    
    @Test
    fun `NavigationState should support copy with modifications`() {
        // Given
        val originalState = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        // When
        val modifiedState = originalState.copy(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = true
        )
        
        // Then
        assert(modifiedState.currentScreen == Screen.MAP)
        assert(modifiedState.canNavigateBack)
        assert(modifiedState.navigationHistory == listOf(Screen.HOME))
        assert(modifiedState.isNavigating)
        
        // Original should remain unchanged
        assert(originalState.currentScreen == Screen.HOME)
        assert(!originalState.canNavigateBack)
        assert(originalState.navigationHistory.isEmpty())
        assert(!originalState.isNavigating)
    }
    
    @Test
    fun `NavigationState should support equality comparison`() {
        // Given
        val state1 = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = listOf(Screen.MAP),
            isNavigating = false
        )
        
        val state2 = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = listOf(Screen.MAP),
            isNavigating = false
        )
        
        val state3 = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = false,
            navigationHistory = listOf(Screen.MAP),
            isNavigating = false
        )
        
        // Then
        assert(state1 == state2)
        assert(state1 != state3)
        assert(state2 != state3)
    }
    
    @Test
    fun `NavigationError sealed class should have correct hierarchy`() {
        // Given
        val errors = listOf(
            NavigationError.NavigationControllerNotFound,
            NavigationError.InvalidRoute,
            NavigationError.NavigationTimeout,
            NavigationError.UnknownError(RuntimeException("Test")),
            NavigationError.InvalidNavigationState,
            NavigationError.NavigationInProgress
        )
        
        // Then
        errors.forEach { error ->
            assert(error is NavigationError) { "All errors should be NavigationError instances" }
        }
    }
    
    @Test
    fun `NavigationError UnknownError should contain throwable`() {
        // Given
        val testException = RuntimeException("Test exception")
        val error = NavigationError.UnknownError(testException)
        
        // Then
        assert(error.throwable == testException)
        assert(error.throwable.message == "Test exception")
    }
    
    @Test
    fun `Screen enum should have all required screens`() {
        // Given
        val expectedScreens = setOf("HOME", "MAP", "FRIENDS", "SETTINGS")
        
        // When
        val actualScreens = Screen.values().map { it.name }.toSet()
        
        // Then
        assert(actualScreens == expectedScreens) { 
            "Expected screens: $expectedScreens, but got: $actualScreens" 
        }
    }
    
    @Test
    fun `Screen routes should be unique`() {
        // Given
        val routes = Screen.values().map { it.route }
        
        // Then
        assert(routes.size == routes.toSet().size) { "All screen routes should be unique" }
    }
    
    @Test
    fun `Screen titles should be unique`() {
        // Given
        val titles = Screen.values().map { it.title }
        
        // Then
        assert(titles.size == titles.toSet().size) { "All screen titles should be unique" }
    }
}