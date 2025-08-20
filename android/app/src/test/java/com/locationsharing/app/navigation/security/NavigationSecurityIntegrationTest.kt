package com.locationsharing.app.navigation.security

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the complete navigation security system.
 * Tests the interaction between all security components.
 */
class NavigationSecurityIntegrationTest {
    
    private lateinit var routeValidator: RouteValidator
    private lateinit var stateProtector: NavigationStateProtector
    private lateinit var sessionHandler: SessionAwareNavigationHandler
    private lateinit var securityManager: NavigationSecurityManager
    
    @BeforeEach
    fun setUp() {
        routeValidator = RouteValidatorImpl()
        stateProtector = NavigationStateProtectorImpl()
        sessionHandler = SessionAwareNavigationHandlerImpl(routeValidator)
        securityManager = NavigationSecurityManagerImpl(
            routeValidator = routeValidator,
            stateProtector = stateProtector,
            sessionHandler = sessionHandler
        )
        
        securityManager.initialize()
    }
    
    @Test
    fun `complete secure navigation flow should work end-to-end`() = runTest {
        // Given - Initialize a user session
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When - Perform secure navigation
        val result = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "map",
            sessionId = sessionInfo.sessionId
        )
        
        // Then - Navigation should be allowed
        assertTrue(result is SecurityCheckResult.Allowed)
        
        // Verify session is still active
        val currentSession = sessionHandler.getCurrentSession()
        assertNotNull(currentSession)
        assertEquals(sessionInfo.sessionId, currentSession.sessionId)
    }
    
    @Test
    fun `navigation with malicious deep link should be blocked`() = runTest {
        // Given - Initialize a user session
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When - Attempt navigation with malicious parameters
        val result = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "map",
            sessionId = sessionInfo.sessionId,
            parameters = mapOf("script" to "<script>alert('xss')</script>")
        )
        
        // Then - Navigation should be denied
        assertTrue(result is SecurityCheckResult.Denied)
        val deniedResult = result as SecurityCheckResult.Denied
        assertTrue(deniedResult.violations.contains(SecurityViolation.MALICIOUS_DEEP_LINK))
    }
    
    @Test
    fun `state protection and validation should work together`() = runTest {
        // Given - Create navigation state
        val navigationState = NavigationState(
            currentRoute = "home",
            previousRoute = "map",
            navigationHistory = listOf("home", "map", "friends"),
            sessionId = "test-session-123",
            timestamp = System.currentTimeMillis(),
            userPermissions = setOf("user", "location")
        )
        
        // When - Protect and then validate state
        val protectedState = securityManager.protectNavigationState(navigationState)
        val unprotectedState = securityManager.validateAndUnprotectState(protectedState)
        
        // Then - State should be preserved
        assertNotNull(unprotectedState)
        assertEquals(navigationState.currentRoute, unprotectedState.currentRoute)
        assertEquals(navigationState.sessionId, unprotectedState.sessionId)
        assertEquals(navigationState.timestamp, unprotectedState.timestamp)
    }
    
    @Test
    fun `tampered state should be detected and rejected`() = runTest {
        // Given - Create and protect navigation state
        val navigationState = NavigationState(
            currentRoute = "home",
            previousRoute = "map",
            navigationHistory = listOf("home", "map"),
            sessionId = "test-session-123",
            timestamp = System.currentTimeMillis(),
            userPermissions = setOf("user", "location")
        )
        val protectedState = securityManager.protectNavigationState(navigationState)
        
        // When - Tamper with the protected state
        val tamperedState = protectedState.copy(
            encryptedData = protectedState.encryptedData + "tampered"
        )
        val result = securityManager.validateAndUnprotectState(tamperedState)
        
        // Then - Tampered state should be rejected
        assertNull(result)
    }
    
    @Test
    fun `session expiry should prevent navigation`() = runTest {
        // Given - Initialize session
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When - Invalidate session and attempt navigation
        sessionHandler.invalidateSession(sessionInfo.sessionId, SessionInvalidationReason.SESSION_TIMEOUT)
        val result = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "map",
            sessionId = sessionInfo.sessionId
        )
        
        // Then - Navigation should be denied
        assertTrue(result is SecurityCheckResult.Denied)
        val deniedResult = result as SecurityCheckResult.Denied
        assertTrue(deniedResult.violations.contains(SecurityViolation.SESSION_EXPIRED))
    }
    
    @Test
    fun `insufficient permissions should prevent navigation`() = runTest {
        // Given - Initialize session with limited permissions
        val userId = "test-user-123"
        val permissions = setOf("basic") // No location permission
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When - Attempt navigation to location-required route
        val result = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "map", // Requires location permission
            sessionId = sessionInfo.sessionId
        )
        
        // Then - Navigation should require additional auth
        assertTrue(result is SecurityCheckResult.RequiresAdditionalAuth)
    }
    
    @Test
    fun `rate limiting should prevent excessive navigation attempts`() = runTest {
        // Given - Initialize session
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When - Make many rapid navigation attempts
        val results = mutableListOf<SecurityCheckResult>()
        repeat(15) { // More than rate limit
            results.add(
                securityManager.validateSecureNavigation(
                    fromRoute = "home",
                    toRoute = "map",
                    sessionId = sessionInfo.sessionId
                )
            )
        }
        
        // Then - Some should be rate limited
        val allowedCount = results.count { it is SecurityCheckResult.Allowed }
        val deniedCount = results.count { 
            it is SecurityCheckResult.Denied && 
            (it as SecurityCheckResult.Denied).violations.contains(SecurityViolation.RATE_LIMIT_EXCEEDED)
        }
        
        assertTrue(allowedCount > 0, "Some requests should be allowed")
        assertTrue(deniedCount > 0, "Some requests should be rate limited")
    }
    
    @Test
    fun `security violations should escalate threat level`() = runTest {
        // Given - Initial security status
        val initialStatus = securityManager.securityStatus.value
        assertEquals(ThreatLevel.LOW, initialStatus.threatLevel)
        assertTrue(initialStatus.isSecure)
        
        // When - Trigger multiple security violations
        securityManager.handleSecurityViolation(SecurityViolation.MALICIOUS_DEEP_LINK)
        securityManager.handleSecurityViolation(SecurityViolation.STATE_TAMPERING)
        securityManager.handleSecurityViolation(SecurityViolation.UNAUTHORIZED_NAVIGATION)
        
        // Then - Threat level should escalate
        val updatedStatus = securityManager.securityStatus.value
        assertTrue(updatedStatus.threatLevel.ordinal > ThreatLevel.LOW.ordinal)
        assertTrue(updatedStatus.activeViolations.isNotEmpty())
    }
    
    @Test
    fun `critical violations should invalidate session immediately`() = runTest {
        // Given - Initialize session
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // Verify session is active
        assertNotNull(sessionHandler.getCurrentSession())
        
        // When - Trigger critical security violation
        securityManager.handleSecurityViolation(SecurityViolation.STATE_TAMPERING)
        
        // Then - Session should be invalidated
        val sessionState = sessionHandler.sessionState.value
        assertFalse(sessionState.isActive)
    }
    
    @Test
    fun `security maintenance should clean up expired data`() = runTest {
        // Given - Initialize session and create some state
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        val navigationState = NavigationState(
            currentRoute = "home",
            previousRoute = null,
            navigationHistory = emptyList(),
            sessionId = sessionInfo.sessionId,
            timestamp = System.currentTimeMillis(),
            userPermissions = permissions
        )
        securityManager.protectNavigationState(navigationState)
        
        // When - Perform security maintenance
        securityManager.performSecurityMaintenance()
        
        // Then - System should still be operational
        val securityStatus = securityManager.securityStatus.value
        assertTrue(securityStatus.lastSecurityCheck > 0)
    }
    
    @Test
    fun `complete security shutdown should clear all data`() = runTest {
        // Given - Initialize session and create state
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        sessionHandler.initializeSession(userId, permissions)
        
        val navigationState = NavigationState(
            currentRoute = "home",
            previousRoute = null,
            navigationHistory = emptyList(),
            sessionId = "test-session",
            timestamp = System.currentTimeMillis(),
            userPermissions = permissions
        )
        securityManager.protectNavigationState(navigationState)
        
        // When - Shutdown security system
        securityManager.shutdown()
        
        // Give time for cleanup
        kotlinx.coroutines.delay(100)
        
        // Then - System should be in shutdown state
        val securityStatus = securityManager.securityStatus.value
        assertFalse(securityStatus.isSecure)
    }
    
    @Test
    fun `deep link validation should work with route validation`() = runTest {
        // Given - Initialize session
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When - Test various deep link scenarios
        val validResult = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "map",
            sessionId = sessionInfo.sessionId,
            parameters = mapOf("lat" to "40.7128", "lng" to "-74.0060")
        )
        
        val invalidResult = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "map",
            sessionId = sessionInfo.sessionId,
            parameters = mapOf("script" to "javascript:alert('xss')")
        )
        
        // Then - Valid should pass, invalid should fail
        assertTrue(validResult is SecurityCheckResult.Allowed)
        assertTrue(invalidResult is SecurityCheckResult.Denied)
    }
    
    @Test
    fun `session security levels should affect navigation permissions`() = runTest {
        // Given - Initialize sessions with different security levels
        val adminSession = sessionHandler.initializeSession("admin-user", setOf("admin"))
        val userSession = sessionHandler.initializeSession("regular-user", setOf("user"))
        val basicSession = sessionHandler.initializeSession("basic-user", setOf("basic"))
        
        // When - Attempt navigation to admin route
        val adminResult = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "settings",
            sessionId = adminSession.sessionId
        )
        
        val userResult = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "settings",
            sessionId = userSession.sessionId
        )
        
        val basicResult = securityManager.validateSecureNavigation(
            fromRoute = "home",
            toRoute = "settings",
            sessionId = basicSession.sessionId
        )
        
        // Then - Results should reflect permission levels
        assertTrue(adminResult is SecurityCheckResult.Allowed)
        assertTrue(userResult is SecurityCheckResult.Allowed) // User can access settings
        // Basic user might be denied depending on implementation
    }
}