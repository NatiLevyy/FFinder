package com.locationsharing.app.data.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Handles phone number verification and linking for Firebase Auth
 */
@Singleton
class PhoneLinker @Inject constructor(
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val TIMEOUT_SECONDS = 60L
    }
    
    /**
     * Callback interface for phone verification process
     */
    interface PhoneVerificationCallbacks {
        fun onVerificationCompleted(credential: PhoneAuthCredential)
        fun onVerificationFailed(exception: FirebaseException)
        fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken)
        fun onCodeAutoRetrievalTimeOut(verificationId: String)
    }
    
    /**
     * Start phone number verification process
     * 
     * @param phoneE164 Phone number in E.164 format (e.g., +1234567890)
     * @param activity Current activity for SMS retrieval
     * @param callbacks Callbacks for verification events
     */
    fun startPhoneVerification(
        phoneE164: String,
        activity: Activity,
        callbacks: PhoneVerificationCallbacks
    ) {
        if (!phoneE164.startsWith("+")) {
            callbacks.onVerificationFailed(
                FirebaseException("Phone number must be in E.164 format (start with +)")
            )
            return
        }
        
        Timber.d("Starting phone verification for: $phoneE164")
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Timber.d("Phone verification completed automatically")
                    callbacks.onVerificationCompleted(credential)
                }
                
                override fun onVerificationFailed(e: FirebaseException) {
                    Timber.e(e, "Phone verification failed")
                    callbacks.onVerificationFailed(e)
                }
                
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Timber.d("Verification code sent: $verificationId")
                    callbacks.onCodeSent(verificationId, token)
                }
                
                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    Timber.d("Code auto-retrieval timeout: $verificationId")
                    callbacks.onCodeAutoRetrievalTimeOut(verificationId)
                }
            })
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    /**
     * Submit verification code and link phone number to current user
     * 
     * @param verificationId Verification ID received from onCodeSent callback
     * @param code SMS verification code entered by user
     * @return Result with linked FirebaseUser or error
     */
    suspend fun submitCode(verificationId: String, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.failure(Exception("No authenticated user to link phone number"))
            
            Timber.d("Submitting verification code for user: ${currentUser.uid}")
            
            // Create phone auth credential
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            
            // Link credential to current user (preserving uid)
            val result = suspendCancellableCoroutine { continuation ->
                currentUser.linkWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Timber.d("Phone number linked successfully")
                            continuation.resume(Result.success(Unit))
                        } else {
                            val exception = task.exception 
                                ?: Exception("Unknown error linking phone number")
                            Timber.e(exception, "Failed to link phone number")
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Error submitting verification code")
            Result.failure(e)
        }
    }
    
    /**
     * Create phone auth credential directly (for auto-retrieved codes)
     * 
     * @param credential Phone auth credential from auto-verification
     * @return Result with linked FirebaseUser or error
     */
    suspend fun linkCredential(credential: PhoneAuthCredential): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser
                ?: return@withContext Result.failure(Exception("No authenticated user to link phone number"))
            
            Timber.d("Linking phone credential for user: ${currentUser.uid}")
            
            val result = suspendCancellableCoroutine { continuation ->
                currentUser.linkWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Timber.d("Phone credential linked successfully")
                            continuation.resume(Result.success(Unit))
                        } else {
                            val exception = task.exception 
                                ?: Exception("Unknown error linking phone credential")
                            Timber.e(exception, "Failed to link phone credential")
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Error linking phone credential")
            Result.failure(e)
        }
    }
    
    /**
     * Get the phone number of the current user (if linked)
     */
    fun getCurrentUserPhoneNumber(): String? {
        return auth.currentUser?.phoneNumber
    }
    
    /**
     * Check if current user has phone number linked
     */
    fun isPhoneLinked(): Boolean {
        return getCurrentUserPhoneNumber() != null
    }
}