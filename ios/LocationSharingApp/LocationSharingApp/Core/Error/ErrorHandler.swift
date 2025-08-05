import Foundation

/**
 * Centralized error handling system for the application.
 * Provides retry mechanisms, error categorization, and user-friendly error messages.
 */
class ErrorHandler {
    
    /**
     * Executes an operation with retry logic and exponential backoff.
     * 
     * - Parameters:
     *   - maxRetries: Maximum number of retry attempts
     *   - initialDelay: Initial delay in seconds
     *   - maxDelay: Maximum delay in seconds
     *   - backoffMultiplier: Multiplier for exponential backoff
     *   - retryCondition: Condition to determine if retry should be attempted
     *   - operation: The operation to execute
     * - Returns: Result of the operation
     */
    static func executeWithRetry<T>(
        maxRetries: Int = 3,
        initialDelay: TimeInterval = 1.0,
        maxDelay: TimeInterval = 30.0,
        backoffMultiplier: Double = 2.0,
        retryCondition: @escaping (Error) -> Bool = isRetryableError,
        operation: @escaping () async throws -> T
    ) async -> Result<T, Error> {
        var lastError: Error?
        
        for attempt in 0...maxRetries {
            do {
                let result = try await operation()
                return .success(result)
            } catch {
                lastError = error
                
                // Don't retry on the last attempt or if error is not retryable
                if attempt == maxRetries || !retryCondition(error) {
                    break
                }
                
                // Calculate delay with exponential backoff and jitter
                let baseDelay = initialDelay * pow(backoffMultiplier, Double(attempt))
                let jitter = Double.random(in: 0...(baseDelay * 0.25)) // Add up to 25% jitter
                let delaySeconds = min(baseDelay + jitter, maxDelay)
                
                try? await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
            }
        }
        
        return .failure(lastError ?? NSError(domain: "UnknownError", code: -1))
    }
    
    /**
     * Determines if an error is retryable based on its type and characteristics.
     */
    private static func isRetryableError(_ error: Error) -> Bool {
        switch error {
        // Network-related errors are generally retryable
        case let urlError as URLError:
            switch urlError.code {
            case .timedOut, .cannotConnectToHost, .networkConnectionLost, .notConnectedToInternet:
                return true
            default:
                return false
            }
            
        // Authentication errors that might be temporary
        case AuthError.networkError, AuthError.tokenExpired:
            return true
            
        // Location errors that might be temporary
        case LocationError.timeout, LocationError.networkNotAvailable:
            return true
            
        // Don't retry permission errors, validation errors, etc.
        default:
            return false
        }
    }
    
    /**
     * Categorizes errors into different types for appropriate handling.
     */
    static func categorizeError(_ error: Error) -> ErrorCategory {
        switch error {
        // Authentication errors
        case AuthError.invalidCredentials, AuthError.userNotFound, AuthError.userDisabled,
             AuthError.invalidEmail, AuthError.weakPassword:
            return .authentication
            
        // Permission errors
        case LocationError.permissionDenied, LocationError.backgroundPermissionDenied:
            return .permission
            
        // Network errors
        case AuthError.networkError, LocationError.networkNotAvailable:
            return .network
        case let urlError as URLError:
            return .network
            
        // Configuration errors
        case LocationError.locationDisabled, LocationError.settingsResolutionRequired:
            return .configuration
            
        // Rate limiting
        case AuthError.tooManyRequests:
            return .rateLimit
            
        // Default to unknown
        default:
            return .unknown
        }
    }
    
    /**
     * Gets a user-friendly error message for display in the UI.
     */
    static func getUserFriendlyMessage(for error: Error) -> String {
        switch error {
        // Authentication errors
        case AuthError.invalidCredentials:
            return "Please check your email and password and try again."
        case AuthError.userNotFound:
            return "No account found with this email address."
        case AuthError.emailAlreadyInUse:
            return "An account with this email already exists."
        case AuthError.weakPassword:
            return "Please choose a stronger password with at least 8 characters."
        case AuthError.tooManyRequests:
            return "Too many attempts. Please wait a moment before trying again."
            
        // Location errors
        case LocationError.permissionDenied:
            return "Location permission is required. Please enable it in Settings."
        case LocationError.locationDisabled:
            return "Location services are disabled. Please enable them in Settings."
        case LocationError.backgroundPermissionDenied:
            return "Background location permission is needed for continuous sharing."
        case LocationError.timeout:
            return "Unable to get your location. Please try again."
            
        // Network errors
        case let urlError as URLError:
            switch urlError.code {
            case .notConnectedToInternet:
                return "Please check your internet connection and try again."
            case .timedOut:
                return "Connection timed out. Please try again."
            default:
                return "Network error. Please check your connection."
            }
        case AuthError.networkError:
            return "Network error. Please check your connection."
            
        // Default message
        default:
            return "Something went wrong. Please try again."
        }
    }
    
    /**
     * Gets recovery suggestions for the user based on the error type.
     */
    static func getRecoverySuggestion(for error: Error) -> String? {
        switch error {
        case LocationError.permissionDenied:
            return "Go to Settings > Privacy & Security > Location Services and enable location for this app"
        case LocationError.locationDisabled:
            return "Go to Settings > Privacy & Security > Location Services and turn it on"
        case LocationError.backgroundPermissionDenied:
            return "Go to Settings > Privacy & Security > Location Services > Location Sharing and select 'Always'"
        case AuthError.networkError:
            return "Check your WiFi or cellular data connection"
        case let urlError as URLError where urlError.code == .notConnectedToInternet:
            return "Check your WiFi or cellular data connection"
        default:
            return nil
        }
    }
    
    /**
     * Determines if an error should be logged for debugging purposes.
     */
    static func shouldLogError(_ error: Error) -> Bool {
        switch categorizeError(error) {
        case .authentication, .validation:
            return false // Don't log user input errors
        default:
            return true
        }
    }
}

/**
 * Categories of errors for appropriate handling strategies.
 */
enum ErrorCategory {
    case authentication
    case permission
    case network
    case validation
    case configuration
    case rateLimit
    case unknown
}

/**
 * Error handling strategy configuration for different modules.
 */
struct ErrorHandlingStrategy {
    let maxRetries: Int
    let initialDelay: TimeInterval
    let maxDelay: TimeInterval
    let backoffMultiplier: Double
    let showUserMessage: Bool
    let logError: Bool
    let retryCondition: (Error) -> Bool
    
    init(
        maxRetries: Int = 3,
        initialDelay: TimeInterval = 1.0,
        maxDelay: TimeInterval = 30.0,
        backoffMultiplier: Double = 2.0,
        showUserMessage: Bool = true,
        logError: Bool = true,
        retryCondition: @escaping (Error) -> Bool = ErrorHandler.isRetryableError
    ) {
        self.maxRetries = maxRetries
        self.initialDelay = initialDelay
        self.maxDelay = maxDelay
        self.backoffMultiplier = backoffMultiplier
        self.showUserMessage = showUserMessage
        self.logError = logError
        self.retryCondition = retryCondition
    }
    
    static let authentication = ErrorHandlingStrategy(
        maxRetries: 2,
        initialDelay: 2.0,
        showUserMessage: true,
        logError: false
    )
    
    static let locationTracking = ErrorHandlingStrategy(
        maxRetries: 5,
        initialDelay: 1.0,
        maxDelay: 10.0,
        showUserMessage: false,
        logError: true
    )
    
    static let networkRequests = ErrorHandlingStrategy(
        maxRetries: 3,
        initialDelay: 1.0,
        maxDelay: 30.0,
        showUserMessage: true,
        logError: true
    )
    
    static let friendOperations = ErrorHandlingStrategy(
        maxRetries: 2,
        initialDelay: 1.5,
        showUserMessage: true,
        logError: true
    )
    
    static let mapOperations = ErrorHandlingStrategy(
        maxRetries: 3,
        initialDelay: 0.5,
        maxDelay: 5.0,
        showUserMessage: false,
        logError: true
    )
}

// MARK: - Private Helper Extension

private extension ErrorHandler {
    static func isRetryableError(_ error: Error) -> Bool {
        switch error {
        case let urlError as URLError:
            switch urlError.code {
            case .timedOut, .cannotConnectToHost, .networkConnectionLost, .notConnectedToInternet:
                return true
            default:
                return false
            }
        case AuthError.networkError, AuthError.tokenExpired:
            return true
        case LocationError.timeout, LocationError.networkNotAvailable:
            return true
        default:
            return false
        }
    }
}