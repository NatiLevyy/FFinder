import Foundation
import Combine

/**
 * ViewModel for managing friends list UI state and operations.
 * 
 * This ViewModel handles friends list display, search functionality,
 * location sharing controls, and friend removal operations.
 */
@MainActor
class FriendsViewModel: ObservableObject {
    @Published var friends: [Friend] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showSuccessMessage = false
    @Published var successMessage = ""
    
    private let friendsRepository: FriendsRepository
    private let locationSharingManager: LocationSharingManager
    private var allFriends: [Friend] = []
    private var cancellables = Set<AnyCancellable>()
    
    init(friendsRepository: FriendsRepository = FriendsRepositoryImpl()) {
        self.friendsRepository = friendsRepository
        self.locationSharingManager = LocationSharingManager(friendsRepository: friendsRepository)
    }
    
    func loadFriends() {
        isLoading = true
        
        Task {
            let result = await friendsRepository.getFriends()
            
            switch result {
            case .success(let friends):
                self.allFriends = friends
                self.friends = friends
                self.isLoading = false
            case .failure(let error):
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    func searchFriends(query: String) {
        if query.isEmpty {
            friends = allFriends
            return
        }
        
        friends = allFriends.filter { friend in
            friend.user.displayName.localizedCaseInsensitiveContains(query) ||
            friend.user.email.localizedCaseInsensitiveContains(query)
        }
    }
    
    func updateLocationSharing(friendId: String, enabled: Bool) {
        Task {
            let result = await friendsRepository.updateLocationSharingPermission(friendId: friendId, enabled: enabled)
            
            switch result {
            case .success:
                self.successMessage = "Location sharing updated"
                self.showSuccessMessage = true
                // Reload friends to get updated state
                self.loadFriends()
            case .failure(let error):
                self.errorMessage = error.localizedDescription
            }
        }
    }
    
    func requestLocationSharing(friendId: String) {
        Task {
            let result = await locationSharingManager.requestLocationSharingPermission(friendId: friendId)
            
            switch result {
            case .success:
                self.successMessage = "Location sharing request sent"
                self.showSuccessMessage = true
                // Reload friends to get updated state
                self.loadFriends()
            case .failure(let error):
                self.errorMessage = error.localizedDescription
            }
        }
    }
    
    func removeFriend(friendId: String) {
        Task {
            let result = await friendsRepository.removeFriend(friendId: friendId)
            
            switch result {
            case .success:
                self.successMessage = "Friend removed"
                self.showSuccessMessage = true
                // Reload friends to get updated list
                self.loadFriends()
            case .failure(let error):
                self.errorMessage = error.localizedDescription
            }
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    func clearSuccessMessage() {
        showSuccessMessage = false
        successMessage = ""
    }
}