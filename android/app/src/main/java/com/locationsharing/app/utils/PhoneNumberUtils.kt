package com.locationsharing.app.utils

import java.util.Locale

/**
 * Utility class for phone number normalization and validation
 * Handles international formats and common phone number patterns
 */
object PhoneNumberUtils {
    
    private val PHONE_NUMBER_REGEX = Regex("[^0-9+]")
    private val LEADING_ZEROS_REGEX = Regex("^0+")
    private val COUNTRY_CODE_REGEX = Regex("^\\+?1?")
    
    /**
     * Normalize a phone number by removing formatting and standardizing format
     * 
     * @param phoneNumber Raw phone number string
     * @param defaultCountryCode Default country code to apply (e.g., "1" for US)
     * @return Normalized phone number in E.164 format or null if invalid
     */
    fun normalizePhoneNumber(
        phoneNumber: String,
        defaultCountryCode: String = "1"
    ): String? {
        if (phoneNumber.isBlank()) return null
        
        try {
            // Remove all non-digit characters except +
            var cleaned = phoneNumber.replace(PHONE_NUMBER_REGEX, "")
            
            if (cleaned.isEmpty()) return null
            
            // Handle different formats
            cleaned = when {
                // Already has country code with +
                cleaned.startsWith("+") -> cleaned
                
                // Has country code without +
                cleaned.length >= 10 && (cleaned.startsWith("1") || cleaned.length == 11) -> {
                    if (cleaned.startsWith("1") && cleaned.length == 11) {
                        "+$cleaned"
                    } else {
                        "+1$cleaned"
                    }
                }
                
                // Local number, add default country code
                cleaned.length >= 7 -> {
                    // Remove leading zeros
                    val withoutZeros = cleaned.replace(LEADING_ZEROS_REGEX, "")
                    if (withoutZeros.length >= 7) {
                        "+$defaultCountryCode$withoutZeros"
                    } else {
                        return null
                    }
                }
                
                // Too short to be valid
                else -> return null
            }
            
            // Validate final format
            return if (isValidE164Format(cleaned)) cleaned else null
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Check if phone number is in valid E.164 format
     */
    private fun isValidE164Format(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) return false
        
        return phoneNumber.matches(Regex("^\\+[1-9]\\d{1,14}$"))
    }
    
    /**
     * Format phone number for display
     * Converts E.164 format to human-readable format
     */
    fun formatForDisplay(phoneNumber: String): String {
        if (!phoneNumber.startsWith("+")) return phoneNumber
        
        return when {
            // US/Canada numbers (+1XXXXXXXXXX)
            phoneNumber.startsWith("+1") && phoneNumber.length == 12 -> {
                val number = phoneNumber.substring(2)
                "(${number.substring(0, 3)}) ${number.substring(3, 6)}-${number.substring(6)}"
            }
            
            // Other international numbers - just add spaces
            phoneNumber.length > 4 -> {
                val countryCode = phoneNumber.substring(0, phoneNumber.length - 10)
                val number = phoneNumber.substring(phoneNumber.length - 10)
                "$countryCode ${number.substring(0, 3)} ${number.substring(3, 6)} ${number.substring(6)}"
            }
            
            else -> phoneNumber
        }
    }
    
    /**
     * Extract country code from E.164 formatted number
     */
    fun extractCountryCode(phoneNumber: String): String? {
        if (!phoneNumber.startsWith("+")) return null
        
        return when {
            phoneNumber.startsWith("+1") -> "1"
            phoneNumber.length >= 3 -> phoneNumber.substring(1, 3)
            else -> null
        }
    }
    
    /**
     * Check if two phone numbers are equivalent
     * Handles different formatting of the same number
     */
    fun areEquivalent(phone1: String?, phone2: String?): Boolean {
        if (phone1.isNullOrBlank() || phone2.isNullOrBlank()) return false
        
        val normalized1 = normalizePhoneNumber(phone1)
        val normalized2 = normalizePhoneNumber(phone2)
        
        return normalized1 != null && normalized2 != null && normalized1 == normalized2
    }
    
    /**
     * Validate if a string could be a phone number
     * Basic validation before normalization
     */
    fun isValidPhoneNumber(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) return false
        
        // Must contain at least 7 digits
        val digitCount = phoneNumber.count { it.isDigit() }
        return digitCount >= 7 && digitCount <= 15
    }
}