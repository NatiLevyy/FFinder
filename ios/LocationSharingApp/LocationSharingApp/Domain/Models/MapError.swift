import Foundation

/**
 * Represents different types of map-related errors that can occur.
 */
enum MapError: Error, LocalizedError {
    
    /// Error when map fails to initialize or load
    case mapInitializationFailed
    
    /// Error when map permissions are not granted
    case mapPermissionDenied
    
    /// Error when trying to perform operations on uninitialized map
    case mapNotInitialized
    
    /// Error when marker operations fail
    case markerOperationFailed(friendId: String, operation: String)
    
    /// Error when location data is invalid for map display
    case invalidLocationData(reason: String)
    
    /// Generic map error with custom message
    case genericMapError(message: String, underlyingError: Error?)
    
    /// Localized error descriptions
    var errorDescription: String? {
        switch self {
        case .mapInitializationFailed:
            return "Failed to initialize map"
        case .mapPermissionDenied:
            return "Map permissions denied"
        case .mapNotInitialized:
            return "Map not initialized"
        case .markerOperationFailed(let friendId, let operation):
            return "Marker operation '\(operation)' failed for friend: \(friendId)"
        case .invalidLocationData(let reason):
            return "Invalid location data: \(reason)"
        case .genericMapError(let message, _):
            return message
        }
    }
    
    /// Failure reasons for debugging
    var failureReason: String? {
        switch self {
        case .mapInitializationFailed:
            return "The map service could not be initialized properly"
        case .mapPermissionDenied:
            return "The user has not granted necessary permissions for map functionality"
        case .mapNotInitialized:
            return "Attempted to use map service before initialization"
        case .markerOperationFailed(let friendId, let operation):
            return "Failed to perform '\(operation)' on marker for friend '\(friendId)'"
        case .invalidLocationData(let reason):
            return "Location data validation failed: \(reason)"
        case .genericMapError(_, let underlyingError):
            return underlyingError?.localizedDescription ?? "Unknown map error occurred"
        }
    }
}