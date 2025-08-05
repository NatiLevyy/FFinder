import Foundation

/**
 * ViewModel for managing friend requests UI state and operations.
 * 
 * This ViewModel handles loading, accepting, and rejecting friend requests.
 */
@MainActor
class FriendRequestsViewModel: ObservableObject {
    @Published var friendRequests: [FriendRequest] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showSuccessMessage = false
    @Published var successMessage = ""
    
    private let friendRequestService: FriendRequestService
    
    init(friendRequestService: FriendRequestService = FriendRequestServiceImpl()) {
        self.friendRequestService = friendRequestService
    }
    
    func loadFriendRequests() {
        isLoading = true
        
        Task {
            let result = await friendRequestService.getPendingFriendRequests()
            
            switch result {
            case .success(let requests):
                self.friendRequests = requests
                self.isLoading = false
            case .failure(let error):
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    func acceptFriendRequest(requestId: String) {
        Task {
            let result = await friendRequestService.acceptFriendRequest(requestId: requestId)
            
            switch result {
            case .success:
                self.successMessage = "Friend request accepted"
                self.showSuccessMessage = true
                // Reload requests to update the list
                self.loadFriendRequests()
            case .failure(let error):
                self.errorMessage = error.localizedDescription
            }
        }
    }
    
    func rejectFriendRequest(requestId: String) {
        Task {
            let result = await friendRequestService.rejectFriendRequest(requestId: requestId)
            
            switch result {
            case .success:
                self.successMessage = "Friend request rejected"
                self.showSuccessMessage = true
                // Reload requests to update the list
                self.loadFriendRequests()
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