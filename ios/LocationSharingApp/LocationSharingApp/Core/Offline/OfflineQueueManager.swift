import Foundation
import Combine

/**
 * Manages offline operation queue and synchronization.
 */
class OfflineQueueManager: ObservableObject {
    
    static let shared = OfflineQueueManager()
    
    @Published private(set) var isProcessing = false
    @Published private(set) var queueSize = 0
    
    private let cacheManager = CacheManager.shared
    private let networkErrorHandler = NetworkErrorHandler()
    private var cancellables = Set<AnyCancellable>()
    
    private init() {
        startAutoProcessing()
    }
    
    // MARK: - Queue Operations
    
    /**
     * Queues a location update operation.
     */
    func queueLocationUpdate(userId: String, latitude: Double, longitude: Double, timestamp: Int64) async {
        let data = LocationUpdateData(userId: userId, latitude: latitude, longitude: longitude, timestamp: timestamp)
        
        do {
            let jsonData = try JSONEncoder().encode(data)
            let jsonString = String(data: jsonData, encoding: .utf8) ?? ""
            
            try await cacheManager.queuePendingOperation(
                operationType: .locationUpdate,
                data: jsonString,
                priority: 10 // High priority for location updates
            )
            
            await updateQueueSize()
        } catch {
            print("Failed to queue location update: \(error)")
        }
    }
    
    /**
     * Queues a friend request operation.
     */
    func queueFriendRequest(email: String) async {
        let data = FriendRequestData(email: email)
        
        do {
            let jsonData = try JSONEncoder().encode(data)
            let jsonString = String(data: jsonData, encoding: .utf8) ?? ""
            
            try await cacheManager.queuePendingOperation(
                operationType: .friendRequest,
                data: jsonString,
                priority: 5
            )
            
            await updateQueueSize()
        } catch {
            print("Failed to queue friend request: \(error)")
        }
    }
    
    /**
     * Queues a location sharing permission update.
     */
    func queueLocationSharingPermission(friendId: String, enabled: Bool) async {
        let data = LocationSharingPermissionData(friendId: friendId, enabled: enabled)
        
        do {
            let jsonData = try JSONEncoder().encode(data)
            let jsonString = String(data: jsonData, encoding: .utf8) ?? ""
            
            try await cacheManager.queuePendingOperation(
                operationType: .locationSharingPermission,
                data: jsonString,
                priority: 7
            )
            
            await updateQueueSize()
        } catch {
            print("Failed to queue location sharing permission: \(error)")
        }
    }
    
    /**
     * Queues a friend removal operation.
     */
    func queueFriendRemoval(friendId: String) async {
        let data = FriendRemovalData(friendId: friendId)
        
        do {
            let jsonData = try JSONEncoder().encode(data)
            let jsonString = String(data: jsonData, encoding: .utf8) ?? ""
            
            try await cacheManager.queuePendingOperation(
                operationType: .friendRemoval,
                data: jsonString,
                priority: 3
            )
            
            await updateQueueSize()
        } catch {
            print("Failed to queue friend removal: \(error)")
        }
    }
    
    // MARK: - Queue Processing
    
    /**
     * Processes the offline queue when network becomes available.
     */
    func processQueue() async {
        guard !isProcessing else { return }
        
        await MainActor.run {
            isProcessing = true
        }
        
        defer {
            Task { @MainActor in
                isProcessing = false
                await updateQueueSize()
            }
        }
        
        do {
            let operations = try await cacheManager.getPendingOperations()
            
            for operation in operations {
                guard networkErrorHandler.isNetworkAvailable else {
                    break // Stop processing if network becomes unavailable
                }
                
                do {
                    try await processOperation(operation)
                    try await cacheManager.completePendingOperation(operation)
                } catch {
                    try await handleOperationFailure(operation, error: error)
                }
            }
        } catch {
            print("Failed to process queue: \(error)")
        }
    }
    
    /**
     * Processes a single operation.
     */
    private func processOperation(_ operation: PendingOperation) async throws {
        guard let operationType = OperationType(rawValue: operation.operationType ?? "") else {
            throw OfflineQueueError.invalidOperationType
        }
        
        switch operationType {
        case .locationUpdate:
            let data = try JSONDecoder().decode(LocationUpdateData.self, from: Data(operation.data?.utf8 ?? "".utf8))
            try await processLocationUpdate(data)
            
        case .friendRequest:
            let data = try JSONDecoder().decode(FriendRequestData.self, from: Data(operation.data?.utf8 ?? "".utf8))
            try await processFriendRequest(data)
            
        case .locationSharingPermission:
            let data = try JSONDecoder().decode(LocationSharingPermissionData.self, from: Data(operation.data?.utf8 ?? "".utf8))
            try await processLocationSharingPermission(data)
            
        case .friendRemoval:
            let data = try JSONDecoder().decode(FriendRemovalData.self, from: Data(operation.data?.utf8 ?? "".utf8))
            try await processFriendRemoval(data)
            
        case .profileUpdate:
            // Handle profile update if needed
            break
        }
    }
    
    /**
     * Processes a location update operation.
     */
    private func processLocationUpdate(_ data: LocationUpdateData) async throws {
        // This would typically call the actual location service
        // For now, we'll simulate the operation
        try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // In a real implementation, this would call:
        // try await locationService.updateLocation(data.userId, data.latitude, data.longitude, data.timestamp)
    }
    
    /**
     * Processes a friend request operation.
     */
    private func processFriendRequest(_ data: FriendRequestData) async throws {
        // This would typically call the actual friend service
        try await Task.sleep(nanoseconds: 200_000_000) // 0.2 seconds
        
        // In a real implementation, this would call:
        // try await friendService.sendFriendRequest(data.email)
    }
    
    /**
     * Processes a location sharing permission operation.
     */
    private func processLocationSharingPermission(_ data: LocationSharingPermissionData) async throws {
        try await Task.sleep(nanoseconds: 150_000_000) // 0.15 seconds
        
        // In a real implementation, this would call:
        // try await locationSharingService.updatePermission(data.friendId, data.enabled)
    }
    
    /**
     * Processes a friend removal operation.
     */
    private func processFriendRemoval(_ data: FriendRemovalData) async throws {
        try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // In a real implementation, this would call:
        // try await friendService.removeFriend(data.friendId)
    }
    
    /**
     * Handles operation failure by incrementing retry count or removing failed operations.
     */
    private func handleOperationFailure(_ operation: PendingOperation, error: Error) async throws {
        let maxRetries = 5
        
        if operation.retryCount < maxRetries {
            try await cacheManager.incrementRetryCount(operation)
        } else {
            // Remove operation after max retries
            try await cacheManager.completePendingOperation(operation)
        }
    }
    
    /**
     * Updates the queue size state.
     */
    @MainActor
    private func updateQueueSize() async {
        do {
            let stats = try await cacheManager.getCacheStats()
            queueSize = stats.pendingOperationCount
        } catch {
            print("Failed to update queue size: \(error)")
        }
    }
    
    /**
     * Starts automatic queue processing when network is available.
     */
    private func startAutoProcessing() {
        networkErrorHandler.$networkState
            .sink { [weak self] state in
                if state == .connected {
                    Task {
                        await self?.processQueue()
                    }
                }
            }
            .store(in: &cancellables)
    }
    
    /**
     * Clears all pending operations.
     */
    func clearQueue() async {
        do {
            let operations = try await cacheManager.getPendingOperations()
            for operation in operations {
                try await cacheManager.completePendingOperation(operation)
            }
            await updateQueueSize()
        } catch {
            print("Failed to clear queue: \(error)")
        }
    }
    
    /**
     * Gets queue statistics.
     */
    func getQueueStats() async -> QueueStats? {
        do {
            let operations = try await cacheManager.getPendingOperations()
            let operationsByType = Dictionary(grouping: operations) { $0.operationType ?? "" }
            
            return QueueStats(
                totalOperations: operations.count,
                locationUpdates: operationsByType[OperationType.locationUpdate.rawValue]?.count ?? 0,
                friendRequests: operationsByType[OperationType.friendRequest.rawValue]?.count ?? 0,
                permissionUpdates: operationsByType[OperationType.locationSharingPermission.rawValue]?.count ?? 0,
                friendRemovals: operationsByType[OperationType.friendRemoval.rawValue]?.count ?? 0,
                oldestOperationAge: operations.map { Date().timeIntervalSince1970 * 1000 - Double($0.createdAt) }.min() ?? 0
            )
        } catch {
            print("Failed to get queue stats: \(error)")
            return nil
        }
    }
}

// MARK: - Data Models

struct LocationUpdateData: Codable {
    let userId: String
    let latitude: Double
    let longitude: Double
    let timestamp: Int64
}

struct FriendRequestData: Codable {
    let email: String
}

struct LocationSharingPermissionData: Codable {
    let friendId: String
    let enabled: Bool
}

struct FriendRemovalData: Codable {
    let friendId: String
}

struct QueueStats {
    let totalOperations: Int
    let locationUpdates: Int
    let friendRequests: Int
    let permissionUpdates: Int
    let friendRemovals: Int
    let oldestOperationAge: Double
}

// MARK: - Errors

enum OfflineQueueError: Error {
    case invalidOperationType
    case serializationError
    case networkUnavailable
}