import Foundation
import Firebase
@testable import LocationSharingApp

/**
 * Mock implementation of Firebase Auth for iOS integration testing.
 * Provides controlled responses for authentication operations without
 * requiring actual Firebase connectivity.
 */
class MockFirebaseAuth {
    
    static let shared = MockFirebaseAuth()
    
    private var isSignedIn = false
    private var shouldFailAuth = false
    private var authError: Error?
    private var mockUser: MockFirebaseUser?
    
    private init() {}
    
    /**
     * Configures the mock to simulate successful authentication.
     */
    func simulateSuccessfulAuth(email: String = "test@example.com", uid: String = "test-uid") {
        isSignedIn = true
        shouldFailAuth = false
        authError = nil
        mockUser = MockFirebaseUser(email: email, uid: uid)
    }
    
    /**
     * Configures the mock to simulate authentication failure.
     */
    func simulateAuthFailure(error: Error) {
        isSignedIn = false
        shouldFailAuth = true
        authError = error
        mockUser = nil
    }
    
    /**
     * Configures the mock to simulate signed out state.
     */
    func simulateSignedOut() {
        isSignedIn = false
        shouldFailAuth = false
        authError = nil
        mockUser = nil
    }
    
    // MARK: - Mock Firebase Auth Methods
    
    var currentUser: MockFirebaseUser? {
        return isSignedIn ? mockUser : nil
    }
    
    func signIn(withEmail email: String, password: String) async throws -> AuthDataResult {
        if shouldFailAuth {
            throw authError ?? NSError(domain: "AuthError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Authentication failed"])
        }
        
        isSignedIn = true
        let result = MockAuthDataResult(user: mockUser!)
        return result
    }
    
    func createUser(withEmail email: String, password: String) async throws -> AuthDataResult {
        if shouldFailAuth {
            throw authError ?? NSError(domain: "AuthError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Registration failed"])
        }
        
        isSignedIn = true
        mockUser = MockFirebaseUser(email: email, uid: "new-user-\(UUID().uuidString)")
        let result = MockAuthDataResult(user: mockUser!)
        return result
    }
    
    func signOut() throws {
        isSignedIn = false
        mockUser = nil
    }
    
    func addStateDidChangeListener(_ listener: @escaping (Auth, User?) -> Void) -> AuthStateDidChangeListenerHandle {
        // Immediately call listener with current state
        listener(Auth.auth(), currentUser)
        return AuthStateDidChangeListenerHandle()
    }
    
    func removeStateDidChangeListener(_ listenerHandle: AuthStateDidChangeListenerHandle) {
        // No-op for mock
    }
}

// MARK: - Mock Classes

class MockFirebaseUser: User {
    private let _email: String
    private let _uid: String
    
    init(email: String, uid: String) {
        self._email = email
        self._uid = uid
        super.init()
    }
    
    override var email: String? {
        return _email
    }
    
    override var uid: String {
        return _uid
    }
    
    override var isEmailVerified: Bool {
        return true
    }
    
    override func getIDToken() async throws -> String {
        return "mock-id-token-\(uid)"
    }
}

class MockAuthDataResult: AuthDataResult {
    private let _user: User
    
    init(user: User) {
        self._user = user
        super.init()
    }
    
    override var user: User {
        return _user
    }
}

// Placeholder for listener handle
class AuthStateDidChangeListenerHandle {
    // Empty implementation for mock
}