import Foundation

protocol FriendsRepository {
    func getFriends() async -> Result<[Friend], Error>
    func sendFriendRequest(email: String) async -> Result<Void, Error>
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error>
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error>
    func removeFriend(friendId: String) async -> Result<Void, Error>
    func updateLocationSharingPermission(friendId: String, enabled: Bool) async -> Result<Void, Error>
    func getFriendRequests() async -> Result<[FriendRequest], Error>
    func searchUser(byEmail email: String) async -> Result<User?, Error>
}