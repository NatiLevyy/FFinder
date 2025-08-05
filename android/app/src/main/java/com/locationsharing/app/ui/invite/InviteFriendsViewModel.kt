package com.locationsharing.app.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactImportResult
import com.locationsharing.app.domain.model.DiscoveredUser
import com.locationsharing.app.domain.model.FriendRequestStatus
import com.locationsharing.app.domain.model.UserDiscoveryResult
import com.locationsharing.app.domain.repository.ContactImportManager
import com.locationsharing.app.domain.repository.ContactImportProgress
import com.locationsharing.app.domain.repository.UserDiscoveryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Enhanced ViewModel for the Invite Friends screen
 * Manages contact import, user discovery, and friend invitation functionality
 */
@HiltViewModel
class InviteFriendsViewModel @Inject constructor(
    private val contactImportManager: ContactImportManager,
    private val userDiscoveryService: UserDiscoveryService,
    private val friendsRepository: com.locationsharing.app.data.friends.FriendsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InviteFriendsUiState())
    val uiState: StateFlow<InviteFriendsUiState> = _uiState.asStateFlow()
    
    companion object {
        // Debug flag to enable test mode
        private const val DEBUG_MODE = true // Set to false for production
    }
    
    init {
        checkInitialState()
    }
    
    /**
     * Check initial state and load cached contacts if available
     */
    private fun checkInitialState() {
        viewModelScope.launch {
            try {
                Timber.d("🚀 InviteFriendsViewModel: Checking initial state...")
                val hasPermission = contactImportManager.hasContactsPermission()
                val cachedContacts = contactImportManager.getCachedContacts()
                
                Timber.d("📋 Initial state check: permission=$hasPermission, cached=${cachedContacts.size} contacts")
                
                _uiState.value = _uiState.value.copy(
                    hasContactsPermission = hasPermission,
                    allContacts = cachedContacts,
                    isLoading = false
                )
                
                // If we have cached contacts, also run discovery
                if (cachedContacts.isNotEmpty()) {
                    Timber.d("🔍 Found cached contacts, starting user discovery...")
                    discoverUsers(cachedContacts)
                } else {
                    Timber.d("📭 No cached contacts found")
                }
                
                Timber.d("✅ Initial state setup complete")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error checking initial state")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Request contacts permission
     * Should be called from Activity context
     */
    fun requestContactsPermission() {
        viewModelScope.launch {
            try {
                Timber.d("📱 Requesting contacts permission...")
                val granted = contactImportManager.requestContactsPermission()
                
                Timber.d("🔐 Permission result: granted=$granted")
                _uiState.value = _uiState.value.copy(
                    hasContactsPermission = granted,
                    permissionRequested = true
                )
                
                if (granted) {
                    Timber.d("✅ Permission granted, starting import...")
                    importContactsAndDiscover()
                } else {
                    Timber.w("❌ Permission denied by user")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error requesting contacts permission")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to request permission: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Import contacts from device and discover users
     */
    fun importContactsAndDiscover() {
        viewModelScope.launch {
            Timber.d("📥 Starting contact import and discovery...")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                importProgress = null
            )
            
            contactImportManager.importContactsWithProgress().collect { progress ->
                when (progress) {
                    is ContactImportProgress.Starting -> {
                        Timber.d("🔄 Contact import starting...")
                        _uiState.value = _uiState.value.copy(
                            importProgress = "Starting import..."
                        )
                    }
                    
                    is ContactImportProgress.Loading -> {
                        val percentage = if (progress.total > 0) {
                            (progress.processed * 100) / progress.total
                        } else 0
                        
                        Timber.d("📊 Contact import progress: ${progress.processed}/${progress.total} ($percentage%)")
                        _uiState.value = _uiState.value.copy(
                            importProgress = "Loading contacts... $percentage%"
                        )
                    }
                    
                    is ContactImportProgress.Completed -> {
                        Timber.d("✅ Contact import completed, handling result...")
                        handleImportResult(progress.result)
                    }
                }
            }
        }
    }
    
    /**
     * Handle the result of contact import
     */
    private fun handleImportResult(result: ContactImportResult) {
        when (result) {
            is ContactImportResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    allContacts = result.contacts,
                    importProgress = null,
                    error = null
                )
                Timber.d("Contact import successful: ${result.contacts.size} contacts")
                
                // Start user discovery
                discoverUsers(result.contacts)
            }
            
            is ContactImportResult.PermissionDenied -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasContactsPermission = false,
                    importProgress = null,
                    error = "Contacts permission is required to find friends"
                )
            }
            
            is ContactImportResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    importProgress = null,
                    error = "Failed to import contacts: ${result.message}"
                )
                Timber.e(result.exception, "Contact import failed")
            }
            
            is ContactImportResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    importProgress = null,
                    error = null
                )
            }
        }
    }
    
    /**
     * Retry contact import
     */
    /**
     * Discover users from imported contacts
     */
    private fun discoverUsers(contacts: List<Contact>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDiscovering = true,
                discoveryProgress = "Finding friends on FFinder..."
            )
            
            userDiscoveryService.discoverUsers(contacts).collect { result ->
                handleDiscoveryResult(result)
            }
        }
    }
    
    /**
     * Handle the result of user discovery
     */
    private fun handleDiscoveryResult(result: UserDiscoveryResult) {
        when (result) {
            is UserDiscoveryResult.Success -> {
                val discoveredUsers = result.discoveredUsers
                val nonUserContacts = _uiState.value.allContacts.filter { contact ->
                    discoveredUsers.none { it.matchedContact.id == contact.id }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDiscovering = false,
                    discoveredUsers = discoveredUsers,
                    nonUserContacts = nonUserContacts,
                    discoveryProgress = null,
                    error = null
                )
                
                // Check friend request status for all discovered users
                checkFriendRequestStatuses(discoveredUsers)
                
                Timber.d("User discovery completed: ${discoveredUsers.size} users found, ${nonUserContacts.size} non-users")
            }
            
            is UserDiscoveryResult.NoMatches -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDiscovering = false,
                    discoveredUsers = emptyList(),
                    nonUserContacts = _uiState.value.allContacts,
                    discoveryProgress = null,
                    error = null
                )
                
                Timber.d("No users discovered from ${result.totalContactsSearched} contacts")
            }
            
            is UserDiscoveryResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDiscovering = false,
                    discoveryProgress = null,
                    error = "Failed to find friends: ${result.message}"
                )
                
                Timber.e(result.exception, "User discovery failed")
            }
            
            is UserDiscoveryResult.Cancelled -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDiscovering = false,
                    discoveryProgress = null
                )
            }
        }
    }
    
    /**
     * Retry contact import and discovery
     */
    fun retryImport() {
        viewModelScope.launch {
            try {
                Timber.d("🔄 Retrying contact import and discovery...")
                contactImportManager.clearCache()
                userDiscoveryService.clearCache()
                importContactsAndDiscover()
            } catch (e: Exception) {
                Timber.e(e, "❌ Error during retry")
                _uiState.value = _uiState.value.copy(
                    error = "Retry failed: ${e.message}",
                    isLoading = false,
                    isDiscovering = false
                )
            }
        }
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Handle network-related errors with appropriate messaging
     */
    private fun handleNetworkError(error: Throwable): String {
        return when {
            error.message?.contains("network", ignoreCase = true) == true -> 
                "No internet connection. Please check your network and try again."
            error.message?.contains("timeout", ignoreCase = true) == true -> 
                "Request timed out. Please try again."
            error.message?.contains("host", ignoreCase = true) == true -> 
                "Unable to connect to server. Please try again later."
            else -> "Network error: ${error.message}"
        }
    }
    
    /**
     * Handle discovered user selection for friend requests
     */
    fun selectDiscoveredUser(user: DiscoveredUser) {
        val currentSelected = _uiState.value.selectedDiscoveredUsers.toMutableSet()
        if (currentSelected.contains(user.userId)) {
            currentSelected.remove(user.userId)
        } else {
            currentSelected.add(user.userId)
        }
        
        _uiState.value = _uiState.value.copy(
            selectedDiscoveredUsers = currentSelected
        )
    }
    
    /**
     * Handle non-user contact selection for invitations
     */
    fun selectNonUserContact(contact: Contact) {
        val currentSelected = _uiState.value.selectedNonUserContacts.toMutableSet()
        if (currentSelected.contains(contact.id)) {
            currentSelected.remove(contact.id)
        } else {
            currentSelected.add(contact.id)
        }
        
        _uiState.value = _uiState.value.copy(
            selectedNonUserContacts = currentSelected
        )
    }
    
    /**
     * Check friend request statuses for discovered users
     */
    private fun checkFriendRequestStatuses(discoveredUsers: List<DiscoveredUser>) {
        viewModelScope.launch {
            try {
                val updatedUsers = discoveredUsers.map { user ->
                    val status = friendsRepository.checkFriendRequestStatus(user.userId)
                    user.copy(friendRequestStatus = status)
                }
                
                _uiState.value = _uiState.value.copy(
                    discoveredUsers = updatedUsers
                )
                
                Timber.d("Friend request statuses checked for ${updatedUsers.size} users")
            } catch (e: Exception) {
                Timber.e(e, "Error checking friend request statuses")
                // Continue with original users if status check fails
            }
        }
    }
    
    /**
     * Send friend requests to selected discovered users
     */
    fun sendFriendRequests() {
        viewModelScope.launch {
            val selectedUserIds = _uiState.value.selectedDiscoveredUsers
            val selectedUsers = _uiState.value.discoveredUsers.filter { 
                it.userId in selectedUserIds && it.canSendFriendRequest 
            }
            
            if (selectedUsers.isEmpty()) return@launch
            
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    currentProgress = "Sending friend requests..."
                )
                
                var successCount = 0
                var errorCount = 0
                
                // Send friend requests using the repository
                selectedUsers.forEach { user ->
                    val result = friendsRepository.sendFriendRequest(user.userId)
                    if (result.isSuccess) {
                        successCount++
                        Timber.d("Friend request sent to ${user.displayName}")
                    } else {
                        errorCount++
                        Timber.e("Failed to send friend request to ${user.displayName}: ${result.exceptionOrNull()?.message}")
                    }
                }
                
                // Update UI state to reflect sent requests
                val updatedUsers = _uiState.value.discoveredUsers.map { user ->
                    if (user.userId in selectedUserIds && selectedUsers.any { it.userId == user.userId }) {
                        user.copy(friendRequestStatus = FriendRequestStatus.SENT)
                    } else {
                        user
                    }
                }
                
                val message = when {
                    errorCount == 0 -> null
                    successCount == 0 -> "Failed to send all friend requests"
                    else -> "Some friend requests failed to send"
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    discoveredUsers = updatedUsers,
                    selectedDiscoveredUsers = emptySet(),
                    currentProgress = null,
                    error = message
                )
                
                Timber.d("Friend requests completed: $successCount sent, $errorCount failed")
                
            } catch (e: Exception) {
                Timber.e(e, "Error sending friend requests")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentProgress = null,
                    error = "Failed to send friend requests: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Send invitations to selected non-user contacts
     */
    fun sendInvitations() {
        val selectedContactIds = _uiState.value.selectedNonUserContacts
        val selectedContacts = _uiState.value.nonUserContacts.filter { it.id in selectedContactIds }
        
        Timber.d("Preparing to send invitations to ${selectedContacts.size} contacts")
        
        // Clear selection (the actual sharing is handled by the UI layer)
        _uiState.value = _uiState.value.copy(
            selectedNonUserContacts = emptySet()
        )
    }
    
    /**
     * Send a single friend request to a specific user
     */
    fun sendSingleFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                Timber.d("👤 Sending friend request to user: $userId")
                val user = _uiState.value.discoveredUsers.find { it.userId == userId }
                if (user == null || !user.canSendFriendRequest) {
                    Timber.w("❌ Cannot send friend request to user $userId: user=${user?.displayName}, canSend=${user?.canSendFriendRequest}")
                    _uiState.value = _uiState.value.copy(
                        error = "Cannot send friend request to this user"
                    )
                    return@launch
                }
                
                Timber.d("📤 Calling friendsRepository.sendFriendRequest for ${user.displayName}")
                val result = friendsRepository.sendFriendRequest(userId)
                
                if (result.isSuccess) {
                    Timber.d("✅ Friend request sent successfully to ${user.displayName}")
                    // Update UI state to reflect sent request
                    val updatedUsers = _uiState.value.discoveredUsers.map { discoveredUser ->
                        if (discoveredUser.userId == userId) {
                            discoveredUser.copy(friendRequestStatus = FriendRequestStatus.SENT)
                        } else {
                            discoveredUser
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        discoveredUsers = updatedUsers
                    )
                    
                    Timber.d("🔄 UI state updated for user: ${user.displayName}")
                } else {
                    Timber.e("❌ Failed to send friend request to ${user.displayName}: ${result.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to send friend request: ${result.exceptionOrNull()?.message}"
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception sending friend request to user: $userId")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send friend request: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cancel a friend request to a specific user
     */
    fun cancelFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                Timber.d("🚫 Cancelling friend request to user: $userId")
                val result = friendsRepository.cancelFriendRequest(userId)
                
                if (result.isSuccess) {
                    Timber.d("✅ Friend request cancelled successfully")
                    // Update UI state to reflect cancelled request
                    val updatedUsers = _uiState.value.discoveredUsers.map { user ->
                        if (user.userId == userId) {
                            user.copy(friendRequestStatus = FriendRequestStatus.NONE)
                        } else {
                            user
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        discoveredUsers = updatedUsers
                    )
                    
                    Timber.d("🔄 UI state updated after cancellation")
                } else {
                    Timber.e("❌ Failed to cancel friend request: ${result.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to cancel friend request: ${result.exceptionOrNull()?.message}"
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Exception cancelling friend request to user: $userId")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cancel friend request: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Debug method to simulate different friend request states for testing
     * Only available in DEBUG_MODE
     */
    fun simulateFriendRequestState(userId: String, status: FriendRequestStatus) {
        if (!DEBUG_MODE) return
        
        Timber.d("🧪 DEBUG: Simulating friend request status $status for user $userId")
        
        val updatedUsers = _uiState.value.discoveredUsers.map { user ->
            if (user.userId == userId) {
                user.copy(friendRequestStatus = status)
            } else {
                user
            }
        }
        
        _uiState.value = _uiState.value.copy(
            discoveredUsers = updatedUsers
        )
        
        Timber.d("🔄 DEBUG: UI state updated with simulated status")
    }
    
    /**
     * Debug method to add test discovered users for visual testing
     * Only available in DEBUG_MODE
     */
    fun addTestDiscoveredUsers() {
        if (!DEBUG_MODE) return
        
        Timber.d("🧪 DEBUG: Adding test discovered users")
        
        val testUsers = listOf(
            DiscoveredUser(
                userId = "test-user-1",
                displayName = "John Doe (NONE)",
                profilePictureUrl = null,
                matchedContact = Contact(
                    id = "test-contact-1",
                    displayName = "John Doe",
                    phoneNumbers = listOf("+1234567890"),
                    emailAddresses = emptyList()
                ),
                friendRequestStatus = FriendRequestStatus.NONE,
                isOnline = true
            ),
            DiscoveredUser(
                userId = "test-user-2",
                displayName = "Jane Smith (SENT)",
                profilePictureUrl = null,
                matchedContact = Contact(
                    id = "test-contact-2",
                    displayName = "Jane Smith",
                    phoneNumbers = listOf("+1234567891"),
                    emailAddresses = emptyList()
                ),
                friendRequestStatus = FriendRequestStatus.SENT,
                isOnline = false
            ),
            DiscoveredUser(
                userId = "test-user-3",
                displayName = "Bob Johnson (ACCEPTED)",
                profilePictureUrl = null,
                matchedContact = Contact(
                    id = "test-contact-3",
                    displayName = "Bob Johnson",
                    phoneNumbers = listOf("+1234567892"),
                    emailAddresses = emptyList()
                ),
                friendRequestStatus = FriendRequestStatus.ACCEPTED,
                isOnline = true
            ),
            DiscoveredUser(
                userId = "test-user-4",
                displayName = "Alice Brown (RECEIVED)",
                profilePictureUrl = null,
                matchedContact = Contact(
                    id = "test-contact-4",
                    displayName = "Alice Brown",
                    phoneNumbers = listOf("+1234567893"),
                    emailAddresses = emptyList()
                ),
                friendRequestStatus = FriendRequestStatus.RECEIVED,
                isOnline = true
            )
        )
        
        val testNonUserContacts = listOf(
            Contact(
                id = "non-user-1",
                displayName = "Mike Wilson",
                phoneNumbers = listOf("+1234567894"),
                emailAddresses = emptyList()
            ),
            Contact(
                id = "non-user-2",
                displayName = "Sarah Davis",
                phoneNumbers = listOf("+1234567895"),
                emailAddresses = emptyList()
            )
        )
        
        _uiState.value = _uiState.value.copy(
            discoveredUsers = testUsers,
            nonUserContacts = testNonUserContacts,
            allContacts = testUsers.map { it.matchedContact } + testNonUserContacts,
            hasContactsPermission = true,
            isLoading = false,
            isDiscovering = false
        )
        
        Timber.d("🔄 DEBUG: Added ${testUsers.size} test users and ${testNonUserContacts.size} non-user contacts")
    }
}

/**
 * Enhanced UI state for the Invite Friends screen
 */
data class InviteFriendsUiState(
    val isLoading: Boolean = true,
    val isDiscovering: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val permissionRequested: Boolean = false,
    val allContacts: List<Contact> = emptyList(),
    val discoveredUsers: List<DiscoveredUser> = emptyList(),
    val nonUserContacts: List<Contact> = emptyList(),
    val selectedDiscoveredUsers: Set<String> = emptySet(),
    val selectedNonUserContacts: Set<String> = emptySet(),
    val importProgress: String? = null,
    val discoveryProgress: String? = null,
    val currentProgress: String? = null,
    val error: String? = null
) {
    val hasContacts: Boolean
        get() = allContacts.isNotEmpty()
    
    val hasDiscoveredUsers: Boolean
        get() = discoveredUsers.isNotEmpty()
    
    val hasNonUserContacts: Boolean
        get() = nonUserContacts.isNotEmpty()
    
    val canImportContacts: Boolean
        get() = hasContactsPermission && !isLoading && !isDiscovering
    
    val selectedDiscoveredUsersCount: Int
        get() = selectedDiscoveredUsers.size
    
    val selectedNonUserContactsCount: Int
        get() = selectedNonUserContacts.size
    
    val isProcessing: Boolean
        get() = isLoading || isDiscovering
    
    val displayProgress: String?
        get() = currentProgress ?: importProgress ?: discoveryProgress
    
    val discoveryComplete: Boolean
        get() = hasContacts && !isProcessing
}