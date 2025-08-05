import Foundation
import FirebaseFirestore
import Combine

/**
 * Implementation of FriendsRepository using Firebase Firestore.
 * 
 * This repository manages friend relationships, friend requests, and location sharing permissions
 * using Firestore as the backend data store.
 */
class FriendsRepositoryImpl: FriendsRepository {
    
    private let firestore = Firestore.firestore()
    
    private struct Collections {
        static let users = "users"
        static let friendships = "friendships"
        static let friendRequests = "friend_requests"
        static let locationPermissions = "location_permissions"
    }
    
    func getFriends() async -> Result<[Friend], Error> {
        do {
            let currentUserId = getCurrentUserId()
            let snapshot = try await firestore.collection(Collections.friendships)
                .whereField("userId", isEqualTo: currentUserId)
                .whereField("status", isEqualTo: FriendshipStatus.accepted.rawValue)
                .getDocuments()
            
            var friends: [Friend] = []
            
            for document in snapshot.documents {
                guard let friendId = document.data()["friendId"] as? String,
                      let user = try await getUserById(friendId),
                      let locationSharingEnabled = document.data()["locationSharingEnabled"] as? Bool else {
                    continue
                }
                
                let locationSharingPermissionString = document.data()["locationSharingPermission"] as? String ?? LocationSharingPermission.none.rawValue
                let locationSharingPermission = LocationSharingPermission(rawValue: locationSharingPermissionString) ?? .none
                
                let friend = Friend(
                    id: document.documentID,
                    user: user,
                    friendshipStatus: .accepted,
                    locationSharingEnabled: locationSharingEnabled,
                    lastKnownLocation: nil, // Will be populated from real-time location updates
                    locationSharingPermission: locationSharingPermission
                )
                
                friends.append(friend)
            }
            
            return .success(friends)
        } catch {
            return .failure(error)
        }
    }
    
    func sendFriendRequest(email: String) async -> Result<Void, Error> {
        do {
            let currentUserId = getCurrentUserId()
            
            // Find target user by email
            guard let targetUser = try await searchUserByEmailInternal(email) else {
                return .failure(FriendsError.userNotFound)
            }
            
            // Check if friendship already exists
            if try await checkExistingFriendship(userId1: currentUserId, userId2: targetUser.id) != nil {
                return .failure(FriendsError.friendshipAlreadyExists)
            }
            
            // Check if friend request already exists
            if try await checkExistingFriendRequest(fromUserId: currentUserId, toUserId: targetUser.id) != nil {
                return .failure(FriendsError.friendRequestAlreadySent)
            }
            
            let friendRequestData: [String: Any] = [
                "fromUserId": currentUserId,
                "toUserId": targetUser.id,
                "status": RequestStatus.pending.rawValue,
                "createdAt": Int64(Date().timeIntervalSince1970 * 1000),
                "respondedAt": NSNull()
            ]
            
            try await firestore.collection(Collections.friendRequests).addDocument(data: friendRequestData)
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error> {
        do {
            let currentUserId = getCurrentUserId()
            
            // Update friend request status
            try await firestore.collection(Collections.friendRequests)
                .document(requestId)
                .updateData([
                    "status": RequestStatus.accepted.rawValue,
                    "respondedAt": Int64(Date().timeIntervalSince1970 * 1000)
                ])
            
            // Get the friend request to create friendship
            let requestDoc = try await firestore.collection(Collections.friendRequests)
                .document(requestId)
                .getDocument()
            
            guard let requestData = requestDoc.data(),
                  let fromUserId = requestData["fromUserId"] as? String,
                  let toUserId = requestData["toUserId"] as? String else {
                throw FriendsError.invalidRequest
            }
            
            // Create friendship for both users
            let friendship1Data: [String: Any] = [
                "userId": fromUserId,
                "friendId": toUserId,
                "status": FriendshipStatus.accepted.rawValue,
                "locationSharingEnabled": false,
                "locationSharingPermission": LocationSharingPermission.none.rawValue,
                "createdAt": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            let friendship2Data: [String: Any] = [
                "userId": toUserId,
                "friendId": fromUserId,
                "status": FriendshipStatus.accepted.rawValue,
                "locationSharingEnabled": false,
                "locationSharingPermission": LocationSharingPermission.none.rawValue,
                "createdAt": Int64(Date().timeIntervalSince1970 * 1000)
            ]
            
            try await firestore.collection(Collections.friendships).addDocument(data: friendship1Data)
            try await firestore.collection(Collections.friendships).addDocument(data: friendship2Data)
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error> {
        do {
            try await firestore.collection(Collections.friendRequests)
                .document(requestId)
                .updateData([
                    "status": RequestStatus.rejected.rawValue,
                    "respondedAt": Int64(Date().timeIntervalSince1970 * 1000)
                ])
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func removeFriend(friendId: String) async -> Result<Void, Error> {
        do {
            let currentUserId = getCurrentUserId()
            
            // Remove both friendship documents
            let friendshipsSnapshot = try await firestore.collection(Collections.friendships)
                .whereField("userId", in: [currentUserId, friendId])
                .whereField("friendId", in: [currentUserId, friendId])
                .getDocuments()
            
            for document in friendshipsSnapshot.documents {
                try await firestore.collection(Collections.friendships)
                    .document(document.documentID)
                    .delete()
            }
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func updateLocationSharingPermission(friendId: String, enabled: Bool) async -> Result<Void, Error> {
        do {
            let currentUserId = getCurrentUserId()
            
            let friendshipSnapshot = try await firestore.collection(Collections.friendships)
                .whereField("userId", isEqualTo: currentUserId)
                .whereField("friendId", isEqualTo: friendId)
                .getDocuments()
            
            if let friendshipDoc = friendshipSnapshot.documents.first {
                try await firestore.collection(Collections.friendships)
                    .document(friendshipDoc.documentID)
                    .updateData([
                        "locationSharingEnabled": enabled,
                        "locationSharingPermission": enabled ? LocationSharingPermission.granted.rawValue : LocationSharingPermission.denied.rawValue
                    ])
            }
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func getFriendRequests() async -> Result<[FriendRequest], Error> {
        do {
            let currentUserId = getCurrentUserId()
            let snapshot = try await firestore.collection(Collections.friendRequests)
                .whereField("toUserId", isEqualTo: currentUserId)
                .whereField("status", isEqualTo: RequestStatus.pending.rawValue)
                .order(by: "createdAt", descending: true)
                .getDocuments()
            
            let friendRequests = snapshot.documents.compactMap { document -> FriendRequest? in
                let data = document.data()
                guard let fromUserId = data["fromUserId"] as? String,
                      let toUserId = data["toUserId"] as? String,
                      let statusString = data["status"] as? String,
                      let status = RequestStatus(rawValue: statusString),
                      let createdAt = data["createdAt"] as? Int64 else {
                    return nil
                }
                
                let respondedAt = data["respondedAt"] as? Int64
                
                return FriendRequest(
                    id: document.documentID,
                    fromUserId: fromUserId,
                    toUserId: toUserId,
                    status: status,
                    createdAt: createdAt,
                    respondedAt: respondedAt
                )
            }
            
            return .success(friendRequests)
        } catch {
            return .failure(error)
        }
    }
    
    func getFriend(friendId: String) async throws -> Friend? {
        let currentUserId = getCurrentUserId()
        let snapshot = try await firestore.collection(Collections.friendships)
            .whereField("userId", isEqualTo: currentUserId)
            .whereField("friendId", isEqualTo: friendId)
            .whereField("status", isEqualTo: FriendshipStatus.accepted.rawValue)
            .limit(to: 1)
            .getDocuments()
        
        guard let document = snapshot.documents.first,
              let user = try await getUserById(friendId),
              let locationSharingEnabled = document.data()["locationSharingEnabled"] as? Bool else {
            return nil
        }
        
        let locationSharingPermissionString = document.data()["locationSharingPermission"] as? String ?? LocationSharingPermission.none.rawValue
        let locationSharingPermission = LocationSharingPermission(rawValue: locationSharingPermissionString) ?? .none
        
        return Friend(
            id: document.documentID,
            user: user,
            friendshipStatus: .accepted,
            locationSharingEnabled: locationSharingEnabled,
            lastKnownLocation: nil,
            locationSharingPermission: locationSharingPermission
        )
    }
    
    func getFriendsPublisher() -> AnyPublisher<[Friend], Error> {
        // For now, return a simple publisher that emits the current friends list
        // In a real implementation, this would use Firestore's real-time listeners
        return Future { promise in
            Task {
                let result = await self.getFriends()
                switch result {
                case .success(let friends):
                    promise(.success(friends))
                case .failure(let error):
                    promise(.failure(error))
                }
            }
        }
        .eraseToAnyPublisher()
    }
    
    func updateLocationSharingPermission(friendId: String, permission: LocationSharingPermission) async -> Result<Void, Error> {
        do {
            let currentUserId = getCurrentUserId()
            
            let friendshipSnapshot = try await firestore.collection(Collections.friendships)
                .whereField("userId", isEqualTo: currentUserId)
                .whereField("friendId", isEqualTo: friendId)
                .getDocuments()
            
            if let friendshipDoc = friendshipSnapshot.documents.first {
                try await firestore.collection(Collections.friendships)
                    .document(friendshipDoc.documentID)
                    .updateData([
                        "locationSharingEnabled": (permission == .granted),
                        "locationSharingPermission": permission.rawValue
                    ])
            }
            
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func searchUser(byEmail email: String) async -> Result<User?, Error> {
        do {
            let user = try await searchUserByEmailInternal(email)
            return .success(user)
        } catch {
            return .failure(error)
        }
    }
    
    // MARK: - Private Helper Methods
    
    private func searchUserByEmailInternal(_ email: String) async throws -> User? {
        let snapshot = try await firestore.collection(Collections.users)
            .whereField("email", isEqualTo: email)
            .limit(to: 1)
            .getDocuments()
        
        guard let document = snapshot.documents.first else {
            return nil
        }
        
        let data = document.data()
        return User(
            id: document.documentID,
            email: data["email"] as? String ?? "",
            displayName: data["displayName"] as? String ?? "",
            profileImageUrl: data["profileImageUrl"] as? String,
            createdAt: data["createdAt"] as? Int64 ?? 0,
            lastActiveAt: data["lastActiveAt"] as? Int64 ?? 0
        )
    }
    
    private func getUserById(_ userId: String) async throws -> User? {
        let document = try await firestore.collection(Collections.users)
            .document(userId)
            .getDocument()
        
        guard document.exists, let data = document.data() else {
            return nil
        }
        
        return User(
            id: document.documentID,
            email: data["email"] as? String ?? "",
            displayName: data["displayName"] as? String ?? "",
            profileImageUrl: data["profileImageUrl"] as? String,
            createdAt: data["createdAt"] as? Int64 ?? 0,
            lastActiveAt: data["lastActiveAt"] as? Int64 ?? 0
        )
    }
    
    private func checkExistingFriendship(userId1: String, userId2: String) async throws -> String? {
        let snapshot = try await firestore.collection(Collections.friendships)
            .whereField("userId", isEqualTo: userId1)
            .whereField("friendId", isEqualTo: userId2)
            .limit(to: 1)
            .getDocuments()
        
        return snapshot.documents.first?.documentID
    }
    
    private func checkExistingFriendRequest(fromUserId: String, toUserId: String) async throws -> String? {
        let snapshot = try await firestore.collection(Collections.friendRequests)
            .whereField("fromUserId", isEqualTo: fromUserId)
            .whereField("toUserId", isEqualTo: toUserId)
            .whereField("status", isEqualTo: RequestStatus.pending.rawValue)
            .limit(to: 1)
            .getDocuments()
        
        return snapshot.documents.first?.documentID
    }
    
    private func getCurrentUserId() -> String {
        // TODO: Get current user ID from authentication service
        // This should be injected from the auth repository
        return "current_user_id" // Placeholder
    }
}

// MARK: - Error Types

enum FriendsError: Error, LocalizedError {
    case userNotFound
    case friendshipAlreadyExists
    case friendRequestAlreadySent
    case invalidRequest
    
    var errorDescription: String? {
        switch self {
        case .userNotFound:
            return "User not found"
        case .friendshipAlreadyExists:
            return "Friendship already exists"
        case .friendRequestAlreadySent:
            return "Friend request already sent"
        case .invalidRequest:
            return "Invalid friend request"
        }
    }
}