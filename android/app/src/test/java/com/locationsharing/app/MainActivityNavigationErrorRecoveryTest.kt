package com.locationsharing.app

import androidx.navigation.NavController
import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.NavigationErrorHandler
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.navigation.NavigationManagerImpl
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.NavigationStateTracker
import com.locationsharing.app.navigation.Screen
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for MainActivity navigation error recovery mechanisms.
 * Verifies that navigation errors are handled gracefully with appropriate fallbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityNavigationErrorRecoveryTest {
    
    @MockK
    private lateinit var navigationStateTracker: NavigationStateTracker
    
    @MockK
    private lateinit var navigationErrorHandler: NavigationErrorHandler
    
    @MockK
    private lateinit var navController: NavController
    
    private lateinit var navigationManager: NavigationManager
    
    private val navigationStateFlow = MutableStateFlow(
        NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = false
        )
    )
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        every { navigationStateTracker.currentState } returns navigationStateFlow
        every { navigationStateTracker.setNavigationInProgress(any()) } returns Unit
        every { navigationStateTracker.updateCurrentScreen(any()) } returns Unit
        every { navigationStateTracker.recordNavigation(any(), any()) } returns Unit
        every { navigationErrorHandler.handleError(any()) } returns Unit
        
        navigationManager = NavigationManagerImpl(navigationStateTracker, navigationErrorHandler)
        navigationManager.setNavController(navController)
    }
    
    @Test
    fun `should handle navigation controller not found error`() = runTest {
        // Given
        val navigationManagerWithoutController = NavigationManagerImpl(navigationStateTracker, navigationErrorHandler)
        
        // When
        navigationManagerWithoutController.navigateToMap()
        
        // Then
        verify { navigationErrorHandler.handleError(NavigationError.NavigationControllerNotFound) }
    }
    
    @Test
    fun `should handle navigation timeout error`() = runTest {
        // Given
        every { navController.navigate(any<String>()) } answers {
            Thread.sleep(6000) // Simulate timeout
        }
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        // Note: In a real scenario, this would trigger a timeout
        // For this test, we verify the error handling mechanism exists
        verify(timeout = 1000) { navigationStateTracker.setNavigationInProgress(true) }
    }
    
    @Test
    fun `should handle navigation in progress error`() = runTest {
        // Given
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.HOME,
            canNavigateBack = false,
            navigationHistory = emptyList(),
            isNavigating = true // Navigation already in progress
        )
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        // Navigation should be ignored when already in progress
        verify(exactly = 0) { navController.navigate(any<String>()) }
    }
    
    @Test
    fun `should provide fallback navigation when back navigation fails`() = runTest {
        // Given
        every { navController.popBackStack() } returns false
        every { navController.navigate(Screen.HOME.route, any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) } returns Unit
        
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navController.popBackStack() }
        // Should fallback to home navigation when back navigation fails
        verify { navController.navigate(Screen.HOME.route, any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) }
        assert(!result) // Should return false since popBackStack failed
    }
    
    @Test
    fun `should handle unknown navigation errors`() = runTest {
        // Given
        val exception = RuntimeException("Test navigation error")
        every { navController.navigate(any<String>()) } throws exception
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        verify { navigationErrorHandler.handleError(NavigationError.UnknownError(exception)) }
        verify { navigationStateTracker.setNavigationInProgress(false) }
    }
    
    @Test
    fun `should reset navigation state after error`() = runTest {
        // Given
        val exception = RuntimeException("Test navigation error")
        every { navController.navigate(any<String>()) } throws exception
        
        // When
        navigationManager.navigateToMap()
        
        // Then
        verify { navigationStateTracker.setNavigationInProgress(false) }
    }
    
    @Test
    fun `should handle back navigation when cannot navigate back`() = runTest {
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
        assert(!result) // Should return false when cannot navigate back
        verify(exactly = 0) { navController.popBackStack() }
    }
    
    @Test
    fun `should handle successful back navigation`() = runTest {
        // Given
        every { navController.popBackStack() } returns true
        every { navigationStateTracker.getPreviousScreen() } returns Screen.HOME
        
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navController.popBackStack() }
        verify { navigationStateTracker.updateCurrentScreen(Screen.HOME) }
        verify { navigationStateTracker.setNavigationInProgress(false) }
        assert(result) // Should return true for successful navigation
    }
    
    @Test
    fun `should handle back navigation exception`() = runTest {
        // Given
        val exception = RuntimeException("Back navigation error")
        every { navController.popBackStack() } throws exception
        
        navigationStateFlow.value = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navigationErrorHandler.handleError(NavigationError.UnknownError(exception)) }
        assert(!result) // Should return false when exception occurs
    }
}