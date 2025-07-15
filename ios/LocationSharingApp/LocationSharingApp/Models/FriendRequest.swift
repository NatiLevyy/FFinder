import Foundation

struct FriendRequest: Codable, Identifiable {
    let id: String
    let fromUserId: String
    let toUserId: String
    let status: RequestStatus
    let createdAt: Int64
    let respondedAt: Int64?
    
    init(id: String, fromUserId: String, toUserId: String, status: RequestStatus, createdAt: Int64, respondedAt: Int64? = nil) {
        self.id = id
        self.fromUserId = fromUserId
        self.toUserId = toUserId
        self.status = status
        self.createdAt = createdAt
        self.respondedAt = respondedAt
    }
}

enum RequestStatus: String, Codable, CaseIterable {
    case pending = "PENDING"
    case accepted = "ACCEPTED"
    case rejected = "REJECTED"
    case expired = "EXPIRED"
}