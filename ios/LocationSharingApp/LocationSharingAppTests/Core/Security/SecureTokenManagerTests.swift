import XCTest
@testable import LocationSharingApp

class SecureTokenManagerTests: XCTestCase {
    
    var secureTokenManager: SecureTokenManager!
    
    // Sample JWT tokens for testing (these are test tokens, not real ones)
    private let validAccessToken = createTestJwtToken(
        header: #"{"alg":"HS256","typ":"JWT"}"#,
        payload: #"{"sub":"user123","exp":\#(Int(Date().timeIntervalSince1970) + 3600),"iat":\#(Int(Date().timeIntervalSince1970))}"#,
        signature: "test_signature"
    )
    
    private let expiredAccessToken = createTestJwtToken(
        header: #"{"alg":"HS256","typ":"JWT"}"#,
        payload: #"{"sub":"user123","exp":\#(Int(Date().timeIntervalSince1970) - 3600),"iat":\#(Int(Date().timeIntervalSince1970))}"#,
        signature: "test_signature"
    )
    
    private let validRefreshToken = createTestJwtToken(
        header: #"{"alg":"HS256","typ":"JWT"}"#,
        payload: #"{"sub":"user123","exp":\#(Int(Date().timeIntervalSince1970) + 7200),"iat":\#(Int(Date().timeIntervalSince1970))}"#,
        signature: "refresh_signature"
    )
    
    override func setUp() {
        super.setUp()
        secureTokenManager = SecureTokenManager.shared
    }
    
    override func tearDown() {
        Task {
            try await secureTokenManager.clearTokens()
        }
        super.tearDown()
    }
    
    func testStoreAndRetrieveTokensSuccessfully() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000 // 1 hour from now
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Then
        XCTAssertEqual(validAccessToken, try await secureTokenManager.getAccessToken())
        XCTAssertEqual(validRefreshToken, try await secureTokenManager.getRefreshToken())
        XCTAssertEqual(expiresAt, try await secureTokenManager.getTokenExpiresAt())
        XCTAssertEqual(userId, try await secureTokenManager.getUserId())
    }
    
    func testHasStoredTokensReturnsTrueWhenTokensExist() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Then
        XCTAssertTrue(try await secureTokenManager.hasStoredTokens())
    }
    
    func testHasStoredTokensReturnsFalseWhenNoTokensExist() async throws {
        // When
        let hasTokens = try await secureTokenManager.hasStoredTokens()
        
        // Then
        XCTAssertFalse(hasTokens)
    }
    
    func testIsTokenValidReturnsTrueForValidNonExpiredToken() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000 // 1 hour from now
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Then
        XCTAssertTrue(try await secureTokenManager.isTokenValid())
    }
    
    func testIsTokenValidReturnsFalseForExpiredToken() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) - 3600000 // 1 hour ago
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: expiredAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Then
        XCTAssertFalse(try await secureTokenManager.isTokenValid())
    }
    
    func testShouldRefreshTokenReturnsTrueWhenTokenIsNearExpiry() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 60000 // 1 minute from now (within refresh buffer)
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Then
        XCTAssertTrue(try await secureTokenManager.shouldRefreshToken())
    }
    
    func testShouldRefreshTokenReturnsFalseWhenTokenHasPlentyOfTimeLeft() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000 // 1 hour from now
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Then
        XCTAssertFalse(try await secureTokenManager.shouldRefreshToken())
    }
    
    func testExtractUserIdFromTokenReturnsCorrectUserId() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let userId = "user123"
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        let extractedUserId = try await secureTokenManager.extractUserIdFromToken(validAccessToken)
        
        // Then
        XCTAssertEqual(userId, extractedUserId)
    }
    
    func testExtractUserIdFromTokenReturnsNilForInvalidToken() async throws {
        // Given
        let invalidToken = "invalid.token.format"
        
        // When
        let extractedUserId = try await secureTokenManager.extractUserIdFromToken(invalidToken)
        
        // Then
        XCTAssertNil(extractedUserId)
    }
    
    func testExtractUserIdFromTokenClearsTokensOnUserIdMismatch() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let storedUserId = "user123"
        let differentUserToken = Self.createTestJwtToken(
            header: #"{"alg":"HS256","typ":"JWT"}"#,
            payload: #"{"sub":"different_user","exp":\#(Int(Date().timeIntervalSince1970) + 3600),"iat":\#(Int(Date().timeIntervalSince1970))}"#,
            signature: "test_signature"
        )
        
        // When
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: storedUserId
        )
        let extractedUserId = try await secureTokenManager.extractUserIdFromToken(differentUserToken)
        
        // Then
        XCTAssertNil(extractedUserId)
        XCTAssertFalse(try await secureTokenManager.hasStoredTokens()) // Tokens should be cleared
    }
    
    func testIsTokenExpiredByPayloadReturnsTrueForExpiredToken() {
        // When
        let isExpired = secureTokenManager.isTokenExpiredByPayload(expiredAccessToken)
        
        // Then
        XCTAssertTrue(isExpired)
    }
    
    func testIsTokenExpiredByPayloadReturnsFalseForValidToken() {
        // When
        let isExpired = secureTokenManager.isTokenExpiredByPayload(validAccessToken)
        
        // Then
        XCTAssertFalse(isExpired)
    }
    
    func testValidateTokenIntegrityReturnsTrueForWellFormedToken() {
        // When
        let isValid = secureTokenManager.validateTokenIntegrity(validAccessToken)
        
        // Then
        XCTAssertTrue(isValid)
    }
    
    func testValidateTokenIntegrityReturnsFalseForMalformedToken() {
        // Given
        let malformedTokens = [
            "invalid.token",
            "not.a.jwt.token",
            "",
            "header.payload.", // Empty signature
            "invalid_base64.invalid_base64.invalid_base64"
        ]
        
        // When & Then
        for token in malformedTokens {
            XCTAssertFalse(secureTokenManager.validateTokenIntegrity(token))
        }
    }
    
    func testClearTokensRemovesAllStoredTokens() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let userId = "user123"
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // When
        try await secureTokenManager.clearTokens()
        
        // Then
        XCTAssertFalse(try await secureTokenManager.hasStoredTokens())
        XCTAssertNil(try await secureTokenManager.getAccessToken())
        XCTAssertNil(try await secureTokenManager.getRefreshToken())
        XCTAssertEqual(0, try await secureTokenManager.getTokenExpiresAt())
        XCTAssertNil(try await secureTokenManager.getUserId())
    }
    
    func testRotateTokensClearsAllTokens() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let userId = "user123"
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // When
        try await secureTokenManager.rotateTokens()
        
        // Then
        XCTAssertFalse(try await secureTokenManager.hasStoredTokens())
    }
    
    func testGetSecurityMetadataReturnsCorrectInformation() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let userId = "user123"
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // When
        let metadata = try await secureTokenManager.getSecurityMetadata()
        
        // Then
        XCTAssertTrue(metadata["hasTokens"] as! Bool)
        XCTAssertTrue((metadata["tokenCount"] as! Int) > 0)
        XCTAssertTrue(metadata["tokenValid"] as! Bool)
        XCTAssertFalse(metadata["tokenExpired"] as! Bool)
        XCTAssertFalse(metadata["shouldRefresh"] as! Bool)
    }
    
    func testInvalidTokenFormatClearsAllTokensForSecurity() async throws {
        // Given
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let userId = "user123"
        try await secureTokenManager.storeTokens(
            accessToken: validAccessToken,
            refreshToken: validRefreshToken,
            expiresAt: expiresAt,
            userId: userId
        )
        
        // Manually corrupt the stored token
        try await SecureStorage.shared.store(key: "secure_access_token", value: "corrupted.token.format", requireValidation: true)
        
        // When
        let retrievedToken = try await secureTokenManager.getAccessToken()
        
        // Then
        XCTAssertNil(retrievedToken)
        XCTAssertFalse(try await secureTokenManager.hasStoredTokens()) // All tokens should be cleared
    }
    
    func testStoreTokensWithInvalidInputThrowsException() async {
        // Given
        let validExpiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        let validUserId = "user123"
        
        // When & Then
        do {
            try await secureTokenManager.storeTokens(
                accessToken: "",
                refreshToken: validRefreshToken,
                expiresAt: validExpiresAt,
                userId: validUserId
            )
            XCTFail("Should have thrown an exception for empty access token")
        } catch {
            // Expected to throw
        }
        
        do {
            try await secureTokenManager.storeTokens(
                accessToken: validAccessToken,
                refreshToken: "",
                expiresAt: validExpiresAt,
                userId: validUserId
            )
            XCTFail("Should have thrown an exception for empty refresh token")
        } catch {
            // Expected to throw
        }
        
        do {
            try await secureTokenManager.storeTokens(
                accessToken: validAccessToken,
                refreshToken: validRefreshToken,
                expiresAt: validExpiresAt,
                userId: ""
            )
            XCTFail("Should have thrown an exception for empty user ID")
        } catch {
            // Expected to throw
        }
        
        do {
            try await secureTokenManager.storeTokens(
                accessToken: "invalid_token",
                refreshToken: validRefreshToken,
                expiresAt: validExpiresAt,
                userId: validUserId
            )
            XCTFail("Should have thrown an exception for invalid token format")
        } catch {
            // Expected to throw
        }
    }
    
    func testConcurrentTokenOperations() async throws {
        // Given
        let numTasks = 10
        let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + 3600000
        
        // When
        await withTaskGroup(of: Void.self) { group in
            for taskId in 1...numTasks {
                group.addTask {
                    let userId = "user\(taskId)"
                    let accessToken = Self.createTestJwtToken(
                        header: #"{"alg":"HS256","typ":"JWT"}"#,
                        payload: #"{"sub":"\#(userId)","exp":\#(Int(Date().timeIntervalSince1970) + 3600),"iat":\#(Int(Date().timeIntervalSince1970))}"#,
                        signature: "signature\(taskId)"
                    )
                    
                    do {
                        try await self.secureTokenManager.storeTokens(
                            accessToken: accessToken,
                            refreshToken: self.validRefreshToken,
                            expiresAt: expiresAt,
                            userId: userId
                        )
                        
                        // Verify storage
                        XCTAssertTrue(try await self.secureTokenManager.hasStoredTokens())
                        
                        // Clear for next iteration
                        try await self.secureTokenManager.clearTokens()
                    } catch {
                        XCTFail("Concurrent token operation failed: \(error)")
                    }
                }
            }
        }
    }
    
    // MARK: - Helper Methods
    
    static func createTestJwtToken(header: String, payload: String, signature: String) -> String {
        let encodedHeader = Data(header.utf8).base64URLEncodedString()
        let encodedPayload = Data(payload.utf8).base64URLEncodedString()
        let encodedSignature = Data(signature.utf8).base64URLEncodedString()
        return "\(encodedHeader).\(encodedPayload).\(encodedSignature)"
    }
}

// MARK: - Base64URL Extension for Testing

private extension Data {
    func base64URLEncodedString() -> String {
        return self.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}