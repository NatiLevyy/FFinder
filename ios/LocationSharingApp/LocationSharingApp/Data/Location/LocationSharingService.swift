import Foundation
import Combine
import FirebaseAuth
import FirebaseDatabase

/**
 * Service responsible for real-time location sharing functionality.
 * Handles broadcasting location updates and subscribing to friend location updates.
 */
class LocationSharingService {
    
    private let firebaseAuth: Auth
    private let firebaseDatabase: Database
    private let locationValidator: LocationValidator
    
    private let userLocationsRef: DatabaseReference
    private let locationSharingRef: DatabaseReference
    private let friendsLocationsRef: DatabaseReference
    
    private static let userLocationsPath = "user_locations"
    private static let locationSharingPath = "location_sharing"
    private static let friendsLocationsPath = "friends_locations"
    private static let locationUpdateTimeoutMs: Int64 = 10000 // 10 seconds
    
    init(firebaseAuth: Auth = Auth.auth(),
         firebaseDatabase: Database = Database.database(),
         locationValidator: LocationValidator = LocationValidator()) {
        self.firebaseAuth = firebaseAuth
        self.firebaseDatabase = firebaseDatabase
        self.locationValidator = locationValidator
        
        self.userLocationsRef = firebaseDatabase.reference().child(Self.userLocationsPath)
        self.locationSharingRef = firebaseDatabase.reference().child(Self.locationSharingPath)
        self.friendsLocationsRef = firebaseDatabase.reference().child(Self.friendsLocationsPath)
    }
    
    /**
     * Broadcasts location update to Firebase for authorized friends to receive.
     * Validates location data before broadcasting.
     */
    func broadcastLocationUpdate(_ location: Location) async -> Result<Void, LocationSharingError> {
        guard let currentUser = firebaseAuth.currentUser else {
            print("User not authenticated, cannot broadcast location")
            return .failure(.userNotAuthenticated)
        }
        
        // Validate location data
        let validationResult = locationValidator.validateLocation(location)
        if !validationResult.isValid {
            print("Invalid location data: \(validationResult.errors)")
            return .failure(.invalidLocationData(validationResult.errors))
        }
        
        do {
            // Check if user has location sharing enabled
            let isSharingEnabled = await isLocationSharingEnabled(userId: currentUser.uid)
            if !isSharingEnabled {
                print("Location sharing is disabled for user: \(currentUser.uid)")
                return .failure(.locationSharingDisabled)
            }
            
            let locationData: [String: Any] = [
                "latitude": location.latitude,
                "longitude": location.longitude,
                "accuracy": location.accuracy,
                "timestamp": location.timestamp,
                "altitude": location.altitude as Any,
                "isActive": true,
                "lastUpdated": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            // Broadcast to user's location node
            try await userLocationsRef.child(currentUser.uid).setValue(locationData)
            
            print("Location broadcasted successfully for user: \(currentUser.uid)")
            return .success(())
        } catch {
            print("Failed to broadcast location update: \(error)")
            return .failure(.broadcastFailed(error))
        }
    }
    
    /**
     * Subscribes to location updates from a specific friend.
     * Returns a Publisher that emits location updates in real-time.
     */
    func subscribeToFriendLocationUpdates(friendId: String) -> AnyPublisher<Location, LocationSharingError> {
        guard let currentUser = firebaseAuth.currentUser else {
            print("User not authenticated, cannot subscribe to friend locations")
            return Fail(error: LocationSharingError.userNotAuthenticated)
                .eraseToAnyPublisher()
        }
        
        return Publishers.Create<Location, LocationSharingError> { subscriber in
            let handle = self.userLocationsRef.child(friendId).observe(.value) { snapshot in
                if snapshot.exists() {
                    if let location = self.parseLocationFromSnapshot(snapshot) {
                        // Validate received location
                        let validationResult = self.locationValidator.validateLocation(location)
                        if validationResult.isValid {
                            subscriber.send(location)
                        } else {
                            print("Received invalid location from friend \(friendId): \(validationResult.errors)")
                        }
                    }
                }
            } withCancel: { error in
                print("Friend location subscription cancelled: \(error)")
                subscriber.send(completion: .failure(.subscriptionFailed(error)))
            }
            
            return AnyCancellable {
                self.userLocationsRef.child(friendId).removeObserver(withHandle: handle)
                print("Unsubscribed from friend location updates: \(friendId)")
            }
        }
        .eraseToAnyPublisher()
    }
    
    /**
     * Subscribes to location updates from multiple friends.
     * Returns a Publisher that emits a dictionary of friend IDs to their locations.
     */
    func subscribeToMultipleFriendsLocationUpdates(friendIds: [String]) -> AnyPublisher<[String: Location], LocationSharingError> {
        guard let currentUser = firebaseAuth.currentUser else {
            print("User not authenticated, cannot subscribe to friends locations")
            return Fail(error: LocationSharingError.userNotAuthenticated)
                .eraseToAnyPublisher()
        }
        
        return Publishers.Create<[String: Location], LocationSharingError> { subscriber in
            var listeners: [String: DatabaseHandle] = [:]
            var currentLocations: [String: Location] = [:]
            
            for friendId in friendIds {
                let handle = self.userLocationsRef.child(friendId).observe(.value) { snapshot in
                    if snapshot.exists() {
                        if let location = self.parseLocationFromSnapshot(snapshot) {
                            let validationResult = self.locationValidator.validateLocation(location)
                            if validationResult.isValid {
                                currentLocations[friendId] = location
                                subscriber.send(currentLocations)
                            } else {
                                print("Received invalid location from friend \(friendId): \(validationResult.errors)")
                            }
                        }
                    } else {
                        // Friend location no longer available
                        currentLocations.removeValue(forKey: friendId)
                        subscriber.send(currentLocations)
                    }
                } withCancel: { error in
                    print("Friend location subscription cancelled for \(friendId): \(error)")
                }
                
                listeners[friendId] = handle
            }
            
            return AnyCancellable {
                for (friendId, handle) in listeners {
                    self.userLocationsRef.child(friendId).removeObserver(withHandle: handle)
                }
                print("Unsubscribed from all friends location updates")
            }
        }
        .eraseToAnyPublisher()
    }
    
    /**
     * Enables location sharing for the current user.
     */
    func enableLocationSharing() async -> Result<Void, LocationSharingError> {
        guard let currentUser = firebaseAuth.currentUser else {
            return .failure(.userNotAuthenticated)
        }
        
        do {
            let sharingData: [String: Any] = [
                "isSharing": true,
                "enabledAt": Int64(Date().timeIntervalSince1970 * 1000),
                "lastUpdated": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            try await locationSharingRef.child(currentUser.uid).setValue(sharingData)
            
            print("Location sharing enabled for user: \(currentUser.uid)")
            return .success(())
        } catch {
            print("Failed to enable location sharing: \(error)")
            return .failure(.configurationFailed(error))
        }
    }
    
    /**
     * Disables location sharing for the current user.
     */
    func disableLocationSharing() async -> Result<Void, LocationSharingError> {
        guard let currentUser = firebaseAuth.currentUser else {
            return .failure(.userNotAuthenticated)
        }
        
        do {
            let sharingData: [String: Any] = [
                "isSharing": false,
                "disabledAt": Int64(Date().timeIntervalSince1970 * 1000),
                "lastUpdated": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            try await locationSharingRef.child(currentUser.uid).setValue(sharingData)
            
            // Mark user as inactive in location updates
            try await userLocationsRef.child(currentUser.uid).child("isActive").setValue(false)
            
            print("Location sharing disabled for user: \(currentUser.uid)")
            return .success(())
        } catch {
            print("Failed to disable location sharing: \(error)")
            return .failure(.configurationFailed(error))
        }
    }
    
    // MARK: - Private Methods
    
    /**
     * Checks if location sharing is enabled for a specific user.
     */
    private func isLocationSharingEnabled(userId: String) async -> Bool {
        do {
            let snapshot = try await locationSharingRef.child(userId).getData()
            if snapshot.exists() {
                return snapshot.childSnapshot(forPath: "isSharing").value as? Bool ?? false
            } else {
                return false
            }
        } catch {
            print("Failed to check location sharing status: \(error)")
            return false
        }
    }
    
    /**
     * Parses location data from Firebase DataSnapshot with error handling.
     */
    private func parseLocationFromSnapshot(_ snapshot: DataSnapshot) -> Location? {
        guard let data = snapshot.value as? [String: Any] else {
            return nil
        }
        
        guard let latitude = data["latitude"] as? Double,
              let longitude = data["longitude"] as? Double else {
            return nil
        }
        
        let accuracy = data["accuracy"] as? Float ?? 0.0
        let timestamp = data["timestamp"] as? Int64 ?? Int64(Date().timeIntervalSince1970 * 1000)
        let altitude = data["altitude"] as? Double
        
        return Location(
            latitude: latitude,
            longitude: longitude,
            accuracy: accuracy,
            timestamp: timestamp,
            altitude: altitude
        )
    }
}

// MARK: - Error Types

enum LocationSharingError: Error {
    case userNotAuthenticated
    case locationSharingDisabled
    case invalidLocationData([String])
    case broadcastFailed(Error)
    case subscriptionFailed(Error)
    case configurationFailed(Error)
    
    var localizedDescription: String {
        switch self {
        case .userNotAuthenticated:
            return "User not authenticated"
        case .locationSharingDisabled:
            return "Location sharing is disabled"
        case .invalidLocationData(let errors):
            return "Invalid location data: \(errors.joined(separator: ", "))"
        case .broadcastFailed(let error):
            return "Failed to broadcast location: \(error.localizedDescription)"
        case .subscriptionFailed(let error):
            return "Failed to subscribe to location updates: \(error.localizedDescription)"
        case .configurationFailed(let error):
            return "Failed to configure location sharing: \(error.localizedDescription)"
        }
    }
}

// MARK: - Publishers Extension (if not already defined)

extension Publishers {
    struct Create<Output, Failure: Error>: Publisher {
        typealias SubscriberHandler = (AnySubscriber<Output, Failure>) -> AnyCancellable
        
        private let handler: SubscriberHandler
        
        init(_ handler: @escaping SubscriberHandler) {
            self.handler = handler
        }
        
        func receive<S>(subscriber: S) where S : Subscriber, Failure == S.Failure, Output == S.Input {
            let subscription = Subscription(handler: handler, subscriber: AnySubscriber(subscriber))
            subscriber.receive(subscription: subscription)
        }
        
        private class Subscription<S: Subscriber>: Combine.Subscription where S.Input == Output, S.Failure == Failure {
            private let handler: SubscriberHandler
            private let subscriber: AnySubscriber<Output, Failure>
            private var cancellable: AnyCancellable?
            
            init(handler: @escaping SubscriberHandler, subscriber: AnySubscriber<Output, Failure>) {
                self.handler = handler
                self.subscriber = subscriber
            }
            
            func request(_ demand: Subscribers.Demand) {
                guard cancellable == nil else { return }
                cancellable = handler(subscriber)
            }
            
            func cancel() {
                cancellable?.cancel()
                cancellable = nil
            }
        }
    }
}