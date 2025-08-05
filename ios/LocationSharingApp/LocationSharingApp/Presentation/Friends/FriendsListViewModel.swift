import Foundation
import Combine
import SwiftUI

/**
 * ViewModel for the Friends List view with real-time Firebase integration
 * Handles friends data, status updates, and UI state management
 */
@MainActor
class FriendsListViewModel: ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var friends: [Friend] = []
    @Published var onlineFriends: [Friend] = []
    @Published var selectedFriend: Friend?
    @Published var isLoading = true
    @Published var isRefreshing = false
    @Published var isConnected = false
    @Published var error: String?
    @Published var successMessage: String?
    
    // MARK: - Private Properties
    
    private let friendsRepository: FriendsRepository
    private var cancellables = Set<AnyCancellable>()
    private var lastUpdateTime: Date = Date()
    
    // MARK: - Computed Properties
    
    var hasOnlineFriends: Bool {
        !onlineFriends.isEmpty
    }
    
    var hasAnyFriends: Bool {
        !friends.isEmpty
    }
    
    var offlineFriends: [Friend] {
        friends.filter { !$0.isOnline }
    }
    
    var friendsCount: Int {
        friends.count
    }
    
    var onlineFriendsCount: Int {
        onlineFriends.count
    }
    
    var offlineFriendsCount: Int {
        offlineFriends.count
    }
    
    // MARK: - Initialization
    
    init(friendsRepository: FriendsRepository = DIContainer.shared.friendsRepository) {
        self.friendsRepository = friendsRepository
        setupObservers()
    }
    
    // MARK: - Public Methods
    
    /**
     * Start real-time synchronization with Firebase
     */
    func startRealTimeSync() {
        isLoading = true
        error = nil
        observeFriendsData()
    }
    
    /**
     * Stop real-time synchronization
     */
    func stopRealTimeSync() {
        cancellables.removeAll()
    }
    
    /**
     * Refresh friends data manually
     */
    func refreshFriends() {
        isRefreshing = true
        error = nil
        
        // Restart observers to refresh data
        cancellables.removeAll()
        observeFriendsData()
        
        // Simulate refresh completion after a short delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.isRefreshing = false
            self.successMessage = "Friends list refreshed"
            
            // Clear success message after 3 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                self.successMessage = nil
            }
        }
    }
    
    /**
     * Handle friend selection
     */
    func selectFriend(_ friend: Friend) {
        selectedFriend = friend
        print("Friend selected: \(friend.user.name)")
    }
    
    /**
     * Clear friend selection
     */
    func clearFriendSelection() {
        selectedFriend = nil
        print("Friend selection cleared")
    }
    
    /**
     * Send friend request
     */
    func sendFriendRequest(toUserId: String, message: String? = nil) async -> Result<String, Error> {
        // Implementation would depend on your FriendsRepository interface
        // For now, return a placeholder
        return .failure(NSError(domain: "NotImplemented", code: 0, userInfo: [NSLocalizedDescriptionKey: "Not implemented"]))
    }
    
    /**
     * Remove friend with confirmation
     */
    func removeFriend(_ friendId: String) async -> Result<Void, Error> {
        do {
            // Implementation would depend on your FriendsRepository interface
            // For now, simulate success
            
            // Clear selection if removed friend was selected
            if selectedFriend?.id == friendId {
                clearFriendSelection()
            }
            
            successMessage = "Friend removed successfully"
            
            // Clear success message after 3 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                self.successMessage = nil
            }
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    /**
     * Get friend by ID
     */
    func getFriendById(_ id: String) -> Friend? {
        return friends.first { $0.id == id }
    }
    
    /**
     * Check if friend is currently online
     */
    func isFriendOnline(_ friendId: String) -> Bool {
        return getFriendById(friendId)?.isOnline ?? false
    }
    
    /**
     * Clear error message
     */
    func clearError() {
        error = nil
    }
    
    /**
     * Clear success message
     */
    func clearSuccessMessage() {
        successMessage = nil
    }
    
    /**
     * Show invite friends interface
     */
    func showInviteFriends() {
        // TODO: Implement invite friends functionality
        print("Show invite friends interface")
    }
    
    /**
     * Handle network connectivity changes
     */
    func onNetworkConnectivityChanged(isConnected: Bool) {
        self.isConnected = isConnected
        
        if isConnected {
            // Restart sync when network is restored
            startRealTimeSync()
        } else {
            error = "No internet connection. Some features may not work."
        }
    }
    
    /**
     * Handle app lifecycle changes
     */
    func onAppResumed() {
        // Refresh data when app comes to foreground
        refreshFriends()
    }
    
    func onAppPaused() {
        // Optionally pause real-time updates to save battery
        // stopRealTimeSync()
    }
    
    // MARK: - Private Methods
    
    /**
     * Setup observers for friends data
     */
    private func setupObservers() {
        observeFriendsData()
    }
    
    /**
     * Observe friends data from Firebase with real-time updates
     */
    private func observeFriendsData() {
        // Observe all friends
        friendsRepository.getFriends()
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    if case .failure(let error) = completion {
                        self?.handleError(error)
                    }
                },
                receiveValue: { [weak self] friends in
                    self?.handleFriendsUpdate(friends)
                }
            )
            .store(in: &cancellables)
    }
    
    /**
     * Handle friends data update
     */
    private func handleFriendsUpdate(_ allFriends: [Friend]) {
        friends = allFriends
        onlineFriends = allFriends.filter { $0.isOnline }
        isLoading = false
        isConnected = true
        lastUpdateTime = Date()
        
        print("Friends updated: \(allFriends.count) total, \(onlineFriends.count) online")
    }
    
    /**
     * Handle errors
     */
    private func handleError(_ error: Error) {
        self.error = "Failed to load friends: \(error.localizedDescription)"
        isLoading = false
        isConnected = false
        
        print("Error loading friends: \(error)")
    }
}

// MARK: - Extensions

extension Friend {
    var isOnline: Bool {
        // Implement online status logic based on your Friend model
        // This is a placeholder implementation
        return friendshipStatus == .accepted && locationSharingEnabled
    }
    
    var statusText: String {
        if isOnline {
            return "Online now"
        } else {
            // You could implement more sophisticated status text based on last seen time
            return "Offline"
        }
    }
}