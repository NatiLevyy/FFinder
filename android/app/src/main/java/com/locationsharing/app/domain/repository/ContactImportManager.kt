package com.locationsharing.app.domain.repository

import com.locationsharing.app.domain.model.Contact
import com.locationsharing.app.domain.model.ContactImportResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing contact import operations
 * Handles permission requests, contact loading, and data processing
 */
interface ContactImportManager {
    
    /**
     * Check if contacts permission is currently granted
     */
    suspend fun hasContactsPermission(): Boolean
    
    /**
     * Request contacts permission from the user
     * This should be called from an Activity context
     */
    suspend fun requestContactsPermission(): Boolean
    
    /**
     * Import all contacts from the device
     * Requires READ_CONTACTS permission
     * 
     * @return Flow of ContactImportResult for progress tracking
     */
    fun importContacts(): Flow<ContactImportResult>
    
    /**
     * Import contacts with progress updates
     * Provides real-time progress information during import
     * 
     * @return Flow that emits progress updates and final result
     */
    fun importContactsWithProgress(): Flow<ContactImportProgress>
    
    /**
     * Get cached contacts if available
     * Returns previously imported contacts without re-importing
     */
    suspend fun getCachedContacts(): List<Contact>
    
    /**
     * Clear cached contacts
     * Forces next import to reload from device
     */
    suspend fun clearCache()
    
    /**
     * Check if contacts have been imported before
     */
    suspend fun hasImportedContacts(): Boolean
}

/**
 * Progress information during contact import
 */
sealed class ContactImportProgress {
    data object Starting : ContactImportProgress()
    data class Loading(val processed: Int, val total: Int) : ContactImportProgress()
    data class Completed(val result: ContactImportResult) : ContactImportProgress()
}