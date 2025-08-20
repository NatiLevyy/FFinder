package com.locationsharing.app.navigation.security

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Interface for protecting navigation state from tampering and unauthorized access.
 */
interface NavigationStateProtector {
    /**
     * Protects navigation state by encrypting sensitive data.
     * 
     * @param state The navigation state to protect
     * @return ProtectedNavigationState with encrypted data
     */
    fun protectState(state: NavigationState): ProtectedNavigationState
    
    /**
     * Unprotects navigation state by decrypting data.
     * 
     * @param protectedState The protected state to decrypt
     * @return NavigationState with decrypted data, or null if invalid
     */
    fun unprotectState(protectedState: ProtectedNavigationState): NavigationState?
    
    /**
     * Validates the integrity of navigation state.
     * 
     * @param state The state to validate
     * @return true if state is valid and untampered
     */
    fun validateStateIntegrity(state: ProtectedNavigationState): Boolean
    
    /**
     * Clears all protected state data.
     */
    fun clearProtectedData()
}

/**
 * Represents navigation state that needs protection.
 */
@Stable
data class NavigationState(
    val currentRoute: String,
    val previousRoute: String?,
    val navigationHistory: List<String>,
    val sessionId: String,
    val timestamp: Long,
    val userPermissions: Set<String>
)

/**
 * Represents protected navigation state with encrypted data.
 */
@Stable
data class ProtectedNavigationState(
    val encryptedData: String,
    val checksum: String,
    val timestamp: Long,
    val version: Int = 1
)

/**
 * Implementation of NavigationStateProtector with AES encryption and integrity checks.
 */
class NavigationStateProtectorImpl : NavigationStateProtector {
    
    companion object {
        private const val TAG = "NavigationStateProtector"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val ALGORITHM = "AES"
        private const val KEY_LENGTH = 256
        private const val IV_LENGTH = 16
    }
    
    private val secretKey: SecretKey by lazy { generateSecretKey() }
    private val stateCache = ConcurrentHashMap<String, ProtectedNavigationState>()
    
    private val _protectionStatus = MutableStateFlow(ProtectionStatus.ACTIVE)
    val protectionStatus: StateFlow<ProtectionStatus> = _protectionStatus.asStateFlow()
    
    override fun protectState(state: NavigationState): ProtectedNavigationState {
        try {
            // Serialize state to JSON-like string
            val stateData = serializeState(state)
            
            // Encrypt the state data
            val encryptedData = encryptData(stateData)
            
            // Generate checksum for integrity verification
            val checksum = generateChecksum(stateData)
            
            val protectedState = ProtectedNavigationState(
                encryptedData = encryptedData,
                checksum = checksum,
                timestamp = System.currentTimeMillis()
            )
            
            // Cache the protected state
            stateCache[state.sessionId] = protectedState
            
            Timber.d("$TAG: Successfully protected navigation state for session: ${state.sessionId}")
            return protectedState
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to protect navigation state")
            _protectionStatus.value = ProtectionStatus.ERROR
            throw SecurityException("Failed to protect navigation state", e)
        }
    }
    
    override fun unprotectState(protectedState: ProtectedNavigationState): NavigationState? {
        try {
            // Validate state integrity first
            if (!validateStateIntegrity(protectedState)) {
                Timber.w("$TAG: State integrity validation failed")
                return null
            }
            
            // Decrypt the state data
            val decryptedData = decryptData(protectedState.encryptedData)
            
            // Deserialize state from decrypted data
            val state = deserializeState(decryptedData)
            
            // Validate timestamp (state should not be too old)
            val currentTime = System.currentTimeMillis()
            val stateAge = currentTime - protectedState.timestamp
            if (stateAge > 24 * 60 * 60 * 1000) { // 24 hours
                Timber.w("$TAG: Protected state is too old: ${stateAge}ms")
                return null
            }
            
            Timber.d("$TAG: Successfully unprotected navigation state")
            return state
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Failed to unprotect navigation state")
            _protectionStatus.value = ProtectionStatus.ERROR
            return null
        }
    }
    
    override fun validateStateIntegrity(state: ProtectedNavigationState): Boolean {
        try {
            // Decrypt data to verify it's valid
            val decryptedData = decryptData(state.encryptedData)
            
            // Verify checksum
            val expectedChecksum = generateChecksum(decryptedData)
            if (expectedChecksum != state.checksum) {
                Timber.w("$TAG: Checksum mismatch - state may be tampered")
                return false
            }
            
            // Verify timestamp is reasonable
            val currentTime = System.currentTimeMillis()
            if (state.timestamp > currentTime || state.timestamp < currentTime - 7 * 24 * 60 * 60 * 1000) {
                Timber.w("$TAG: Invalid timestamp in protected state")
                return false
            }
            
            return true
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error validating state integrity")
            return false
        }
    }
    
    override fun clearProtectedData() {
        try {
            stateCache.clear()
            _protectionStatus.value = ProtectionStatus.CLEARED
            Timber.d("$TAG: Cleared all protected navigation data")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error clearing protected data")
            _protectionStatus.value = ProtectionStatus.ERROR
        }
    }
    
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_LENGTH)
        return keyGenerator.generateKey()
    }
    
    private fun encryptData(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        
        // Combine IV and encrypted data
        val combined = iv + encryptedBytes
        return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
    }
    
    private fun decryptData(encryptedData: String): String {
        val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
        
        // Extract IV and encrypted data
        val iv = combined.sliceArray(0 until IV_LENGTH)
        val encrypted = combined.sliceArray(IV_LENGTH until combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        
        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes)
    }
    
    private fun generateChecksum(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray())
        return android.util.Base64.encodeToString(hashBytes, android.util.Base64.DEFAULT)
    }
    
    private fun serializeState(state: NavigationState): String {
        // Simple JSON-like serialization (in production, use proper JSON library)
        return buildString {
            append("{")
            append("\"currentRoute\":\"${state.currentRoute}\",")
            append("\"previousRoute\":\"${state.previousRoute ?: ""}\",")
            append("\"navigationHistory\":[${state.navigationHistory.joinToString(",") { "\"$it\"" }}],")
            append("\"sessionId\":\"${state.sessionId}\",")
            append("\"timestamp\":${state.timestamp},")
            append("\"userPermissions\":[${state.userPermissions.joinToString(",") { "\"$it\"" }}]")
            append("}")
        }
    }
    
    private fun deserializeState(data: String): NavigationState {
        // Simple JSON-like deserialization (in production, use proper JSON library)
        // This is a simplified implementation for demonstration
        val currentRoute = extractValue(data, "currentRoute")
        val previousRoute = extractValue(data, "previousRoute").takeIf { it.isNotEmpty() }
        val sessionId = extractValue(data, "sessionId")
        val timestamp = extractValue(data, "timestamp").toLong()
        
        // For simplicity, using empty collections (in production, parse properly)
        val navigationHistory = emptyList<String>()
        val userPermissions = emptySet<String>()
        
        return NavigationState(
            currentRoute = currentRoute,
            previousRoute = previousRoute,
            navigationHistory = navigationHistory,
            sessionId = sessionId,
            timestamp = timestamp,
            userPermissions = userPermissions
        )
    }
    
    private fun extractValue(json: String, key: String): String {
        val pattern = "\"$key\":\"([^\"]*)\""
        val regex = Regex(pattern)
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }
}

/**
 * Status of the navigation state protection system.
 */
enum class ProtectionStatus {
    ACTIVE,
    ERROR,
    CLEARED
}