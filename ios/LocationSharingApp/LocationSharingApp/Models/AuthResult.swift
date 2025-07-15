import Foundation

/**
 * Represents the result of an authentication operation.
 */
struct AuthResult: Codable {
    /// The authenticated user information
    let user: User
    /// The JWT authentication token
    let token: String
    /// The refresh token for token renewal
    let refreshToken: String
    /// The timestamp when the token expires
    let expiresAt: Int64
    
    init(user: User, token: String, refreshToken: String, expiresAt: Int64) {
        self.user = user
        self.token = token
        self.refreshToken = refreshToken
        self.expiresAt = expiresAt
    }
}