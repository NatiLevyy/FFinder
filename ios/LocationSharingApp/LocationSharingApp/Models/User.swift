import Foundation

struct User: Codable, Identifiable {
    let id: String
    let email: String
    let displayName: String
    let profileImageUrl: String?
    let createdAt: Int64
    let lastActiveAt: Int64
    
    init(id: String, email: String, displayName: String, profileImageUrl: String? = nil, createdAt: Int64, lastActiveAt: Int64) {
        self.id = id
        self.email = email
        self.displayName = displayName
        self.profileImageUrl = profileImageUrl
        self.createdAt = createdAt
        self.lastActiveAt = lastActiveAt
    }
}