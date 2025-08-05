import SwiftUI

/**
 * SwiftUI view for user authentication (sign in).
 * 
 * Handles user login with email and password, form validation,
 * error display, and navigation to main app or registration.
 * Implements requirements 1.1, 1.5, 1.6.
 */
struct LoginView: View {
    @StateObject private var viewModel: AuthViewModel
    @State private var email = ""
    @State private var password = ""
    @State private var showingRegister = false
    @State private var showingPasswordReset = false
    
    init(authRepository: AuthRepository) {
        self._viewModel = StateObject(wrappedValue: AuthViewModel(authRepository: authRepository))
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // App Logo/Title
                    VStack(spacing: 8) {
                        Text("Location Sharing")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                        
                        Text("Welcome back! Please sign in to continue.")
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, 48)
                    
                    // Form Fields
                    VStack(spacing: 16) {
                        // Email Field
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "envelope")
                                    .foregroundColor(.secondary)
                                    .frame(width: 20)
                                
                                TextField("Email", text: $email)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                    .keyboardType(.emailAddress)
                                    .autocapitalization(.none)
                                    .disableAutocorrection(true)
                                    .onChange(of: email) { _ in
                                        viewModel.clearError()
                                    }
                            }
                        }
                        
                        // Password Field
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "lock")
                                    .foregroundColor(.secondary)
                                    .frame(width: 20)
                                
                                SecureField("Password", text: $password)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                    .onChange(of: password) { _ in
                                        viewModel.clearError()
                                    }
                            }
                        }
                        
                        // Forgot Password Link
                        HStack {
                            Spacer()
                            Button("Forgot Password?") {
                                if email.isEmpty {
                                    viewModel.uiState.error = "Please enter your email address first"
                                } else {
                                    showingPasswordReset = true
                                }
                            }
                            .font(.footnote)
                            .foregroundColor(.blue)
                        }
                    }
                    .padding(.top, 32)
                    
                    // Error Message
                    if let error = viewModel.uiState.error {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color.red, lineWidth: 1)
                            )
                    }
                    
                    // Sign In Button
                    Button(action: {
                        viewModel.signIn(email: email, password: password)
                    }) {
                        HStack {
                            if viewModel.uiState.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                            } else {
                                Text("Sign In")
                                    .fontWeight(.semibold)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                    .disabled(viewModel.uiState.isLoading)
                    .padding(.top, 16)
                    
                    // Divider
                    Rectangle()
                        .frame(height: 1)
                        .foregroundColor(.gray.opacity(0.3))
                        .padding(.top, 24)
                    
                    // Sign Up Link
                    HStack {
                        Text("Don't have an account?")
                            .foregroundColor(.secondary)
                        
                        Button("Sign Up") {
                            showingRegister = true
                        }
                        .fontWeight(.semibold)
                        .foregroundColor(.blue)
                    }
                    .font(.footnote)
                    .padding(.top, 16)
                    
                    Spacer()
                }
                .padding(.horizontal, 24)
            }
            .navigationBarHidden(true)
            .disabled(viewModel.uiState.isLoading)
        }
        .sheet(isPresented: $showingRegister) {
            RegisterView(authRepository: DIContainer.shared.authRepository)
        }
        .alert("Password Reset", isPresented: $showingPasswordReset) {
            Button("Send Reset Email") {
                viewModel.resetPassword(email: email)
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("A password reset email will be sent to \(email)")
        }
        .alert("Password Reset Sent", isPresented: .constant(viewModel.uiState.passwordResetSent)) {
            Button("OK") {
                viewModel.clearPasswordResetSent()
            }
        } message: {
            Text("Password reset email sent. Please check your inbox.")
        }
        .onChange(of: viewModel.uiState.isAuthenticated) { isAuthenticated in
            if isAuthenticated {
                // Navigation to main app would be handled by parent view
                // or through a navigation coordinator
            }
        }
    }
}

// MARK: - Preview
struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView(authRepository: MockAuthRepository())
    }
}

// MARK: - Mock Repository for Preview
private class MockAuthRepository: AuthRepository {
    func signIn(email: String, password: String) async -> Result<AuthResult, AuthError> {
        // Simulate network delay
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        return .failure(.invalidCredentials)
    }
    
    func signUp(email: String, password: String, displayName: String) async -> Result<AuthResult, AuthError> {
        return .failure(.emailAlreadyInUse)
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
        return .success(())
    }
}