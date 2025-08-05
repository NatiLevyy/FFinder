import SwiftUI

/**
 * SwiftUI view for user registration (sign up).
 * 
 * Handles new user registration with email, password, and display name,
 * form validation, error display, and navigation to main app.
 * Implements requirements 1.1, 1.5, 1.6.
 */
struct RegisterView: View {
    @StateObject private var viewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode
    
    @State private var displayName = ""
    @State private var email = ""
    @State private var password = ""
    
    init(authRepository: AuthRepository) {
        self._viewModel = StateObject(wrappedValue: AuthViewModel(authRepository: authRepository))
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 8) {
                        Text("Create Account")
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.primary)
                        
                        Text("Join our location sharing community")
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.top, 32)
                    
                    // Form Fields
                    VStack(spacing: 16) {
                        // Display Name Field
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "person")
                                    .foregroundColor(.secondary)
                                    .frame(width: 20)
                                
                                TextField("Display Name", text: $displayName)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())
                                    .autocapitalization(.words)
                                    .onChange(of: displayName) { _ in
                                        viewModel.clearError()
                                    }
                            }
                        }
                        
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
                            
                            Text("At least 6 characters")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .padding(.leading, 28)
                        }
                    }
                    .padding(.top, 24)
                    
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
                    
                    // Sign Up Button
                    Button(action: {
                        viewModel.signUp(email: email, password: password, displayName: displayName)
                    }) {
                        HStack {
                            if viewModel.uiState.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                            } else {
                                Text("Create Account")
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
                    
                    // Terms and Privacy
                    Text("By creating an account, you agree to our Terms of Service and Privacy Policy")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 16)
                        .padding(.horizontal, 16)
                    
                    Spacer()
                }
                .padding(.horizontal, 24)
            }
            .navigationTitle("Sign Up")
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(viewModel.uiState.isLoading)
                }
            }
            .disabled(viewModel.uiState.isLoading)
        }
        .onChange(of: viewModel.uiState.isAuthenticated) { isAuthenticated in
            if isAuthenticated {
                // Navigation to main app would be handled by parent view
                // or through a navigation coordinator
                presentationMode.wrappedValue.dismiss()
            }
        }
    }
}

// MARK: - Preview
struct RegisterView_Previews: PreviewProvider {
    static var previews: some View {
        RegisterView(authRepository: MockAuthRepository())
    }
}

// MARK: - Mock Repository for Preview
private class MockAuthRepository: AuthRepository {
    func signIn(email: String, password: String) async -> Result<AuthResult, AuthError> {
        return .failure(.invalidCredentials)
    }
    
    func signUp(email: String, password: String, displayName: String) async -> Result<AuthResult, AuthError> {
        // Simulate network delay
        try? await Task.sleep(nanoseconds: 1_000_000_000)
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