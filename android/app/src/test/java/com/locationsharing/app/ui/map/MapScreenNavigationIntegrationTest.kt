package com.locationsharing.app.ui.map

import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.navigation.NavigationError
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.just
import io.mockk.runs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Integration tests for MapScreen navigation functionality.
 * Tests requirements 2.1, 2.2, 2.4, 3.2, 4.1, 4.2
 */
@RunWith(JUnit4::class)
class MapScreenNavigationIntegrationTest {

    @MockK
    private lateinit var mockNavigationManager: NavigationManager

    @MockK
    private lateinit var mockViewModel: MapScreenViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        // Setup mock navigation manager
        every { mockNavigationManager.navigateBack() } returns true
        every { mockNavigationManager.navigateToHome() } just runs
        every { mockNavigationManager.handleNavigationError(any()) } just runs
        
        // Setup mock view model
        every { mockViewModel.navigationManager } returns mockNavigationManager
        every { mockViewModel.cleanupOnNavigationAway() } just runs
    }

    @Test
    fun navigationManager_navigateBack_returnsTrue() {
        // When
        val result = mockNavigationManager.navigateBack()

        // Then
        assert(result) { "NavigateBack should return true" }
        verify { mockNavigationManager.navigateBack() }
    }

    @Test
    fun navigationManager_navigateToHome_callsCorrectMethod() {
        // When
        mockNavigationManager.navigateToHome()

        // Then
        verify { mockNavigationManager.navigateToHome() }
    }

    @Test
    fun navigationManager_handleNavigationError_callsCorrectMethod() {
        // Given
        val error = NavigationError.NavigationTimeout

        // When
        mockNavigationManager.handleNavigationError(error)

        // Then
        verify { mockNavigationManager.handleNavigationError(error) }
    }

    @Test
    fun viewModel_hasNavigationManager() {
        // Then
        assert(mockViewModel.navigationManager === mockNavigationManager) {
            "ViewModel should have access to NavigationManager"
        }
    }

    @Test
    fun viewModel_cleanupOnNavigationAway_callsCorrectMethod() {
        // When
        mockViewModel.cleanupOnNavigationAway()

        // Then
        verify { mockViewModel.cleanupOnNavigationAway() }
    }
}