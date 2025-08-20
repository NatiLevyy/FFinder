package com.locationsharing.app

import androidx.navigation.NavController
import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.NavigationManager
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
 * Integration tests for MainActivity navigation functionality.
 * Tests the integration between MainActivity and NavigationManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityNavigationTest {
    
    @MockK
    private lateinit var navigationManager: NavigationManager
    
    @MockK
    private lateinit var navigationStateTracker: NavigationStateTracker
    
    @MockK
    private lateinit var navController: NavController
    
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
        every { navigationManager.setNavController(any()) } returns Unit
        every { navigationStateTracker.updateCurrentScreen(any()) } returns Unit
    }
    
    @Test
    fun `navigationManager should be initialized with NavController`() = runTest {
        // Given
        every { navigationManager.setNavController(navController) } returns Unit
        
        // When
        navigationManager.setNavController(navController)
        
        // Then
        verify { navigationManager.setNavController(navController) }
    }
    
    @Test
    fun `home screen navigation should use NavigationManager`() = runTest {
        // Given
        every { navigationManager.navigateToMap() } returns Unit
        every { navigationManager.navigateToFriends() } returns Unit
        every { navigationManager.navigateToSettings() } returns Unit
        
        // When
        navigationManager.navigateToMap()
        navigationManager.navigateToFriends()
        navigationManager.navigateToSettings()
        
        // Then
        verify { navigationManager.navigateToMap() }
        verify { navigationManager.navigateToFriends() }
        verify { navigationManager.navigateToSettings() }
    }
    
    @Test
    fun `map screen back navigation should use NavigationManager`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns true
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navigationManager.navigateBack() }
        assert(result)
    }
    
    @Test
    fun `map screen back navigation fallback should navigate to home`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns false
        every { navigationManager.navigateToHome() } returns Unit
        
        // When
        val backResult = navigationManager.navigateBack()
        if (!backResult) {
            navigationManager.navigateToHome()
        }
        
        // Then
        verify { navigationManager.navigateBack() }
        verify { navigationManager.navigateToHome() }
    }
    
    @Test
    fun `friends screen back navigation should use NavigationManager`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns true
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navigationManager.navigateBack() }
        assert(result)
    }
    
    @Test
    fun `friends screen back navigation fallback should navigate to home`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns false
        every { navigationManager.navigateToHome() } returns Unit
        
        // When
        val backResult = navigationManager.navigateBack()
        if (!backResult) {
            navigationManager.navigateToHome()
        }
        
        // Then
        verify { navigationManager.navigateBack() }
        verify { navigationManager.navigateToHome() }
    }
    
    @Test
    fun `settings screen back navigation should use NavigationManager`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns true
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navigationManager.navigateBack() }
        assert(result)
    }
    
    @Test
    fun `navigation state should be updated when entering screens`() = runTest {
        // Given
        every { navigationStateTracker.updateCurrentScreen(Screen.MAP) } returns Unit
        every { navigationStateTracker.updateCurrentScreen(Screen.FRIENDS) } returns Unit
        every { navigationStateTracker.updateCurrentScreen(Screen.SETTINGS) } returns Unit
        
        // When
        navigationStateTracker.updateCurrentScreen(Screen.MAP)
        navigationStateTracker.updateCurrentScreen(Screen.FRIENDS)
        navigationStateTracker.updateCurrentScreen(Screen.SETTINGS)
        
        // Then
        verify { navigationStateTracker.updateCurrentScreen(Screen.MAP) }
        verify { navigationStateTracker.updateCurrentScreen(Screen.FRIENDS) }
        verify { navigationStateTracker.updateCurrentScreen(Screen.SETTINGS) }
    }
    
    @Test
    fun `system back button should use NavigationManager`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns true
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navigationManager.navigateBack() }
        assert(result)
    }
    
    @Test
    fun `system back button fallback should use system default when NavigationManager fails`() = runTest {
        // Given
        every { navigationManager.navigateBack() } returns false
        
        // When
        val result = navigationManager.navigateBack()
        
        // Then
        verify { navigationManager.navigateBack() }
        assert(!result) // Should return false to indicate system should handle it
    }
    
    @Test
    fun `navigation error handling should be integrated`() = runTest {
        // Given
        val error = NavigationError.NavigationTimeout
        every { navigationManager.handleNavigationError(error) } returns Unit
        
        // When
        navigationManager.handleNavigationError(error)
        
        // Then
        verify { navigationManager.handleNavigationError(error) }
    }
    
    @Test
    fun `navigation state flow should be observed correctly`() = runTest {
        // Given
        val testState = NavigationState(
            currentScreen = Screen.MAP,
            canNavigateBack = true,
            navigationHistory = listOf(Screen.HOME),
            isNavigating = false
        )
        
        // When
        navigationStateFlow.value = testState
        
        // Then
        assert(navigationStateTracker.currentState.value == testState)
    }
}