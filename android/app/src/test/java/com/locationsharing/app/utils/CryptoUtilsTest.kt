package com.locationsharing.app.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CryptoUtils
 * Tests secure hashing functionality for contact privacy
 */
class CryptoUtilsTest {
    
    @Test
    fun `hashPhoneNumber produces consistent hashes`() {
        val phoneNumber = "+15551234567"
        val hash1 = CryptoUtils.hashPhoneNumber(phoneNumber)
        val hash2 = CryptoUtils.hashPhoneNumber(phoneNumber)
        
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertEquals(hash1, hash2)
    }
    
    @Test
    fun `hashPhoneNumber produces different hashes for different numbers`() {
        val phone1 = "+15551234567"
        val phone2 = "+15551234568"
        
        val hash1 = CryptoUtils.hashPhoneNumber(phone1)
        val hash2 = CryptoUtils.hashPhoneNumber(phone2)
        
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertNotEquals(hash1, hash2)
    }
    
    @Test
    fun `hashPhoneNumber returns null for invalid input`() {
        assertNull(CryptoUtils.hashPhoneNumber(null))
        assertNull(CryptoUtils.hashPhoneNumber(""))
        assertNull(CryptoUtils.hashPhoneNumber("   "))
    }
    
    @Test
    fun `hashPhoneNumber handles formatted numbers`() {
        val formatted = "+1 (555) 123-4567"
        val normalized = "+15551234567"
        
        val hash1 = CryptoUtils.hashPhoneNumber(formatted)
        val hash2 = CryptoUtils.hashPhoneNumber(normalized)
        
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertEquals(hash1, hash2)
    }
    
    @Test
    fun `hashEmail produces consistent hashes`() {
        val email = "test@example.com"
        val hash1 = CryptoUtils.hashEmail(email)
        val hash2 = CryptoUtils.hashEmail(email)
        
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertEquals(hash1, hash2)
    }
    
    @Test
    fun `hashEmail normalizes case`() {
        val lowercase = "test@example.com"
        val uppercase = "TEST@EXAMPLE.COM"
        val mixed = "Test@Example.Com"
        
        val hash1 = CryptoUtils.hashEmail(lowercase)
        val hash2 = CryptoUtils.hashEmail(uppercase)
        val hash3 = CryptoUtils.hashEmail(mixed)
        
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertNotNull(hash3)
        assertEquals(hash1, hash2)
        assertEquals(hash2, hash3)
    }
    
    @Test
    fun `hashEmail returns null for invalid input`() {
        assertNull(CryptoUtils.hashEmail(null))
        assertNull(CryptoUtils.hashEmail(""))
        assertNull(CryptoUtils.hashEmail("invalid-email"))
        assertNull(CryptoUtils.hashEmail("@example.com"))
        assertNull(CryptoUtils.hashEmail("test@"))
    }
    
    @Test
    fun `hashPhoneNumbers processes multiple numbers`() {
        val phoneNumbers = listOf(
            "+15551234567",
            "+15551234568",
            "invalid-number",
            "+15551234569"
        )
        
        val hashes = CryptoUtils.hashPhoneNumbers(phoneNumbers)
        
        assertEquals(3, hashes.size) // Invalid number should be excluded
        assertTrue(hashes.containsKey("+15551234567"))
        assertTrue(hashes.containsKey("+15551234568"))
        assertTrue(hashes.containsKey("+15551234569"))
        assertFalse(hashes.containsKey("invalid-number"))
    }
    
    @Test
    fun `hashEmails processes multiple emails`() {
        val emails = listOf(
            "test1@example.com",
            "test2@example.com",
            "invalid-email",
            "test3@example.com"
        )
        
        val hashes = CryptoUtils.hashEmails(emails)
        
        assertEquals(3, hashes.size) // Invalid email should be excluded
        assertTrue(hashes.containsKey("test1@example.com"))
        assertTrue(hashes.containsKey("test2@example.com"))
        assertTrue(hashes.containsKey("test3@example.com"))
        assertFalse(hashes.containsKey("invalid-email"))
    }
    
    @Test
    fun `createContactHash works with phone only`() {
        val phoneNumber = "+15551234567"
        val hash = CryptoUtils.createContactHash(phoneNumber, null)
        
        assertNotNull(hash)
        assertEquals(CryptoUtils.hashPhoneNumber(phoneNumber), hash)
    }
    
    @Test
    fun `createContactHash works with email only`() {
        val email = "test@example.com"
        val hash = CryptoUtils.createContactHash(null, email)
        
        assertNotNull(hash)
        assertEquals(CryptoUtils.hashEmail(email), hash)
    }
    
    @Test
    fun `createContactHash creates composite hash for both`() {
        val phoneNumber = "+15551234567"
        val email = "test@example.com"
        val hash = CryptoUtils.createContactHash(phoneNumber, email)
        
        assertNotNull(hash)
        assertNotEquals(CryptoUtils.hashPhoneNumber(phoneNumber), hash)
        assertNotEquals(CryptoUtils.hashEmail(email), hash)
    }
    
    @Test
    fun `createContactHash returns null for invalid inputs`() {
        assertNull(CryptoUtils.createContactHash(null, null))
        assertNull(CryptoUtils.createContactHash("", ""))
        assertNull(CryptoUtils.createContactHash("invalid", "invalid"))
    }
    
    @Test
    fun `generateSecureRandomString produces different values`() {
        val random1 = CryptoUtils.generateSecureRandomString()
        val random2 = CryptoUtils.generateSecureRandomString()
        
        assertNotEquals(random1, random2)
        assertEquals(32, random1.length)
        assertEquals(32, random2.length)
    }
    
    @Test
    fun `generateSecureRandomString respects length parameter`() {
        val random16 = CryptoUtils.generateSecureRandomString(16)
        val random64 = CryptoUtils.generateSecureRandomString(64)
        
        assertEquals(16, random16.length)
        assertEquals(64, random64.length)
    }
    
    @Test
    fun `verifyHashIntegrity works correctly`() {
        val phoneNumber = "+15551234567"
        val email = "test@example.com"
        
        val phoneHash = CryptoUtils.hashPhoneNumber(phoneNumber)!!
        val emailHash = CryptoUtils.hashEmail(email)!!
        
        assertTrue(CryptoUtils.verifyHashIntegrity(phoneNumber, phoneHash))
        assertTrue(CryptoUtils.verifyHashIntegrity(email, emailHash))
        
        assertFalse(CryptoUtils.verifyHashIntegrity(phoneNumber, emailHash))
        assertFalse(CryptoUtils.verifyHashIntegrity(email, phoneHash))
    }
    
    @Test
    fun `hash length is consistent`() {
        val hash1 = CryptoUtils.hashPhoneNumber("+15551234567")
        val hash2 = CryptoUtils.hashEmail("test@example.com")
        val hash3 = CryptoUtils.createContactHash("+15551234567", "test@example.com")
        
        assertNotNull(hash1)
        assertNotNull(hash2)
        assertNotNull(hash3)
        
        // SHA-256 produces 64-character hex strings
        assertEquals(64, hash1!!.length)
        assertEquals(64, hash2!!.length)
        assertEquals(64, hash3!!.length)
    }
}