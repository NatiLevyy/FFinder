import Foundation
import FirebaseAuth
@testable import LocationSharingApp

// MARK: - Mock Firebase Auth

class MockFirebaseAuth: Auth {
    
    // MARK: - Properties
    
    var currentUser: FirebaseAuth.User?
    
    // MARK: - Method Call Tracking
    
    var signInCalled = false
    var createUserCalled = false
    var signOutCalled = false
    var sendPasswordResetCalled = false
    
    // MARK: - Method Results
    
    var signInResult: Result<(MockFirebaseUser, String), Error> = .failure(NSError(domain: "MockError", code: 0))
    var createUserResult: Result<(MockFirebaseUser, String), Error> = .failure(NSError(domain: "MockError", code: 0))
    var signOutResult: Result<Void, Error> = .success(())
    var sendPasswordResetResult: Result<Void, Error> = .success(())
    
    // MARK: - Mock Methods
    
    override func signIn(withEmail email: String, password: String) async throws -> AuthDataResult {
        signInCalled = true
        
        switch signInResult {
        case .success(let (user, token)):
            user.getIDTokenResult = .success(token)
            return MockAuthDataResult(user: user)
        case .failure(let error):
            throw error
        }
    }
    
    override func createUser(withEmail email: String, password: String) async throws -> AuthDataResult {
        createUserCalled = true
        
        switch createUserResult {
        case .success(let (user, token)):
            user.getIDTokenResult = .success(token)
            return MockAuthDataResult(user: user)
        case .failure(let error):
            throw error
        }
    }
    
    override func signOut() throws {
        signOutCalled = true
        
        switch signOutResult {
        case .success:
            currentUser = nil
        case .failure(let error):
            throw error
        }
    }
    
    override func sendPasswordReset(withEmail email: String) async throws {
        sendPasswordResetCalled = true
        
        switch sendPasswordResetResult {
        case .success:
            break
        case .failure(let error):
            throw error
        }
    }
}

// MARK: - Mock Auth Data Result

class MockAuthDataResult: AuthDataResult {
    
    private let mockUser: FirebaseAuth.User
    
    init(user: FirebaseAuth.User) {
        self.mockUser = user
    }
    
    override var user: FirebaseAuth.User {
        return mockUser
    }
}

// MARK: - Mock Firebase User

class MockFirebaseUser: FirebaseAuth.User {
    
    // MARK: - Properties
    
    private let _uid: String
    private let _email: String?
    private let _displayName: String?
    private let _photoURL: URL?
    
    // MARK: - Method Call Tracking
    
    var updateProfileCalled = false
    var getIDTokenCalled = false
    
    // MARK: - Method Results
    
    var getIDTokenResult: Result<String, Error> = .failure(NSError(domain: "MockError", code: 0))
    
    // MARK: - Initialization
    
    init(uid: String, email: String?, displayName: String? = nil, photoURL: URL? = nil) {
        self._uid = uid
        self._email = email
        self._displayName = displayName
        self._photoURL = photoURL
    }
    
    // MARK: - Overrides
    
    override var uid: String {
        return _uid
    }
    
    override var email: String? {
        return _email
    }
    
    override var displayName: String? {
        return _displayName
    }
    
    override var photoURL: URL? {
        return _photoURL
    }
    
    override var metadata: UserMetadata {
        return MockUserMetadata()
    }
    
    override func createProfileChangeRequest() -> UserProfileChangeRequest {
        return MockUserProfileChangeRequest { [weak self] in
            self?.updateProfileCalled = true
        }
    }
    
    override func getIDTokenResult() async throws -> IDTokenResult {
        getIDTokenCalled = true
        
        switch getIDTokenResult {
        case .success(let token):
            return MockIDTokenResult(token: token)
        case .failure(let error):
            throw error
        }
    }
    
    override func getIDTokenResult(forcingRefresh: Bool) async throws -> IDTokenResult {
        getIDTokenCalled = true
        
        switch getIDTokenResult {
        case .success(let token):
            return MockIDTokenResult(token: token)
        case .failure(let error):
            throw error
        }
    }
}

// MARK: - Mock User Metadata

class MockUserMetadata: UserMetadata {
    
    override var creationDate: Date? {
        return Date()
    }
    
    override var lastSignInDate: Date? {
        return Date()
    }
}

// MARK: - Mock User Profile Change Request

class MockUserProfileChangeRequest: UserProfileChangeRequest {
    
    private let onCommit: () -> Void
    
    init(onCommit: @escaping () -> Void) {
        self.onCommit = onCommit
    }
    
    override func commitChanges() async throws {
        onCommit()
    }
}

// MARK: - Mock ID Token Result

class MockIDTokenResult: IDTokenResult {
    
    private let _token: String
    
    init(token: String) {
        self._token = token
    }
    
    override var token: String {
        return _token
    }
    
    override var expirationDate: Date {
        return Date().addingTimeInterval(3600) // 1 hour from now
    }
    
    override var issuedAtDate: Date {
        return Date()
    }
    
    override var authDate: Date {
        return Date()
    }
    
    override var claims: [String : Any] {
        return [
            "sub": "mock_user_id",
            "email": "test@example.com",
            "exp": Int(Date().addingTimeInterval(3600).timeIntervalSince1970)
        ]
    }
}