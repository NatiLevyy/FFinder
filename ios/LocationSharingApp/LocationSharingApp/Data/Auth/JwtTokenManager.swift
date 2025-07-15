import Foundation
import Security

/**
 * Manages JWT tokens including storage, retrieval, and validation.
 * Uses Keychain for secure token storage.
 */
class JwtTokenManager {
    
    // MARK: - Constants
    
    private struct Constants {
        static let accessTokenKey = "access_token"
        static let refreshTokenKey = "refresh_token"
        static let tokenExpiresAtKey = "token_expires_at"
        static let tokenRefreshBufferSeconds: TimeInterval = 5 * 60 // 5 minutes
        static let keychainService = "com.locationsharing.app.auth"
    }
    
    // MARK: - Singleton
    
    static let shared = JwtTokenManager()
    
    private init() {}
    
    // MARK: - Token Storage
    
    /**
     * Stores authentication tokens securely in Keychain.
     * 
     * - Parameters:
     *   - accessToken: The JWT access token
     *   - refreshToken: The refresh token
     *   - expiresAt: The timestamp when the access token expires
     */
    func storeTokens(accessToken: String, refreshToken: String, expiresAt: Int64) async throws {
        try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.global(qos: .background).async {
                do {
                    try self.storeInKeychain(key: Constants.accessTokenKey, value: accessToken)
                    try self.storeInKeychain(key: Constants.refreshTokenKey, value: refreshToken)
                    try self.storeInKeychain(key: Constants.tokenExpiresAtKey, value: String(expiresAt))
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Retrieves the stored access token.
     * 
     * - Returns: The access token or nil if not found
     */
    func getAccessToken() async -> String? {
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .background).async {
                let token = self.retrieveFromKeychain(key: Constants.accessTokenKey)
                continuation.resume(returning: token)
            }
        }
    }
    
    /**
     * Retrieves the stored refresh token.
     * 
     * - Returns: The refresh token or nil if not found
     */
    func getRefreshToken() async -> String? {
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .background).async {
                let token = self.retrieveFromKeychain(key: Constants.refreshTokenKey)
                continuation.resume(returning: token)
            }
        }
    }
    
    /**
     * Gets the token expiration timestamp.
     * 
     * - Returns: The expiration timestamp or 0 if not found
     */
    func getTokenExpiresAt() async -> Int64 {
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .background).async {
                if let expiresAtString = self.retrieveFromKeychain(key: Constants.tokenExpiresAtKey),
                   let expiresAt = Int64(expiresAtString) {
                    continuation.resume(returning: expiresAt)
                } else {
                    continuation.resume(returning: 0)
                }
            }
        }
    }
    
    // MARK: - Token Validation
    
    /**
     * Checks if the current access token is valid and not expired.
     * 
     * - Returns: True if the token is valid, false otherwise
     */
    func isTokenValid() async -> Bool {
        guard let token = await getAccessToken() else { return false }
        
        let expiresAt = await getTokenExpiresAt()
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let bufferMs = Int64(Constants.tokenRefreshBufferSeconds * 1000)
        
        return expiresAt > currentTime + bufferMs
    }
    
    /**
     * Checks if the token needs to be refreshed.
     * 
     * - Returns: True if the token should be refreshed, false otherwise
     */
    func shouldRefreshToken() async -> Bool {
        let expiresAt = await getTokenExpiresAt()
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let bufferMs = Int64(Constants.tokenRefreshBufferSeconds * 1000)
        
        return expiresAt > 0 && expiresAt <= currentTime + bufferMs
    }
    
    /**
     * Extracts user ID from JWT token payload.
     * 
     * - Parameter token: The JWT token
     * - Returns: The user ID or nil if extraction fails
     */
    func extractUserIdFromToken(_ token: String) -> String? {
        let parts = token.components(separatedBy: ".")
        guard parts.count == 3 else { return nil }
        
        guard let payloadData = Data(base64URLEncoded: parts[1]),
              let payloadString = String(data: payloadData, encoding: .utf8) else {
            return nil
        }
        
        // Simple extraction - in production, use a proper JWT library
        let regex = try? NSRegularExpression(pattern: "\"sub\":\"([^\"]+)\"")
        let range = NSRange(payloadString.startIndex..., in: payloadString)
        
        if let match = regex?.firstMatch(in: payloadString, range: range),
           let subRange = Range(match.range(at: 1), in: payloadString) {
            return String(payloadString[subRange])
        }
        
        return nil
    }
    
    /**
     * Checks if a JWT token is expired based on its payload.
     * 
     * - Parameter token: The JWT token
     * - Returns: True if the token is expired, false otherwise
     */
    func isTokenExpired(_ token: String) -> Bool {
        let parts = token.components(separatedBy: ".")
        guard parts.count == 3 else { return true }
        
        guard let payloadData = Data(base64URLEncoded: parts[1]),
              let payloadString = String(data: payloadData, encoding: .utf8) else {
            return true
        }
        
        let regex = try? NSRegularExpression(pattern: "\"exp\":(\\d+)")
        let range = NSRange(payloadString.startIndex..., in: payloadString)
        
        if let match = regex?.firstMatch(in: payloadString, range: range),
           let expRange = Range(match.range(at: 1), in: payloadString),
           let exp = Int64(payloadString[expRange]) {
            let expMs = exp * 1000 // Convert to milliseconds
            let currentMs = Int64(Date().timeIntervalSince1970 * 1000)
            return expMs <= currentMs
        }
        
        return true // If no exp claim or parsing fails, consider expired
    }
    
    /**
     * Clears all stored authentication tokens.
     */
    func clearTokens() async throws {
        try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.global(qos: .background).async {
                do {
                    try self.deleteFromKeychain(key: Constants.accessTokenKey)
                    try self.deleteFromKeychain(key: Constants.refreshTokenKey)
                    try self.deleteFromKeychain(key: Constants.tokenExpiresAtKey)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Checks if any authentication tokens are stored.
     * 
     * - Returns: True if tokens exist, false otherwise
     */
    func hasStoredTokens() async -> Bool {
        let accessToken = await getAccessToken()
        let refreshToken = await getRefreshToken()
        return accessToken != nil && refreshToken != nil
    }
    
    // MARK: - Private Keychain Methods
    
    private func storeInKeychain(key: String, value: String) throws {
        let data = value.data(using: .utf8)!
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key,
            kSecValueData as String: data
        ]
        
        // Delete existing item first
        SecItemDelete(query as CFDictionary)
        
        // Add new item
        let status = SecItemAdd(query as CFDictionary, nil)
        
        guard status == errSecSuccess else {
            throw AuthError.unknown("Failed to store token in keychain: \(status)")
        }
    }
    
    private func retrieveFromKeychain(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data,
              let string = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        return string
    }
    
    private func deleteFromKeychain(key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        // It's okay if the item doesn't exist
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw AuthError.unknown("Failed to delete token from keychain: \(status)")
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