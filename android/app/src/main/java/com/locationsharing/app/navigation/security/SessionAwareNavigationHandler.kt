package com.locationsharing.app.navigation.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for handling navigation with session awareness and security.
 */
interface SessionAwareNavigationHandler {
    /**
     * Current session state.
     */
    val sessionState: StateFlow<SessionState>
    
    /**
     * Initializes a new navigation session.
     * 
     * @param userId The user ID for the session
     * @param permissions Set of permissions for the user
     * @return SessionInfo for the new session
     */
    fun initializeSession(userId: String, permissions: Set<String>): SessionInfo
    
    /**
     * Validates if navigation is allowed for the current session.
     * 
     * @param route The route to navigate to
     * @param sessionId The session ID
     * @return NavigationPermission indicating if navigation is allowed
     */
    fun validateNavigationPermission(route: String, sessionId: String): NavigationPermission
    
    /**
     * Updates session activity timestamp.
     * 
     * @param sessionId The session ID to update
     */
    fun updateSessionActivity(sessionId: String)
    
    /**
     * Invalidates a session.
     * 
     * @param sessionId The session ID to invalidate
     * @param reason The reason for invalidation
     */
    fun invalidateSession(sessionId: String, reason: SessionInvalidationReason)
    
    /**
     * Cleans up expired sessions.
     */
    fun cleanupExpiredSessions()
    
    /**
     * Gets current active session info.
     * 
     * @return SessionInfo if session is active, null otherwise
     */
    fun getCurrentSession(): SessionInfo?
}

/**
 * Represents the current session state.
 */
data class SessionState(
    val isActive: Boolean,
    val sessionInfo: SessionInfo?,
    val lastActivity: Long,
    val securityLevel: SecurityLevel
)

/**
 * Information about a navigation session.
 */
data class SessionInfo(
    val sessionId: String,
    val userId: String,
    val permissions: Set<String>,
    val createdAt: Long,
    val lastActivity: Long,
    val securityLevel: SecurityLevel,
    val deviceFingerprint: String
)

/**
 * Result of navigation permission check.
 */
sealed class NavigationPermission {
    object Allowed : NavigationPermission()
    data class Denied(val reason: DenialReason) : NavigationPermission()
}

/**
 * Reasons for navigation denial.
 */
enum class DenialReason {
    SESSION_EXPIRED,
    INSUFFICIENT_PERMISSIONS,
    INVALID_SESSION,
    SECURITY_VIOLATION,
    RATE_LIMITED
}

/**
 * Security levels for sessions.
 */
enum class SecurityLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Reasons for session invalidation.
 */
enum class SessionInvalidationReason {
    USER_LOGOUT,
    SESSION_TIMEOUT,
    SECURITY_VIOLATION,
    PERMISSION_REVOKED,
    DEVICE_CHANGE
}

/**
 * Implementation of SessionAwareNavigationHandler with comprehensive session management.
 */
class SessionAwareNavigationHandlerImpl(
    private val routeValidator: RouteValidator
) : SessionAwareNavigationHandler {
    
    companion object {
        private const val TAG = "SessionAwareNavigationHandler"
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
        private const val MAX_SESSIONS_PER_USER = 3
        private const val RATE_LIMIT_WINDOW_MS = 1000L // 1 second
        private const val MAX_NAVIGATIONS_PER_WINDOW = 10
    }
    
    private val activeSessions = ConcurrentHashMap<String, SessionInfo>()
    private val navigationHistory = ConcurrentHashMap<String, MutableList<Long>>()
    
    private val _sessionState = MutableStateFlow(
        SessionState(
            isActive = false,
            sessionInfo = null,
            lastActivity = 0L,
            securityLevel = SecurityLevel.LOW
        )
    )
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private var currentSessionId: String? = null
    
    override fun initializeSession(userId: String, permissions: Set<String>): SessionInfo {
        try {
            // Clean up any existing sessions for this user if limit exceeded
            cleanupUserSessions(userId)
            
            // Generate new session
            val sessionId = generateSessionId()
            val currentTime = System.currentTimeMillis()
            val deviceFingerprint = generateDeviceFingerprint()
            
            val sessionInfo = SessionInfo(
                sessionId = sessionId,
                userId = userId,
                permissions = permissions,
                createdAt = currentTime,
                lastActivity = currentTime,
                securityLevel = determineSecurityLevel(permissions),
                deviceFingerprint = deviceFingerprint
            )
            
            // Store session
            activeSessions[sessionId] = sessionInfo
            currentSessionId = sessionId
            
            // Update session state
            _sessionState.value = SessionState(
                isActive = true,
                sessionInfo = sessionInfo,
                lastActivity = currentTime,
                securityLevel = sessionInfo.securityLevel
            )
            
            Timber.d("$TAG: Initialized new session for user: $userId")
            return sessionInfo
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to initialize session for user: $userId")
            throw SecurityException("Failed to initialize session", e)
        }
    }
    
    override fun validateNavigationPermission(route: String, sessionId: String): NavigationPermission {
        try {
            // Get session info
            val sessionInfo = activeSessions[sessionId]
                ?: return NavigationPermission.Denied(DenialReason.INVALID_SESSION)
            
            // Check if session is expired
            val currentTime = System.currentTimeMillis()
            if (currentTime - sessionInfo.lastActivity > SESSION_TIMEOUT_MS) {
                invalidateSession(sessionId, SessionInvalidationReason.SESSION_TIMEOUT)
                return NavigationPermission.Denied(DenialReason.SESSION_EXPIRED)
            }
            
            // Check rate limiting
            if (isRateLimited(sessionId)) {
                Timber.w("$TAG: Navigation rate limited for session: $sessionId")
                return NavigationPermission.Denied(DenialReason.RATE_LIMITED)
            }
            
            // Validate route
            val routeValidation = routeValidator.validateRoute(route)
            if (routeValidation is ValidationResult.Invalid) {
                Timber.w("$TAG: Route validation failed for: $route")
                return NavigationPermission.Denied(DenialReason.SECURITY_VIOLATION)
            }
            
            // Check permissions for route
            if (!hasPermissionForRoute(route, sessionInfo.permissions)) {
                Timber.w("$TAG: Insufficient permissions for route: $route")
                return NavigationPermission.Denied(DenialReason.INSUFFICIENT_PERMISSIONS)
            }
            
            // Record navigation attempt
            recordNavigationAttempt(sessionId)
            
            return NavigationPermission.Allowed
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error validating navigation permission")
            return NavigationPermission.Denied(DenialReason.SECURITY_VIOLATION)
        }
    }
    
    override fun updateSessionActivity(sessionId: String) {
        try {
            val sessionInfo = activeSessions[sessionId]
            if (sessionInfo != null) {
                val currentTime = System.currentTimeMillis()
                val updatedSession = sessionInfo.copy(lastActivity = currentTime)
                activeSessions[sessionId] = updatedSession
                
                // Update current session state if this is the active session
                if (sessionId == currentSessionId) {
                    _sessionState.value = _sessionState.value.copy(
                        lastActivity = currentTime,
                        sessionInfo = updatedSession
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error updating session activity")
        }
    }
    
    override fun invalidateSession(sessionId: String, reason: SessionInvalidationReason) {
        try {
            val sessionInfo = activeSessions.remove(sessionId)
            if (sessionInfo != null) {
                // Clear navigation history for this session
                navigationHistory.remove(sessionId)
                
                // Update session state if this was the current session
                if (sessionId == currentSessionId) {
                    currentSessionId = null
                    _sessionState.value = SessionState(
                        isActive = false,
                        sessionInfo = null,
                        lastActivity = System.currentTimeMillis(),
                        securityLevel = SecurityLevel.LOW
                    )
                }
                
                Timber.d("$TAG: Invalidated session: $sessionId, reason: $reason")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error invalidating session: $sessionId")
        }
    }
    
    override fun cleanupExpiredSessions() {
        try {
            val currentTime = System.currentTimeMillis()
            val expiredSessions = activeSessions.filter { (_, sessionInfo) ->
                currentTime - sessionInfo.lastActivity > SESSION_TIMEOUT_MS
            }
            
            expiredSessions.forEach { (sessionId, _) ->
                invalidateSession(sessionId, SessionInvalidationReason.SESSION_TIMEOUT)
            }
            
            if (expiredSessions.isNotEmpty()) {
                Timber.d("$TAG: Cleaned up ${expiredSessions.size} expired sessions")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error cleaning up expired sessions")
        }
    }
    
    override fun getCurrentSession(): SessionInfo? {
        return currentSessionId?.let { activeSessions[it] }
    }
    
    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }
    
    private fun generateDeviceFingerprint(): String {
        // Simple device fingerprint (in production, use more sophisticated method)
        return android.os.Build.MODEL + "_" + android.os.Build.VERSION.SDK_INT
    }
    
    private fun determineSecurityLevel(permissions: Set<String>): SecurityLevel {
        return when {
            permissions.contains("admin") -> SecurityLevel.HIGH
            permissions.contains("location") -> SecurityLevel.MEDIUM
            else -> SecurityLevel.LOW
        }
    }
    
    private fun cleanupUserSessions(userId: String) {
        val userSessions = activeSessions.filter { (_, sessionInfo) ->
            sessionInfo.userId == userId
        }
        
        if (userSessions.size >= MAX_SESSIONS_PER_USER) {
            // Remove oldest sessions
            val sortedSessions = userSessions.toList().sortedBy { (_, sessionInfo) ->
                sessionInfo.lastActivity
            }
            
            val sessionsToRemove = sortedSessions.take(userSessions.size - MAX_SESSIONS_PER_USER + 1)
            sessionsToRemove.forEach { (sessionId, _) ->
                invalidateSession(sessionId, SessionInvalidationReason.DEVICE_CHANGE)
            }
        }
    }
    
    private fun isRateLimited(sessionId: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val history = navigationHistory.getOrPut(sessionId) { mutableListOf() }
        
        // Remove old entries outside the rate limit window
        history.removeAll { it < currentTime - RATE_LIMIT_WINDOW_MS }
        
        return history.size >= MAX_NAVIGATIONS_PER_WINDOW
    }
    
    private fun recordNavigationAttempt(sessionId: String) {
        val currentTime = System.currentTimeMillis()
        val history = navigationHistory.getOrPut(sessionId) { mutableListOf() }
        history.add(currentTime)
    }
    
    private fun hasPermissionForRoute(route: String, permissions: Set<String>): Boolean {
        // Define route permission requirements
        val routePermissions = mapOf(
            "admin" to setOf("admin"),
            "settings" to setOf("user", "admin"),
            "friends" to setOf("user", "admin"),
            "map" to setOf("location", "user", "admin")
        )
        
        // Check if user has required permissions for the route
        val requiredPermissions = routePermissions[route] ?: emptySet()
        return requiredPermissions.isEmpty() || permissions.any { it in requiredPermissions }
    }
}