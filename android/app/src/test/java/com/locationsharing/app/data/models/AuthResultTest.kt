package com.locationsharing.app.data.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AuthResultTest {
    
    @Test
    fun `AuthResult should be created with all required fields`() {
        // Given
        val user = User(
            id = "user123",
            email = "test@example.com",
            displayName = "Test User",
            profileImageUrl = null,
            createdAt = 1640995200000L,
            lastActiveAt = 1640995200000L
        )
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val refreshToken = "refresh_token_123"
        val expiresAt = 1640995200000L
        
        // When
        val authResult = AuthResult(
            user = user,
            token = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
        
        // Then
        assertEquals(user, authResult.user)
        assertEquals(token, authResult.token)
        assertEquals(refreshToken, authResult.refreshToken)
        assertEquals(expiresAt, authResult.expiresAt)
    }
    
    @Test
    fun `AuthResult should support equality comparison`() {
        // Given
        val user = User(
            id = "user123",
            email = "test@example.com",
            displayName = "Test User",
            profileImageUrl = null,
            createdAt = 1640995200000L,
            lastActiveAt = 1640995200000L
        )
        
        val authResult1 = AuthResult(
            user = user,
            token = "token123",
            refreshToken = "refresh123",
            expiresAt = 1640995200000L
        )
        
        val authResult2 = AuthResult(
            user = user,
            token = "token123",
            refreshToken = "refresh123",
            expiresAt = 1640995200000L
        )
        
        // Then
        assertEquals(authResult1, authResult2)
        assertEquals(authResult1.hashCode(), authResult2.hashCode())
    }
    
    @Test
    fun `AuthResult should support copy with modifications`() {
        // Given
        val user = User(
            id = "user123",
            email = "test@example.com",
            displayName = "Test User",
            profileImageUrl = null,
            createdAt = 1640995200000L,
            lastActiveAt = 1640995200000L
        )
        
        val originalAuthResult = AuthResult(
            user = user,
            token = "old_token",
            refreshToken = "old_refresh",
            expiresAt = 1640995200000L
        )
        
        // When
        val updatedAuthResult = originalAuthResult.copy(
            token = "new_token",
            refreshToken = "new_refresh"
        )
        
        // Then
        assertEquals("new_token", updatedAuthResult.token)
        assertEquals("new_refresh", updatedAuthResult.refreshToken)
        assertEquals(originalAuthResult.user, updatedAuthResult.user)
        assertEquals(originalAuthResult.expiresAt, updatedAuthResult.expiresAt)
    }
}