import XCTest
@testable import LocationSharingApp

class JwtTokenManagerTests: XCTestCase {
    
    var tokenManager: JwtTokenManager!
    
    override func setUp() {
        super.setUp()
        tokenManager = JwtTokenManager.shared
        
        // Clear any existing tokens before each test
        Task {
            try? await tokenManager.clearTokens()
        }
    }
    
    override func tearDown() {
        // Clear tokens after each test
        Task {
            try? await tokenManager.clearTokens()
        }
        super.tearDown()
    }
    
    func testStoreAndRetrieveTokens() async throws {
        // Given
        let accessToken = "access_token_123"
        let refreshToken = "refresh_token_123"
        let expiresAt: Int64 = 1640995200000
        
        // When
        try await tokenManager.storeTokens(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresAt: expiresAt
        )
        
        // Then
        let retrievedAccessToken = await tokenManager.getAccessToken()
        let retrievedRefreshToken = await tokenManager.getRefreshToken()
        let retrievedExpiresAt = await tokenManager.getTokenExpiresAt()
        
        XCTAssertEqual(retrievedAccessToken, accessToken)
        XCTAssertEqual(retrievedRefreshToken, refreshToken)
        XCTAssertEqual(retrievedExpiresAt, expiresAt)
    }
    
    func testGetTokensWhenNoneStored() async {
        // When
        let accessToken = await tokenManager.getAccessToken()
        let refreshToken = await tokenManager.getRefreshToken()
        let expiresAt = await tokenManager.getTokenExpiresAt()
        
        // Then
        XCTAssertNil(accessToken)
        XCTAssertNil(refreshToken)
        XCTAssertEqual(expiresAt, 0)
    }
    
    func testIsTokenValidWithValidToken() async throws {
        // Given
        let futureTime = Int64(Date().timeIntervalSince1970 * 1000) + (10 * 60 * 1000) // 10 minutes in future
        
        try await tokenManager.storeTokens(
            accessToken: "valid_token",
            refreshToken: "refresh_token",
            expiresAt: futureTime
        )
        
        // When
        let isValid = await tokenManager.isTokenValid()
        
        // Then
        XCTAssertTrue(isValid)
    }
    
    func testIsTokenValidWithExpiredToken() async throws {
        // Given
        let pastTime = Int64(Date().timeIntervalSince1970 * 1000) - (10 * 60 * 1000) // 10 minutes in past
        
        try await tokenManager.storeTokens(
            accessToken: "expired_token",
            refreshToken: "refresh_token",
            expiresAt: pastTime
        )
        
        // When
        let isValid = await tokenManager.isTokenValid()
        
        // Then
        XCTAssertFalse(isValid)
    }
    
    func testIsTokenValidWithNoToken() async {
        // When
        let isValid = await tokenManager.isTokenValid()
        
        // Then
        XCTAssertFalse(isValid)
    }
    
    func testShouldRefreshTokenWhenExpiringSoon() async throws {
        // Given
        let soonToExpire = Int64(Date().timeIntervalSince1970 * 1000) + (2 * 60 * 1000) // 2 minutes in future
        
        try await tokenManager.storeTokens(
            accessToken: "token",
            refreshToken: "refresh_token",
            expiresAt: soonToExpire
        )
        
        // When
        let shouldRefresh = await tokenManager.shouldRefreshToken()
        
        // Then
        XCTAssertTrue(shouldRefresh)
    }
    
    func testShouldRefreshTokenWhenNotExpiringSoon() async throws {
        // Given
        let futureTime = Int64(Date().timeIntervalSince1970 * 1000) + (10 * 60 * 1000) // 10 minutes in future
        
        try await tokenManager.storeTokens(
            accessToken: "token",
            refreshToken: "refresh_token",
            expiresAt: futureTime
        )
        
        // When
        let shouldRefresh = await tokenManager.shouldRefreshToken()
        
        // Then
        XCTAssertFalse(shouldRefresh)
    }
    
    func testExtractUserIdFromValidToken() {
        // Given
        // This is a mock JWT with payload: {"sub":"user123","exp":1640995200}
        let mockJwt = "header.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjQwOTk1MjAwfQ.signature"
        
        // When
        let userId = tokenManager.extractUserIdFromToken(mockJwt)
        
        // Then
        XCTAssertEqual(userId, "user123")
    }
    
    func testExtractUserIdFromInvalidToken() {
        // Given
        let invalidJwt = "invalid.jwt"
        
        // When
        let userId = tokenManager.extractUserIdFromToken(invalidJwt)
        
        // Then
        XCTAssertNil(userId)
    }
    
    func testIsTokenExpiredWithExpiredToken() {
        // Given
        // JWT with exp claim set to past timestamp (1640995200 = Jan 1, 2022)
        let expiredJwt = "header.eyJzdWIiOiJ1c2VyMTIzIiwiZXhwIjoxNjQwOTk1MjAwfQ.signature"
        
        // When
        let isExpired = tokenManager.isTokenExpired(expiredJwt)
        
        // Then
        XCTAssertTrue(isExpired)
    }
    
    func testIsTokenExpiredWithInvalidToken() {
        // Given
        let invalidJwt = "invalid.token"
        
        // When
        let isExpired = tokenManager.isTokenExpired(invalidJwt)
        
        // Then
        XCTAssertTrue(isExpired)
    }
    
    func testClearTokens() async throws {
        // Given
        try await tokenManager.storeTokens(
            accessToken: "access_token",
            refreshToken: "refresh_token",
            expiresAt: 1640995200000
        )
        
        // Verify tokens are stored
        let hasTokensBefore = await tokenManager.hasStoredTokens()
        XCTAssertTrue(hasTokensBefore)
        
        // When
        try await tokenManager.clearTokens()
        
        // Then
        let hasTokensAfter = await tokenManager.hasStoredTokens()
        XCTAssertFalse(hasTokensAfter)
        
        let accessToken = await tokenManager.getAccessToken()
        let refreshToken = await tokenManager.getRefreshToken()
        XCTAssertNil(accessToken)
        XCTAssertNil(refreshToken)
    }
    
    func testHasStoredTokensWhenBothExist() async throws {
        // Given
        try await tokenManager.storeTokens(
            accessToken: "access_token",
            refreshToken: "refresh_token",
            expiresAt: 1640995200000
        )
        
        // When
        let hasTokens = await tokenManager.hasStoredTokens()
        
        // Then
        XCTAssertTrue(hasTokens)
    }
    
    func testHasStoredTokensWhenNoneExist() async {
        // When
        let hasTokens = await tokenManager.hasStoredTokens()
        
        // Then
        XCTAssertFalse(hasTokens)
    }
}