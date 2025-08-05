import Foundation

/**
 * Service interface for managing friend request operations.
 * 
 * This service provides high-level operations for friend request management,
 * abstracting the complexity of repository interactions and providing
 * business logic for friend request workflows.
 */
protocol FriendRequestService {
    
    /**
     * Searches for a user by email address.
     * 
     * @param email The email address to search for
     * @return Result containing the user if found, null if not found, or error if failed
     */
    func searchUser(byEmail email: String) async -> Result<User?, Error>
    
    /**
     * Sends a friend request to a user identified by email.
     * 
     * @param email The email address of the user to send request to
     * @return Result indicating success or failure with error details
     */
    func sendFriendRequest(email: String) async -> Result<Void, Error>
    
    /**
     * Retrieves all pending friend requests for the current user.
     * 
     * @return Result containing list of pending friend requests or error if failed
     */
    func getPendingFriendRequests() async -> Result<[FriendRequest], Error>
    
    /**
     * Accepts a friend request and creates the friendship.
     * 
     * @param requestId The ID of the friend request to accept
     * @return Result indicating success or failure with error details
     */
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error>
    
    /**
     * Rejects a friend request.
     * 
     * @param requestId The ID of the friend request to reject
     * @return Result indicating success or failure with error details
     */
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error>
    
    /**
     * Validates if a friend request can be sent to the specified email.
     * 
     * @param email The email address to validate
     * @return Result containing validation result or error if failed
     */
    func validateFriendRequest(email: String) async -> Result<FriendRequestValidation, Error>
}

/**
 * Represents the validation result for a friend request.
 */
struct FriendRequestValidation {
    let isValid: Bool
    let reason: String?
    
    init(isValid: Bool, reason: String? = nil) {
        self.isValid = isValid
        self.reason = reason
    }
}