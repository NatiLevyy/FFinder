import XCTest
import Combine
@testable import LocationSharingApp

/**
 * Unit tests for AuthViewModel covering form validation and authentication state management.
 * Tests requirements 1.1, 1.5, 1.6.
 */
@MainActor
class AuthViewModelTests: XCTestCase {
    
    var viewModel: AuthViewModel!
    var mockAuthRepository: MockAuthRepository!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        mockAuthRepository = MockAuthRepository()
        viewModel = AuthViewModel(authRepository: mockAuthRepository)
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        viewModel = nil
        mockAuthRepository = nil
        cancellables = nil
        super.tearDown()
    }
    
    func testInitialState_shouldBeCorrect() {
        // Then
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(viewModel.uiState.isAuthenticated)
        XCTAssertNil(viewModel.uiState.authResult)
        XCTAssertNil(viewModel.uiState.error)
        XCTAssertFalse(viewModel.uiState.passwordResetSent)
    }
    
    func testSignIn_withValidCredentials_shouldSucceed() async {
        // Given
        let testAuthResult = createTestAuthResult()
        mockAuthRepository.signInResult = .success(testAuthResult)
        
        // When
        viewModel.signIn(email: "test@example.com", password: "password123")
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertTrue(viewModel.uiState.isAuthenticated)
        XCTAssertEqual(viewModel.uiState.authResult?.user.email, "test@example.com")
        XCTAssertNil(viewModel.uiState.error)
    }
    
    func testSignIn_withInvalidCredentials_shouldShowError() async {
        // Given
        mockAuthRepository.signInResult = .failure(.invalidCredentials)
        
        // When
        viewModel.signIn(email: "test@example.com", password: "wrongpassword")
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(viewModel.uiState.isAuthenticated)
        XCTAssertNil(viewModel.uiState.authResult)
        XCTAssertEqual(viewModel.uiState.error, "Invalid email or password")
    }
    
    func testSignIn_withEmptyEmail_shouldShowValidationError() {
        // When
        viewModel.signIn(email: "", password: "password123")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Email is required")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(mockAuthRepository.signInCalled)
    }
    
    func testSignIn_withInvalidEmail_shouldShowValidationError() {
        // When
        viewModel.signIn(email: "invalid-email", password: "password123")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Please enter a valid email address")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(mockAuthRepository.signInCalled)
    }
    
    func testSignIn_withEmptyPassword_shouldShowValidationError() {
        // When
        viewModel.signIn(email: "test@example.com", password: "")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Password is required")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(mockAuthRepository.signInCalled)
    }
    
    func testSignUp_withValidData_shouldSucceed() async {
        // Given
        let testAuthResult = createTestAuthResult()
        mockAuthRepository.signUpResult = .success(testAuthResult)
        
        // When
        viewModel.signUp(email: "test@example.com", password: "password123", displayName: "Test User")
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertTrue(viewModel.uiState.isAuthenticated)
        XCTAssertEqual(viewModel.uiState.authResult?.user.displayName, "Test User")
        XCTAssertNil(viewModel.uiState.error)
    }
    
    func testSignUp_withExistingEmail_shouldShowError() async {
        // Given
        mockAuthRepository.signUpResult = .failure(.emailAlreadyInUse)
        
        // When
        viewModel.signUp(email: "test@example.com", password: "password123", displayName: "Test User")
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(viewModel.uiState.isAuthenticated)
        XCTAssertNil(viewModel.uiState.authResult)
        XCTAssertEqual(viewModel.uiState.error, "Email address is already in use")
    }
    
    func testSignUp_withEmptyDisplayName_shouldShowValidationError() {
        // When
        viewModel.signUp(email: "test@example.com", password: "password123", displayName: "")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Display name is required")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(mockAuthRepository.signUpCalled)
    }
    
    func testSignUp_withShortDisplayName_shouldShowValidationError() {
        // When
        viewModel.signUp(email: "test@example.com", password: "password123", displayName: "A")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Display name must be at least 2 characters")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(mockAuthRepository.signUpCalled)
    }
    
    func testSignUp_withShortPassword_shouldShowValidationError() {
        // When
        viewModel.signUp(email: "test@example.com", password: "123", displayName: "Test User")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Password must be at least 6 characters")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(mockAuthRepository.signUpCalled)
    }
    
    func testResetPassword_withValidEmail_shouldSucceed() async {
        // Given
        mockAuthRepository.resetPasswordResult = .success(())
        
        // When
        viewModel.resetPassword(email: "test@example.com")
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertTrue(viewModel.uiState.passwordResetSent)
        XCTAssertNil(viewModel.uiState.error)
    }
    
    func testResetPassword_withInvalidEmail_shouldShowValidationError() {
        // When
        viewModel.resetPassword(email: "invalid-email")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Please enter a valid email address")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(viewModel.uiState.passwordResetSent)
        XCTAssertFalse(mockAuthRepository.resetPasswordCalled)
    }
    
    func testResetPassword_withEmptyEmail_shouldShowValidationError() {
        // When
        viewModel.resetPassword(email: "")
        
        // Then
        XCTAssertEqual(viewModel.uiState.error, "Please enter a valid email address")
        XCTAssertFalse(viewModel.uiState.isLoading)
        XCTAssertFalse(viewModel.uiState.passwordResetSent)
        XCTAssertFalse(mockAuthRepository.resetPasswordCalled)
    }
    
    func testClearError_shouldClearErrorMessage() {
        // Given - trigger an error first
        viewModel.signIn(email: "", password: "password")
        XCTAssertEqual(viewModel.uiState.error, "Email is required")
        
        // When
        viewModel.clearError()
        
        // Then
        XCTAssertNil(viewModel.uiState.error)
    }
    
    func testClearPasswordResetSent_shouldClearPasswordResetFlag() async {
        // Given
        mockAuthRepository.resetPasswordResult = .success(())
        viewModel.resetPassword(email: "test@example.com")
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        XCTAssertTrue(viewModel.uiState.passwordResetSent)
        
        // When
        viewModel.clearPasswordResetSent()
        
        // Then
        XCTAssertFalse(viewModel.uiState.passwordResetSent)
    }
    
    func testLoadingState_shouldBeManagedCorrectlyDuringSignIn() async {
        // Given
        let testAuthResult = createTestAuthResult()
        mockAuthRepository.signInResult = .success(testAuthResult)
        mockAuthRepository.signInDelay = 0.2 // 200ms delay
        
        // When
        viewModel.signIn(email: "test@example.com", password: "password123")
        
        // Then - loading should be true initially
        XCTAssertTrue(viewModel.uiState.isLoading)
        
        // Wait for async operation to complete
        try? await Task.sleep(nanoseconds: 300_000_000) // 0.3 seconds
        
        // Then - loading should be false after completion
        XCTAssertFalse(viewModel.uiState.isLoading)
    }
    
    // MARK: - Helper Methods
    
    private func createTestAuthResult() -> AuthResult {
        let testUser = User(
            id: "test-id",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970),
            lastActiveAt: Int64(Date().timeIntervalSince1970)
        )
        
        return AuthResult(
            user: testUser,
            token: "test-token",
            refreshToken: "test-refresh-token",
            expiresAt: Int64(Date().timeIntervalSince1970) + 3600
        )
    }
}

// MARK: - Mock AuthRepository

class MockAuthRepository: AuthRepository {
    var signInResult: Result<AuthResult, AuthError> = .failure(.invalidCredentials)
    var signUpResult: Result<AuthResult, AuthError> = .failure(.emailAlreadyInUse)
    var resetPasswordResult: Result<Void, AuthError> = .success(())
    
    var signInCalled = false
    var signUpCalled = false
    var resetPasswordCalled = false
    
    var signInDelay: TimeInterval = 0.1
    var signUpDelay: TimeInterval = 0.1
    var resetPasswordDelay: TimeInterval = 0.1
    
    func signIn(email: String, password: String) async -> Result<AuthResult, AuthError> {
        signInCalled = true
        try? await Task.sleep(nanoseconds: UInt64(signInDelay * 1_000_000_000))
        return signInResult
    }
    
    func signUp(email: String, password: String, displayName: String) async -> Result<AuthResult, AuthError> {
        signUpCalled = true
        try? await Task.sleep(nanoseconds: UInt64(signUpDelay * 1_000_000_000))
        return signUpResult
    }
    
    func signOut() async -> Result<Void, AuthError> {
        return .success(())
    }
    
    func getCurrentUser() async -> User? {
        return nil
    }
    
    func refreshToken() async -> Result<String, AuthError> {
        return .failure(.tokenExpired)
    }
    
    func isUserAuthenticated() async -> Bool {
        return false
    }
    
    func resetPassword(email: String) async -> Result<Void, AuthError> {
        resetPasswordCalled = true
        try? await Task.sleep(nanoseconds: UInt64(resetPasswordDelay * 1_000_000_000))
        return resetPasswordResult
    }
}