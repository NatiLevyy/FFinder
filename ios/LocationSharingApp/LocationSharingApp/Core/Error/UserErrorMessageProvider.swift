import Foundation

/**
 * Provides user-friendly error messages with localization support.
 */
class UserErrorMessageProvider {
    
    /**
     * Gets a localized, user-friendly error message for the given error.
     */
    static func getErrorMessage(for error: Error) -> String {
        switch error {
        // Authentication errors
        case AuthError.invalidCredentials:
            return NSLocalizedString("error.invalid_credentials", comment: "Invalid credentials error")
        case AuthError.userNotFound:
            return NSLocalizedString("error.user_not_found", comment: "User not found error")
        case AuthError.emailAlreadyInUse:
            return NSLocalizedString("error.email_already_in_use", comment: "Email already in use error")
        case AuthError.weakPassword:
            return NSLocalizedString("error.weak_password", comment: "Weak password error")
        case AuthError.tooManyRequests:
            return NSLocalizedString("error.too_many_requests", comment: "Too many requests error")
        case AuthError.networkError:
            return NSLocalizedString("error.network_auth", comment: "Network authentication error")
            
        // Location errors
        case LocationError.permissionDenied:
            return NSLocalizedString("error.location_permission_denied", comment: "Location permission denied error")
        case LocationError.locationDisabled:
            return NSLocalizedString("error.location_disabled", comment: "Location disabled error")
        case LocationError.backgroundPermissionDenied:
            return NSLocalizedString("error.background_permission_denied", comment: "Background permission denied error")
        case LocationError.timeout:
            return NSLocalizedString("error.location_timeout", comment: "Location timeout error")
        case LocationError.inaccurateLocation:
            return NSLocalizedString("error.location_inaccurate", comment: "Inaccurate location error")
            
        // Network errors
        case NetworkError.noConnection:
            return NSLocalizedString("error.no_connection", comment: "No connection error")
        case NetworkError.timeout:
            return NSLocalizedString("error.network_timeout", comment: "Network timeout error")
        case NetworkError.unauthorized:
            return NSLocalizedString("error.unauthorized", comment: "Unauthorized error")
        case NetworkError.forbidden:
            return NSLocalizedString("error.forbidden", comment: "Forbidden error")
        case NetworkError.notFound:
            return NSLocalizedString("error.not_found", comment: "Not found error")
        case NetworkError.serviceUnavailable:
            return NSLocalizedString("error.service_unavailable", comment: "Service unavailable error")
            
        // URL errors
        case let urlError as URLError:
            switch urlError.code {
            case .notConnectedToInternet:
                return NSLocalizedString("error.no_internet", comment: "No internet connection error")
            case .timedOut:
                return NSLocalizedString("error.request_timeout", comment: "Request timeout error")
            case .cannotConnectToHost:
                return NSLocalizedString("error.cannot_connect", comment: "Cannot connect to host error")
            default:
                return NSLocalizedString("error.network_generic", comment: "Generic network error")
            }
            
        // Default fallback
        default:
            return NSLocalizedString("error.generic", comment: "Generic error message")
        }
    }
    
    /**
     * Gets a localized recovery suggestion for the given error.
     */
    static func getRecoverySuggestion(for error: Error) -> String? {
        switch error {
        case LocationError.permissionDenied:
            return NSLocalizedString("recovery.location_permission", comment: "Location permission recovery")
        case LocationError.locationDisabled:
            return NSLocalizedString("recovery.location_disabled", comment: "Location disabled recovery")
        case LocationError.backgroundPermissionDenied:
            return NSLocalizedString("recovery.background_permission", comment: "Background permission recovery")
        case NetworkError.noConnection:
            return NSLocalizedString("recovery.no_connection", comment: "No connection recovery")
        case AuthError.networkError:
            return NSLocalizedString("recovery.network_auth", comment: "Network auth recovery")
        case let urlError as URLError where urlError.code == .notConnectedToInternet:
            return NSLocalizedString("recovery.no_internet", comment: "No internet recovery")
        default:
            return nil
        }
    }
    
    /**
     * Gets a localized action button text for the given error.
     */
    static func getActionButtonText(for error: Error) -> String? {
        switch error {
        case LocationError.permissionDenied,
             LocationError.locationDisabled,
             LocationError.backgroundPermissionDenied:
            return NSLocalizedString("action.open_settings", comment: "Open settings action")
            
        case NetworkError.noConnection,
             NetworkError.timeout,
             AuthError.networkError:
            return NSLocalizedString("action.retry", comment: "Retry action")
            
        case let urlError as URLError where urlError.code == .notConnectedToInternet:
            return NSLocalizedString("action.retry", comment: "Retry action")
            
        default:
            return nil
        }
    }
    
    /**
     * Gets error severity level for UI presentation.
     */
    static func getErrorSeverity(for error: Error) -> ErrorSeverity {
        switch error {
        // Critical errors that block core functionality
        case LocationError.permissionDenied,
             LocationError.locationDisabled,
             AuthError.userDisabled:
            return .critical
            
        // High priority errors that affect main features
        case AuthError.invalidCredentials,
             AuthError.tokenExpired,
             NetworkError.unauthorized:
            return .high
            
        // Medium priority errors that affect some features
        case NetworkError.noConnection,
             NetworkError.timeout,
             LocationError.timeout:
            return .medium
            
        // Low priority errors that are recoverable
        case LocationError.inaccurateLocation:
            return .low
            
        // URL errors - categorize based on type
        case let urlError as URLError:
            switch urlError.code {
            case .notConnectedToInternet:
                return .medium
            case .timedOut:
                return .medium
            case .cannotConnectToHost:
                return .high
            default:
                return .medium
            }
            
        // Default to medium
        default:
            return .medium
        }
    }
    
    /**
     * Gets the appropriate alert style for the error severity.
     */
    static func getAlertStyle(for severity: ErrorSeverity) -> AlertStyle {
        switch severity {
        case .low:
            return .toast
        case .medium:
            return .banner
        case .high:
            return .alert
        case .critical:
            return .blockingAlert
        }
    }
    
    /**
     * Formats error information for logging purposes.
     */
    static func formatErrorForLogging(_ error: Error, context: String? = nil) -> String {
        var logMessage = "Error: \(type(of: error))"
        
        if let context = context {
            logMessage += " in \(context)"
        }
        
        logMessage += " - \(error.localizedDescription)"
        
        // Add additional details for specific error types
        switch error {
        case let networkError as NetworkError:
            logMessage += " (Status: \(networkError.statusCode))"
        case let urlError as URLError:
            logMessage += " (Code: \(urlError.code.rawValue))"
        default:
            break
        }
        
        return logMessage
    }
}

/**
 * Error severity levels for UI presentation.
 */
enum ErrorSeverity {
    case low      // Info/warning level - can be shown as toast or banner
    case medium   // Important - should be shown in dialog or prominent UI
    case high     // Urgent - requires user attention and action
    case critical // Blocking - prevents app functionality, needs immediate resolution
}

/**
 * Alert presentation styles based on error severity.
 */
enum AlertStyle {
    case toast         // Brief notification
    case banner        // Persistent banner at top/bottom
    case alert         // Modal alert dialog
    case blockingAlert // Full-screen blocking alert
}