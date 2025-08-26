package com.locationsharing.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class NavigationManagerImplTest {
    
    private lateinit var navigationManager: NavigationManagerImpl
    private lateinit var mockNavController: NavController
    private lateinit var mockNavigationStateTracker: NavigationStateTracker
    private lateinit var mockNavigationErrorHandler: NavigationErrorHandler
    private lateinit var navigationStateFlow: MutableStateFlow<NavigationState>
    
    @Before
    fun setUp() {
        mockNavController = mockk(relaxed = true)
        mockNavigationStateTracker = mockk(relaxed = true)
        mockNavigationErrorHandler = mockk(relaxed = true)
        
        navigationStateFlow = MutableStateFlow(
            NavigationState(
                currentScreen = Screen.HOME,
                canNavigateBack = false,
                navigationHistory = emptyList(),
                isNavigating = false
            )
        )
        
        every { mockNavigationStateTracker.currentState } returns navigationStateFlow
        
        navigationManager = NavigationManagerImpl(
            mockNavigationStateTracker,
            mockNavigationErrorHandler
        )
        
        navigationManager.setNavController(mockNavController)
    }
    
    @Test
    fun `navigateToHome should navigate to home screen with proper options`() = runTest {
        // When
        navigationManager.navigateToHome()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavigationStateTracker.recordNavigation(Screen.HOME, Screen.HOME) }
        verify { mockNavigationStateTracker.updateCurrentScreen(Screen.HOME) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        val routeSlot = slot<String>()
        val optionsSlot = slot<NavOptionsBuilder.() -> Unit>()
        verify { mockNavController.navigate(capture(routeSlot), capture(optionsSlot)) }
        
        assert(routeSlot.captured == Screen.HOME.route)
    }
    
    @Test
    fun `navigateToMap should navigate to map screen`() = runTest {
        // When
        navigationManager.navigateToMap()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavigationStateTracker.recordNavigation(Screen.HOME, Screen.MAP) }
        verify { mockNavigationStateTracker.updateCurrentScreen(Screen.MAP) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        verify { mockNavController.navigate(Screen.MAP.route) }
    }
    
    @Test
    fun `navigateToFriends should navigate to friends screen`() = runTest {
        // When
        navigationManager.navigateToFriends()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavigationStateTracker.recordNavigation(Screen.HOME, Screen.FRIENDS) }
        verify { mockNavigationStateTracker.updateCurrentScreen(Screen.FRIENDS) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        verify { mockNavController.navigate(Screen.FRIENDS.route) }
    }
    
    @Test
    fun `navigateToSettings should navigate to settings screen`() = runTest {
        // When
        navigationManager.navigateToSettings()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavigationStateTracker.recordNavigation(Screen.HOME, Screen.SETTINGS) }
        verify { mockNavigationStateTracker.updateCurrentScreen(Screen.SETTINGS) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        
        verify { mockNavController.navigate(Screen.SETTINGS.route) }
    }
    
    @Test
    fun `navigateBack should return false when cannot navigate back`() {
        // Given
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        assert(!result)
    }
    
    @Test
    fun `navigateBack should return false when navigation is in progress`() {
        // Given
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = true
        )
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        assert(!result)
    }
    
    @Test
    fun `navigateBack should pop back stack when can navigate back`() {
        // Given
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        every { mockNavController.popBackStack() } returns true
        every { mockNavigationStateTracker.getPreviousScreen() } returns Screen.HOME
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        assert(result)
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavController.popBackStack() }
        verify { mockNavigationStateTracker.updateCurrentScreen(Screen.HOME) }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
    }
    
    @Test
    fun `navigateBack should fallback to home when pop back stack fails`() {
        // Given
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        every { mockNavController.popBackStack() } returns false
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        assert(result)
        verify { mockNavigationStateTracker.setNavigationInProgress(true) }
        verify { mockNavController.popBackStack() }
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
    }
    
    @Test
    fun `handleNavigationError should log error and reset navigation state`() {
        // Given
        val error = NavigationError.NavigationTimeout
        
        // When
        navigationManager.handleNavigationError(error)
        
        // Then
        verify { mockNavigationStateTracker.setNavigationInProgress(false) }
        verify { mockNavigationErrorHandler.handleError(error) }
    }
    
    @Test
    fun `navigation should handle NavigationControllerNotFound error when navController is null`() = runTest {
        // Given
        navigationManager.setNavController(null)
        
        // When
        navigationManager.navigateToMap()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then
        verify { mockNavigationErrorHandler.handleError(NavigationError.NavigationControllerNotFound) }
    }
    
    @Test
    fun `navigation should ignore subsequent calls when navigation is in progress`() = runTest {
        // Given
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = true
        )
        
        // When
        navigationManager.navigateToMap()
        
        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        
        // Then
        verify(exactly = 0) { mockNavController.navigate(any<String>()) }
    }
}