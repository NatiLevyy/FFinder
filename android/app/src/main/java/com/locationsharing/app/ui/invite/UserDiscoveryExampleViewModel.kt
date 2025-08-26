package com.locationsharing.app.ui.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactImportResult
import com.locationsharing.app.domain.model.DiscoveredUser
import com.locationsharing.app.domain.model.UserDiscoveryResult
import com.locationsharing.app.domain.repository.ContactImportManager
import com.locationsharing.app.domain.repository.UserDiscoveryService
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
// import javax.inject.Inject

/**
 * Example ViewModel demonstrating the complete flow:
 * ContactImportManager -> UserDiscoveryService -> UI State
 * 
 * This shows how to connect both services for a complete invite friends experience
 */
// @HiltViewModel
class UserDiscoveryExampleViewModel constructor(
    private val contactImportManager: ContactImportManager,
    private val userDiscoveryService: UserDiscoveryService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
    
    /**
     * Complete flow: Import contacts -> Discover users -> Update UI
     */
    fun startCompleteFlow() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    currentStep = "Checking permissions...",
                    error = null
                )
                
                // Step 1: Check permissions
                if (!contactImportManager.hasContactsPermission()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsPermission = true,
                        currentStep = "Permission required"
                    )
                    return@launch
                }
                
                // Step 2: Import contacts
                _uiState.value = _uiState.value.copy(
                    currentStep = "Importing contacts..."
                )
                
                contactImportManager.importContacts().collect { importResult ->
                    when (importResult) {
                        is ContactImportResult.Success -> {
                            val contacts = importResult.contacts
                            _uiState.value = _uiState.value.copy(
                                importedContacts = contacts,
                                currentStep = "Imported ${contacts.size} contacts"
                            )
                            
                            // Step 3: Discover users
                            discoverUsersFromContacts(contacts)
                        }
                        
                        is ContactImportResult.PermissionDenied -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                needsPermission = true,
                                error = "Contacts permission required"
                            )
                        }
                        
                        is ContactImportResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to import contacts: ${importResult.message}"
                            )
                        }
                        
                        is ContactImportResult.Cancelled -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                currentStep = "Import cancelled"
                            )
                        }
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error in complete flow")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Step 3: Discover users from imported contacts
     */
    private suspend fun discoverUsersFromContacts(contacts: List<Contact>) {
        _uiState.value = _uiState.value.copy(
            currentStep = "Finding friends on FFinder..."
        )
        
        userDiscoveryService.discoverUsers(contacts).collect { discoveryResult ->
            when (discoveryResult) {
                is UserDiscoveryResult.Success -> {
                    val discoveredUsers = discoveryResult.discoveredUsers
                    val nonUserContacts = contacts.filter { contact ->
                        discoveredUsers.none { it.matchedContact.id == contact.id }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        discoveredUsers = discoveredUsers,
                        nonUserContacts = nonUserContacts,
                        currentStep = "Found ${discoveredUsers.size} friends on FFinder",
                        discoveryStats = DiscoveryStatsUi(
                            totalContacts = contacts.size,
                            usersFound = discoveredUsers.size,
                            nonUsers = nonUserContacts.size,
                            matchRate = if (contacts.isNotEmpty()) {
                                (discoveredUsers.size.toFloat() / contacts.size.toFloat()) * 100
                            } else 0f
                        )
                    )
                    
                    Timber.d("Discovery complete: ${discoveredUsers.size} users, ${nonUserContacts.size} non-users")
                }
                
                is UserDiscoveryResult.NoMatches -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        discoveredUsers = emptyList(),
                        nonUserContacts = contacts,
                        currentStep = "No friends found on FFinder",
                        discoveryStats = DiscoveryStatsUi(
                            totalContacts = contacts.size,
                            usersFound = 0,
                            nonUsers = contacts.size,
                            matchRate = 0f
                        )
                    )
                }
                
                is UserDiscoveryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to find friends: ${discoveryResult.message}"
                    )
                }
                
                is UserDiscoveryResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStep = "Discovery cancelled"
                    )
                }
            }
        }
    }
    
    /**
     * Example of how to use individual services
     */
    fun demonstrateIndividualServices() {
        viewModelScope.launch {
            // Example 1: Just import contacts
            val importResult = contactImportManager.importContacts()
            importResult.collect { result ->
                when (result) {
                    is ContactImportResult.Success -> {
                        Timber.d("Imported ${result.contacts.size} contacts")
                        // Use contacts for something else
                    }
                    else -> { /* Handle other cases */ }
                }
            }
            
            // Example 2: Just discover users from existing contacts
            val existingContacts = listOf(
                Contact(
                    id = "1",
                    displayName = "John Doe",
                    phoneNumbers = listOf("+15551234567"),
                    emailAddresses = listOf("john@example.com")
                )
            )
            
            val discoveryResult = userDiscoveryService.discoverUsers(existingContacts)
            discoveryResult.collect { result ->
                when (result) {
                    is UserDiscoveryResult.Success -> {
                        Timber.d("Discovered ${result.discoveredUsers.size} users")
                        // Use discovered users
                    }
                    else -> { /* Handle other cases */ }
                }
            }
            
            // Example 3: Hash contacts separately
            val contactHashes = userDiscoveryService.hashContacts(existingContacts)
            Timber.d("Generated ${contactHashes.size} contact hashes")
            
            // Example 4: Find specific user
            val specificUser = userDiscoveryService.findUserByContact(
                phoneNumber = "+15551234567"
            )
            if (specificUser != null) {
                Timber.d("Found user: ${specificUser.displayName}")
            }
            
            // Example 5: Get discovery statistics
            val stats = userDiscoveryService.getDiscoveryStats()
            Timber.d("Discovery stats: ${stats.totalUsersDiscovered} users discovered")
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun retry() {
        startCompleteFlow()
    }
}

/**
 * UI state for the example
 */
data class ExampleUiState(
    val isLoading: Boolean = false,
    val needsPermission: Boolean = false,
    val currentStep: String = "",
    val importedContacts: List<Contact> = emptyList(),
    val discoveredUsers: List<DiscoveredUser> = emptyList(),
    val nonUserContacts: List<Contact> = emptyList(),
    val discoveryStats: DiscoveryStatsUi? = null,
    val error: String? = null
) {
    val hasResults: Boolean
        get() = importedContacts.isNotEmpty()
    
    val hasDiscoveredUsers: Boolean
        get() = discoveredUsers.isNotEmpty()
    
    val hasNonUserContacts: Boolean
        get() = nonUserContacts.isNotEmpty()
}

/**
 * UI-friendly discovery statistics
 */
data class DiscoveryStatsUi(
    val totalContacts: Int,
    val usersFound: Int,
    val nonUsers: Int,
    val matchRate: Float
) {
    val matchRateFormatted: String
        get() = "%.1f%%".format(matchRate)
}