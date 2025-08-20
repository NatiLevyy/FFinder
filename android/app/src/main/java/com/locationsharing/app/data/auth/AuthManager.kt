package com.locationsharing.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase authentication with anonymous sign-in as fallback
 */
@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    
    /**
     * Ensures user is signed in with anonymous authentication if no current user exists.
     * This method should be called on app startup and before any Firestore operations.
     * 
     * @return FirebaseUser instance (either existing or newly created anonymous user)
     * @throws Exception if authentication fails
     */
    suspend fun ensureSignedIn(): FirebaseUser = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            Timber.d("User already authenticated: ${currentUser.uid} (anonymous: ${currentUser.isAnonymous})")
            return@withContext currentUser
        }
        
        try {
            Timber.d("No authenticated user found, signing in anonymously")
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Anonymous sign-in returned null user")
            
            Timber.d("Anonymous sign-in successful: ${user.uid}")
            return@withContext user
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign in anonymously")
            throw Exception("Authentication failed: ${e.message}", e)
        }
    }
    
    /**
     * Gets the current authenticated user, or null if not signed in
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Checks if the current user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Checks if the current user is anonymous (not linked to phone/email)
     */
    fun isAnonymous(): Boolean {
        return auth.currentUser?.isAnonymous ?: false
    }
    
    /**
     * Signs out the current user
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            Timber.d("User signed out successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error signing out user")
            throw e
        }
    }
}