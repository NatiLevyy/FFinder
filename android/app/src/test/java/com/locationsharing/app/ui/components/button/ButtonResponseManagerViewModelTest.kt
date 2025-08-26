package com.locationsharing.app.ui.components.button

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

/**
 * Unit tests for ButtonResponseManagerViewModel.
 */
class ButtonResponseManagerViewModelTest {
    
    private lateinit var mockButtonResponseManager: ButtonResponseManager
    private lateinit var viewModel: ButtonResponseManagerViewModel
    
    @BeforeEach
    fun setUp() {
        mockButtonResponseManager = mock()
        viewModel = ButtonResponseManagerViewModel(mockButtonResponseManager)
    }
    
    @Test
    fun `viewModel should provide ButtonResponseManager`() {
        // Given & When & Then
        assertEquals(mockButtonResponseManager, viewModel.buttonResponseManager)
    }
    
    @Test
    fun `onCleared should clear all button states`() {
        // When
        viewModel.onCleared()
        
        // Then
        verify(mockButtonResponseManager).clearAllStates()
    }
}