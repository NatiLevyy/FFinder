package com.locationsharing.app.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.locationsharing.app.domain.model.ContactImportResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for ContactImportManagerImpl
 * Tests permission handling, contact import, and caching functionality
 */
@RunWith(AndroidJUnit4::class)
class ContactImportManagerImplTest {
    
    private lateinit var context: Context
    private lateinit var contactImportManager: ContactImportManagerImpl
    
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        contactImportManager = ContactImportManagerImpl(context)
        
        // Mock ContextCompat
        mockkStatic(ContextCompat::class)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }
    
    @Test
    fun `hasContactsPermission returns true when permission granted`() = runTest {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val hasPermission = contactImportManager.hasContactsPermission()
        
        // Then
        assertTrue(hasPermission)
    }
    
    @Test
    fun `hasContactsPermission returns false when permission denied`() = runTest {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val hasPermission = contactImportManager.hasContactsPermission()
        
        // Then
        assertFalse(hasPermission)
    }
    
    @Test
    fun `importContacts returns PermissionDenied when permission not granted`() = runTest {
        // Given
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val result = contactImportManager.importContacts().first()
        
        // Then
        assertEquals(ContactImportResult.PermissionDenied, result)
    }
    
    @Test
    fun `getCachedContacts returns empty list when no cache`() = runTest {
        // When
        val cachedContacts = contactImportManager.getCachedContacts()
        
        // Then
        assertTrue(cachedContacts.isEmpty())
    }
    
    @Test
    fun `hasImportedContacts returns false initially`() = runTest {
        // When
        val hasImported = contactImportManager.hasImportedContacts()
        
        // Then
        assertFalse(hasImported)
    }
    
    @Test
    fun `clearCache clears cached contacts`() = runTest {
        // When
        contactImportManager.clearCache()
        
        // Then
        val cachedContacts = contactImportManager.getCachedContacts()
        assertTrue(cachedContacts.isEmpty())
    }
}