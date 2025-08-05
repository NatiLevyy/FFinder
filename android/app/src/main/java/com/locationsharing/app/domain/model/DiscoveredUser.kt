package com.locationsharing.app.domain.model

/**
 * Represents a user discovered through contact matching
 * Contains both the Firebase user data and the original contact that matched
 */
data class DiscoveredUser(
    val userId: String,
    val displayName: String,
    val profilePictureUrl: String? = null,
    val matchedContact: Contact,
    val friendRequestStatus: FriendRequestStatus = FriendRequestStatus.NONE,
    val matchType: ContactMatchType = ContactMatchType.PHONE,
    val lastActive: Long? = null,
    val isOnline: Boolean = false
) {
    /**
     * Get the primary contact method that matched
     */
    val primaryContactMethod: String?
        get() = when (matchType) {
            ContactMatchType.PHONE -> matchedContact.primaryPhoneNumber
            ContactMatchType.EMAIL -> matchedContact.primaryEmailAddress
            ContactMatchType.BOTH -> matchedContact.primaryPhoneNumber ?: matchedContact.primaryEmailAddress
        }
    
    /**
     * Check if this user can receive friend requests
     */
    val canSendFriendRequest: Boolean
        get() = friendRequestStatus == FriendRequestStatus.NONE
    
    /**
     * Check if friend request is pending
     */
    val hasPendingRequest: Boolean
        get() = friendRequestStatus in listOf(
            FriendRequestStatus.SENT,
            FriendRequestStatus.RECEIVED
        )
    
    /**
     * Check if users are already friends
     */
    val isAlreadyFriend: Boolean
        get() = friendRequestStatus == FriendRequestStatus.ACCEPTED
}

/**
 * Enum representing friend request status
 */
enum class FriendRequestStatus {
    NONE,           // No friend request exists
    SENT,           // Friend request sent to this user
    RECEIVED,       // Friend request received from this user
    ACCEPTED,       // Friend request accepted (users are friends)
    DECLINED,       // Friend request declined
    BLOCKED         // User is blocked
}

/**
 * Enum representing how the contact was matched
 */
enum class ContactMatchType {
    PHONE,          // Matched by phone number
    EMAIL,          // Matched by email address
    BOTH            // Matched by both phone and email
}

/**
 * Result of user discovery operation
 */
sealed class UserDiscoveryResult {
    /**
     * Successful discovery with results
     */
    data class Success(
        val discoveredUsers: List<DiscoveredUser>,
        val totalContactsSearched: Int,
        val matchedContactsCount: Int = discoveredUsers.size
    ) : UserDiscoveryResult() {
        
        val hasMatches: Boolean
            get() = discoveredUsers.isNotEmpty()
        
        val matchRate: Float
            get() = if (totalContactsSearched > 0) {
                matchedContactsCount.toFloat() / totalContactsSearched.toFloat()
            } else 0f
    }
    
    /**
     * Discovery failed due to network or server error
     */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Discovery failed"
    ) : UserDiscoveryResult()
    
    /**
     * Discovery completed but no users found
     */
    data class NoMatches(
        val totalContactsSearched: Int
    ) : UserDiscoveryResult()
    
    /**
     * Discovery was cancelled
     */
    data object Cancelled : UserDiscoveryResult()
}

/**
 * Extension functions for easier result handling
 */
val UserDiscoveryResult.isSuccess: Boolean
    get() = this is UserDiscoveryResult.Success

val UserDiscoveryResult.isError: Boolean
    get() = this is UserDiscoveryResult.Error

val UserDiscoveryResult.discoveredUsers: List<DiscoveredUser>
    get() = when (this) {
        is UserDiscoveryResult.Success -> discoveredUsers
        else -> emptyList()
    }