package com.locationsharing.app.domain.repository

import com.locationsharing.app.data.models.AuthResult
import com.locationsharing.app.data.models.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AuthRepositoryTest {
    
    private lateinit var authRepository: AuthRepository
    
    @BeforeEach
    fun setUp() {
        authRepository = mockk()
    }
    
    @Test
    fun `signIn should return AuthResult on success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val expectedUser = User(
            id = "user123",
            email = email,
            displayName = "Test User",
            profileImageUrl = null,
            createdAt = 1640995200000L,
            lastActiveAt = 1640995200000L
        )
        val expectedAuthResult = AuthResult(
            user = expectedUser,
            token = "access_token",
            refreshToken = "refresh_token",
            expiresAt = 1640995200000L
        )
        
        coEvery { authRepository.signIn(email, password) } returns Result.success(expectedAuthResult)
        
        // When
        val result = authRepository.signIn(email, password)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAuthResult, result.getOrNull())
        coVerify { authRepository.signIn(email, password) }
    }
    
    @Test
    fun `signUp should return AuthResult on success`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val displayName = "Test User"
        val expectedUser = User(
            id = "user123",
            email = email,
            displayName = displayName,
            profileImageUrl = null,
            createdAt = 1640995200000L,
            lastActiveAt = 1640995200000L
        )
        val expectedAuthResult = AuthResult(
            user = expectedUser,
            token = "access_token",
            refreshToken = "refresh_token",
            expiresAt = 1640995200000L
        )
        
        coEvery { authRepository.signUp(email, password, displayName) } returns Result.success(expectedAuthResult)
        
        // When
        val result = authRepository.signUp(email, password, displayName)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAuthResult, result.getOrNull())
        coVerify { authRepository.signUp(email, password, displayName) }
    }
    
    @Test
    fun `signOut should return success on successful sign out`() = runTest {
        // Given
        coEvery { authRepository.signOut() } returns Result.success(Unit)
        
        // When
        val result = authRepository.signOut()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { authRepository.signOut() }
    }
    
    @Test
    fun `getCurrentUser should return user when authenticated`() = runTest {
        // Given
        val expectedUser = User(
            id = "user123",
            email = "test@example.com",
            displayName = "Test User",
            profileImageUrl = null,
            createdAt = 1640995200000L,
            lastActiveAt = 1640995200000L
        )
        
        coEvery { authRepository.getCurrentUser() } returns expectedUser
        
        // When
        val result = authRepository.getCurrentUser()
        
        // Then
        assertEquals(expectedUser, result)
        coVerify { authRepository.getCurrentUser() }
    }
    
    @Test
    fun `getCurrentUser should return null when not authenticated`() = runTest {
        // Given
        coEvery { authRepository.getCurrentUser() } returns null
        
        // When
        val result = authRepository.getCurrentUser()
        
        // Then
        assertNull(result)
        coVerify { authRepository.getCurrentUser() }
    }
    
    @Test
    fun `refreshToken should return new token on success`() = runTest {
        // Given
        val expectedToken = "new_access_token"
        coEvery { authRepository.refreshToken() } returns Result.success(expectedToken)
        
        // When
        val result = authRepository.refreshToken()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedToken, result.getOrNull())
        coVerify { authRepository.refreshToken() }
    }
    
    @Test
    fun `isUserAuthenticated should return true when user is authenticated`() = runTest {
        // Given
        coEvery { authRepository.isUserAuthenticated() } returns true
        
        // When
        val result = authRepository.isUserAuthenticated()
        
        // Then
        assertTrue(result)
        coVerify { authRepository.isUserAuthenticated() }
    }
    
    @Test
    fun `isUserAuthenticated should return false when user is not authenticated`() = runTest {
        // Given
        coEvery { authRepository.isUserAuthenticated() } returns false
        
        // When
        val result = authRepository.isUserAuthenticated()
        
        // Then
        assertFalse(result)
        coVerify { authRepository.isUserAuthenticated() }
    }
    
    @Test
    fun `resetPassword should return success on successful reset`() = runTest {
        // Given
        val email = "test@example.com"
        coEvery { authRepository.resetPassword(email) } returns Result.success(Unit)
        
        // When
        val result = authRepository.resetPassword(email)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { authRepository.resetPassword(email) }
    }
}