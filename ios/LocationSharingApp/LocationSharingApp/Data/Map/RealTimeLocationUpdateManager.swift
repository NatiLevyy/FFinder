import Foundation
import Combine
import os.log

/**
 * Manages real-time location updates for friend markers on the map.
 * 
 * This class handles the continuous updating of friend locations,
 * batching updates for efficiency, and managing update intervals.
 */
class RealTimeLocationUpdateManager: ObservableObject {
    
    private let locationMarkerManager: LocationMarkerManager
    private var cancellables = Set<AnyCancellable>()
    private let updateSubject = PassthroughSubject<FriendLocationUpdate, Never>()
    
    @Published private(set) var isUpdating = false
    private var updateTasks: [String: Task<Void, Never>] = [:]
    
    private static let logger = Logger(subsystem: "com.locationsharing.app", category: "RealTimeLocationUpdateManager")
    private static let updateBatchDelayMs: UInt64 = 1000 // 1 second
    private static let maxUpdateFrequencyMs: Int64 = 5000 // 5 seconds
    
    /**
     * Represents a friend location update event.
     */
    struct FriendLocationUpdate {
        let friendId: String
        let friendName: String
        let location: Location
        let timestamp: Int64
        
        init(friendId: String, friendName: String, location: Location, timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
            self.friendId = friendId
            self.friendName = friendName
            self.location = location
            self.timestamp = timestamp
        }
    }
    
    /**
     * Initializes the real-time location update manager.
     * 
     * - Parameter locationMarkerManager: The LocationMarkerManager instance to use for updates
     */
    init(locationMarkerManager: LocationMarkerManager) {
        self.locationMarkerManager = locationMarkerManager
        setupUpdateProcessing()
    }
    
    /**
     * Starts real-time location updates processing.
     */
    func startRealTimeUpdates() {
        guard !isUpdating else {
            Self.logger.debug("Real-time updates already started")
            return
        }
        
        isUpdating = true
        Self.logger.debug("Started real-time location updates")
    }
    
    /**
     * Stops real-time location updates processing.
     */
    func stopRealTimeUpdates() {
        isUpdating = false
        
        // Cancel all pending update tasks
        for (_, task) in updateTasks {
            task.cancel()
        }
        updateTasks.removeAll()
        
        Self.logger.debug("Stopped real-time location updates")
    }
    
    /**
     * Submits a friend location update for processing.
     * 
     * - Parameters:
     *   - friendId: Unique identifier for the friend
     *   - friendName: Display name for the friend
     *   - location: The friend's updated location
     */
    func submitLocationUpdate(friendId: String, friendName: String, location: Location) {
        guard isUpdating else {
            Self.logger.warning("Cannot submit location update: real-time updates not started")
            return
        }
        
        let update = FriendLocationUpdate(friendId: friendId, friendName: friendName, location: location)
        updateSubject.send(update)
    }
    
    /**
     * Submits multiple friend location updates in batch.
     * 
     * - Parameter updates: Dictionary of friend IDs to (friendName, location) pairs
     */
    func submitBatchLocationUpdates(_ updates: [String: (String, Location)]) {
        guard isUpdating else {
            Self.logger.warning("Cannot submit batch updates: real-time updates not started")
            return
        }
        
        for (friendId, friendData) in updates {
            let (friendName, location) = friendData
            let update = FriendLocationUpdate(friendId: friendId, friendName: friendName, location: location)
            updateSubject.send(update)
        }
        
        Self.logger.debug("Submitted batch location updates for \(updates.count) friends")
    }
    
    /**
     * Gets the current update status for all friends.
     * 
     * - Returns: Dictionary of friend IDs to their last update timestamps
     */
    func getUpdateStatus() -> [String: Int64] {
        return locationMarkerManager.getAllMarkerStates().mapValues { $0.lastUpdated }
    }
    
    /**
     * Forces an immediate update for a specific friend.
     * 
     * - Parameters:
     *   - friendId: Unique identifier for the friend
     *   - friendName: Display name for the friend
     *   - location: The friend's location
     */
    func forceImmediateUpdate(friendId: String, friendName: String, location: Location) {
        Task {
            do {
                locationMarkerManager.updateFriendMarker(
                    friendId: friendId,
                    friendName: friendName,
                    location: location,
                    forceUpdate: true
                )
                
                Self.logger.debug("Forced immediate update for friend: \(friendName)")
            } catch {
                Self.logger.error("Error forcing immediate update for friend \(friendId): \(error.localizedDescription)")
            }
        }
    }
    
    /**
     * Sets up the update processing pipeline.
     */
    private func setupUpdateProcessing() {
        updateSubject
            .buffer(size: 50, prefetch: .keepFull, whenFull: .dropOldest)
            .sink { [weak self] update in
                self?.processLocationUpdate(update)
            }
            .store(in: &cancellables)
    }
    
    /**
     * Processes a single location update with rate limiting.
     * 
     * - Parameter update: The location update to process
     */
    private func processLocationUpdate(_ update: FriendLocationUpdate) {
        // Cancel any existing update task for this friend
        updateTasks[update.friendId]?.cancel()
        
        // Create new update task with rate limiting
        let updateTask = Task {
            do {
                // Apply rate limiting
                try await Task.sleep(nanoseconds: Self.updateBatchDelayMs * 1_000_000)
                
                // Check if task was cancelled
                try Task.checkCancellation()
                
                // Update marker on map
                locationMarkerManager.updateFriendMarker(
                    friendId: update.friendId,
                    friendName: update.friendName,
                    location: update.location
                )
                
                Self.logger.debug("Processed location update for friend: \(update.friendName)")
            } catch is CancellationError {
                // Task was cancelled, this is expected
                Self.logger.debug("Location update cancelled for friend: \(update.friendId)")
            } catch {
                Self.logger.error("Error processing location update for friend \(update.friendId): \(error.localizedDescription)")
            }
            
            // Remove completed task
            updateTasks.removeValue(forKey: update.friendId)
        }
        
        updateTasks[update.friendId] = updateTask
    }
    
    /**
     * Cleans up resources when the manager is no longer needed.
     */
    func cleanup() {
        stopRealTimeUpdates()
        cancellables.removeAll()
        Self.logger.debug("Cleaned up real-time location update manager")
    }
    
    deinit {
        cleanup()
    }
}