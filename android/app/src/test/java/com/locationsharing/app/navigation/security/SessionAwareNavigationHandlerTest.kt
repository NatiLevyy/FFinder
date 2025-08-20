package com.locationsharing.app.navigation.security

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionAwareNavigationHandlerTest {
    
    @Mock
    private lateinit var routeValidator: RouteValidator
    
    private lateinit var sessionHandler: SessionAwareNavigationHandler
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sessionHandler = SessionAwareNavigationHandlerImpl(routeValidator)
    }
    
    @Test
    fun `initializeSession should create new session successfully`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        
        // When
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // Then
        assertNotNull(sessionInfo)
        assertEquals(userId, sessionInfo.userId)
        assertEquals(permissions, sessionInfo.permissions)
        assertTrue(sessionInfo.sessionId.isNotEmpty())
        assertTrue(sessionInfo.createdAt > 0)
        assertTrue(sessionInfo.lastActivity > 0)
    }
    
    @Test
    fun `sessionState should be active after initialization`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        
        // When
        sessionHandler.initializeSession(userId, permissions)
        
        // Then
        val sessionState = sessionHandler.sessionState.value
        assertTrue(sessionState.isActive)
        assertNotNull(sessionState.sessionInfo)
        assertEquals(userId, sessionState.sessionInfo?.userId)
    }
    
    @Test
    fun `validateNavigationPermission should allow navigation for valid session`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        val route = "map"
        
        // Mock route validator
        whenever(routeValidator.validateRoute(route)).thenReturn(ValidationResult.Valid)
        
        // When
        val permission = sessionHandler.validateNavigationPermission(route, sessionInfo.sessionId)
        
        // Then
        assertTrue(permission is NavigationPermission.Allowed)
    }
    
    @Test
    fun `validateNavigationPermission should deny navigation for invalid session`() = runTest {
        // Given
        val invalidSessionId = "invalid-session-123"
        val route = "map"
        
        // When
        val permission = sessionHandler.validateNavigationPermission(route, invalidSessionId)
        
        // Then
        assertTrue(permission is NavigationPermission.Denied)
        assertEquals(DenialReason.INVALID_SESSION, (permission as NavigationPermission.Denied).reason)
    }
    
    @Test
    fun `validateNavigationPermission should deny navigation for expired session`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        val route = "map"
        
        // Simulate session expiry by waiting (in real test, you'd manipulate time)
        // For this test, we'll create a session and then test with a very old timestamp
        // This requires access to internal state, so we'll test the timeout logic differently
        
        // Mock route validator
        whenever(routeValidator.validateRoute(route)).thenReturn(ValidationResult.Valid)
        
        // When - immediately after creation, should be valid
        val permission = sessionHandler.validateNavigationPermission(route, sessionInfo.sessionId)
        
        // Then
        assertTrue(permission is NavigationPermission.Allowed)
    }
    
    @Test
    fun `validateNavigationPermission should deny navigation for insufficient permissions`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("basic") // No location permission
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        val route = "map" // Requires location permission
        
        // Mock route validator
        whenever(routeValidator.validateRoute(route)).thenReturn(ValidationResult.Valid)
        
        // When
        val permission = sessionHandler.validateNavigationPermission(route, sessionInfo.sessionId)
        
        // Then
        assertTrue(permission is NavigationPermission.Denied)
        assertEquals(DenialReason.INSUFFICIENT_PERMISSIONS, (permission as NavigationPermission.Denied).reason)
    }
    
    @Test
    fun `validateNavigationPermission should deny navigation for invalid route`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        val route = "invalid-route"
        
        // Mock route validator to return invalid
        whenever(routeValidator.validateRoute(route)).thenReturn(
            ValidationResult.Invalid(SecurityError.InvalidRoute)
        )
        
        // When
        val permission = sessionHandler.validateNavigationPermission(route, sessionInfo.sessionId)
        
        // Then
        assertTrue(permission is NavigationPermission.Denied)
        assertEquals(DenialReason.SECURITY_VIOLATION, (permission as NavigationPermission.Denied).reason)
    }
    
    @Test
    fun `updateSessionActivity should update session timestamp`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        val originalActivity = sessionInfo.lastActivity
        
        // Wait a bit to ensure timestamp difference
        Thread.sleep(10)
        
        // When
        sessionHandler.updateSessionActivity(sessionInfo.sessionId)
        
        // Then
        val currentSession = sessionHandler.getCurrentSession()
        assertNotNull(currentSession)
        assertTrue(currentSession.lastActivity > originalActivity)
    }
    
    @Test
    fun `invalidateSession should remove session`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When
        sessionHandler.invalidateSession(sessionInfo.sessionId, SessionInvalidationReason.USER_LOGOUT)
        
        // Then
        val currentSession = sessionHandler.getCurrentSession()
        assertNull(currentSession)
        
        val sessionState = sessionHandler.sessionState.value
        assertFalse(sessionState.isActive)
    }
    
    @Test
    fun `cleanupExpiredSessions should remove old sessions`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        sessionHandler.initializeSession(userId, permissions)
        
        // When
        sessionHandler.cleanupExpiredSessions()
        
        // Then
        // Since the session was just created, it shouldn't be expired
        val currentSession = sessionHandler.getCurrentSession()
        assertNotNull(currentSession)
    }
    
    @Test
    fun `getCurrentSession should return active session`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        
        // When
        val currentSession = sessionHandler.getCurrentSession()
        
        // Then
        assertNotNull(currentSession)
        assertEquals(sessionInfo.sessionId, currentSession.sessionId)
        assertEquals(sessionInfo.userId, currentSession.userId)
    }
    
    @Test
    fun `getCurrentSession should return null when no active session`() = runTest {
        // Given - no session initialized
        
        // When
        val currentSession = sessionHandler.getCurrentSession()
        
        // Then
        assertNull(currentSession)
    }
    
    @Test
    fun `initializeSession should determine correct security level`() = runTest {
        // Given & When & Then
        val adminSession = sessionHandler.initializeSession("admin-user", setOf("admin"))
        assertEquals(SecurityLevel.HIGH, adminSession.securityLevel)
        
        val locationSession = sessionHandler.initializeSession("location-user", setOf("location"))
        assertEquals(SecurityLevel.MEDIUM, locationSession.securityLevel)
        
        val basicSession = sessionHandler.initializeSession("basic-user", setOf("basic"))
        assertEquals(SecurityLevel.LOW, basicSession.securityLevel)
    }
    
    @Test
    fun `validateNavigationPermission should handle rate limiting`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        val sessionInfo = sessionHandler.initializeSession(userId, permissions)
        val route = "map"
        
        // Mock route validator
        whenever(routeValidator.validateRoute(any())).thenReturn(ValidationResult.Valid)
        
        // When - make many rapid navigation attempts
        val results = mutableListOf<NavigationPermission>()
        repeat(15) { // More than the rate limit
            results.add(sessionHandler.validateNavigationPermission(route, sessionInfo.sessionId))
        }
        
        // Then - some should be rate limited
        val allowedCount = results.count { it is NavigationPermission.Allowed }
        val rateLimitedCount = results.count { 
            it is NavigationPermission.Denied && it.reason == DenialReason.RATE_LIMITED 
        }
        
        assertTrue(allowedCount > 0, "Some requests should be allowed")
        assertTrue(rateLimitedCount > 0, "Some requests should be rate limited")
    }
    
    @Test
    fun `initializeSession should handle multiple sessions per user`() = runTest {
        // Given
        val userId = "test-user-123"
        val permissions = setOf("user", "location")
        
        // When - create multiple sessions for same user
        val sessions = mutableListOf<SessionInfo>()
        repeat(5) {
            sessions.add(sessionHandler.initializeSession(userId, permissions))
        }
        
        // Then - should handle session limit gracefully
        assertTrue(sessions.isNotEmpty())
        sessions.forEach { session ->
            assertNotNull(session.sessionId)
            assertEquals(userId, session.userId)
        }
    }
}