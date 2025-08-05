import Foundation
import CoreData
import Combine

/**
 * Manages local data caching for offline support.
 */
class CacheManager: ObservableObject {
    
    static let shared = CacheManager()
    
    private init() {}
    
    // MARK: - Core Data Stack
    
    lazy var persistentContainer: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "LocationSharingCache")
        container.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Core Data error: \(error)")
            }
        }
        return container
    }()
    
    private var context: NSManagedObjectContext {
        return persistentContainer.viewContext
    }
    
    // MARK: - Location Caching
    
    /**
     * Caches a location update locally.
     */
    func cacheLocation(userId: String, location: Location) async throws {
        try await context.perform {
            let cachedLocation = CachedLocation(context: self.context)
            cachedLocation.userId = userId
            cachedLocation.latitude = location.latitude
            cachedLocation.longitude = location.longitude
            cachedLocation.accuracy = location.accuracy
            cachedLocation.timestamp = Int64(location.timestamp)
            cachedLocation.altitude = location.altitude ?? 0
            cachedLocation.cachedAt = Int64(Date().timeIntervalSince1970 * 1000)
            
            try self.context.save()
        }
    }
    
    /**
     * Gets the last known location for a user.
     */
    func getLastKnownLocation(userId: String) async throws -> Location? {
        return try await context.perform {
            let request: NSFetchRequest<CachedLocation> = CachedLocation.fetchRequest()
            request.predicate = NSPredicate(format: "userId == %@", userId)
            request.sortDescriptors = [NSSortDescriptor(key: "timestamp", ascending: false)]
            request.fetchLimit = 1
            
            let results = try self.context.fetch(request)
            return results.first?.toDomainModel()
        }
    }
    
    /**
     * Gets all cached locations for a user within a time range.
     */
    func getCachedLocations(userId: String, since: Int64) async throws -> [Location] {
        return try await context.perform {
            let request: NSFetchRequest<CachedLocation> = CachedLocation.fetchRequest()
            request.predicate = NSPredicate(format: "userId == %@ AND timestamp >= %lld", userId, since)
            request.sortDescriptors = [NSSortDescriptor(key: "timestamp", ascending: false)]
            
            let results = try self.context.fetch(request)
            return results.map { $0.toDomainModel() }
        }
    }
    
    /**
     * Observes location updates for a user.
     */
    func observeUserLocation(userId: String) -> AnyPublisher<Location?, Never> {
        let request: NSFetchRequest<CachedLocation> = CachedLocation.fetchRequest()
        request.predicate = NSPredicate(format: "userId == %@", userId)
        request.sortDescriptors = [NSSortDescriptor(key: "timestamp", ascending: false)]
        request.fetchLimit = 1
        
        return NotificationCenter.default
            .publisher(for: .NSManagedObjectContextObjectsDidChange, object: context)
            .compactMap { _ in
                try? self.context.fetch(request).first?.toDomainModel()
            }
            .eraseToAnyPublisher()
    }
    
    /**
     * Clears old location data beyond the retention period.
     */
    func clearOldLocations(retentionPeriodMs: Int64 = 7 * 24 * 60 * 60 * 1000) async throws {
        let cutoffTime = Int64(Date().timeIntervalSince1970 * 1000) - retentionPeriodMs
        
        try await context.perform {
            let request: NSFetchRequest<NSFetchRequestResult> = CachedLocation.fetchRequest()
            request.predicate = NSPredicate(format: "timestamp < %lld", cutoffTime)
            
            let deleteRequest = NSBatchDeleteRequest(fetchRequest: request)
            try self.context.execute(deleteRequest)
            try self.context.save()
        }
    }
    
    // MARK: - Friends Caching
    
    /**
     * Caches friends list locally.
     */
    func cacheFriends(_ friends: [Friend]) async throws {
        try await context.perform {
            // Clear existing friends
            let deleteRequest: NSFetchRequest<NSFetchRequestResult> = CachedFriend.fetchRequest()
            let batchDeleteRequest = NSBatchDeleteRequest(fetchRequest: deleteRequest)
            try self.context.execute(batchDeleteRequest)
            
            // Add new friends
            for friend in friends {
                let cachedFriend = CachedFriend(context: self.context)
                cachedFriend.id = friend.id
                cachedFriend.userId = friend.user.id
                cachedFriend.email = friend.user.email
                cachedFriend.displayName = friend.user.displayName
                cachedFriend.profileImageUrl = friend.user.profileImageUrl
                cachedFriend.friendshipStatus = friend.friendshipStatus.rawValue
                cachedFriend.locationSharingEnabled = friend.locationSharingEnabled
                cachedFriend.locationSharingPermission = friend.locationSharingPermission.rawValue
                cachedFriend.lastKnownLatitude = friend.lastKnownLocation?.latitude ?? 0
                cachedFriend.lastKnownLongitude = friend.lastKnownLocation?.longitude ?? 0
                cachedFriend.lastKnownTimestamp = Int64(friend.lastKnownLocation?.timestamp ?? 0)
                cachedFriend.cachedAt = Int64(Date().timeIntervalSince1970 * 1000)
            }
            
            try self.context.save()
        }
    }
    
    /**
     * Gets cached friends list.
     */
    func getCachedFriends() async throws -> [Friend] {
        return try await context.perform {
            let request: NSFetchRequest<CachedFriend> = CachedFriend.fetchRequest()
            request.sortDescriptors = [NSSortDescriptor(key: "displayName", ascending: true)]
            
            let results = try self.context.fetch(request)
            return results.compactMap { $0.toDomainModel() }
        }
    }
    
    /**
     * Observes friends list changes.
     */
    func observeFriends() -> AnyPublisher<[Friend], Never> {
        let request: NSFetchRequest<CachedFriend> = CachedFriend.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(key: "displayName", ascending: true)]
        
        return NotificationCenter.default
            .publisher(for: .NSManagedObjectContextObjectsDidChange, object: context)
            .compactMap { _ in
                try? self.context.fetch(request).compactMap { $0.toDomainModel() }
            }
            .eraseToAnyPublisher()
    }
    
    // MARK: - Pending Operations
    
    /**
     * Queues an operation to be executed when online.
     */
    func queuePendingOperation(
        operationType: OperationType,
        data: String,
        priority: Int = 0
    ) async throws {
        try await context.perform {
            let operation = PendingOperation(context: self.context)
            operation.operationType = operationType.rawValue
            operation.data = data
            operation.priority = Int32(priority)
            operation.createdAt = Int64(Date().timeIntervalSince1970 * 1000)
            operation.retryCount = 0
            
            try self.context.save()
        }
    }
    
    /**
     * Gets all pending operations ordered by priority and creation time.
     */
    func getPendingOperations() async throws -> [PendingOperation] {
        return try await context.perform {
            let request: NSFetchRequest<PendingOperation> = PendingOperation.fetchRequest()
            request.sortDescriptors = [
                NSSortDescriptor(key: "priority", ascending: false),
                NSSortDescriptor(key: "createdAt", ascending: true)
            ]
            
            return try self.context.fetch(request)
        }
    }
    
    /**
     * Marks an operation as completed and removes it from the queue.
     */
    func completePendingOperation(_ operation: PendingOperation) async throws {
        try await context.perform {
            self.context.delete(operation)
            try self.context.save()
        }
    }
    
    /**
     * Increments retry count for a failed operation.
     */
    func incrementRetryCount(_ operation: PendingOperation) async throws {
        try await context.perform {
            operation.retryCount += 1
            operation.lastAttemptAt = Int64(Date().timeIntervalSince1970 * 1000)
            try self.context.save()
        }
    }
    
    /**
     * Removes operations that have exceeded max retry attempts.
     */
    func cleanupFailedOperations(maxRetries: Int = 5) async throws {
        try await context.perform {
            let request: NSFetchRequest<NSFetchRequestResult> = PendingOperation.fetchRequest()
            request.predicate = NSPredicate(format: "retryCount >= %d", maxRetries)
            
            let deleteRequest = NSBatchDeleteRequest(fetchRequest: request)
            try self.context.execute(deleteRequest)
            try self.context.save()
        }
    }
    
    // MARK: - Cache Management
    
    /**
     * Clears all cached data.
     */
    func clearAllCache() async throws {
        try await context.perform {
            // Clear locations
            let locationRequest: NSFetchRequest<NSFetchRequestResult> = CachedLocation.fetchRequest()
            let locationDeleteRequest = NSBatchDeleteRequest(fetchRequest: locationRequest)
            try self.context.execute(locationDeleteRequest)
            
            // Clear friends
            let friendRequest: NSFetchRequest<NSFetchRequestResult> = CachedFriend.fetchRequest()
            let friendDeleteRequest = NSBatchDeleteRequest(fetchRequest: friendRequest)
            try self.context.execute(friendDeleteRequest)
            
            // Clear pending operations
            let operationRequest: NSFetchRequest<NSFetchRequestResult> = PendingOperation.fetchRequest()
            let operationDeleteRequest = NSBatchDeleteRequest(fetchRequest: operationRequest)
            try self.context.execute(operationDeleteRequest)
            
            try self.context.save()
        }
    }
    
    /**
     * Gets cache statistics.
     */
    func getCacheStats() async throws -> CacheStats {
        return try await context.perform {
            let locationCount = try self.context.count(for: CachedLocation.fetchRequest())
            let friendCount = try self.context.count(for: CachedFriend.fetchRequest())
            let operationCount = try self.context.count(for: PendingOperation.fetchRequest())
            
            return CacheStats(
                locationCount: locationCount,
                friendCount: friendCount,
                pendingOperationCount: operationCount,
                totalCacheSize: locationCount + friendCount + operationCount
            )
        }
    }
    
    // MARK: - Lifecycle Methods
    
    /**
     * Initializes the cache manager.
     */
    func initialize() async {
        do {
            try await cleanupExpiredEntries()
            try await cleanupFailedOperations()
        } catch {
            print("Cache initialization error: \(error)")
        }
    }
    
    /**
     * Cleans up expired cache entries.
     */
    func cleanupExpiredEntries() async throws {
        try await clearOldLocations()
    }
    
    /**
     * Trims memory based on the specified level.
     */
    func trimMemory(_ level: TrimLevel) async {
        do {
            switch level {
            case .uiHidden:
                // Light cleanup - remove UI-related cache
                break
            case .moderate:
                // Moderate cleanup - remove some non-essential data
                try await clearOldLocations(retentionPeriodMs: 3 * 24 * 60 * 60 * 1000) // 3 days
            case .low:
                // Aggressive cleanup - remove more data
                try await clearOldLocations(retentionPeriodMs: 24 * 60 * 60 * 1000) // 1 day
            case .critical:
                // Critical cleanup - remove almost everything non-essential
                try await clearOldLocations(retentionPeriodMs: 6 * 60 * 60 * 1000) // 6 hours
                try await cleanupFailedOperations(maxRetries: 2)
            }
        } catch {
            print("Memory trim error: \(error)")
        }
    }
    
    /**
     * Clears non-essential cache data.
     */
    func clearNonEssentialCache() async {
        do {
            try await clearOldLocations(retentionPeriodMs: 60 * 60 * 1000) // Keep only last hour
            try await cleanupFailedOperations(maxRetries: 1)
        } catch {
            print("Clear non-essential cache error: \(error)")
        }
    }
    
    /**
     * Performs cleanup operations.
     */
    func cleanup() async {
        do {
            try await cleanupFailedOperations(maxRetries: 0) // Remove all failed operations
        } catch {
            print("Cache cleanup error: \(error)")
        }
    }
    
    enum TrimLevel {
        case uiHidden
        case moderate
        case low
        case critical
    }
    
    // MARK: - Core Data Save
    
    func saveContext() {
        if context.hasChanges {
            do {
                try context.save()
            } catch {
                print("Save error: \(error)")
            }
        }
    }
}

// MARK: - Supporting Types

enum OperationType: String, CaseIterable {
    case locationUpdate = "LOCATION_UPDATE"
    case friendRequest = "FRIEND_REQUEST"
    case locationSharingPermission = "LOCATION_SHARING_PERMISSION"
    case friendRemoval = "FRIEND_REMOVAL"
    case profileUpdate = "PROFILE_UPDATE"
}

struct CacheStats {
    let locationCount: Int
    let friendCount: Int
    let pendingOperationCount: Int
    let totalCacheSize: Int
}

// MARK: - Core Data Model Extensions

extension CachedLocation {
    func toDomainModel() -> Location {
        return Location(
            latitude: latitude,
            longitude: longitude,
            accuracy: Float(accuracy),
            timestamp: timestamp,
            altitude: altitude == 0 ? nil : altitude
        )
    }
}

extension CachedFriend {
    func toDomainModel() -> Friend? {
        guard let friendshipStatus = FriendshipStatus(rawValue: friendshipStatus ?? ""),
              let locationSharingPermission = LocationSharingPermission(rawValue: locationSharingPermission ?? "") else {
            return nil
        }
        
        let user = User(
            id: userId ?? "",
            email: email ?? "",
            displayName: displayName ?? "",
            profileImageUrl: profileImageUrl,
            createdAt: 0,
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let lastKnownLocation = (lastKnownLatitude != 0 && lastKnownLongitude != 0 && lastKnownTimestamp != 0) ?
            Location(
                latitude: lastKnownLatitude,
                longitude: lastKnownLongitude,
                accuracy: 0,
                timestamp: lastKnownTimestamp,
                altitude: nil
            ) : nil
        
        return Friend(
            id: id ?? "",
            user: user,
            friendshipStatus: friendshipStatus,
            locationSharingEnabled: locationSharingEnabled,
            lastKnownLocation: lastKnownLocation,
            locationSharingPermission: locationSharingPermission
        )
    }
}