package com.locationsharing.app.domain.model

/**
 * Domain model representing a contact from the user's phone
 * Used for friend discovery and invitation functionality
 */
data class Contact(
    val id: String,
    val displayName: String,
    val phoneNumbers: List<String>,
    val emailAddresses: List<String>,
    val photoUri: String? = null,
    val lookupKey: String? = null
) {
    /**
     * Get the primary phone number (first non-empty number)
     */
    val primaryPhoneNumber: String?
        get() = phoneNumbers.firstOrNull { it.isNotBlank() }
    
    /**
     * Get the primary email address (first non-empty email)
     */
    val primaryEmailAddress: String?
        get() = emailAddresses.firstOrNull { it.isNotBlank() }
    
    /**
     * Check if contact has any phone numbers
     */
    val hasPhoneNumber: Boolean
        get() = phoneNumbers.any { it.isNotBlank() }
    
    /**
     * Check if contact has any email addresses
     */
    val hasEmailAddress: Boolean
        get() = emailAddresses.any { it.isNotBlank() }
    
    /**
     * Check if contact is valid for friend discovery
     * (has at least phone number or email)
     */
    val isValidForDiscovery: Boolean
        get() = hasPhoneNumber || hasEmailAddress
}