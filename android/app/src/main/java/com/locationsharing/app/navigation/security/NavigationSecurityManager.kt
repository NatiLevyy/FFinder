package com.locationsharing.app.navigation.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for all navigation security operations.
 * Coordinates route validation, state protection, and session management.
 */
interface NavigationSecurityManager {
    /**
     * Current security status.
     */
    val securityStatus: StateFlow<SecurityStatus>
    
    /**
     * Initializes the security manager.
     */
    fun initialize()
    
    /**
     * Validates if navigation is secure and allowed.
     * 
     * @param fromRoute The source route
     * @param toRoute The destination route
     * @param sessionId The current session ID
     * @param parameters Optional parameters (for deep links)
     * @return SecurityCheckResult indicating if navigation is allowed
     */
    suspend fun validateSecureNavigation(
        fromRoute: String,
        toRoute: String,
        sessionId: String,
        parameters: Map<String, String> = emptyMap()
    ): SecurityCheckResult
    
    /**
     * Protects navigation state before storing or transmitting.
     * 
     * @param state The navigation state to protect
     * @return ProtectedNavigationState with security measures applied
     */
    fun protectNavigationState(state: NavigationState): ProtectedNavigationState
    
    /**
     * Validates and unprotects navigation state.
     * 
     * @param protectedState The protected state to validate and decrypt
     * @return NavigationState if valid, null if compromised
     */
    fun validateAndUnprotectState(protectedState: ProtectedNavigationState): NavigationState?
    
    /**
     * Handles security violations and takes appropriate action.
     * 
     * @param violation The security violation that occurred
     * @param context Additional context about the violation
     */
    fun handleSecurityViolation(violation: SecurityViolation, context: String = "")
    
    /**
     * Performs security maintenance tasks.
     */
    suspend fun performSecurityMaintenance()
    
    /**
     * Shuts down the security manager and clears sensitive data.
     */
    fun shutdown()
}

/**
 * Result of a comprehensive security check.
 */
sealed class SecurityCheckResult {
    object Allowed : SecurityCheckResult()
    data class Denied(val violations: List<SecurityViolation>) : SecurityCheckResult()
    data class RequiresAdditionalAuth(val reason: String) : SecurityCheckResult()
}

/**
 * Types of security violations.
 */
enum class SecurityViolation {
    INVALID_ROUTE,
    UNAUTHORIZED_NAVIGATION,
    MALICIOUS_DEEP_LINK,
    SESSION_EXPIRED,
    STATE_TAMPERING,
    RATE_LIMIT_EXCEEDED,
    PERMISSION_VIOLATION,
    SUSPICIOUS_ACTIVITY
}

/**
 * Overall security status of the navigation system.
 */
data class SecurityStatus(
    val isSecure: Boolean,
    val threatLevel: ThreatLevel,
    val activeViolations: List<SecurityViolation>,
    val lastSecurityCheck: Long,
    val securityVersion: String = "1.0.0"
)

/**
 * Threat levels for the navigation system.
 */
enum class ThreatLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Implementation of NavigationSecurityManager.
 */
@Singleton
class NavigationSecurityManagerImpl @Inject constructor(
    private val routeValidator: RouteValidator,
    private val stateProtector: NavigationStateProtector,
    private val sessionHandler: SessionAwareNavigationHandler
) : NavigationSecurityManager {
    
    companion object {
        private const val TAG = "NavigationSecurityManager"
        private const val SECURITY_CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MAX_VIOLATIONS_PER_SESSION = 5
    }
    
    private val securityScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val violationHistory = mutableMapOf<String, MutableList<SecurityViolation>>()
    
    private val _securityStatus = MutableStateFlow(
        SecurityStatus(
            isSecure = true,
            threatLevel = ThreatLevel.LOW,
            activeViolations = emptyList(),
            lastSecurityCheck = System.currentTimeMillis()
        )
    )
    override val securityStatus: StateFlow<SecurityStatus> = _securityStatus.asStateFlow()
    
    private var isInitialized = false
    
    override fun initialize() {
        if (isInitialized) {
            Timber.w("$TAG: Security manager already initialized")
            return
        }
        
        try {
            // Start periodic security maintenance
            securityScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(SECURITY_CHECK_INTERVAL_MS)
                    performSecurityMaintenance()
                }
            }
            
            isInitialized = true
            updateSecurityStatus()
            
            Timber.d("$TAG: Navigation security manager initialized")
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to initialize security manager")
            _securityStatus.value = _securityStatus.value.copy(
                isSecure = false,
                threatLevel = ThreatLevel.CRITICAL
            )
        }
    }
    
    override suspend fun validateSecureNavigation(
        fromRoute: String,
        toRoute: String,
        sessionId: String,
        parameters: Map<String, String>
    ): SecurityCheckResult {
        try {
            val violations = mutableListOf<SecurityViolation>()
            
            // 1. Validate routes
            val routeValidation = routeValidator.validateNavigation(fromRoute, toRoute)
            if (routeValidation is ValidationResult.Invalid) {
                violations.add(mapSecurityError(routeValidation.error))
            }
            
            // 2. Validate deep link parameters if present
            if (parameters.isNotEmpty()) {
                val deepLinkValidation = routeValidator.validateDeepLink(toRoute, parameters)
                if (deepLinkValidation is ValidationResult.Invalid) {
                    violations.add(mapSecurityError(deepLinkValidation.error))
                }
            }
            
            // 3. Check session permissions
            val permissionCheck = sessionHandler.validateNavigationPermission(toRoute, sessionId)
            if (permissionCheck is NavigationPermission.Denied) {
                violations.add(mapDenialReason(permissionCheck.reason))
            }
            
            // 4. Check for suspicious activity patterns
            if (isSuspiciousActivity(sessionId, fromRoute, toRoute)) {
                violations.add(SecurityViolation.SUSPICIOUS_ACTIVITY)
            }
            
            // 5. Update session activity if navigation is allowed
            if (violations.isEmpty()) {
                sessionHandler.updateSessionActivity(sessionId)
                return SecurityCheckResult.Allowed
            }
            
            // Record violations
            recordViolations(sessionId, violations)
            
            // Check if additional authentication is required
            if (violations.any { it in listOf(SecurityViolation.SESSION_EXPIRED, SecurityViolation.PERMISSION_VIOLATION) }) {
                return SecurityCheckResult.RequiresAdditionalAuth("Session validation required")
            }
            
            return SecurityCheckResult.Denied(violations)
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error during security validation")
            return SecurityCheckResult.Denied(listOf(SecurityViolation.SUSPICIOUS_ACTIVITY))
        }
    }
    
    override fun protectNavigationState(state: NavigationState): ProtectedNavigationState {
        return try {
            stateProtector.protectState(state)
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to protect navigation state")
            handleSecurityViolation(SecurityViolation.STATE_TAMPERING, "Protection failed")
            throw SecurityException("Failed to protect navigation state", e)
        }
    }
    
    override fun validateAndUnprotectState(protectedState: ProtectedNavigationState): NavigationState? {
        return try {
            // First validate integrity
            if (!stateProtector.validateStateIntegrity(protectedState)) {
                handleSecurityViolation(SecurityViolation.STATE_TAMPERING, "Integrity check failed")
                return null
            }
            
            // Then unprotect
            stateProtector.unprotectState(protectedState)
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to validate and unprotect state")
            handleSecurityViolation(SecurityViolation.STATE_TAMPERING, "Unprotection failed")
            null
        }
    }
    
    override fun handleSecurityViolation(violation: SecurityViolation, context: String) {
        try {
            Timber.w("$TAG: Security violation detected: $violation, context: $context")
            
            // Update security status
            val currentStatus = _securityStatus.value
            val updatedViolations = currentStatus.activeViolations + violation
            val newThreatLevel = calculateThreatLevel(updatedViolations)
            
            _securityStatus.value = currentStatus.copy(
                isSecure = newThreatLevel != ThreatLevel.CRITICAL,
                threatLevel = newThreatLevel,
                activeViolations = updatedViolations,
                lastSecurityCheck = System.currentTimeMillis()
            )
            
            // Take action based on violation severity
            when (violation) {
                SecurityViolation.STATE_TAMPERING,
                SecurityViolation.MALICIOUS_DEEP_LINK -> {
                    // Immediate session invalidation for critical violations
                    sessionHandler.getCurrentSession()?.let { session ->
                        sessionHandler.invalidateSession(
                            session.sessionId,
                            SessionInvalidationReason.SECURITY_VIOLATION
                        )
                    }
                }
                SecurityViolation.RATE_LIMIT_EXCEEDED,
                SecurityViolation.SUSPICIOUS_ACTIVITY -> {
                    // Log and monitor for patterns
                    Timber.w("$TAG: Monitoring violation: $violation")
                }
                else -> {
                    // Standard logging for other violations
                    Timber.d("$TAG: Handled violation: $violation")
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error handling security violation: $violation")
        }
    }
    
    override suspend fun performSecurityMaintenance() {
        try {
            Timber.d("$TAG: Performing security maintenance")
            
            // Clean up expired sessions
            sessionHandler.cleanupExpiredSessions()
            
            // Clear old violation history
            cleanupViolationHistory()
            
            // Clear old protected state data
            stateProtector.clearProtectedData()
            
            // Update security status
            updateSecurityStatus()
            
            Timber.d("$TAG: Security maintenance completed")
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error during security maintenance")
        }
    }
    
    override fun shutdown() {
        try {
            // Cancel all security operations
            securityScope.launch {
                // Clear all sensitive data
                stateProtector.clearProtectedData()
                violationHistory.clear()
                
                // Update status to indicate shutdown
                _securityStatus.value = SecurityStatus(
                    isSecure = false,
                    threatLevel = ThreatLevel.LOW,
                    activeViolations = emptyList(),
                    lastSecurityCheck = System.currentTimeMillis()
                )
            }
            
            isInitialized = false
            Timber.d("$TAG: Navigation security manager shut down")
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error during security manager shutdown")
        }
    }
    
    private fun mapSecurityError(error: SecurityError): SecurityViolation {
        return when (error) {
            SecurityError.InvalidRoute -> SecurityViolation.INVALID_ROUTE
            SecurityError.UnauthorizedNavigation -> SecurityViolation.UNAUTHORIZED_NAVIGATION
            SecurityError.MaliciousDeepLink -> SecurityViolation.MALICIOUS_DEEP_LINK
            SecurityError.SessionExpired -> SecurityViolation.SESSION_EXPIRED
            is SecurityError.CustomError -> SecurityViolation.SUSPICIOUS_ACTIVITY
        }
    }
    
    private fun mapDenialReason(reason: DenialReason): SecurityViolation {
        return when (reason) {
            DenialReason.SESSION_EXPIRED -> SecurityViolation.SESSION_EXPIRED
            DenialReason.INSUFFICIENT_PERMISSIONS -> SecurityViolation.PERMISSION_VIOLATION
            DenialReason.INVALID_SESSION -> SecurityViolation.SESSION_EXPIRED
            DenialReason.SECURITY_VIOLATION -> SecurityViolation.SUSPICIOUS_ACTIVITY
            DenialReason.RATE_LIMITED -> SecurityViolation.RATE_LIMIT_EXCEEDED
        }
    }
    
    private fun isSuspiciousActivity(sessionId: String, fromRoute: String, toRoute: String): Boolean {
        // Simple heuristics for suspicious activity detection
        val violations = violationHistory[sessionId] ?: return false
        
        // Too many violations in short time
        if (violations.size > MAX_VIOLATIONS_PER_SESSION) {
            return true
        }
        
        // Rapid navigation attempts (could indicate automated attacks)
        // This would require tracking navigation timestamps
        
        return false
    }
    
    private fun recordViolations(sessionId: String, violations: List<SecurityViolation>) {
        val sessionViolations = violationHistory.getOrPut(sessionId) { mutableListOf() }
        sessionViolations.addAll(violations)
        
        // Limit violation history size
        if (sessionViolations.size > MAX_VIOLATIONS_PER_SESSION * 2) {
            sessionViolations.removeAt(0)
        }
    }
    
    private fun calculateThreatLevel(violations: List<SecurityViolation>): ThreatLevel {
        val criticalViolations = violations.count { 
            it in listOf(SecurityViolation.STATE_TAMPERING, SecurityViolation.MALICIOUS_DEEP_LINK) 
        }
        val highViolations = violations.count { 
            it in listOf(SecurityViolation.UNAUTHORIZED_NAVIGATION, SecurityViolation.PERMISSION_VIOLATION) 
        }
        
        return when {
            criticalViolations > 0 -> ThreatLevel.CRITICAL
            highViolations > 2 -> ThreatLevel.HIGH
            violations.size > 5 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }
    }
    
    private fun cleanupViolationHistory() {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 24 * 60 * 60 * 1000L // 24 hours
        
        // In a real implementation, you'd track timestamps for violations
        // For now, just limit the size
        violationHistory.values.forEach { violations ->
            if (violations.size > MAX_VIOLATIONS_PER_SESSION) {
                violations.removeAt(0)
            }
        }
    }
    
    private fun updateSecurityStatus() {
        val currentTime = System.currentTimeMillis()
        val currentStatus = _securityStatus.value
        
        _securityStatus.value = currentStatus.copy(
            lastSecurityCheck = currentTime,
            isSecure = isInitialized && currentStatus.threatLevel != ThreatLevel.CRITICAL
        )
    }
}