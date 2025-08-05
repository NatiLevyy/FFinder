package com.locationsharing.app.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PhoneNumberUtils
 * Tests phone number normalization, validation, and formatting
 */
class PhoneNumberUtilsTest {
    
    @Test
    fun `normalizePhoneNumber handles US numbers correctly`() {
        // Test various US number formats
        assertEquals("+15551234567", PhoneNumberUtils.normalizePhoneNumber("555-123-4567"))
        assertEquals("+15551234567", PhoneNumberUtils.normalizePhoneNumber("(555) 123-4567"))
        assertEquals("+15551234567", PhoneNumberUtils.normalizePhoneNumber("555.123.4567"))
        assertEquals("+15551234567", PhoneNumberUtils.normalizePhoneNumber("5551234567"))
        assertEquals("+15551234567", PhoneNumberUtils.normalizePhoneNumber("15551234567"))
        assertEquals("+15551234567", PhoneNumberUtils.normalizePhoneNumber("+15551234567"))
    }
    
    @Test
    fun `normalizePhoneNumber handles international numbers`() {
        assertEquals("+441234567890", PhoneNumberUtils.normalizePhoneNumber("+44 123 456 7890"))
        assertEquals("+33123456789", PhoneNumberUtils.normalizePhoneNumber("+33-1-23-45-67-89"))
    }
    
    @Test
    fun `normalizePhoneNumber returns null for invalid numbers`() {
        assertNull(PhoneNumberUtils.normalizePhoneNumber(""))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("123"))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("abc"))
        assertNull(PhoneNumberUtils.normalizePhoneNumber("123-abc-4567"))
    }
    
    @Test
    fun `formatForDisplay formats US numbers correctly`() {
        assertEquals("(555) 123-4567", PhoneNumberUtils.formatForDisplay("+15551234567"))
    }
    
    @Test
    fun `isValidPhoneNumber validates correctly`() {
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("555-123-4567"))
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("+15551234567"))
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("123"))
        assertFalse(PhoneNumberUtils.isValidPhoneNumber(""))
        assertFalse(PhoneNumberUtils.isValidPhoneNumber(null))
    }
    
    @Test
    fun `areEquivalent compares numbers correctly`() {
        assertTrue(PhoneNumberUtils.areEquivalent("555-123-4567", "(555) 123-4567"))
        assertTrue(PhoneNumberUtils.areEquivalent("+15551234567", "15551234567"))
        assertFalse(PhoneNumberUtils.areEquivalent("555-123-4567", "555-123-4568"))
        assertFalse(PhoneNumberUtils.areEquivalent("", "555-123-4567"))
    }
    
    @Test
    fun `extractCountryCode works correctly`() {
        assertEquals("1", PhoneNumberUtils.extractCountryCode("+15551234567"))
        assertEquals("44", PhoneNumberUtils.extractCountryCode("+441234567890"))
        assertNull(PhoneNumberUtils.extractCountryCode("5551234567"))
    }
}