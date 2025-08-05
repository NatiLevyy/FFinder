package com.locationsharing.app.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactImportResult
import com.locationsharing.app.domain.repository.ContactImportManager
import com.locationsharing.app.domain.repository.ContactImportProgress
import com.locationsharing.app.utils.PhoneNumberUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of ContactImportManager
 * Handles contact import from Android ContactsContract API with proper error handling
 */
@Singleton
class ContactImportManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactImportManager {
    
    companion object {
        private const val TAG = "ContactImportManager"
        private const val BATCH_SIZE = 50
    }
    
    // Cache for imported contacts
    private var cachedContacts: List<Contact>? = null
    private var lastImportTime: Long = 0
    private val cacheValidityMs = 5 * 60 * 1000L // 5 minutes
    
    override suspend fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override suspend fun requestContactsPermission(): Boolean {
        // Note: This method should be called from Activity context
        // The actual permission request should be handled by the UI layer
        return hasContactsPermission()
    }
    
    override fun importContacts(): Flow<ContactImportResult> = flow {
        try {
            Timber.d("Starting contact import")
            
            if (!hasContactsPermission()) {
                Timber.w("Contacts permission not granted")
                emit(ContactImportResult.PermissionDenied)
                return@flow
            }
            
            // Check cache first
            if (isCacheValid()) {
                Timber.d("Returning cached contacts: ${cachedContacts?.size}")
                emit(ContactImportResult.Success(cachedContacts ?: emptyList()))
                return@flow
            }
            
            val contacts = loadContactsFromDevice()
            
            // Cache the results
            cachedContacts = contacts
            lastImportTime = System.currentTimeMillis()
            
            Timber.d("Contact import completed: ${contacts.size} contacts")
            emit(ContactImportResult.Success(contacts))
            
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception during contact import")
            emit(ContactImportResult.PermissionDenied)
        } catch (e: Exception) {
            Timber.e(e, "Error during contact import")
            emit(ContactImportResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun importContactsWithProgress(): Flow<ContactImportProgress> = flow {
        try {
            emit(ContactImportProgress.Starting)
            
            if (!hasContactsPermission()) {
                emit(ContactImportProgress.Completed(ContactImportResult.PermissionDenied))
                return@flow
            }
            
            // Check cache first
            if (isCacheValid()) {
                emit(ContactImportProgress.Completed(
                    ContactImportResult.Success(cachedContacts ?: emptyList())
                ))
                return@flow
            }
            
            val contacts = loadContactsWithProgress { processed, total ->
                emit(ContactImportProgress.Loading(processed, total))
            }
            
            // Cache the results
            cachedContacts = contacts
            lastImportTime = System.currentTimeMillis()
            
            emit(ContactImportProgress.Completed(ContactImportResult.Success(contacts)))
            
        } catch (e: SecurityException) {
            emit(ContactImportProgress.Completed(ContactImportResult.PermissionDenied))
        } catch (e: Exception) {
            emit(ContactImportProgress.Completed(ContactImportResult.Error(e)))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getCachedContacts(): List<Contact> {
        return if (isCacheValid()) {
            cachedContacts ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    override suspend fun clearCache() {
        cachedContacts = null
        lastImportTime = 0
        Timber.d("Contact cache cleared")
    }
    
    override suspend fun hasImportedContacts(): Boolean {
        return cachedContacts?.isNotEmpty() == true
    }
    
    /**
     * Check if cached contacts are still valid
     */
    private fun isCacheValid(): Boolean {
        return cachedContacts != null && 
               (System.currentTimeMillis() - lastImportTime) < cacheValidityMs
    }
    
    /**
     * Load contacts from device without progress updates
     */
    private suspend fun loadContactsFromDevice(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val contactMap = mutableMapOf<String, ContactBuilder>()
        
        // Query all contacts with phone numbers and emails
        queryContacts { cursor ->
            val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)) ?: ""
            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE))
            val data1 = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1))
            val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.LOOKUP_KEY))
            val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI))
            
            val builder = contactMap.getOrPut(contactId) {
                ContactBuilder(contactId, displayName, lookupKey, photoUri)
            }
            
            when (mimeType) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    data1?.let { phoneNumber ->
                        PhoneNumberUtils.normalizePhoneNumber(phoneNumber)?.let { normalized ->
                            builder.addPhoneNumber(normalized)
                        }
                    }
                }
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    data1?.let { email ->
                        if (isValidEmail(email)) {
                            builder.addEmail(email.lowercase().trim())
                        }
                    }
                }
            }
        }
        
        // Build final contact list
        contactMap.values.mapNotNull { builder ->
            builder.build()?.takeIf { it.isValidForDiscovery }
        }.also { finalContacts ->
            Timber.d("Loaded ${finalContacts.size} valid contacts from ${contactMap.size} total")
        }
    }
    
    /**
     * Load contacts with progress updates
     */
    private suspend fun loadContactsWithProgress(
        onProgress: suspend (processed: Int, total: Int) -> Unit
    ): List<Contact> {
        val contactMap = mutableMapOf<String, ContactBuilder>()
        var totalRows = 0
        var processedRows = 0
        
        // First pass: count total rows
        withContext(Dispatchers.IO) {
            queryContacts { cursor ->
                totalRows++
            }
        }
        
        // Second pass: process contacts with progress
        val result = withContext(Dispatchers.IO) {
            queryContacts { cursor ->
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)) ?: ""
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE))
                val data1 = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1))
                val lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.LOOKUP_KEY))
                val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI))
                
                val builder = contactMap.getOrPut(contactId) {
                    ContactBuilder(contactId, displayName, lookupKey, photoUri)
                }
                
                when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        data1?.let { phoneNumber ->
                            PhoneNumberUtils.normalizePhoneNumber(phoneNumber)?.let { normalized ->
                                builder.addPhoneNumber(normalized)
                            }
                        }
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        data1?.let { email ->
                            if (isValidEmail(email)) {
                                builder.addEmail(email.lowercase().trim())
                            }
                        }
                    }
                }
                
                processedRows++
            }
            
            // Build final contact list
            contactMap.values.mapNotNull { builder ->
                builder.build()?.takeIf { it.isValidForDiscovery }
            }
        }
        
        // Final progress update
        onProgress(totalRows, totalRows)
        
        return result
    }
    
    /**
     * Query contacts using ContactsContract API
     */
    private fun queryContacts(processor: (Cursor) -> Unit) {
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.PHOTO_URI
        )
        
        val selection = "${ContactsContract.Data.MIMETYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
        )
        
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.Data.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                try {
                    processor(cursor)
                } catch (e: Exception) {
                    Timber.w(e, "Error processing contact row")
                }
            }
        }
    }
    
    /**
     * Validate email address format
     */
    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && 
               android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Helper class for building contacts from multiple cursor rows
     */
    private class ContactBuilder(
        private val id: String,
        private val displayName: String,
        private val lookupKey: String?,
        private val photoUri: String?
    ) {
        private val phoneNumbers = mutableSetOf<String>()
        private val emails = mutableSetOf<String>()
        
        fun addPhoneNumber(phoneNumber: String) {
            phoneNumbers.add(phoneNumber)
        }
        
        fun addEmail(email: String) {
            emails.add(email)
        }
        
        fun build(): Contact? {
            return if (displayName.isNotBlank() && (phoneNumbers.isNotEmpty() || emails.isNotEmpty())) {
                Contact(
                    id = id,
                    displayName = displayName,
                    phoneNumbers = phoneNumbers.toList(),
                    emailAddresses = emails.toList(),
                    photoUri = photoUri,
                    lookupKey = lookupKey
                )
            } else {
                null
            }
        }
    }
}