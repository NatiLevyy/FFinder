package com.locationsharing.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class JwtTokenManagerTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var jwtTokenManager: JwtTokenManager
    
    @BeforeEach
    fun setUp() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk()
        
        // Mock MasterKey.Builder
        mockkConstructor(MasterKey.Builder::class)
        every { anyConstructed<MasterKey.Builder>().setKeyScheme(any()) } returns mockk()
        every { anyConstructed<MasterKey.Builder>().build() } returns mockk()
        
        // Mock EncryptedSharedPreferences creation
        mockkStatic(EncryptedSharedPreferences::class)
        every { 
            EncryptedSharedPreferences.create(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns sharedPreferences
        
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } just Runs
        
        jwtTokenManager = JwtTokenManager(context)
    }
    
    @Test
    fun `storeTokens should store all tokens in encrypted preferences`() = runTest {
        // Given
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"
        val expiresAt = 1640995200000L
        
        // When
        jwtTokenManager.storeTokens(accessToken, refreshToken, expiresAt)
        
        // Then
        verify { editor.putString("access_token", accessToken) }
        verify { editor.putString("refresh_token", refreshToken) }
        verify { editor.putLong("token_expires_at", expiresAt) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getAccessToken should return stored token`() = runTest {
        // Given
        val expectedToken = "access_token_123"
        every { sharedPreferences.getString("access_token", null) } returns expectedToken
        
        // When
        val result = jwtTokenManager.getAccessToken()
        
        // Then
        assertEquals(expectedToken, result)
    }
    
    @Test
    fun `getAccessToken should return null when no token stored`() = runTest {
        // Given
        every { sharedPreferences.getString("access_token", null) } returns null
        
        // When
        val result = jwtTokenManager.getAccessToken()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getRefreshToken should return stored token`() = runTest {
        // Given
        val expectedToken = "refresh_token_123"
        every { sharedPreferences.getString("refresh_token", null) } returns expectedToken
        
        // When
        val result = jwtTokenManager.getRefreshToken()
        
        // Then
        assertEquals(expectedToken, result)
    }
    
    @Test
    fun `getTokenExpiresAt should return stored timestamp`() = runTest {
        // Given
        val expectedTimestamp = 1640995200000L
        every { sharedPreferences.getLong("token_expires_at", 0L) } returns expectedTimestamp
        
        // When
        val result = jwtTokenManager.getTokenExpiresAt()
        
        // Then
        assertEquals(expectedTimestamp, result)
    }
    
    @Test
    fun `isTokenValid should return true for valid non-expired token`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + (10 * 60 * 1000) // 10 minutes in future
        
        every { sharedPreferences.getString("access_token", null) } returns "valid_token"
        every { sharedPreferences.getLong("token_expires_at", 0L) } returns futureTime
        
        // When
        val result = jwtTokenManager.isTokenValid()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isTokenValid should return false for expired token`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val pastTime = currentTime - (10 * 60 * 1000) // 10 minutes in past
        
        every { sharedPreferences.getString("access_token", null) } returns "expired_token"
        every { sharedPreferences.getLong("token_expires_at", 0L) } returns pastTime
        
        // When
        val result = jwtTokenManager.isTokenValid()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isTokenValid should return false when no token exists`() = runTest {
        // Given
        every { sharedPreferences.getString("access_token", null) } returns null
        
        // When
        val result = jwtTokenManager.isTokenValid()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `shouldRefreshToken should return true when token expires soon`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val soonToExpire = currentTime + (2 * 60 * 1000) // 2 minutes in future (less than 5 min buffer)
        
        every { sharedPreferences.getLong("token_expires_at", 0L) } returns soonToExpire
        
        // When
        val result = jwtTokenManager.shouldRefreshToken()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `shouldRefreshToken should return false when token has plenty of time`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + (10 * 60 * 1000) // 10 minutes in future
        
        every { sharedPreferences.getLong("token_expires_at", 0L) } returns futureTime
        
        // When
        val result = jwtTokenManager.shouldRefreshToken()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `extractUserIdFromToken should return user ID from valid JWT`() {
        // Given
        // This is a mock JWT with payload: {"sub":"user123","exp":1640995200}
        val mockJwt = "header.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjQwOTk1MjAwfQ.signature"
        
        // When
        val result = jwtTokenManager.extractUserIdFromToken(mockJwt)
        
        // Then
        assertEquals("user123", result)
    }
    
    @Test
    fun `extractUserIdFromToken should return null for invalid JWT`() {
        // Given
        val invalidJwt = "invalid.jwt"
        
        // When
        val result = jwtTokenManager.extractUserIdFromToken(invalidJwt)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `isTokenExpired should return true for expired token`() {
        // Given
        // JWT with exp claim set to past timestamp
        val expiredJwt = "header.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjQwOTk1MjAwfQ.signature"
        
        // When
        val result = jwtTokenManager.isTokenExpired(expiredJwt)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isTokenExpired should return true for invalid token`() {
        // Given
        val invalidJwt = "invalid.token"
        
        // When
        val result = jwtTokenManager.isTokenExpired(invalidJwt)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `clearTokens should remove all stored tokens`() = runTest {
        // When
        jwtTokenManager.clearTokens()
        
        // Then
        verify { editor.remove("access_token") }
        verify { editor.remove("refresh_token") }
        verify { editor.remove("token_expires_at") }
        verify { editor.apply() }
    }
    
    @Test
    fun `hasStoredTokens should return true when both tokens exist`() = runTest {
        // Given
        every { sharedPreferences.contains("access_token") } returns true
        every { sharedPreferences.contains("refresh_token") } returns true
        
        // When
        val result = jwtTokenManager.hasStoredTokens()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `hasStoredTokens should return false when access token missing`() = runTest {
        // Given
        every { sharedPreferences.contains("access_token") } returns false
        every { sharedPreferences.contains("refresh_token") } returns true
        
        // When
        val result = jwtTokenManager.hasStoredTokens()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `hasStoredTokens should return false when refresh token missing`() = runTest {
        // Given
        every { sharedPreferences.contains("access_token") } returns true
        every { sharedPreferences.contains("refresh_token") } returns false
        
        // When
        val result = jwtTokenManager.hasStoredTokens()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `extractUserIdFromToken should handle malformed JWT gracefully`() {
        // Given
        val malformedJwts = listOf(
            "not.a.jwt",
            "only.two.parts",
            "header.invalid_base64.signature",
            "",
            "single_part"
        )
        
        malformedJwts.forEach { jwt ->
            // When
            val result = jwtTokenManager.extractUserIdFromToken(jwt)
            
            // Then
            assertNull(result, "Should return null for malformed JWT: $jwt")
        }
    }
    
    @Test
    fun `isTokenExpired should handle edge cases`() {
        // Given
        val edgeCases = listOf(
            "invalid.token",
            "header.invalid_base64.signature",
            "header.eyJzdWIiOiJ1c2VyMTIzIn0.signature", // No exp claim
            ""
        )
        
        edgeCases.forEach { jwt ->
            // When
            val result = jwtTokenManager.isTokenExpired(jwt)
            
            // Then
            assertTrue(result, "Should return true (expired) for edge case: $jwt")
        }
    }
    
    @Test
    fun `token validation should handle concurrent access`() = runTest {
        // Given
        val accessToken = "concurrent_token"
        val refreshToken = "concurrent_refresh"
        val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes future
        
        every { sharedPreferences.getString("access_token", null) } returns accessToken
        every { sharedPreferences.getString("refresh_token", null) } returns refreshToken
        every { sharedPreferences.getLong("token_expires_at", 0L) } returns expiresAt
        every { sharedPreferences.contains("access_token") } returns true
        every { sharedPreferences.contains("refresh_token") } returns true
        
        // When - simulate concurrent access
        val results = (1..10).map {
            async {
                jwtTokenManager.isTokenValid()
            }
        }.awaitAll()
        
        // Then
        results.forEach { result ->
            assertTrue(result, "All concurrent calls should return true for valid token")
        }
    }
}