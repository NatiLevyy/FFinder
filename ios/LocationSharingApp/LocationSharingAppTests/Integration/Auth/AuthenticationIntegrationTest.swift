import XCTest
import Foundation
@testable import LocationSharingApp

/**
 * Integration tests for iOS authentication flow covering the complete
 * authentication process from UI to Firebase backend.
 */
class AuthenticationIntegrationTest: BaseIntegrationTest {
    
    var authRepository: AuthRepositoryImpl!
    var sessionManager: SessionManager!
    var mockFirebaseAuth: MockFirebaseAuth!
    
    private let testEmail = "test@example.com"
    private let testPassword = "password123"
    private let testUid = "test-user-id"
    private let testDisplayName = "Test User"
    
    override func setUp() {
        super.setUp()
        
        // Initialize dependencies
        mockFirebaseAuth = MockFirebaseAuth.shared
        sessionManager = SessionManager()
        authRepository = AuthRepositoryImpl(sessionManager: sessionManager)
        
        // Reset auth state before each test
        mockFirebaseAuth.simulateSignedOut()
        try? sessionManager.clearSession()
    }
    
    override func tearDown() {
        authRepository = nil
        sessionManager = nil
        mockFirebaseAuth = nil
        super.tearDown()
    }
    
    func testSuccessfulLoginFlow() async throws {
        // Given: Mock Firebase Auth is configured for successful login
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        
        // When: User attempts to sign in
        let result = try await authRepository.signIn(email: testEmail, password: testPassword)
        
        // Then: Authentication should succeed
        XCTAssertTrue(result.isSuccess, "Sign in should succeed")
        let user = try result.get()
        XCTAssertEqual(user.email, testEmail, "Email should match")
        XCTAssertEqual(user.id, testUid, "UID should match")
        
        // And: Session should be established
        let currentUser = sessionManager.getCurrentUser()
        XCTAssertNotNil(currentUser, "Current user should be set")
        XCTAssertEqual(currentUser?.email, testEmail, "Session user email should match")
    }
    
    func testFailedLoginWithInvalidCredentials() async throws {
        // Given: Mock Firebase Auth is configured to fail with invalid credentials
        let error = NSError(domain: "FirebaseAuthError", code: 17009, userInfo: [NSLocalizedDescriptionKey: "Invalid email"])
        mockFirebaseAuth.simulateAuthFailure(error: error)
        
        // When: User attempts to sign in with invalid credentials
        let result = try await authRepository.signIn(email: "invalid@email", password: "wrongpassword")
        
        // Then: Authentication should fail
        XCTAssertTrue(result.isFailure, "Sign in should fail")
        
        // And: No session should be established
        let currentUser = sessionManager.getCurrentUser()
        XCTAssertNil(currentUser, "Current user should be nil")
    }
    
    func testSuccessfulRegistrationFlow() async throws {
        // Given: Mock Firebase Auth is configured for successful registration
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        
        // When: User attempts to register
        let result = try await authRepository.signUp(email: testEmail, password: testPassword, displayName: testDisplayName)
        
        // Then: Registration should succeed
        XCTAssertTrue(result.isSuccess, "Registration should succeed")
        let user = try result.get()
        XCTAssertEqual(user.email, testEmail, "Email should match")
        XCTAssertEqual(user.id, testUid, "UID should match")
        XCTAssertEqual(user.displayName, testDisplayName, "Display name should match")
        
        // And: Session should be established
        let currentUser = sessionManager.getCurrentUser()
        XCTAssertNotNil(currentUser, "Current user should be set")
    }
    
    func testFailedRegistrationWithExistingUser() async throws {
        // Given: Mock Firebase Auth is configured to fail with user collision
        let error = NSError(domain: "FirebaseAuthError", code: 17007, userInfo: [NSLocalizedDescriptionKey: "Email already in use"])
        mockFirebaseAuth.simulateAuthFailure(error: error)
        
        // When: User attempts to register with existing email
        let result = try await authRepository.signUp(email: testEmail, password: testPassword, displayName: testDisplayName)
        
        // Then: Registration should fail
        XCTAssertTrue(result.isFailure, "Registration should fail")
        
        // And: No session should be established
        let currentUser = sessionManager.getCurrentUser()
        XCTAssertNil(currentUser, "Current user should be nil")
    }
    
    func testSignOutFlow() async throws {
        // Given: User is signed in
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        _ = try await authRepository.signIn(email: testEmail, password: testPassword)
        
        // Verify user is signed in
        let userBeforeSignOut = sessionManager.getCurrentUser()
        XCTAssertNotNil(userBeforeSignOut, "User should be signed in")
        
        // When: User signs out
        let result = try await authRepository.signOut()
        
        // Then: Sign out should succeed
        XCTAssertTrue(result.isSuccess, "Sign out should succeed")
        
        // And: Session should be cleared
        let userAfterSignOut = sessionManager.getCurrentUser()
        XCTAssertNil(userAfterSignOut, "User should be signed out")
    }
    
    func testSessionPersistence() async throws {
        // Given: User signs in successfully
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        _ = try await authRepository.signIn(email: testEmail, password: testPassword)
        
        // When: Session is retrieved after sign in
        let currentUser = sessionManager.getCurrentUser()
        
        // Then: Session should persist user information
        XCTAssertNotNil(currentUser, "Current user should be available")
        XCTAssertEqual(currentUser?.email, testEmail, "Email should be persisted")
        XCTAssertEqual(currentUser?.id, testUid, "UID should be persisted")
        
        // And: Session should indicate user is authenticated
        XCTAssertTrue(sessionManager.isUserAuthenticated(), "User should be authenticated")
    }
    
    func testTokenRefreshFlow() async throws {
        // Given: User is signed in with a token that needs refresh
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        _ = try await authRepository.signIn(email: testEmail, password: testPassword)
        
        // When: Token refresh is requested
        let result = try await authRepository.refreshToken()
        
        // Then: Token refresh should succeed
        XCTAssertTrue(result.isSuccess, "Token refresh should succeed")
        let newToken = try result.get()
        XCTAssertFalse(newToken.isEmpty, "Token should not be empty")
        XCTAssertTrue(newToken.contains("mock-id-token"), "Token should be mock token")
    }
    
    func testCompleteAuthenticationFlow() async throws {
        // This test covers the complete authentication flow from sign up to sign out
        
        // Step 1: Register new user
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        let registrationResult = try await authRepository.signUp(email: testEmail, password: testPassword, displayName: testDisplayName)
        XCTAssertTrue(registrationResult.isSuccess, "Registration should succeed")
        
        // Step 2: Verify session is established
        var currentUser = sessionManager.getCurrentUser()
        XCTAssertNotNil(currentUser, "User should be signed in after registration")
        
        // Step 3: Sign out
        let signOutResult = try await authRepository.signOut()
        XCTAssertTrue(signOutResult.isSuccess, "Sign out should succeed")
        
        // Step 4: Verify session is cleared
        currentUser = sessionManager.getCurrentUser()
        XCTAssertNil(currentUser, "User should be signed out")
        
        // Step 5: Sign in again
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        let signInResult = try await authRepository.signIn(email: testEmail, password: testPassword)
        XCTAssertTrue(signInResult.isSuccess, "Sign in should succeed")
        
        // Step 6: Verify session is re-established
        currentUser = sessionManager.getCurrentUser()
        XCTAssertNotNil(currentUser, "User should be signed in again")
        XCTAssertEqual(currentUser?.email, testEmail, "Email should match")
    }
    
    func testAuthenticationStateChanges() async throws {
        // This test verifies that authentication state changes are properly handled
        
        let expectation = XCTestExpectation(description: "Auth state change")
        var stateChangeCount = 0
        
        // Monitor auth state changes
        let handle = mockFirebaseAuth.addStateDidChangeListener { auth, user in
            stateChangeCount += 1
            if stateChangeCount == 2 { // Initial state + sign in
                expectation.fulfill()
            }
        }
        
        // Sign in user
        mockFirebaseAuth.simulateSuccessfulAuth(email: testEmail, uid: testUid)
        _ = try await authRepository.signIn(email: testEmail, password: testPassword)
        
        // Wait for state change
        await fulfillment(of: [expectation], timeout: 5.0)
        
        // Cleanup
        mockFirebaseAuth.removeStateDidChangeListener(handle)
        
        XCTAssertGreaterThanOrEqual(stateChangeCount, 1, "Auth state should have changed")
    }
}