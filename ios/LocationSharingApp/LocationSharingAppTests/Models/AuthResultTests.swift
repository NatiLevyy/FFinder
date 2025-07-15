import XCTest
@testable import LocationSharingApp

class AuthResultTests: XCTestCase {
    
    func testAuthResultCreation() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        let token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        let refreshToken = "refresh_token_123"
        let expiresAt: Int64 = 1640995200000
        
        // When
        let authResult = AuthResult(
            user: user,
            token: token,
            refreshToken: refreshToken,
            expiresAt: expiresAt
        )
        
        // Then
        XCTAssertEqual(authResult.user.id, user.id)
        XCTAssertEqual(authResult.user.email, user.email)
        XCTAssertEqual(authResult.token, token)
        XCTAssertEqual(authResult.refreshToken, refreshToken)
        XCTAssertEqual(authResult.expiresAt, expiresAt)
    }
    
    func testAuthResultCodable() throws {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        let authResult = AuthResult(
            user: user,
            token: "token123",
            refreshToken: "refresh123",
            expiresAt: 1640995200000
        )
        
        // When
        let encoded = try JSONEncoder().encode(authResult)
        let decoded = try JSONDecoder().decode(AuthResult.self, from: encoded)
        
        // Then
        XCTAssertEqual(decoded.user.id, authResult.user.id)
        XCTAssertEqual(decoded.user.email, authResult.user.email)
        XCTAssertEqual(decoded.token, authResult.token)
        XCTAssertEqual(decoded.refreshToken, authResult.refreshToken)
        XCTAssertEqual(decoded.expiresAt, authResult.expiresAt)
    }
    
    func testAuthResultEquality() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        
        let authResult1 = AuthResult(
            user: user,
            token: "token123",
            refreshToken: "refresh123",
            expiresAt: 1640995200000
        )
        
        let authResult2 = AuthResult(
            user: user,
            token: "token123",
            refreshToken: "refresh123",
            expiresAt: 1640995200000
        )
        
        // Then
        XCTAssertEqual(authResult1.user.id, authResult2.user.id)
        XCTAssertEqual(authResult1.token, authResult2.token)
        XCTAssertEqual(authResult1.refreshToken, authResult2.refreshToken)
        XCTAssertEqual(authResult1.expiresAt, authResult2.expiresAt)
    }
}