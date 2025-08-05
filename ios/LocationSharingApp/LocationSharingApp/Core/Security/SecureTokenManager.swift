import Foundation

/**
 * Enhanced JWT token manager with additional security features.
 * Uses SecureStorage for encrypted token storage with validation.
 */
class SecureTokenManager {
    
    // MARK: - Constants
    
    private struct Constants {
        static let accessTokenKey = "secure_access_token"
        static let refreshTokenKey = "secure_refresh_token"
        static let tokenExpiresAtKey = "secure_token_expires_at"
        static let userIdKey = "secure_user_id"
        static let tokenRefreshBufferSeconds: TimeInterval = 5 * 60 // 5 minutes
    }
    
    // MARK: - Singleton
    
    static let shared = SecureTokenManager()
    
    // MARK: - Private Properties
    
    private let secureStorage = SecureStorage.shared
    
    // MARK: - Initialization
    
    private init() {}
    
    // MARK: - Token Storage
    
    /**
     * Stores authentication tokens securely with validation.
     * 
     * - Parameters:
     *   - accessToken: The JWT access token
     *   - refreshToken: The refresh token
     *   - expiresAt: The timestamp when the access token expires
     *   - userId: The user ID associated with the tokens
     */
    func storeTokens(
        accessToken: String,
        refreshToken: String,
        expiresAt: Int64,
        userId: String
    ) async throws {
        try validateToken(accessToken)
        try validateToken(refreshToken)
        try validateUserId(userId)
        
        // Store tokens with validation
        try await secureStorage.store(key: Constants.accessTokenKey, value: accessToken, requireValidation: true)
        try await secureStorage.store(key: Constants.refreshTokenKey, value: refreshToken, requireValidation: true)
        try await secureStorage.store(key: Constants.tokenExpiresAtKey, value: String(expiresAt), requireValidation: true)
        try await secureStorage.store(key: Constants.userIdKey, value: userId, requireValidation: true)
    }
    
    /**
     * Retrieves the stored access token with validation.
     * 
     * - Returns: The access token or nil if not found or validation fails
     */
    func getAccessToken() async throws -> String? {
        let token = try await secureStorage.retrieve(key: Constants.accessTokenKey, requireValidation: true)
        
        if let token = token, !isTokenFormatValid(token) {
            // Token format is invalid, clear all tokens for security
            try await clearTokens()
            return nil
        }
        
        return token
    }
    
    /**
     * Retrieves the stored refresh token with validation.
     * 
     * - Returns: The refresh token or nil if not found or validation fails
     */
    func getRefreshToken() async throws -> String? {
        let token = try await secureStorage.retrieve(key: Constants.refreshTokenKey, requireValidation: true)
        
        if let token = token, !isTokenFormatValid(token) {
            // Token format is invalid, clear all tokens for security
            try await clearTokens()
            return nil
        }
        
        return token
    }
    
    /**
     * Gets the token expiration timestamp with validation.
     * 
     * - Returns: The expiration timestamp or 0 if not found or validation fails
     */
    func getTokenExpiresAt() async throws -> Int64 {
        let expiresAtString = try await secureStorage.retrieve(key: Constants.tokenExpiresAtKey, requireValidation: true)
        return Int64(expiresAtString ?? "0") ?? 0
    }
    
    /**
     * Gets the stored user ID with validation.
     * 
     * - Returns: The user ID or nil if not found or validation fails
     */
    func getUserId() async throws -> String? {
        return try await secureStorage.retrieve(key: Constants.userIdKey, requireValidation: true)
    }
    
    // MARK: - Token Validation
    
    /**
     * Checks if the current access token is valid and not expired.
     * 
     * - Returns: True if the token is valid, false otherwise
     */
    func isTokenValid() async throws -> Bool {
        guard let token = try await getAccessToken() else { return false }
        
        let expiresAt = try await getTokenExpiresAt()
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let bufferMs = Int64(Constants.tokenRefreshBufferSeconds * 1000)
        
        // Validate token format and expiration
        return isTokenFormatValid(token) &&
               expiresAt > currentTime + bufferMs &&
               !isTokenExpiredByPayload(token)
    }
    
    /**
     * Checks if the token needs to be refreshed.
     * 
     * - Returns: True if the token should be refreshed, false otherwise
     */
    func shouldRefreshToken() async throws -> Bool {
        let expiresAt = try await getTokenExpiresAt()
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let bufferMs = Int64(Constants.tokenRefreshBufferSeconds * 1000)
        
        return expiresAt > 0 && expiresAt <= currentTime + bufferMs
    }
    
    /**
     * Extracts user ID from JWT token payload with validation.
     * 
     * - Parameter token: The JWT token
     * - Returns: The user ID or nil if extraction fails or validation fails
     */
    func extractUserIdFromToken(_ token: String) async throws -> String? {
        guard isTokenFormatValid(token) else { return nil }
        
        do {
            let parts = token.components(separatedBy: ".")
            guard parts.count == 3 else { return nil }
            
            guard let payloadData = Data(base64URLEncoded: parts[1]),
                  let payloadString = String(data: payloadData, encoding: .utf8) else {
                return nil
            }
            
            let regex = try NSRegularExpression(pattern: "\"sub\":\"([^\"]+)\"")
            let range = NSRange(payloadString.startIndex..., in: payloadString)
            
            var extractedUserId: String?
            if let match = regex.firstMatch(in: payloadString, range: range),
               let subRange = Range(match.range(at: 1), in: payloadString) {
                extractedUserId = String(payloadString[subRange])
            }
            
            // Validate extracted user ID against stored user ID
            let storedUserId = try await getUserId()
            if let storedUserId = storedUserId, 
               let extractedUserId = extractedUserId,
               storedUserId != extractedUserId {
                // User ID mismatch, potential security issue
                try await clearTokens()
                return nil
            }
            
            return extractedUserId
        } catch {
            return nil
        }
    }
    
    /**
     * Checks if a JWT token is expired based on its payload.
     * 
     * - Parameter token: The JWT token
     * - Returns: True if the token is expired, false otherwise
     */
    func isTokenExpiredByPayload(_ token: String) -> Bool {
        guard isTokenFormatValid(token) else { return true }
        
        do {
            let parts = token.components(separatedBy: ".")
            guard parts.count == 3 else { return true }
            
            guard let payloadData = Data(base64URLEncoded: parts[1]),
                  let payloadString = String(data: payloadData, encoding: .utf8) else {
                return true
            }
            
            let regex = try NSRegularExpression(pattern: "\"exp\":(\\d+)")
            let range = NSRange(payloadString.startIndex..., in: payloadString)
            
            if let match = regex.firstMatch(in: payloadString, range: range),
               let expRange = Range(match.range(at: 1), in: payloadString),
               let exp = Int64(payloadString[expRange]) {
                let expMs = exp * 1000 // Convert to milliseconds
                let currentMs = Int64(Date().timeIntervalSince1970 * 1000)
                return expMs <= currentMs
            }
            
            return true // If no exp claim, consider expired
        } catch {
            return true // If parsing fails, consider expired
        }
    }
    
    /**
     * Validates token integrity by checking signature (simplified validation).
     * 
     * - Parameter token: The JWT token
     * - Returns: True if the token appears to have valid structure, false otherwise
     */
    func validateTokenIntegrity(_ token: String) -> Bool {
        guard isTokenFormatValid(token) else { return false }
        
        do {
            let parts = token.components(separatedBy: ".")
            guard parts.count == 3 else { return false }
            
            // Validate header
            guard let headerData = Data(base64URLEncoded: parts[0]),
                  let headerString = String(data: headerData, encoding: .utf8) else {
                return false
            }
            
            if !headerString.contains("\"alg\"") || !headerString.contains("\"typ\"") {
                return false
            }
            
            // Validate payload
            guard let payloadData = Data(base64URLEncoded: parts[1]),
                  let payloadString = String(data: payloadData, encoding: .utf8) else {
                return false
            }
            
            if !payloadString.contains("\"sub\"") || !payloadString.contains("\"exp\"") {
                return false
            }
            
            // Signature exists (basic check)
            return !parts[2].isEmpty
        } catch {
            return false
        }
    }
    
    /**
     * Clears all stored authentication tokens securely.
     */
    func clearTokens() async throws {
        try await secureStorage.remove(key: Constants.accessTokenKey)
        try await secureStorage.remove(key: Constants.refreshTokenKey)
        try await secureStorage.remove(key: Constants.tokenExpiresAtKey)
        try await secureStorage.remove(key: Constants.userIdKey)
    }
    
    /**
     * Checks if any authentication tokens are stored.
     * 
     * - Returns: True if tokens exist, false otherwise
     */
    func hasStoredTokens() async throws -> Bool {
        let hasAccessToken = try await secureStorage.contains(key: Constants.accessTokenKey)
        let hasRefreshToken = try await secureStorage.contains(key: Constants.refreshTokenKey)
        return hasAccessToken && hasRefreshToken
    }
    
    /**
     * Rotates tokens by clearing old ones (for security purposes).
     * Should be called periodically or when security breach is suspected.
     */
    func rotateTokens() async throws {
        try await clearTokens()
    }
    
    /**
     * Gets security metadata about stored tokens.
     * 
     * - Returns: Dictionary containing security information
     */
    func getSecurityMetadata() async throws -> [String: Any] {
        var metadata: [String: Any] = [:]
        
        metadata["hasTokens"] = try await hasStoredTokens()
        
        let allKeys = await secureStorage.getAllKeys()
        metadata["tokenCount"] = allKeys.filter { $0.hasPrefix("secure_") }.count
        
        if let accessToken = try await getAccessToken() {
            metadata["tokenValid"] = try await isTokenValid()
            metadata["tokenExpired"] = isTokenExpiredByPayload(accessToken)
            metadata["shouldRefresh"] = try await shouldRefreshToken()
        }
        
        return metadata
    }
    
    // MARK: - Private Methods
    
    private func validateToken(_ token: String) throws {
        guard !token.isEmpty else {
            throw SecureTokenError.invalidToken("Token cannot be empty")
        }
        
        guard token.count <= 5000 else {
            throw SecureTokenError.invalidToken("Token too long")
        }
        
        guard isTokenFormatValid(token) else {
            throw SecureTokenError.invalidToken("Invalid token format")
        }
    }
    
    private func validateUserId(_ userId: String) throws {
        guard !userId.isEmpty else {
            throw SecureTokenError.invalidUserId("User ID cannot be empty")
        }
        
        guard userId.count <= 100 else {
            throw SecureTokenError.invalidUserId("User ID too long")
        }
        
        let validUserIdRegex = "^[a-zA-Z0-9_.-]+$"
        let predicate = NSPredicate(format: "SELF MATCHES %@", validUserIdRegex)
        guard predicate.evaluate(with: userId) else {
            throw SecureTokenError.invalidUserId("User ID contains invalid characters")
        }
    }
    
    private func isTokenFormatValid(_ token: String) -> Bool {
        let parts = token.components(separatedBy: ".")
        guard parts.count == 3 else { return false }
        
        let base64URLRegex = "^[A-Za-z0-9_-]+$"
        let predicate = NSPredicate(format: "SELF MATCHES %@", base64URLRegex)
        
        return parts.allSatisfy { part in
            !part.isEmpty && predicate.evaluate(with: part)
        }
    }
}

// MARK: - Error Types

enum SecureTokenError: Error, LocalizedError {
    case invalidToken(String)
    case invalidUserId(String)
    case tokenExpired
    case validationFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidToken(let message):
            return "Invalid token: \(message)"
        case .invalidUserId(let message):
            return "Invalid user ID: \(message)"
        case .tokenExpired:
            return "Token has expired"
        case .validationFailed(let message):
            return "Token validation failed: \(message)"
        }
    }
}

// MARK: - Base64URL Extension

private extension Data {
    init?(base64URLEncoded string: String) {
        var base64 = string
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        
        // Add padding if needed
        let remainder = base64.count % 4
        if remainder > 0 {
            base64 += String(repeating: "=", count: 4 - remainder)
        }
        
        self.init(base64Encoded: base64)
    }
}