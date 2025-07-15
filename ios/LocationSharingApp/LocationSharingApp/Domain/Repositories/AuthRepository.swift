import Foundation
import Combine

protocol AuthRepository {
    func signIn(email: String, password: String) async -> Result<AuthResult, AuthError>
    func signUp(email: String, password: String, displayName: String) async -> Result<AuthResult, AuthError>
    func signOut() async -> Result<Void, AuthError>
    func getCurrentUser() async -> User?
    func refreshToken() async -> Result<String, AuthError>
    func isUserAuthenticated() async -> Bool
    func resetPassword(email: String) async -> Result<Void, AuthError>
}