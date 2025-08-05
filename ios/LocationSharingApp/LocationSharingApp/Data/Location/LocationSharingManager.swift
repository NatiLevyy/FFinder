import Foundation
import Combine

/**
 * Manager responsible for handling location sharing permissions between friends.
 * 
 * This manager coordinates location sharing requests, permission tracking,
 * and notification of permission changes to maintain privacy control.
 */
class LocationSharingManager: ObservableObject {
    
    private let friendsRepository: FriendsRepository
    
    @Published private var locationSharingStatus: [String: LocationSharingPermission] = [:]
    
    init(friendsRepository: FriendsRepository) {
        self.friendsRepository = friendsRepository
    }
    
    /**
     * Requests location sharing permission from a specific friend.
     * 
     * - Parameter friendId: The ID of the friend to request permission from
     * - Returns: Result indicating success or failure of the request
     */
    func requestLocationSharingPermission(friendId: String) async -> Result<Void, LocationSharingError> {
        do {
            // Update the friend's permission status to REQUESTED
            let result = try await friendsRepository.updateLocationSharingPermission(friendId: friendId, permission: .requested)
            
            switch result {
            case .success:
                // Update local status tracking
                await MainActor.run {
                    locationSharingStatus[friendId] = .requested
                }
                
                // TODO: Send push notification to friend about the request
                // This will be implemented when notification system is added
                
                return .success(())
                
            case .failure(let error):
                return .failure(.requestFailed("Failed to request location sharing permission: \(error.localizedDescription)"))
            }
        } catch {
            return .failure(.requestFailed("Failed to request location sharing permission: \(error.localizedDescription)"))
        }
    }
    
    /**
     * Grants location sharing permission to a specific friend.
     * 
     * - Parameter friendId: The ID of the friend to grant permission to
     * - Returns: Result indicating success or failure of granting permission
     */
    func grantLocationSharingPermission(friendId: String) async -> Result<Void, LocationSharingError> {
        do {
            let result = try await friendsRepository.updateLocationSharingPermission(friendId: friendId, permission: .granted)
            
            switch result {
            case .success:
                // Update local status tracking
                await MainActor.run {
                    locationSharingStatus[friendId] = .granted
                }
                
                // TODO: Notify friend that permission was granted
                // TODO: Start sharing location updates with this friend
                
                return .success(())
                
            case .failure(let error):
                return .failure(.permissionUpdateFailed("Failed to grant location sharing permission: \(error.localizedDescription)"))
            }
        } catch {
            return .failure(.permissionUpdateFailed("Failed to grant location sharing permission: \(error.localizedDescription)"))
        }
    }
    
    /**
     * Denies location sharing permission to a specific friend.
     * 
     * - Parameter friendId: The ID of the friend to deny permission to
     * - Returns: Result indicating success or failure of denying permission
     */
    func denyLocationSharingPermission(friendId: String) async -> Result<Void, LocationSharingError> {
        do {
            let result = try await friendsRepository.updateLocationSharingPermission(friendId: friendId, permission: .denied)
            
            switch result {
            case .success:
                // Update local status tracking
                await MainActor.run {
                    locationSharingStatus[friendId] = .denied
                }
                
                // TODO: Notify friend that permission was denied
                
                return .success(())
                
            case .failure(let error):
                return .failure(.permissionUpdateFailed("Failed to deny location sharing permission: \(error.localizedDescription)"))
            }
        } catch {
            return .failure(.permissionUpdateFailed("Failed to deny location sharing permission: \(error.localizedDescription)"))
        }
    }
    
    /**
     * Revokes location sharing permission from a specific friend.
     * 
     * - Parameter friendId: The ID of the friend to revoke permission from
     * - Returns: Result indicating success or failure of revoking permission
     */
    func revokeLocationSharingPermission(friendId: String) async -> Result<Void, LocationSharingError> {
        do {
            let result = try await friendsRepository.updateLocationSharingPermission(friendId: friendId, permission: .none)
            
            switch result {
            case .success:
                // Update local status tracking
                await MainActor.run {
                    locationSharingStatus[friendId] = .none
                }
                
                // TODO: Stop sharing location updates with this friend
                // TODO: Notify friend that location sharing was revoked
                
                return .success(())
                
            case .failure(let error):
                return .failure(.permissionUpdateFailed("Failed to revoke location sharing permission: \(error.localizedDescription)"))
            }
        } catch {
            return .failure(.permissionUpdateFailed("Failed to revoke location sharing permission: \(error.localizedDescription)"))
        }
    }
    
    /**
     * Gets the current location sharing permission status for a specific friend.
     * 
     * - Parameter friendId: The ID of the friend to check permission for
     * - Returns: The current location sharing permission status
     */
    func getLocationSharingPermission(friendId: String) async -> LocationSharingPermission {
        do {
            let friend = try await friendsRepository.getFriend(friendId: friendId)
            return friend?.locationSharingPermission ?? .none
        } catch {
            return .none
        }
    }
    
    /**
     * Gets all friends with their current location sharing permission status.
     * 
     * - Returns: Publisher of friends with their location sharing status
     */
    func getFriendsWithLocationSharingStatus() -> AnyPublisher<[Friend], Never> {
        return friendsRepository.getFriendsPublisher()
            .replaceError(with: [])
            .eraseToAnyPublisher()
    }
    
    /**
     * Checks if location sharing is currently active with a specific friend.
     * 
     * - Parameter friendId: The ID of the friend to check
     * - Returns: True if location sharing is active, false otherwise
     */
    func isLocationSharingActive(friendId: String) async -> Bool {
        let permission = await getLocationSharingPermission(friendId: friendId)
        return permission == .granted
    }
    
    /**
     * Gets a list of all friends who have granted location sharing permission.
     * 
     * - Returns: List of friend IDs who have granted permission
     */
    func getAuthorizedFriends() async -> [String] {
        do {
            let friends = try await friendsRepository.getFriends()
            switch friends {
            case .success(let friendsList):
                return friendsList.filter { $0.locationSharingPermission == .granted }
                                 .map { $0.id }
            case .failure:
                return []
            }
        } catch {
            return []
        }
    }
    
    /**
     * Initializes the location sharing status tracking by loading current friend permissions.
     */
    func initializeLocationSharingStatus() async {
        do {
            let friends = try await friendsRepository.getFriends()
            switch friends {
            case .success(let friendsList):
                let statusMap = Dictionary(uniqueKeysWithValues: friendsList.map { ($0.id, $0.locationSharingPermission) })
                await MainActor.run {
                    locationSharingStatus = statusMap
                }
            case .failure:
                await MainActor.run {
                    locationSharingStatus = [:]
                }
            }
        } catch {
            await MainActor.run {
                locationSharingStatus = [:]
            }
        }
    }
    
    /**
     * Gets the current location sharing status map.
     * 
     * - Returns: Dictionary mapping friend IDs to their location sharing permission status
     */
    func getCurrentLocationSharingStatus() -> [String: LocationSharingPermission] {
        return locationSharingStatus
    }
    
    // MARK: - Lifecycle Methods
    
    /**
     * Initializes the location sharing manager.
     */
    func initialize() async {
        await initializeLocationSharingStatus()
    }
    
    /**
     * Resumes location sharing when app comes to foreground.
     */
    func resumeLocationSharing() async {
        // Refresh location sharing status
        await initializeLocationSharingStatus()
        // Resume any paused location sharing activities
    }
    
    /**
     * Optimizes location sharing for background operation.
     */
    func optimizeForBackground() async {
        // Reduce frequency of location sharing updates
        // Optimize battery usage
    }
    
    /**
     * Pauses non-essential location sharing services.
     */
    func pauseNonEssentialServices() async {
        // Pause non-critical location sharing operations
        // Keep only essential services running
    }
    
    /**
     * Cleans up location sharing resources.
     */
    func cleanup() async {
        // Stop all location sharing activities
        // Clean up resources
        await MainActor.run {
            locationSharingStatus = [:]
        }
    }
}

/**
 * Errors related to location sharing operations.
 */
enum LocationSharingError: Error, LocalizedError {
    case requestFailed(String)
    case permissionUpdateFailed(String)
    case notificationFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .requestFailed(let message):
            return "Request failed: \(message)"
        case .permissionUpdateFailed(let message):
            return "Permission update failed: \(message)"
        case .notificationFailed(let message):
            return "Notification failed: \(message)"
        }
    }
}