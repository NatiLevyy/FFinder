package com.locationsharing.app.data.discovery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.locationsharing.app.data.auth.AuthManager
import com.locationsharing.app.data.user.UserProfileManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for discovering users through contact phone numbers
 */
@Singleton
class UserDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager,
    private val userProfileManager: UserProfileManager
) {
    
    companion object {
        private const val USERS_PUBLIC_COLLECTION = "users_public"
        private const val CHUNK_SIZE = 10 // Firestore whereIn limit
    }
    
    /**
     * Discover FFinder users from device contacts
     * 
     * @return List of Friend candidates found in contacts
     */
    suspend fun discoverContactsOnFFinder(): Result<List<FriendCandidate>> = withContext(Dispatchers.IO) {
        try {
            // Check permission
            if (!hasContactsPermission()) {
                return@withContext Result.failure(Exception("Contacts permission not granted"))
            }
            
            // Ensure user is authenticated
            authManager.ensureSignedIn()
            
            // Get contacts from device
            val contacts = getDeviceContacts()
            if (contacts.isEmpty()) {
                Timber.d("No contacts found on device")
                return@withContext Result.success(emptyList())
            }
            
            Timber.d("Found ${contacts.size} contacts, normalizing phone numbers")
            
            // Normalize phone numbers to E.164 and compute hashes
            val phoneHashes = contacts.mapNotNull { contact ->
                val normalizedPhone = normalizeToE164(contact.phoneNumber)
                if (normalizedPhone != null) {
                    val hash = userProfileManager.computeContactPhoneHash(normalizedPhone)
                    ContactHash(
                        originalContact = contact,
                        phoneHash = hash
                    )
                } else {
                    Timber.w("Could not normalize phone number: ${contact.phoneNumber}")
                    null
                }
            }
            
            if (phoneHashes.isEmpty()) {
                Timber.d("No valid phone numbers found in contacts")
                return@withContext Result.success(emptyList())
            }
            
            Timber.d("Querying Firestore for ${phoneHashes.size} phone hashes")
            
            // Query Firestore in chunks of 10 (whereIn limit)
            val friendCandidates = mutableListOf<FriendCandidate>()
            val phoneHashMap = phoneHashes.associateBy { it.phoneHash }
            
            phoneHashes.chunked(CHUNK_SIZE).forEach { chunk ->
                val hashes = chunk.map { it.phoneHash }
                
                try {
                    val querySnapshot = firestore.collection(USERS_PUBLIC_COLLECTION)
                        .whereIn("phoneHash", hashes)
                        .whereEqualTo("discoverable", true)
                        .get()
                        .await()
                    
                    querySnapshot.documents.forEach { doc ->
                        val phoneHash = doc.getString("phoneHash")
                        val uid = doc.id
                        val displayName = doc.getString("displayName")
                        
                        phoneHash?.let { hash ->
                            phoneHashMap[hash]?.let { contactHash ->
                                friendCandidates.add(
                                    FriendCandidate(
                                        uid = uid,
                                        displayName = displayName,
                                        contactName = contactHash.originalContact.name,
                                        phoneNumber = contactHash.originalContact.phoneNumber
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error querying chunk: $hashes")
                }
            }
            
            Timber.d("Found ${friendCandidates.size} FFinder users in contacts")
            Result.success(friendCandidates)
        } catch (e: Exception) {
            Timber.e(e, "Error discovering contacts on FFinder")
            Result.failure(e)
        }
    }
    
    /**
     * Get device contacts with phone numbers
     */
    private suspend fun getDeviceContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<DeviceContact>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val number = it.getString(numberIndex) ?: continue
                    
                    if (number.isNotBlank()) {
                        contacts.add(DeviceContact(name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading device contacts")
        }
        
        // Remove duplicates and sort
        contacts.distinctBy { it.phoneNumber }.sortedBy { it.name }
    }
    
    /**
     * Normalize phone number to E.164 format
     * This is a basic implementation - in production, use a library like libphonenumber
     */
    private fun normalizeToE164(phoneNumber: String): String? {
        // Remove all non-digit characters except +
        val cleaned = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        return when {
            // Already in E.164 format
            cleaned.startsWith("+") && cleaned.length >= 10 -> cleaned
            
            // US/Canada number without country code
            cleaned.length == 10 && cleaned.matches(Regex("^[2-9].*")) -> "+1$cleaned"
            
            // US/Canada number with leading 1
            cleaned.length == 11 && cleaned.startsWith("1") -> "+$cleaned"
            
            // Other cases - try to guess country code (basic implementation)
            cleaned.length >= 8 -> {
                // For now, assume US if no country code and reasonable length
                if (!cleaned.startsWith("+")) "+1$cleaned" else cleaned
            }
            
            else -> {
                Timber.w("Could not normalize phone number: $phoneNumber")
                null
            }
        }
    }
    
    /**
     * Check if app has contacts permission
     */
    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Represents a device contact with phone number
 */
data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

/**
 * Represents a contact with computed phone hash
 */
private data class ContactHash(
    val originalContact: DeviceContact,
    val phoneHash: String
)

/**
 * Represents a friend candidate discovered through contacts
 */
data class FriendCandidate(
    val uid: String,
    val displayName: String?,
    val contactName: String,
    val phoneNumber: String
)