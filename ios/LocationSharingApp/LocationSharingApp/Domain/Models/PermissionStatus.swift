import Foundation

/**
 * Represents the status of location permissions.
 */
enum PermissionStatus {
    /// Permission has been granted
    case granted
    
    /// Permission has been denied by the user
    case denied
    
    /// Permission has been permanently denied
    case permanentlyDenied
    
    /// Permission request is pending user response
    case pending
    
    /// Permission status is unknown or not yet checked
    case unknown
    
    // MARK: - Computed Properties
    
    /**
     * Checks if the permission is granted.
     */
    var isGranted: Bool {
        return self == .granted
    }
    
    /**
     * Checks if the permission is denied.
     */
    var isDenied: Bool {
        return self == .denied || self == .permanentlyDenied
    }
    
    /**
     * Checks if the permission is permanently denied.
     */
    var isPermanentlyDenied: Bool {
        return self == .permanentlyDenied
    }
    
    /**
     * Checks if we can request the permission.
     */
    var canRequest: Bool {
        return self == .denied || self == .unknown
    }
    
    // MARK: - Description
    
    var description: String {
        switch self {
        case .granted:
            return "Granted"
        case .denied:
            return "Denied"
        case .permanentlyDenied:
            return "Permanently Denied"
        case .pending:
            return "Pending"
        case .unknown:
            return "Unknown"
        }
    }
}

// MARK: - CaseIterable

extension PermissionStatus: CaseIterable {}

// MARK: - Equatable

extension PermissionStatus: Equatable {}

// MARK: - Codable

extension PermissionStatus: Codable {}