import Foundation

/**
 * Represents different types of authentication errors that can occur.
 */
enum AuthError: Error, LocalizedError {
    /// Invalid email or password provided
    case invalidCredentials
    /// User account not found
    case userNotFound
    /// User account has been disabled
    case userDisabled
    /// Email address is already in use
    case emailAlreadyInUse
    /// Email address format is invalid
    case invalidEmail
    /// Password is too weak
    case weakPassword
    /// Authentication token has expired
    case tokenExpired
    /// Authentication token is invalid
    case invalidToken
    /// Network connection error
    case networkError
    /// Too many authentication attempts
    case tooManyRequests
    /// Unknown authentication error
    case unknown(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidCredentials:
            return "Invalid email or password"
        case .userNotFound:
            return "User account not found"
        case .userDisabled:
            return "User account has been disabled"
        case .emailAlreadyInUse:
            return "Email address is already in use"
        case .invalidEmail:
            return "Invalid email address format"
        case .weakPassword:
            return "Password is too weak"
        case .tokenExpired:
            return "Authentication token has expired"
        case .invalidToken:
            return "Invalid authentication token"
        case .networkError:
            return "Network connection error"
        case .tooManyRequests:
            return "Too many authentication attempts. Please try again later"
        case .unknown(let message):
            return message
        }
    }
}