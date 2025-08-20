package com.locationsharing.app.data.location

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.locationsharing.app.data.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exception thrown when Firestore rules deny permission
 */
class PermissionDeniedException(message: String) : Exception(message)

/**
 * Repository for location sharing operations with proper authentication
 */
@Singleton
class LocationSharingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authManager: AuthManager
) {
    
    companion object {
        private const val LOCATIONS_COLLECTION = "locations"
    }
    
    /**
     * Toggle location sharing on/off with lat/lng and proper error handling
     */
    suspend fun toggleSharing(enable: Boolean, lat: Double?, lng: Double?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Ensure user is authenticated before proceeding
            val user = authManager.ensureSignedIn()
            val userId = user.uid
            
            Timber.d("Toggling location sharing for user $userId: $enable, lat: $lat, lng: $lng")
            
            val locationData = mapOf(
                "sharing" to enable,
                "lat" to lat,
                "lng" to lng,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            firestore.collection(LOCATIONS_COLLECTION)
                .document(userId)
                .set(locationData, SetOptions.merge())
                .await()
            
            Timber.d("Location sharing toggled successfully: $enable")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Timber.e(e, "Firestore error toggling location sharing")
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    val error = PermissionDeniedException("Permission denied by server rules")
                    Result.failure(error)
                }
                else -> Result.failure(e)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle location sharing")
            Result.failure(e)
        }
    }
    
    /**
     * Get current sharing status for authenticated user
     */
    suspend fun getSharingStatus(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val user = authManager.ensureSignedIn()
            val userId = user.uid
            
            val doc = firestore.collection(LOCATIONS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val sharing = doc.getBoolean("sharing") ?: false
            Result.success(sharing)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get sharing status")
            Result.failure(e)
        }
    }
}