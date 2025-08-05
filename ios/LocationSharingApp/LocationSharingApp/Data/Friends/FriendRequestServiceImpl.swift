import Foundation

/**
 * Implementation of FriendRequestService that coordinates friend request operations.
 * 
 * This service provides business logic for friend request management,
 * including validation, error handling, and coordination between
 * repository operations.
 */
class FriendRequestServiceImpl: FriendRequestService {
    
    private let friendsRepository: FriendsRepository
    private let sessionManager: SessionManager
    
    init(friendsRepository: FriendsRepository = FriendsRepositoryImpl(), sessionManager: SessionManager = SessionManager()) {
        self.friendsRepository = friendsRepository
        self.sessionManager = sessionManager
    }
    
    func searchUser(byEmail email: String) async -> Result<User?, Error> {
        guard isValidEmail(email) else {
            return .failure(FriendsError.invalidRequest)
        }
        
        return await friendsRepository.searchUser(byEmail: email)
    }
    
    func sendFriendRequest(email: String) async -> Result<Void, Error> {
        // Validate the request first
        let validationResult = await validateFriendRequest(email: email)
        
        switch validationResult {
        case .success(let validation):
            guard validation.isValid else {
                if let reason = validation.reason {
                    switch reason {
                    case "Invalid email":
                        return .failure(FriendsError.invalidRequest)
                    case "Cannot request self":
                        return .failure(FriendsError.invalidRequest)
                    case "User not found":
                        return .failure(FriendsError.userNotFound)
                    case "Friendship already exists":
                        return .failure(FriendsError.friendshipAlreadyExists)
                    case "Request already sent":
                        return .failure(FriendsError.friendRequestAlreadySent)
                    default:
                        return .failure(FriendsError.invalidRequest)
                    }
                }
                return .failure(FriendsError.invalidRequest)
            }
            
            // Send the friend request
            return await friendsRepository.sendFriendRequest(email: email)
            
        case .failure(let error):
            return .failure(error)
        }
    }
    
    func getPendingFriendRequests() async -> Result<[FriendRequest], Error> {
        return await friendsRepository.getFriendRequests()
    }
    
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error> {
        guard !requestId.isEmpty else {
            return .failure(FriendsError.invalidRequest)
        }
        
        return await friendsRepository.acceptFriendRequest(requestId: requestId)
    }
    
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error> {
        guard !requestId.isEmpty else {
            return .failure(FriendsError.invalidRequest)
        }
        
        return await friendsRepository.rejectFriendRequest(requestId: requestId)
    }
    
    func validateFriendRequest(email: String) async -> Result<FriendRequestValidation, Error> {
        // Check email format
        guard isValidEmail(email) else {
            return .success(FriendRequestValidation(isValid: false, reason: "Invalid email"))
        }
        
        // Check if trying to send request to self
        if let currentUser = await sessionManager.getCurrentUser(),
           currentUser.email == email {
            return .success(FriendRequestValidation(isValid: false, reason: "Cannot request self"))
        }
        
        // Check if user exists
        let userResult = await friendsRepository.searchUser(byEmail: email)
        switch userResult {
        case .success(let user):
            if user == nil {
                return .success(FriendRequestValidation(isValid: false, reason: "User not found"))
            }
            
            // Additional validation would be done in the repository
            // (checking existing friendship, existing requests, etc.)
            return .success(FriendRequestValidation(isValid: true))
            
        case .failure(let error):
            return .failure(error)
        }
    }
    
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return !email.isEmpty && emailPredicate.evaluate(with: email)
    }
}