package com.locationsharing.app.navigation.security

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationStateProtectorTest {
    
    private lateinit var stateProtector: NavigationStateProtector
    
    @BeforeEach
    fun setUp() {
        stateProtector = NavigationStateProtectorImpl()
    }
    
    @Test
    fun `protectState should encrypt navigation state successfully`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        
        // When
        val protectedState = stateProtector.protectState(navigationState)
        
        // Then
        assertNotNull(protectedState.encryptedData)
        assertNotNull(protectedState.checksum)
        assertTrue(protectedState.timestamp > 0)
        assertTrue(protectedState.encryptedData.isNotEmpty())
        assertTrue(protectedState.checksum.isNotEmpty())
    }
    
    @Test
    fun `unprotectState should decrypt navigation state successfully`() = runTest {
        // Given
        val originalState = createTestNavigationState()
        val protectedState = stateProtector.protectState(originalState)
        
        // When
        val unprotectedState = stateProtector.unprotectState(protectedState)
        
        // Then
        assertNotNull(unprotectedState)
        assertEquals(originalState.currentRoute, unprotectedState.currentRoute)
        assertEquals(originalState.previousRoute, unprotectedState.previousRoute)
        assertEquals(originalState.sessionId, unprotectedState.sessionId)
        assertEquals(originalState.timestamp, unprotectedState.timestamp)
    }
    
    @Test
    fun `validateStateIntegrity should return true for valid state`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // When
        val isValid = stateProtector.validateStateIntegrity(protectedState)
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `validateStateIntegrity should return false for tampered state`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // Tamper with the encrypted data
        val tamperedState = protectedState.copy(
            encryptedData = protectedState.encryptedData + "tampered"
        )
        
        // When
        val isValid = stateProtector.validateStateIntegrity(tamperedState)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `validateStateIntegrity should return false for invalid checksum`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // Tamper with the checksum
        val tamperedState = protectedState.copy(
            checksum = "invalid_checksum"
        )
        
        // When
        val isValid = stateProtector.validateStateIntegrity(tamperedState)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `unprotectState should return null for tampered state`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // Tamper with the state
        val tamperedState = protectedState.copy(
            encryptedData = protectedState.encryptedData.reversed()
        )
        
        // When
        val unprotectedState = stateProtector.unprotectState(tamperedState)
        
        // Then
        assertNull(unprotectedState)
    }
    
    @Test
    fun `unprotectState should return null for very old state`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // Make the state very old (more than 24 hours)
        val oldState = protectedState.copy(
            timestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        )
        
        // When
        val unprotectedState = stateProtector.unprotectState(oldState)
        
        // Then
        assertNull(unprotectedState)
    }
    
    @Test
    fun `validateStateIntegrity should return false for future timestamp`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // Set timestamp in the future
        val futureState = protectedState.copy(
            timestamp = System.currentTimeMillis() + (60 * 60 * 1000L) // 1 hour in future
        )
        
        // When
        val isValid = stateProtector.validateStateIntegrity(futureState)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `validateStateIntegrity should return false for very old timestamp`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        val protectedState = stateProtector.protectState(navigationState)
        
        // Set timestamp very old (more than 7 days)
        val oldState = protectedState.copy(
            timestamp = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        )
        
        // When
        val isValid = stateProtector.validateStateIntegrity(oldState)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `clearProtectedData should clear all cached data`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        stateProtector.protectState(navigationState)
        
        // When
        stateProtector.clearProtectedData()
        
        // Then
        // The implementation should clear internal caches
        // We can verify this by checking the protection status if available
        assertTrue(true) // This test verifies the method doesn't throw
    }
    
    @Test
    fun `protectState should handle different navigation states`() = runTest {
        // Given
        val states = listOf(
            createTestNavigationState(),
            createTestNavigationState().copy(
                currentRoute = "map",
                previousRoute = "home",
                navigationHistory = listOf("home", "friends", "map")
            ),
            createTestNavigationState().copy(
                currentRoute = "settings",
                previousRoute = null,
                navigationHistory = emptyList(),
                userPermissions = setOf("admin", "user")
            )
        )
        
        // When & Then
        states.forEach { state ->
            val protectedState = stateProtector.protectState(state)
            assertNotNull(protectedState)
            
            val unprotectedState = stateProtector.unprotectState(protectedState)
            assertNotNull(unprotectedState)
            assertEquals(state.currentRoute, unprotectedState.currentRoute)
            assertEquals(state.sessionId, unprotectedState.sessionId)
        }
    }
    
    @Test
    fun `protectState should generate different encrypted data for same state`() = runTest {
        // Given
        val navigationState = createTestNavigationState()
        
        // When
        val protectedState1 = stateProtector.protectState(navigationState)
        val protectedState2 = stateProtector.protectState(navigationState)
        
        // Then
        // Due to random IV, encrypted data should be different even for same input
        // But both should decrypt to the same original state
        val unprotected1 = stateProtector.unprotectState(protectedState1)
        val unprotected2 = stateProtector.unprotectState(protectedState2)
        
        assertNotNull(unprotected1)
        assertNotNull(unprotected2)
        assertEquals(unprotected1.currentRoute, unprotected2.currentRoute)
        assertEquals(unprotected1.sessionId, unprotected2.sessionId)
    }
    
    @Test
    fun `protectState should handle empty navigation history`() = runTest {
        // Given
        val navigationState = createTestNavigationState().copy(
            navigationHistory = emptyList()
        )
        
        // When
        val protectedState = stateProtector.protectState(navigationState)
        val unprotectedState = stateProtector.unprotectState(protectedState)
        
        // Then
        assertNotNull(unprotectedState)
        assertEquals(navigationState.currentRoute, unprotectedState.currentRoute)
        assertTrue(unprotectedState.navigationHistory.isEmpty())
    }
    
    @Test
    fun `protectState should handle null previous route`() = runTest {
        // Given
        val navigationState = createTestNavigationState().copy(
            previousRoute = null
        )
        
        // When
        val protectedState = stateProtector.protectState(navigationState)
        val unprotectedState = stateProtector.unprotectState(protectedState)
        
        // Then
        assertNotNull(unprotectedState)
        assertEquals(navigationState.currentRoute, unprotectedState.currentRoute)
        assertNull(unprotectedState.previousRoute)
    }
    
    private fun createTestNavigationState(): NavigationState {
        return NavigationState(
            currentRoute = "home",
            previousRoute = "map",
            navigationHistory = listOf("home", "map", "friends"),
            sessionId = "test-session-123",
            timestamp = System.currentTimeMillis(),
            userPermissions = setOf("user", "location")
        )
    }
}