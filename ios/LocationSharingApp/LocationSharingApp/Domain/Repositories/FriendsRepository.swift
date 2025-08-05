import Foundation
import Combine

/**
 * Repository interface for managing friend relationships and requests.
 * 
 * This repository provides data access methods for friend management,
 * including friend requests, friendships, and location sharing permissions.
 */
protocol FriendsRepository {
    
    /**
     * Retrieves all friends for the current user.
     * 
     * @return Result containing list of friends or error if failed
     */
    func getFriends() async -> Result<[Friend], Error>
    
    /**
     * Retrieves a specific friend by ID.
     * 
     * @param friendId The ID of the friend to retrieve
     * @return The friend if found, nil otherwise
     */
    func getFriend(friendId: String) async throws -> Friend?
    
    /**
     * Retrieves all friends as a Publisher for real-time updates.
     * 
     * @return Publisher of friends list
     */
    func getFriendsPublisher() -> AnyPublisher<[Friend], Error>
    
    /**
     * Sends a friend request to a user identified by email.
     * 
     * @param email The email address of the user to send request to
     * @return Result indicating success or failure with error details
     */
    func sendFriendRequest(email: String) async -> Result<Void, Error>
    
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
     * Removes a friend and stops location sharing.
     * 
     * @param friendId The ID of the friend to remove
     * @return Result indicating success or failure with error details
     */
    func removeFriend(friendId: String) async -> Result<Void, Error>
    
    /**
     * Updates location sharing permission with a specific friend.
     * 
     * @param friendId The ID of the friend
     * @param permission The location sharing permission to set
     * @return Result indicating success or failure with error details
     */
    func updateLocationSharingPermission(friendId: String, permission: LocationSharingPermission) async -> Result<Void, Error>
    
    /**
     * Updates location sharing permission with a specific friend (legacy method).
     * 
     * @param friendId The ID of the friend
     * @param enabled Whether location sharing should be enabled
     * @return Result indicating success or failure with error details
     */
    func updateLocationSharingPermission(friendId: String, enabled: Bool) async -> Result<Void, Error>
    
    /**
     * Retrieves all pending friend requests for the current user.
     * 
     * @return Result containing list of pending friend requests or error if failed
     */
    func getFriendRequests() async -> Result<[FriendRequest], Error>
    
    /**
     * Searches for a user by email address.
     * 
     * @param email The email address to search for
     * @return Result containing the user if found, null if not found, or error if failed
     */
    func searchUser(byEmail email: String) async -> Result<User?, Error>
}