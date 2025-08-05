import XCTest
import FirebaseAuth
@testable import LocationSharingApp

class AuthRepositoryImplTests: XCTestCase {
    
    var authRepository: AuthRepositoryImpl!
    var mockFirebaseAuth: MockFirebaseAuth!
    var mockJwtTokenManager: MockJwtTokenManager!
    var mockSessionManager: MockSessionManager!
    
    override func setUp() {
        super.setUp()
        mockFirebaseAuth = MockFirebaseAuth()
        mockJwtTokenManager = MockJwtTokenManager()
        mockSessionManager = MockSessionManager()
        
        authRepository = AuthRepositoryImpl(
            firebaseAuth: mockFirebaseAuth,
            jwtTokenManager: mockJwtTokenManager,
            sessionManager: mockSessionManager
        )
    }
    
    override func tearDown() {
        authRepository = nil
        mockFirebaseAuth = nil
        mockJwtTokenManager = nil
        mockSessionManager = nil
        super.tearDown()
    }
    
    // MARK: - Sign In Tests
    
    func testSignInWithValidCredentials_ReturnsSuccess() async {
        // Given
        let email = "test@example.com"
        let password = "password123"
        let userId = "user123"
        let displayName = "Test User"
        let idToken = "mock_id_token"
        
        let mockUser = MockFirebaseUser(
            uid: userId,
            email: email,
            displayName: displayName
        )
        
        mockFirebaseAuth.signInResult = .success((mockUser, idToken))
        mockJwtTokenManager.storeTokensResult = .success(())
        
        // When
        let result = await authRepository.signIn(email: email, password: password)
        
        // Then
        switch result {
        case .success(let authResult):
            XCTAssertEqual(authResult.user.id, userId)
            XCTAssertEqual(authResult.user.email, email)
            XCTAssertEqual(authResult.user.displayName, displayName)
            XCTAssertEqual(authResult.token, idToken)
            XCTAssertTrue(mockJwtTokenManager.storeTokensCalled)
            XCTAssertTrue(mockSessionManager.setCurrentUserCalled)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testSignInWithInvalidCredentials_ReturnsFailure() async {
        // Given
        let email = "test@example.com"
        let password = "wrongpassword"
        let error = NSError(domain: "FirebaseAuthErrorDomain", code: AuthErrorCode.wrongPassword.rawValue)
        
        mockFirebaseAuth.signInResult = .failure(error)
        
        // When
        let result = await authRepository.signIn(email: email, password: password)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let authError):
            XCTAssertEqual(authError, .invalidCredentials)
        }
    }
    
    // MARK: - Sign Up Tests
    
    func testSignUpWithValidData_ReturnsSuccess() async {
        // Given
        let email = "newuser@example.com"
        let password = "password123"
        let displayName = "New User"
        let userId = "newuser123"
        let idToken = "mock_id_token"
        
        let mockUser = MockFirebaseUser(
            uid: userId,
            email: email,
            displayName: displayName
        )
        
        mockFirebaseAuth.createUserResult = .success((mockUser, idToken))
        mockJwtTokenManager.storeTokensResult = .success(())
        
        // When
        let result = await authRepository.signUp(email: email, password: password, displayName: displayName)
        
        // Then
        switch result {
        case .success(let authResult):
            XCTAssertEqual(authResult.user.id, userId)
            XCTAssertEqual(authResult.user.email, email)
            XCTAssertEqual(authResult.user.displayName, displayName)
            XCTAssertTrue(mockUser.updateProfileCalled)
            XCTAssertTrue(mockJwtTokenManager.storeTokensCalled)
            XCTAssertTrue(mockSessionManager.setCurrentUserCalled)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testSignUpWithExistingEmail_ReturnsFailure() async {
        // Given
        let email = "existing@example.com"
        let password = "password123"
        let displayName = "Test User"
        let error = NSError(domain: "FirebaseAuthErrorDomain", code: AuthErrorCode.emailAlreadyInUse.rawValue)
        
        mockFirebaseAuth.createUserResult = .failure(error)
        
        // When
        let result = await authRepository.signUp(email: email, password: password, displayName: displayName)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let authError):
            XCTAssertEqual(authError, .emailAlreadyInUse)
        }
    }
    
    // MARK: - Sign Out Tests
    
    func testSignOut_ClearsTokensAndSession() async {
        // Given
        mockFirebaseAuth.signOutResult = .success(())
        mockJwtTokenManager.clearTokensResult = .success(())
        
        // When
        let result = await authRepository.signOut()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseAuth.signOutCalled)
            XCTAssertTrue(mockJwtTokenManager.clearTokensCalled)
            XCTAssertTrue(mockSessionManager.clearCurrentUserCalled)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    // MARK: - Get Current User Tests
    
    func testGetCurrentUser_WithAuthenticatedUser_ReturnsUser() async {
        // Given
        let userId = "user123"
        let email = "test@example.com"
        let displayName = "Test User"
        
        let mockUser = MockFirebaseUser(
            uid: userId,
            email: email,
            displayName: displayName
        )
        
        mockFirebaseAuth.currentUser = mockUser
        
        // When
        let user = await authRepository.getCurrentUser()
        
        // Then
        XCTAssertNotNil(user)
        XCTAssertEqual(user?.id, userId)
        XCTAssertEqual(user?.email, email)
        XCTAssertEqual(user?.displayName, displayName)
    }
    
    func testGetCurrentUser_WithoutAuthenticatedUser_ReturnsNil() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let user = await authRepository.getCurrentUser()
        
        // Then
        XCTAssertNil(user)
    }
    
    // MARK: - Refresh Token Tests
    
    func testRefreshToken_WithValidUser_ReturnsNewToken() async {
        // Given
        let userId = "user123"
        let newToken = "new_token"
        
        let mockUser = MockFirebaseUser(uid: userId, email: "test@example.com")
        mockUser.getIDTokenResult = .success(newToken)
        mockFirebaseAuth.currentUser = mockUser
        mockJwtTokenManager.storeTokensResult = .success(())
        
        // When
        let result = await authRepository.refreshToken()
        
        // Then
        switch result {
        case .success(let token):
            XCTAssertEqual(token, newToken)
            XCTAssertTrue(mockJwtTokenManager.storeTokensCalled)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testRefreshToken_WithoutUser_ReturnsFailure() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await authRepository.refreshToken()
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            XCTAssertEqual(error, .userNotFound)
        }
    }
    
    // MARK: - Authentication Status Tests
    
    func testIsUserAuthenticated_WithValidUserAndToken_ReturnsTrue() async {
        // Given
        let mockUser = MockFirebaseUser(uid: "user123", email: "test@example.com")
        mockFirebaseAuth.currentUser = mockUser
        mockJwtTokenManager.isTokenValidResult = true
        
        // When
        let isAuthenticated = await authRepository.isUserAuthenticated()
        
        // Then
        XCTAssertTrue(isAuthenticated)
    }
    
    func testIsUserAuthenticated_WithoutUser_ReturnsFalse() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        mockJwtTokenManager.isTokenValidResult = true
        
        // When
        let isAuthenticated = await authRepository.isUserAuthenticated()
        
        // Then
        XCTAssertFalse(isAuthenticated)
    }
    
    func testIsUserAuthenticated_WithInvalidToken_ReturnsFalse() async {
        // Given
        let mockUser = MockFirebaseUser(uid: "user123", email: "test@example.com")
        mockFirebaseAuth.currentUser = mockUser
        mockJwtTokenManager.isTokenValidResult = false
        
        // When
        let isAuthenticated = await authRepository.isUserAuthenticated()
        
        // Then
        XCTAssertFalse(isAuthenticated)
    }
    
    // MARK: - Reset Password Tests
    
    func testResetPassword_WithValidEmail_ReturnsSuccess() async {
        // Given
        let email = "test@example.com"
        mockFirebaseAuth.sendPasswordResetResult = .success(())
        
        // When
        let result = await authRepository.resetPassword(email: email)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseAuth.sendPasswordResetCalled)
        case .failure:
            XCTFail("Expected success but got failure")
        }
    }
    
    func testResetPassword_WithInvalidEmail_ReturnsFailure() async {
        // Given
        let email = "invalid-email"
        let error = NSError(domain: "FirebaseAuthErrorDomain", code: AuthErrorCode.invalidEmail.rawValue)
        mockFirebaseAuth.sendPasswordResetResult = .failure(error)
        
        // When
        let result = await authRepository.resetPassword(email: email)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let authError):
            XCTAssertEqual(authError, .invalidEmail)
        }
    }
}