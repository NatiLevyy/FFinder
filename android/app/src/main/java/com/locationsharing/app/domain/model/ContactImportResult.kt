package com.locationsharing.app.domain.model

/**
 * Sealed class representing the result of contact import operation
 */
sealed class ContactImportResult {
    /**
     * Successful contact import
     */
    data class Success(
        val contacts: List<Contact>,
        val totalCount: Int = contacts.size,
        val validCount: Int = contacts.count { it.isValidForDiscovery }
    ) : ContactImportResult()
    
    /**
     * Contact import failed due to permission denial
     */
    data object PermissionDenied : ContactImportResult()
    
    /**
     * Contact import failed due to system error
     */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error occurred"
    ) : ContactImportResult()
    
    /**
     * Contact import was cancelled by user
     */
    data object Cancelled : ContactImportResult()
}

/**
 * Extension functions for easier result handling
 */
val ContactImportResult.isSuccess: Boolean
    get() = this is ContactImportResult.Success

val ContactImportResult.isError: Boolean
    get() = this is ContactImportResult.Error

val ContactImportResult.isPermissionDenied: Boolean
    get() = this is ContactImportResult.PermissionDenied

val ContactImportResult.contacts: List<Contact>
    get() = when (this) {
        is ContactImportResult.Success -> contacts
        else -> emptyList()
    }