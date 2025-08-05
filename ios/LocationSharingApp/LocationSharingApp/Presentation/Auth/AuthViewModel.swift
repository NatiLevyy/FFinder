import Foundation
import Combine

/**
 * ViewModel for managing authentication state and form validation.
 * 
 * Handles login and registration flows with proper error handling
 * and form validation according to requirements 1.1, 1.5, 1.6.
 */
@MainActor
class AuthViewModel: ObservableObject {
    
    @Published var uiState = AuthUiState()
    
    private let authRepository: AuthRepository
    private var cancellables = Set<AnyCancellable>()
    
    init(authRepository: AuthRepository) {
        self.authRepository = authRepository
    }
    
    /**
     * Attempts to sign in the user with provided credentials.
     * 
     * - Parameters:
     *   - email: User's email address
     *   - password: User's password
     */
    func signIn(email: String, password: String) {
        guard validateSignInForm(email: email, password: password) else { return }
        
        uiState.isLoading = true
        uiState.error = nil
        
        Task {
            let result = await authRepository.signIn(email: email, password: password)
            
            switch result {
            case .success(let authResult):
                uiState.isLoading = false
                uiState.isAuthenticated = true
                uiState.authResult = authResult
            case .failure(let error):
                uiState.isLoading = false
                uiState.error = error.localizedDescription
            }
        }
    }
    
    /**
     * Attempts to register a new user with provided information.
     * 
     * - Parameters:
     *   - email: User's email address
     *   - password: User's password
     *   - displayName: User's display name
     */
    func signUp(email: String, password: String, displayName: String) {
        guard validateSignUpForm(email: email, password: password, displayName: displayName) else { return }
        
        uiState.isLoading = true
        uiState.error = nil
        
        Task {
            let result = await authRepository.signUp(email: email, password: password, displayName: displayName)
            
            switch result {
            case .success(let authResult):
                uiState.isLoading = false
                uiState.isAuthenticated = true
                uiState.authResult = authResult
            case .failure(let error):
                uiState.isLoading = false
                uiState.error = error.localizedDescription
            }
        }
    }
    
    /**
     * Initiates password reset for the provided email.
     * 
     * - Parameter email: User's email address
     */
    func resetPassword(email: String) {
        guard isValidEmail(email) else {
            uiState.error = "Please enter a valid email address"
            return
        }
        
        uiState.isLoading = true
        uiState.error = nil
        
        Task {
            let result = await authRepository.resetPassword(email: email)
            
            switch result {
            case .success:
                uiState.isLoading = false
                uiState.passwordResetSent = true
            case .failure(let error):
                uiState.isLoading = false
                uiState.error = error.localizedDescription
            }
        }
    }
    
    /**
     * Clears any error messages from the UI state.
     */
    func clearError() {
        uiState.error = nil
    }
    
    /**
     * Clears the password reset sent flag.
     */
    func clearPasswordResetSent() {
        uiState.passwordResetSent = false
    }
    
    // MARK: - Private Methods
    
    /**
     * Validates the sign-in form inputs.
     * 
     * - Parameters:
     *   - email: User's email address
     *   - password: User's password
     * - Returns: true if form is valid, false otherwise
     */
    private func validateSignInForm(email: String, password: String) -> Bool {
        if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            uiState.error = "Email is required"
            return false
        }
        
        if !isValidEmail(email) {
            uiState.error = "Please enter a valid email address"
            return false
        }
        
        if password.isEmpty {
            uiState.error = "Password is required"
            return false
        }
        
        return true
    }
    
    /**
     * Validates the sign-up form inputs.
     * 
     * - Parameters:
     *   - email: User's email address
     *   - password: User's password
     *   - displayName: User's display name
     * - Returns: true if form is valid, false otherwise
     */
    private func validateSignUpForm(email: String, password: String, displayName: String) -> Bool {
        let trimmedDisplayName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        
        if trimmedEmail.isEmpty {
            uiState.error = "Email is required"
            return false
        }
        
        if !isValidEmail(trimmedEmail) {
            uiState.error = "Please enter a valid email address"
            return false
        }
        
        if password.isEmpty {
            uiState.error = "Password is required"
            return false
        }
        
        if password.count < 6 {
            uiState.error = "Password must be at least 6 characters"
            return false
        }
        
        if trimmedDisplayName.isEmpty {
            uiState.error = "Display name is required"
            return false
        }
        
        if trimmedDisplayName.count < 2 {
            uiState.error = "Display name must be at least 2 characters"
            return false
        }
        
        return true
    }
    
    /**
     * Validates email format using NSPredicate.
     * 
     * - Parameter email: Email address to validate
     * - Returns: true if email format is valid, false otherwise
     */
    private func isValidEmail(_ email: String) -> Bool {
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", emailRegex)
        return emailPredicate.evaluate(with: email)
    }
}

/**
 * Represents the UI state for authentication screens.
 */
struct AuthUiState {
    var isLoading: Bool = false
    var isAuthenticated: Bool = false
    var authResult: AuthResult? = nil
    var error: String? = nil
    var passwordResetSent: Bool = false
}