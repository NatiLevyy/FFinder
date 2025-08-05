package com.locationsharing.app.utils

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for cryptographic operations
 * Handles secure hashing for contact privacy and data protection
 */
object CryptoUtils {
    
    private const val HASH_ALGORITHM = "SHA-256"
    private const val APP_SALT = "FFinder_2024_Contact_Salt_v1"
    
    /**
     * Hash a phone number using SHA-256 with app-specific salt
     * This ensures privacy while allowing server-side matching
     * 
     * @param phoneNumber Phone number in E.164 format (e.g., +15551234567)
     * @return Hashed phone number as hex string, or null if input is invalid
     */
    fun hashPhoneNumber(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        
        return try {
            // Normalize phone number (remove any remaining formatting)
            val normalized = phoneNumber.replace(Regex("[^+0-9]"), "")
            if (normalized.isEmpty()) return null
            
            // Create salted input
            val saltedInput = "$APP_SALT:$normalized"
            
            // Generate SHA-256 hash
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(saltedInput.toByteArray(Charsets.UTF_8))
            
            // Convert to hex string
            hashBytes.joinToString("") { "%02x".format(it) }
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Hash an email address using SHA-256 with app-specific salt
     * 
     * @param email Email address (will be normalized to lowercase)
     * @return Hashed email as hex string, or null if input is invalid
     */
    fun hashEmail(email: String?): String? {
        if (email.isNullOrBlank()) return null
        
        return try {
            // Normalize email (lowercase and trim)
            val normalized = email.lowercase().trim()
            if (normalized.isEmpty() || !isValidEmailFormat(normalized)) return null
            
            // Create salted input
            val saltedInput = "$APP_SALT:$normalized"
            
            // Generate SHA-256 hash
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(saltedInput.toByteArray(Charsets.UTF_8))
            
            // Convert to hex string
            hashBytes.joinToString("") { "%02x".format(it) }
            
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Hash multiple phone numbers efficiently
     * 
     * @param phoneNumbers List of phone numbers to hash
     * @return Map of original phone number to hash (only successful hashes included)
     */
    fun hashPhoneNumbers(phoneNumbers: List<String>): Map<String, String> {
        return phoneNumbers.mapNotNull { phone ->
            hashPhoneNumber(phone)?.let { hash ->
                phone to hash
            }
        }.toMap()
    }
    
    /**
     * Hash multiple email addresses efficiently
     * 
     * @param emails List of email addresses to hash
     * @return Map of original email to hash (only successful hashes included)
     */
    fun hashEmails(emails: List<String>): Map<String, String> {
        return emails.mapNotNull { email ->
            hashEmail(email)?.let { hash ->
                email to hash
            }
        }.toMap()
    }
    
    /**
     * Generate a secure random string for user IDs or tokens
     * 
     * @param length Length of the random string
     * @return Secure random hex string
     */
    fun generateSecureRandomString(length: Int = 32): String {
        val random = SecureRandom()
        val bytes = ByteArray(length / 2)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Validate email format (basic validation)
     */
    private fun isValidEmailFormat(email: String): Boolean {
        return email.contains("@") && 
               email.contains(".") && 
               email.length >= 5 &&
               !email.startsWith("@") &&
               !email.endsWith("@")
    }
    
    /**
     * Create a hash for contact matching that includes both phone and email
     * This creates a composite hash for contacts with multiple identifiers
     * 
     * @param phoneNumber Phone number (optional)
     * @param email Email address (optional)
     * @return Composite hash or null if both inputs are invalid
     */
    fun createContactHash(phoneNumber: String?, email: String?): String? {
        val phoneHash = hashPhoneNumber(phoneNumber)
        val emailHash = hashEmail(email)
        
        return when {
            phoneHash != null && emailHash != null -> {
                // Create composite hash from both
                val composite = "$phoneHash:$emailHash"
                val digest = MessageDigest.getInstance(HASH_ALGORITHM)
                val hashBytes = digest.digest(composite.toByteArray(Charsets.UTF_8))
                hashBytes.joinToString("") { "%02x".format(it) }
            }
            phoneHash != null -> phoneHash
            emailHash != null -> emailHash
            else -> null
        }
    }
    
    /**
     * Verify that a hash was created with the current salt
     * Useful for migration scenarios or security audits
     */
    fun verifyHashIntegrity(originalValue: String, hash: String): Boolean {
        return when {
            originalValue.startsWith("+") -> hashPhoneNumber(originalValue) == hash
            originalValue.contains("@") -> hashEmail(originalValue) == hash
            else -> false
        }
    }
}