package com.locationsharing.app.navigation.security

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationSecurityManagerTest {
    
    @Mock
    private lateinit var routeValidator: RouteValidator
    
    @Mock
    private lateinit var stateProtector: NavigationStateProtector
    
    @Mock
    private lateinit var sessionHandler: SessionAwareNavigationHandler
    
    private lateinit var securityManager: NavigationSecurityManager
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        securityManager = NavigationSecurityManagerImpl(
            routeValidator = routeValidator,
            stateProtector = stateProtector,
            sessionHandler = sessionHandler
        )
    }
    
    @Test
    fun `initialize should set up security manager successfully`() = runTest {
        // When
        securityManager.initialize()
        
        // Then
        val securityStatus = securityManager.securityStatus.value
        assertTrue(securityStatus.isSecure)
        assertEquals(ThreatLevel.LOW, securityStatus.threatLevel)
        assertTrue(securityStatus.activeViolations.isEmpty())
    }
    
    @Test
    fun `validateSecureNavigation should allow valid navigation`() = runTest {
        // Given
        securityManager.initialize()
        val fromRoute = "home"
        val toRoute = "map"
        val sessionId = "test-session-123"
        
        // Mock all validators to return success
        whenever(routeValidator.validateNavigation(fromRoute, toRoute))
            .thenReturn(ValidationResult.Valid)
        whenever(sessionHandler.validateNavigationPermission(toRoute, sessionId))
            .thenReturn(NavigationPermission.Allowed)
        
        // When
        val result = securityManager.validateSecureNavigation(fromRoute, toRoute, sessionId)
        
        // Then
        assertTrue(result is SecurityCheckResult.Allowed)
        verify(sessionHandler).updateSessionActivity(sessionId)
    }
    
    @Test
    fun `validateSecureNavigation should deny navigation for invalid route`() = runTest {
        // Given
        securityManager.initialize()
        val fromRoute = "home"
        val toRoute = "invalid-route"
        val sessionId = "test-session-123"
        
        // Mock route validator to return invalid
        whenever(routeValidator.validateNavigation(fromRoute, toRoute))
            .thenReturn(ValidationResult.Invalid(SecurityError.InvalidRoute))
        
        // When
        val result = securityManager.validateSecureNavigation(fromRoute, toRoute, sessionId)
        
        // Then
        assertTrue(result is SecurityCheckResult.Denied)
        val deniedResult = result as SecurityCheckResult.Denied
        assertTrue(deniedResult.violations.contains(SecurityViolation.INVALID_ROUTE))
    }
    
    @Test
    fun `validateSecureNavigation should deny navigation for malicious deep link`() = runTest {
        // Given
        securityManager.initialize()
        val fromRoute = "home"
        val toRoute = "map"
        val sessionId = "test-session-123"
        val maliciousParams = mapOf("script" to "<script>alert('xss')</script>")
        
        // Mock validators
        whenever(routeValidator.validateNavigation(fromRoute, toRoute))
            .thenReturn(ValidationResult.Valid)
        whenever(routeValidator.validateDeepLink(toRoute, maliciousParams))
            .thenReturn(ValidationResult.Invalid(SecurityError.MaliciousDeepLink))
        
        // When
        val result = securityManager.validateSecureNavigation(fromRoute, toRoute, sessionId, maliciousParams)
        
        // Then
        assertTrue(result is SecurityCheckResult.Denied)
        val deniedResult = result as SecurityCheckResult.Denied
        assertTrue(deniedResult.violations.contains(SecurityViolation.MALICIOUS_DEEP_LINK))
    }
    
    @Test
    fun `validateSecureNavigation should require additional auth for expired session`() = runTest {
        // Given
        securityManager.initialize()
        val fromRoute = "home"
        val toRoute = "map"
        val sessionId = "expired-session-123"
        
        // Mock validators
        whenever(routeValidator.validateNavigation(fromRoute, toRoute))
            .thenReturn(ValidationResult.Valid)
        whenever(sessionHandler.validateNavigationPermission(toRoute, sessionId))
            .thenReturn(NavigationPermission.Denied(DenialReason.SESSION_EXPIRED))
        
        // When
        val result = securityManager.validateSecureNavigation(fromRoute, toRoute, sessionId)
        
        // Then
        assertTrue(result is SecurityCheckResult.RequiresAdditionalAuth)
    }
    
    @Test
    fun `validateSecureNavigation should deny navigation for insufficient permissions`() = runTest {
        // Given
        securityManager.initialize()
        val fromRoute = "home"
        val toRoute = "admin"
        val sessionId = "user-session-123"
        
        // Mock validators
        whenever(routeValidator.validateNavigation(fromRoute, toRoute))
            .thenReturn(ValidationResult.Valid)
        whenever(sessionHandler.validateNavigationPermission(toRoute, sessionId))
            .thenReturn(NavigationPermission.Denied(DenialReason.INSUFFICIENT_PERMISSIONS))
        
        // When
        val result = securityManager.validateSecureNavigation(fromRoute, toRoute, sessionId)
        
        // Then
        assertTrue(result is SecurityCheckResult.RequiresAdditionalAuth)
    }
    
    @Test
    fun `protectNavigationState should delegate to state protector`() = runTest {
        // Given
        securityManager.initialize()
        val navigationState = createTestNavigationState()
        val expectedProtectedState = ProtectedNavigationState(
            encryptedData = "encrypted",
            checksum = "checksum",
            timestamp = System.currentTimeMillis()
        )
        
        whenever(stateProtector.protectState(navigationState))
            .thenReturn(expectedProtectedState)
        
        // When
        val result = securityManager.protectNavigationState(navigationState)
        
        // Then
        assertEquals(expectedProtectedState, result)
        verify(stateProtector).protectState(navigationState)
    }
    
    @Test
    fun `validateAndUnprotectState should validate integrity first`() = runTest {
        // Given
        securityManager.initialize()
        val protectedState = ProtectedNavigationState(
            encryptedData = "encrypted",
            checksum = "checksum",
            timestamp = System.currentTimeMillis()
        )
        val expectedState = createTestNavigationState()
        
        whenever(stateProtector.validateStateIntegrity(protectedState))
            .thenReturn(true)
        whenever(stateProtector.unprotectState(protectedState))
            .thenReturn(expectedState)
        
        // When
        val result = securityManager.validateAndUnprotectState(protectedState)
        
        // Then
        assertEquals(expectedState, result)
        verify(stateProtector).validateStateIntegrity(protectedState)
        verify(stateProtector).unprotectState(protectedState)
    }
    
    @Test
    fun `validateAndUnprotectState should return null for tampered state`() = runTest {
        // Given
        securityManager.initialize()
        val tamperedState = ProtectedNavigationState(
            encryptedData = "tampered",
            checksum = "invalid",
            timestamp = System.currentTimeMillis()
        )
        
        whenever(stateProtector.validateStateIntegrity(tamperedState))
            .thenReturn(false)
        
        // When
        val result = securityManager.validateAndUnprotectState(tamperedState)
        
        // Then
        assertEquals(null, result)
        verify(stateProtector).validateStateIntegrity(tamperedState)
    }
    
    @Test
    fun `handleSecurityViolation should update security status`() = runTest {
        // Given
        securityManager.initialize()
        val initialStatus = securityManager.securityStatus.value
        assertTrue(initialStatus.isSecure)
        assertEquals(ThreatLevel.LOW, initialStatus.threatLevel)
        
        // When
        securityManager.handleSecurityViolation(SecurityViolation.MALICIOUS_DEEP_LINK, "test context")
        
        // Then
        val updatedStatus = securityManager.securityStatus.value
        assertTrue(updatedStatus.activeViolations.contains(SecurityViolation.MALICIOUS_DEEP_LINK))
        assertTrue(updatedStatus.threatLevel.ordinal > ThreatLevel.LOW.ordinal)
    }
    
    @Test
    fun `handleSecurityViolation should invalidate session for critical violations`() = runTest {
        // Given
        securityManager.initialize()
        val mockSession = SessionInfo(
            sessionId = "test-session",
            userId = "test-user",
            permissions = setOf("user"),
            createdAt = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis(),
            securityLevel = SecurityLevel.MEDIUM,
            deviceFingerprint = "test-device"
        )
        
        whenever(sessionHandler.getCurrentSession()).thenReturn(mockSession)
        
        // When
        securityManager.handleSecurityViolation(SecurityViolation.STATE_TAMPERING)
        
        // Then
        verify(sessionHandler).invalidateSession(
            mockSession.sessionId,
            SessionInvalidationReason.SECURITY_VIOLATION
        )
    }
    
    @Test
    fun `performSecurityMaintenance should clean up expired data`() = runTest {
        // Given
        securityManager.initialize()
        
        // When
        securityManager.performSecurityMaintenance()
        
        // Then
        verify(sessionHandler).cleanupExpiredSessions()
        verify(stateProtector).clearProtectedData()
    }
    
    @Test
    fun `shutdown should clear sensitive data and update status`() = runTest {
        // Given
        securityManager.initialize()
        assertTrue(securityManager.securityStatus.value.isSecure)
        
        // When
        securityManager.shutdown()
        
        // Then
        // Give some time for the coroutine to complete
        kotlinx.coroutines.delay(100)
        
        verify(stateProtector).clearProtectedData()
        assertFalse(securityManager.securityStatus.value.isSecure)
    }
    
    @Test
    fun `validateSecureNavigation should handle multiple violations`() = runTest {
        // Given
        securityManager.initialize()
        val fromRoute = "home"
        val toRoute = "invalid-route"
        val sessionId = "expired-session"
        
        // Mock multiple failures
        whenever(routeValidator.validateNavigation(fromRoute, toRoute))
            .thenReturn(ValidationResult.Invalid(SecurityError.InvalidRoute))
        whenever(sessionHandler.validateNavigationPermission(toRoute, sessionId))
            .thenReturn(NavigationPermission.Denied(DenialReason.SESSION_EXPIRED))
        
        // When
        val result = securityManager.validateSecureNavigation(fromRoute, toRoute, sessionId)
        
        // Then
        assertTrue(result is SecurityCheckResult.Denied)
        val deniedResult = result as SecurityCheckResult.Denied
        assertTrue(deniedResult.violations.size >= 2)
        assertTrue(deniedResult.violations.contains(SecurityViolation.INVALID_ROUTE))
        assertTrue(deniedResult.violations.contains(SecurityViolation.SESSION_EXPIRED))
    }
    
    private fun createTestNavigationState(): NavigationState {
        return NavigationState(
            currentRoute = "home",
            previousRoute = "map",
            navigationHistory = listOf("home", "map"),
            sessionId = "test-session-123",
            timestamp = System.currentTimeMillis(),
            userPermissions = setOf("user", "location")
        )
    }
}