import XCTest
@testable import LocationSharingApp

class AuthRepositoryTests: XCTestCase {
    
    var mockAuthRepository: MockAuthRepository!
    
    override func setUp() {
        super.setUp()
        mockAuthRepository = MockAuthRepository()
    }
    
    func testSignInSuccess() async {
        // Given
        let email = "test@example.com"
        let password = "password123"
        let expectedUser = User(
            id: "user123",
            email: email,
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        let expectedAuthResult = AuthResult(
            user: expectedUser,
            token: "access_token",
            refreshToken: "refresh_token",
            expiresAt: 1640995200000
        )
        
        mockAuthRepository.signInResult = .success(expectedAuthResult)
        
        // When
        let result = await mockAuthRepository.signIn(email: email, password: password)
        
        // Then
        switch result {
        case .success(let authResult):
            XCTAssertEqual(authResult.user.id, expectedUser.id)
            XCTAssertEqual(authResult.user.email, expectedUser.email)
            XCTAssertEqual(authResult.token, expectedAuthResult.token)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testSignInFailure() async {
        // Given
        let email = "test@example.com"
        let password = "wrong_password"
        mockAuthRepository.signInResult = .failure(.invalidCredentials)
        
        // When
        let result = await mockAuthRepository.signIn(email: email, password: password)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            XCTAssertEqual(error.errorDescription, AuthError.invalidCredentials.errorDescription)
        }
    }
    
    func testSignUpSuccess() async {
        // Given
        let email = "test@example.com"
        let password = "password123"
        let displayName = "Test User"
        let expectedUser = User(
            id: "user123",
            email: email,
            displayName: displayName,
            profileImageUrl: nil,
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        let expectedAuthResult = AuthResult(
            user: expectedUser,
            token: "access_token",
            refreshToken: "refresh_token",
            expiresAt: 1640995200000
        )
        
        mockAuthRepository.signUpResult = .success(expectedAuthResult)
        
        // When
        let result = await mockAuthRepository.signUp(email: email, password: password, displayName: displayName)
        
        // Then
        switch result {
        case .success(let authResult):
            XCTAssertEqual(authResult.user.displayName, displayName)
            XCTAssertEqual(authResult.user.email, email)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testSignOutSuccess() async {
        // Given
        mockAuthRepository.signOutResult = .success(())
        
        // When
        let result = await mockAuthRepository.signOut()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true) // Success case
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testGetCurrentUserWhenAuthenticated() async {
        // Given
        let expectedUser = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        mockAuthRepository.currentUser = expectedUser
        
        // When
        let result = await mockAuthRepository.getCurrentUser()
        
        // Then
        XCTAssertEqual(result?.id, expectedUser.id)
        XCTAssertEqual(result?.email, expectedUser.email)
    }
    
    func testGetCurrentUserWhenNotAuthenticated() async {
        // Given
        mockAuthRepository.currentUser = nil
        
        // When
        let result = await mockAuthRepository.getCurrentUser()
        
        // Then
        XCTAssertNil(result)
    }
    
    func testRefreshTokenSuccess() async {
        // Given
        let expectedToken = "new_access_token"
        mockAuthRepository.refreshTokenResult = .success(expectedToken)
        
        // When
        let result = await mockAuthRepository.refreshToken()
        
        // Then
        switch result {
        case .success(let token):
            XCTAssertEqual(token, expectedToken)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testIsUserAuthenticatedTrue() async {
        // Given
        mockAuthRepository.isAuthenticated = true
        
        // When
        let result = await mockAuthRepository.isUserAuthenticated()
        
        // Then
        XCTAssertTrue(result)
    }
    
    func testIsUserAuthenticatedFalse() async {
        // Given
        mockAuthRepository.isAuthenticated = false
        
        // When
        let result = await mockAuthRepository.isUserAuthenticated()
        
        // Then
        XCTAssertFalse(result)
    }
    
    func testResetPasswordSuccess() async {
        // Given
        let email = "test@example.com"
        mockAuthRepository.resetPasswordResult = .success(())
        
        // When
        let result = await mockAuthRepository.resetPassword(email: email)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true) // Success case
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
}

// MARK: - Mock Implementation

class MockAuthRepository: AuthRepository {
    var signInResult: Result<AuthResult, AuthError> = .failure(.unknown("Not configured"))
    var signUpResult: Result<AuthResult, AuthError> = .failure(.unknown("Not configured"))
    var signOutResult: Result<Void, AuthError> = .failure(.unknown("Not configured"))
    var currentUser: User?
    var refreshTokenResult: Result<String, AuthError> = .failure(.unknown("Not configured"))
    var isAuthenticated: Bool = false
    var resetPasswordResult: Result<Void, AuthError> = .failure(.unknown("Not configured"))
    
    func signIn(email: String, password: String) async -> Result<AuthResult, AuthError> {
        return signInResult
    }
    
    func signUp(email: String, password: String, displayName: String) async -> Result<AuthResult, AuthError> {
        return signUpResult
    }
    
    func signOut() async -> Result<Void, AuthError> {
        return signOutResult
    }
    
    func getCurrentUser() async -> User? {
        return currentUser
    }
    
    func refreshToken() async -> Result<String, AuthError> {
        return refreshTokenResult
    }
    
    func isUserAuthenticated() async -> Bool {
        return isAuthenticated
    }
    
    func resetPassword(email: String) async -> Result<Void, AuthError> {
        return resetPasswordResult
    }
}