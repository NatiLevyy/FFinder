package com.locationsharing.app.data.models

/**
 * Represents different types of authentication errors that can occur.
 */
sealed class AuthError : Exception() {
    
    /**
     * Invalid email or password provided
     */
    object InvalidCredentials : AuthError() {
        override val message: String = "Invalid email or password"
    }
    
    /**
     * User account not found
     */
    object UserNotFound : AuthError() {
        override val message: String = "User account not found"
    }
    
    /**
     * User account has been disabled
     */
    object UserDisabled : AuthError() {
        override val message: String = "User account has been disabled"
    }
    
    /**
     * Email address is already in use
     */
    object EmailAlreadyInUse : AuthError() {
        override val message: String = "Email address is already in use"
    }
    
    /**
     * Email address format is invalid
     */
    object InvalidEmail : AuthError() {
        override val message: String = "Invalid email address format"
    }
    
    /**
     * Password is too weak
     */
    object WeakPassword : AuthError() {
        override val message: String = "Password is too weak"
    }
    
    /**
     * Authentication token has expired
     */
    object TokenExpired : AuthError() {
        override val message: String = "Authentication token has expired"
    }
    
    /**
     * Authentication token is invalid
     */
    object InvalidToken : AuthError() {
        override val message: String = "Invalid authentication token"
    }
    
    /**
     * Network connection error
     */
    object NetworkError : AuthError() {
        override val message: String = "Network connection error"
    }
    
    /**
     * Too many authentication attempts
     */
    object TooManyRequests : AuthError() {
        override val message: String = "Too many authentication attempts. Please try again later"
    }
    
    /**
     * Unknown authentication error
     */
    data class Unknown(override val message: String, val cause: Throwable? = null) : AuthError()
}