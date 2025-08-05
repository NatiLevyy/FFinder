import Foundation
import Combine
import FirebaseAuth
import FirebaseDatabase

/**
 * Implementation of LocationRepository that integrates with Firebase Realtime Database
 * for real-time location sharing and local caching for offline scenarios.
 */
class LocationRepositoryImpl: LocationRepository {
    
    private let firebaseAuth: Auth
    private let firebaseDatabase: Database
    private let locationCache: LocationCache
    
    private let userLocationsRef: DatabaseReference
    private let locationSharingRef: DatabaseReference
    
    private static let userLocationsPath = "user_locations"
    private static let locationSharingPath = "location_sharing"
    
    init(firebaseAuth: Auth = Auth.auth(),
         firebaseDatabase: Database = Database.database(),
         locationCache: LocationCache = LocationCache()) {
        self.firebaseAuth = firebaseAuth
        self.firebaseDatabase = firebaseDatabase
        self.locationCache = locationCache
        
        self.userLocationsRef = firebaseDatabase.reference().child(Self.userLocationsPath)
        self.locationSharingRef = firebaseDatabase.reference().child(Self.locationSharingPath)
    }
    
    func updateLocation(_ location: Location) async -> Result<Void, Error> {
        guard let currentUser = firebaseAuth.currentUser else {
            print("User not authenticated, cannot update location")
            return .failure(LocationRepositoryError.userNotAuthenticated)
        }
        
        do {
            let locationData: [String: Any] = [
                "latitude": location.latitude,
                "longitude": location.longitude,
                "accuracy": location.accuracy,
                "timestamp": location.timestamp,
                "altitude": location.altitude as Any,
                "isActive": true
            ]
            
            // Update Firebase
            try await userLocationsRef.child(currentUser.uid).setValue(locationData)
            
            // Cache locally for offline support
            await locationCache.cacheUserLocation(userId: currentUser.uid, location: location)
            
            print("Location updated successfully for user: \(currentUser.uid)")
            return .success(())
        } catch {
            print("Failed to update location: \(error)")
            return .failure(error)
        }
    }
    
    func getCurrentLocation() async -> Location? {
        guard let currentUser = firebaseAuth.currentUser else {
            return nil
        }
        
        do {
            // Try to get from Firebase first
            let snapshot = try await userLocationsRef.child(currentUser.uid).getData()
            if snapshot.exists() {
                if let location = parseLocationFromSnapshot(snapshot) {
                    return location
                }
            }
            
            // Fallback to cached location
            return await locationCache.getCachedUserLocation(userId: currentUser.uid)
        } catch {
            print("Failed to get current location, trying cache: \(error)")
            // Fallback to cached location on error
            return await locationCache.getCachedUserLocation(userId: currentUser.uid)
        }
    }
    
    func getLocationUpdates() -> AnyPublisher<Location, Never> {
        guard let currentUser = firebaseAuth.currentUser else {
            print("User not authenticated, cannot get location updates")
            return Empty<Location, Never>().eraseToAnyPublisher()
        }
        
        return Publishers.Create { subscriber in
            let handle = self.userLocationsRef.child(currentUser.uid).observe(.value) { snapshot in
                if snapshot.exists() {
                    if let location = self.parseLocationFromSnapshot(snapshot) {
                        // Cache the location
                        Task {
                            await self.locationCache.cacheUserLocation(userId: currentUser.uid, location: location)
                        }
                        subscriber.send(location)
                    }
                }
            } withCancel: { error in
                print("Location updates cancelled: \(error)")
                subscriber.send(completion: .finished)
            }
            
            return AnyCancellable {
                self.userLocationsRef.child(currentUser.uid).removeObserver(withHandle: handle)
            }
        }
        .eraseToAnyPublisher()
    }
    
    func getFriendLocations(friendIds: [String]) async -> Result<[String: Location], Error> {
        do {
            var locations: [String: Location] = [:]
            
            // Get locations from Firebase
            for friendId in friendIds {
                do {
                    let snapshot = try await userLocationsRef.child(friendId).getData()
                    if snapshot.exists() {
                        if let location = parseLocationFromSnapshot(snapshot) {
                            locations[friendId] = location
                            // Cache friend location
                            await locationCache.cacheFriendLocation(friendId: friendId, location: location)
                        }
                    } else {
                        // Try to get from cache if not available online
                        if let cachedLocation = await locationCache.getCachedFriendLocation(friendId: friendId) {
                            locations[friendId] = cachedLocation
                        }
                    }
                } catch {
                    print("Failed to get location for friend: \(friendId), trying cache: \(error)")
                    // Try cache on individual friend failure
                    if let cachedLocation = await locationCache.getCachedFriendLocation(friendId: friendId) {
                        locations[friendId] = cachedLocation
                    }
                }
            }
            
            return .success(locations)
        } catch {
            print("Failed to get friend locations: \(error)")
            
            // Fallback to cached locations
            var cachedLocations: [String: Location] = [:]
            for friendId in friendIds {
                if let cachedLocation = await locationCache.getCachedFriendLocation(friendId: friendId) {
                    cachedLocations[friendId] = cachedLocation
                }
            }
            
            if !cachedLocations.isEmpty {
                return .success(cachedLocations)
            } else {
                return .failure(error)
            }
        }
    }
    
    func startLocationSharing() async -> Result<Void, Error> {
        guard let currentUser = firebaseAuth.currentUser else {
            return .failure(LocationRepositoryError.userNotAuthenticated)
        }
        
        do {
            let sharingData: [String: Any] = [
                "isSharing": true,
                "startedAt": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            try await locationSharingRef.child(currentUser.uid).setValue(sharingData)
            
            print("Location sharing started for user: \(currentUser.uid)")
            return .success(())
        } catch {
            print("Failed to start location sharing: \(error)")
            return .failure(error)
        }
    }
    
    func stopLocationSharing() async -> Result<Void, Error> {
        guard let currentUser = firebaseAuth.currentUser else {
            return .failure(LocationRepositoryError.userNotAuthenticated)
        }
        
        do {
            let sharingData: [String: Any] = [
                "isSharing": false,
                "stoppedAt": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            try await locationSharingRef.child(currentUser.uid).setValue(sharingData)
            
            // Mark user as inactive in location updates
            try await userLocationsRef.child(currentUser.uid).child("isActive").setValue(false)
            
            print("Location sharing stopped for user: \(currentUser.uid)")
            return .success(())
        } catch {
            print("Failed to stop location sharing: \(error)")
            return .failure(error)
        }
    }
    
    // MARK: - Private Methods
    
    /**
     * Parses location data from Firebase DataSnapshot
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

enum LocationRepositoryError: Error {
    case userNotAuthenticated
    case invalidLocationData
    case networkError(Error)
    
    var localizedDescription: String {
        switch self {
        case .userNotAuthenticated:
            return "User not authenticated"
        case .invalidLocationData:
            return "Invalid location data"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}

// MARK: - Publishers Extension

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