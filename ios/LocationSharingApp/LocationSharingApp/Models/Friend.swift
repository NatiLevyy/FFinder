import Foundation

struct Friend: Codable, Identifiable {
    let id: String
    let user: User
    let friendshipStatus: FriendshipStatus
    let locationSharingEnabled: Bool
    let lastKnownLocation: Location?
    let locationSharingPermission: LocationSharingPermission
    
    init(id: String, user: User, friendshipStatus: FriendshipStatus, locationSharingEnabled: Bool, lastKnownLocation: Location? = nil, locationSharingPermission: LocationSharingPermission) {
        self.id = id
        self.user = user
        self.friendshipStatus = friendshipStatus
        self.locationSharingEnabled = locationSharingEnabled
        self.lastKnownLocation = lastKnownLocation
        self.locationSharingPermission = locationSharingPermission
    }
}

enum FriendshipStatus: String, Codable, CaseIterable {
    case pending = "PENDING"
    case accepted = "ACCEPTED"
    case blocked = "BLOCKED"
}

enum LocationSharingPermission: String, Codable, CaseIterable {
    case none = "NONE"
    case requested = "REQUESTED"
    case granted = "GRANTED"
    case denied = "DENIED"
}