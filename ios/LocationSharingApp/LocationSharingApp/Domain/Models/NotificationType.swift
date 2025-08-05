import Foundation

/**
 * Enum representing different types of notifications in the app
 */
enum NotificationType: String, CaseIterable {
    case friendRequest = "friend_request"
    case friendRequestAccepted = "friend_request_accepted"
    case locationSharingRequest = "location_sharing_request"
    case locationSharingGranted = "location_sharing_granted"
    case locationSharingRevoked = "location_sharing_revoked"
    
    var displayName: String {
        switch self {
        case .friendRequest:
            return "Friend Requests"
        case .friendRequestAccepted:
            return "Friend Request Accepted"
        case .locationSharingRequest:
            return "Location Sharing Requests"
        case .locationSharingGranted:
            return "Location Sharing Granted"
        case .locationSharingRevoked:
            return "Location Sharing Revoked"
        }
    }
}