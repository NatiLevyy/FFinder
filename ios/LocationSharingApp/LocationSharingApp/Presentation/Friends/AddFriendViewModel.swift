import Foundation

/**
 * ViewModel for adding new friends functionality.
 * 
 * This ViewModel handles user search by email and friend request sending operations.
 */
@MainActor
class AddFriendViewModel: ObservableObject {
    @Published var foundUser: User?
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var requestSent = false
    @Published var userNotFound = false
    
    private let friendRequestService: FriendRequestService
    
    init(friendRequestService: FriendRequestService = FriendRequestServiceImpl()) {
        self.friendRequestService = friendRequestService
    }
    
    func searchUser(email: String) {
        isLoading = true
        userNotFound = false
        
        Task {
            let result = await friendRequestService.searchUser(byEmail: email)
            
            switch result {
            case .success(let user):
                if let user = user {
                    self.foundUser = user
                    self.userNotFound = false
                } else {
                    self.foundUser = nil
                    self.userNotFound = true
                }
                self.isLoading = false
            case .failure(let error):
                self.errorMessage = error.localizedDescription
                self.foundUser = nil
                self.isLoading = false
            }
        }
    }
    
    func sendFriendRequest(email: String) {
        isLoading = true
        
        Task {
            let result = await friendRequestService.sendFriendRequest(email: email)
            
            switch result {
            case .success:
                self.requestSent = true
                self.isLoading = false
            case .failure(let error):
                let errorMessage: String
                if let friendsError = error as? FriendsError {
                    switch friendsError {
                    case .userNotFound:
                        errorMessage = "User not found with that email"
                    case .friendshipAlreadyExists:
                        errorMessage = "You are already friends with this user"
                    case .friendRequestAlreadySent:
                        errorMessage = "Friend request already sent to this user"
                    case .invalidRequest:
                        errorMessage = "Invalid friend request"
                    }
                } else {
                    errorMessage = error.localizedDescription
                }
                
                self.errorMessage = errorMessage
                self.isLoading = false
            }
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
}