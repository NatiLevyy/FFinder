package com.locationsharing.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.locationsharing.app.data.auth.PhoneLinker
import com.locationsharing.app.data.discovery.UserDiscoveryService
import com.locationsharing.app.data.user.UserProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for phone verification flow
 */
@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    private val phoneLinker: PhoneLinker,
    private val userProfileManager: UserProfileManager,
    private val userDiscoveryService: UserDiscoveryService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PhoneVerificationUiState())
    val uiState: StateFlow<PhoneVerificationUiState> = _uiState.asStateFlow()
    
    private var currentVerificationId: String? = null
    
    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber)
    }
    
    fun updateVerificationCode(code: String) {
        _uiState.value = _uiState.value.copy(verificationCode = code)
    }
    
    fun startVerification(activity: Activity) {
        val phoneNumber = _uiState.value.phoneNumber
        
        if (phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your phone number")
            return
        }
        
        if (!phoneNumber.startsWith("+")) {
            _uiState.value = _uiState.value.copy(error = "Please include country code (e.g., +1)")
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
        
        Timber.d("Starting phone verification for: $phoneNumber")
        
        phoneLinker.startPhoneVerification(
            phoneE164 = phoneNumber,
            activity = activity,
            callbacks = object : PhoneLinker.PhoneVerificationCallbacks {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Timber.d("Verification completed automatically")
                    handleAutoVerification(credential)
                }
                
                override fun onVerificationFailed(exception: FirebaseException) {
                    Timber.e(exception, "Verification failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Verification failed: ${exception.message}"
                    )
                }
                
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Timber.d("Code sent with verification ID: $verificationId")
                    currentVerificationId = verificationId
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationState = PhoneVerificationState.CodeSent(verificationId)
                    )
                }
                
                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    Timber.d("Auto-retrieval timeout for verification ID: $verificationId")
                    // Code sent state is already set, no need to update UI
                }
            }
        )
    }
    
    fun verifyCode() {
        val verificationId = currentVerificationId
        val code = _uiState.value.verificationCode
        
        if (verificationId == null) {
            _uiState.value = _uiState.value.copy(error = "Verification session expired. Please try again.")
            return
        }
        
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter the verification code")
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            verificationState = PhoneVerificationState.Verifying
        )
        
        viewModelScope.launch {
            try {
                val result = phoneLinker.submitCode(verificationId, code)
                
                if (result.isSuccess) {
                    Timber.d("Phone verification successful")
                    createUserProfile()
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "Phone verification failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Invalid verification code. Please try again.",
                        verificationState = PhoneVerificationState.CodeSent(verificationId)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during phone verification")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Verification failed: ${e.message}",
                    verificationState = PhoneVerificationState.CodeSent(verificationId)
                )
            }
        }
    }
    
    fun resendCode(activity: Activity) {
        currentVerificationId = null
        _uiState.value = _uiState.value.copy(
            verificationState = PhoneVerificationState.Initial
        )
        startVerification(activity)
    }
    
    private fun handleAutoVerification(credential: PhoneAuthCredential) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            verificationState = PhoneVerificationState.Verifying
        )
        
        viewModelScope.launch {
            try {
                val result = phoneLinker.linkCredential(credential)
                
                if (result.isSuccess) {
                    Timber.d("Auto-verification successful")
                    createUserProfile()
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "Auto-verification failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Verification failed: ${error?.message}",
                        verificationState = PhoneVerificationState.Initial
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during auto-verification")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Verification failed: ${e.message}",
                    verificationState = PhoneVerificationState.Initial
                )
            }
        }
    }
    
    private suspend fun createUserProfile() {
        try {
            val phoneNumber = _uiState.value.phoneNumber
            val result = userProfileManager.createOrUpdateUserProfile(
                phoneE164 = phoneNumber,
                displayName = null // Can be set later
            )
            
            if (result.isSuccess) {
                Timber.d("User profile created successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    verificationState = PhoneVerificationState.Completed
                )
            } else {
                val error = result.exceptionOrNull()
                Timber.e(error, "Failed to create user profile")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to set up profile: ${error?.message}",
                    verificationState = PhoneVerificationState.Initial
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating user profile")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to set up profile: ${e.message}",
                verificationState = PhoneVerificationState.Initial
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for phone verification
 */
data class PhoneVerificationUiState(
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val verificationState: PhoneVerificationState = PhoneVerificationState.Initial,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * States of phone verification flow
 */
sealed class PhoneVerificationState {
    object Initial : PhoneVerificationState()
    data class CodeSent(val verificationId: String) : PhoneVerificationState()
    object Verifying : PhoneVerificationState()
    object Completed : PhoneVerificationState()
}