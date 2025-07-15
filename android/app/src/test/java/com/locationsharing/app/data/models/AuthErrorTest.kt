package com.locationsharing.app.data.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AuthErrorTest {
    
    @Test
    fun `InvalidCredentials should have correct message`() {
        // Given
        val error = AuthError.InvalidCredentials
        
        // Then
        assertEquals("Invalid email or password", error.message)
        assertTrue(error is AuthError.InvalidCredentials)
    }
    
    @Test
    fun `UserNotFound should have correct message`() {
        // Given
        val error = AuthError.UserNotFound
        
        // Then
        assertEquals("User account not found", error.message)
        assertTrue(error is AuthError.UserNotFound)
    }
    
    @Test
    fun `UserDisabled should have correct message`() {
        // Given
        val error = AuthError.UserDisabled
        
        // Then
        assertEquals("User account has been disabled", error.message)
        assertTrue(error is AuthError.UserDisabled)
    }
    
    @Test
    fun `EmailAlreadyInUse should have correct message`() {
        // Given
        val error = AuthError.EmailAlreadyInUse
        
        // Then
        assertEquals("Email address is already in use", error.message)
        assertTrue(error is AuthError.EmailAlreadyInUse)
    }
    
    @Test
    fun `InvalidEmail should have correct message`() {
        // Given
        val error = AuthError.InvalidEmail
        
        // Then
        assertEquals("Invalid email address format", error.message)
        assertTrue(error is AuthError.InvalidEmail)
    }
    
    @Test
    fun `WeakPassword should have correct message`() {
        // Given
        val error = AuthError.WeakPassword
        
        // Then
        assertEquals("Password is too weak", error.message)
        assertTrue(error is AuthError.WeakPassword)
    }
    
    @Test
    fun `TokenExpired should have correct message`() {
        // Given
        val error = AuthError.TokenExpired
        
        // Then
        assertEquals("Authentication token has expired", error.message)
        assertTrue(error is AuthError.TokenExpired)
    }
    
    @Test
    fun `InvalidToken should have correct message`() {
        // Given
        val error = AuthError.InvalidToken
        
        // Then
        assertEquals("Invalid authentication token", error.message)
        assertTrue(error is AuthError.InvalidToken)
    }
    
    @Test
    fun `NetworkError should have correct message`() {
        // Given
        val error = AuthError.NetworkError
        
        // Then
        assertEquals("Network connection error", error.message)
        assertTrue(error is AuthError.NetworkError)
    }
    
    @Test
    fun `TooManyRequests should have correct message`() {
        // Given
        val error = AuthError.TooManyRequests
        
        // Then
        assertEquals("Too many authentication attempts. Please try again later", error.message)
        assertTrue(error is AuthError.TooManyRequests)
    }
    
    @Test
    fun `Unknown error should have custom message`() {
        // Given
        val customMessage = "Custom error message"
        val customCause = RuntimeException("Root cause")
        val error = AuthError.Unknown(customMessage, customCause)
        
        // Then
        assertEquals(customMessage, error.message)
        assertEquals(customCause, error.cause)
        assertTrue(error is AuthError.Unknown)
    }
    
    @Test
    fun `Unknown error should work without cause`() {
        // Given
        val customMessage = "Custom error message"
        val error = AuthError.Unknown(customMessage)
        
        // Then
        assertEquals(customMessage, error.message)
        assertNull(error.cause)
        assertTrue(error is AuthError.Unknown)
    }
    
    @Test
    fun `AuthError should be Exception`() {
        // Given
        val error = AuthError.InvalidCredentials
        
        // Then
        assertTrue(error is Exception)
        assertTrue(error is Throwable)
    }
}