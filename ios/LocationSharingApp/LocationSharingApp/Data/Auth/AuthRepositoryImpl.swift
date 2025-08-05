import Foundation
import FirebaseAuth
import Combine

/**
 * Implementation of AuthRepository using Firebase Authentication.
 * Handles user authentication, session management, and token operations.
 */
class AuthRepositoryImpl: AuthRepository {
    
    // MARK: - Dependencies
    
    private let firebaseAuth: Auth
    private let jwtTokenManager: JwtTokenManager
    private let sessionManager: SessionManager
    
    // MARK: - Initialization
    
    init(
        firebaseAuth: Auth = Auth.auth(),
        jwtTokenManager: JwtTokenManager = JwtTokenManager.shared,
        sessionManager: SessionManager = SessionManager.shared
    ) {
        self.firebaseAuth = firebaseAuth
        self.jwtTokenManager = jwtTokenManager
        self.sessionManager = sessionManager
    }
    
    // MARK: - AuthRepository Implementation
    
    func signIn(email: String, password: String) async -> Result<AuthResult, AuthError> {
        do {
            let authDataResult = try await firebaseAuth.signIn(withEmail: email, password: password)
            let firebaseUser = authDataResult.user
            
            let user = firebaseUser.toUser()
            let idTokenResult = try await firebaseUser.getIDTokenResult()
            let idToken = idTokenResult.token
            
            let authResult = AuthResult(
                user: user,
                token: idToken,
                refreshToken: firebaseUser.uid, // Firebase handles refresh internally
                expiresAt: Int64(Date().timeIntervalSince1970 * 1000) + (60 * 60 * 1000) // 1 hour
            )
            
            // Store tokens securely
            try await jwtTokenManager.storeTokens(
                accessToken: idToken,
                refreshToken: firebaseUser.uid,
                expiresAt: authResult.expiresAt
            )
            
            // Update session
            await sessionManager.setCurrentUser(user)
            
            return .success(authResult)
        } catch let error as NSError {
            return .failure(error.toAuthError())
        }
    }
    
    func signUp(email: String, password: String, displayName: String) async -> Result<AuthResult, AuthError> {
        do {
            let authDataResult = try await firebaseAuth.createUser(withEmail: email, password: password)
            let firebaseUser = authDataResult.user
            
            // Update user profile with display name
            let changeRequest = firebaseUser.createProfileChangeRequest()
            changeRequest.displayName = displayName
            try await changeRequest.commitChanges()
            
            let user = firebaseUser.toUser(customDisplayName: displayName)
            let idTokenResult = try await firebaseUser.getIDTokenResult()
            let idToken = idTokenResult.token
            
            let authResult = AuthResult(
                user: user,
                token: idToken,
                refreshToken: firebaseUser.uid,
                expiresAt: Int64(Date().timeIntervalSince1970 * 1000) + (60 * 60 * 1000) // 1 hour
            )
            
            // Store tokens securely
            try await jwtTokenManager.storeTokens(
                accessToken: idToken,
                refreshToken: firebaseUser.uid,
                expiresAt: authResult.expiresAt
            )
            
            // Update session
            await sessionManager.setCurrentUser(user)
            
            return .success(authResult)
        } catch let error as NSError {
            return .failure(error.toAuthError())
        }
    }
    
    func signOut() async -> Result<Void, AuthError> {
        do {
            try firebaseAuth.signOut()
            try await jwtTokenManager.clearTokens()
            await sessionManager.clearCurrentUser()
            return .success(())
        } catch let error as NSError {
            return .failure(error.toAuthError())
        }
    }
    
    func getCurrentUser() async -> User? {
        guard let firebaseUser = firebaseAuth.currentUser else { return nil }
        return firebaseUser.toUser()
    }
    
    func refreshToken() async -> Result<String, AuthError> {
        guard let firebaseUser = firebaseAuth.currentUser else {
            return .failure(.userNotFound)
        }
        
        do {
            let idTokenResult = try await firebaseUser.getIDTokenResult(forcingRefresh: true)
            let newToken = idTokenResult.token
            
            // Update stored token
            let expiresAt = Int64(Date().timeIntervalSince1970 * 1000) + (60 * 60 * 1000) // 1 hour
            try await jwtTokenManager.storeTokens(
                accessToken: newToken,
                refreshToken: firebaseUser.uid,
                expiresAt: expiresAt
            )
            
            return .success(newToken)
        } catch let error as NSError {
            return .failure(error.toAuthError())
        }
    }
    
    func isUserAuthenticated() async -> Bool {
        guard firebaseAuth.currentUser != nil else { return false }
        let hasValidToken = await jwtTokenManager.isTokenValid()
        return hasValidToken
    }
    
    func resetPassword(email: String) async -> Result<Void, AuthError> {
        do {
            try await firebaseAuth.sendPasswordReset(withEmail: email)
            return .success(())
        } catch let error as NSError {
            return .failure(error.toAuthError())
        }
    }
}

// MARK: - Firebase User Extensions

private extension FirebaseAuth.User {
    
    /**
     * Converts Firebase User to our User model.
     */
    func toUser(customDisplayName: String? = nil) -> User {
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        return User(
            id: uid,
            email: email ?? "",
            displayName: customDisplayName ?? displayName ?? "",
            profileImageUrl: photoURL?.absoluteString,
            createdAt: metadata.creationDate?.timeIntervalSince1970.milliseconds ?? currentTime,
            lastActiveAt: currentTime
        )
    }
}

// MARK: - Error Mapping Extensions

private extension NSError {
    
    /**
     * Converts NSError from Firebase Auth to our AuthError.
     */
    func toAuthError() -> AuthError {
        guard let errorCode = AuthErrorCode.Code(rawValue: code) else {
            return .unknown(localizedDescription)
        }
        
        switch errorCode {
        case .invalidEmail:
            return .invalidEmail
        case .wrongPassword, .invalidCredential:
            return .invalidCredentials
        case .userNotFound:
            return .userNotFound
        case .userDisabled:
            return .userDisabled
        case .emailAlreadyInUse:
            return .emailAlreadyInUse
        case .weakPassword:
            return .weakPassword
        case .tooManyRequests:
            return .tooManyRequests
        case .networkError:
            return .networkError
        default:
            return .unknown(localizedDescription)
        }
    }
}

// MARK: - Helper Extensions

private extension TimeInterval {
    var milliseconds: Int64 {
        return Int64(self * 1000)
    }
}