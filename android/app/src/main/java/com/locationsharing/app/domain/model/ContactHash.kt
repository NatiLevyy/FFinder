package com.locationsharing.app.domain.model

/**
 * Represents a contact with hashed identifiers for privacy-preserving discovery
 * Contains both the original contact and the hashed versions for server queries
 */
data class ContactHash(
    val originalContact: Contact,
    val hashedPhone: String? = null,
    val hashedEmail: String? = null,
    val compositeHash: String? = null
) {
    /**
     * Check if this contact has any valid hashes for discovery
     */
    val isValidForDiscovery: Boolean
        get() = hashedPhone != null || hashedEmail != null
    
    /**
     * Get all available hashes for querying
     */
    val allHashes: List<String>
        get() = listOfNotNull(hashedPhone, hashedEmail, compositeHash)
    
    /**
     * Get the primary hash (phone preferred over email)
     */
    val primaryHash: String?
        get() = hashedPhone ?: hashedEmail ?: compositeHash
    
    /**
     * Check if contact has phone hash
     */
    val hasPhoneHash: Boolean
        get() = hashedPhone != null
    
    /**
     * Check if contact has email hash
     */
    val hasEmailHash: Boolean
        get() = hashedEmail != null
    
    /**
     * Get match types available for this contact
     */
    val availableMatchTypes: List<ContactMatchType>
        get() = buildList {
            if (hasPhoneHash) add(ContactMatchType.PHONE)
            if (hasEmailHash) add(ContactMatchType.EMAIL)
            if (hasPhoneHash && hasEmailHash) add(ContactMatchType.BOTH)
        }
}

/**
 * Builder class for creating ContactHash instances
 */
class ContactHashBuilder(private val contact: Contact) {
    private var hashedPhone: String? = null
    private var hashedEmail: String? = null
    private var compositeHash: String? = null
    
    fun withHashedPhone(hash: String?): ContactHashBuilder {
        hashedPhone = hash
        return this
    }
    
    fun withHashedEmail(hash: String?): ContactHashBuilder {
        hashedEmail = hash
        return this
    }
    
    fun withCompositeHash(hash: String?): ContactHashBuilder {
        compositeHash = hash
        return this
    }
    
    fun build(): ContactHash {
        return ContactHash(
            originalContact = contact,
            hashedPhone = hashedPhone,
            hashedEmail = hashedEmail,
            compositeHash = compositeHash
        )
    }
}

/**
 * Extension function to create ContactHash from Contact
 */
fun Contact.toContactHash(
    hashedPhone: String? = null,
    hashedEmail: String? = null,
    compositeHash: String? = null
): ContactHash {
    return ContactHash(
        originalContact = this,
        hashedPhone = hashedPhone,
        hashedEmail = hashedEmail,
        compositeHash = compositeHash
    )
}