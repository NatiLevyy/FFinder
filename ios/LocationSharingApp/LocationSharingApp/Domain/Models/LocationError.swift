import Foundation

/**
 * Represents various errors that can occur during location operations.
 */
enum LocationError: Error {
    /// Location permissions are not granted
    case permissionDenied
    
    /// Location services are disabled on the device
    case locationDisabled
    
    /// GPS provider is not available
    case gpsNotAvailable
    
    /// Network provider is not available
    case networkNotAvailable
    
    /// Location request timed out
    case timeout
    
    /// Location accuracy is too low for the requested operation
    case inaccurateLocation(accuracy: Double)
    
    /// Location data is too old to be useful
    case outdatedLocation(age: TimeInterval)
    
    /// Background location permission is required but not granted
    case backgroundPermissionDenied
    
    /// Location settings need to be resolved
    case settingsResolutionRequired
    
    /// An unknown error occurred during location operations
    case unknown(Error)
    
    // MARK: - Error Description
    
    var localizedDescription: String {
        switch self {
        case .permissionDenied:
            return "Location permission denied"
        case .locationDisabled:
            return "Location services are disabled"
        case .gpsNotAvailable:
            return "GPS provider is not available"
        case .networkNotAvailable:
            return "Network location provider is not available"
        case .timeout:
            return "Location request timed out"
        case .inaccurateLocation(let accuracy):
            return "Location accuracy too low: \(accuracy)m"
        case .outdatedLocation(let age):
            return "Location data is too old: \(age)s"
        case .backgroundPermissionDenied:
            return "Background location permission denied"
        case .settingsResolutionRequired:
            return "Location settings need to be resolved"
        case .unknown(let error):
            return "Unknown location error: \(error.localizedDescription)"
        }
    }
    
    // MARK: - Error Code
    
    var errorCode: Int {
        switch self {
        case .permissionDenied:
            return 1001
        case .locationDisabled:
            return 1002
        case .gpsNotAvailable:
            return 1003
        case .networkNotAvailable:
            return 1004
        case .timeout:
            return 1005
        case .inaccurateLocation:
            return 1006
        case .outdatedLocation:
            return 1007
        case .backgroundPermissionDenied:
            return 1008
        case .settingsResolutionRequired:
            return 1009
        case .unknown:
            return 1999
        }
    }
    
    // MARK: - Recovery Suggestions
    
    var recoverySuggestion: String {
        switch self {
        case .permissionDenied:
            return "Please grant location permission in Settings"
        case .locationDisabled:
            return "Please enable Location Services in Settings"
        case .gpsNotAvailable:
            return "Please ensure GPS is enabled and try again"
        case .networkNotAvailable:
            return "Please check your internet connection"
        case .timeout:
            return "Please try again or move to an area with better signal"
        case .inaccurateLocation:
            return "Please move to an area with better GPS signal"
        case .outdatedLocation:
            return "Please wait for a fresh location update"
        case .backgroundPermissionDenied:
            return "Please grant 'Always' location permission in Settings"
        case .settingsResolutionRequired:
            return "Please enable high accuracy location mode"
        case .unknown:
            return "Please try again or contact support"
        }
    }
}

// MARK: - Equatable

extension LocationError: Equatable {
    static func == (lhs: LocationError, rhs: LocationError) -> Bool {
        switch (lhs, rhs) {
        case (.permissionDenied, .permissionDenied),
             (.locationDisabled, .locationDisabled),
             (.gpsNotAvailable, .gpsNotAvailable),
             (.networkNotAvailable, .networkNotAvailable),
             (.timeout, .timeout),
             (.backgroundPermissionDenied, .backgroundPermissionDenied),
             (.settingsResolutionRequired, .settingsResolutionRequired):
            return true
        case (.inaccurateLocation(let lhsAccuracy), .inaccurateLocation(let rhsAccuracy)):
            return lhsAccuracy == rhsAccuracy
        case (.outdatedLocation(let lhsAge), .outdatedLocation(let rhsAge)):
            return lhsAge == rhsAge
        case (.unknown(let lhsError), .unknown(let rhsError)):
            return lhsError.localizedDescription == rhsError.localizedDescription
        default:
            return false
        }
    }
}