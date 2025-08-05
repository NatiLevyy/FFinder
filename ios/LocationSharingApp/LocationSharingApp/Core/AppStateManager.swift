import Foundation
import SwiftUI

@MainActor
class AppStateManager: ObservableObject {
    @Published var currentState: AppState = .loading
    
    private let authRepository = DIContainer.shared.authRepository
    private let errorHandler = DIContainer.shared.errorHandler
    
    enum AppState {
        case loading
        case authenticated
        case unauthenticated
        case error(String)
    }
    
    func initialize() async {
        currentState = .loading
        
        do {
            // Check authentication status
            let isAuthenticated = await authRepository.isUserAuthenticated()
            
            if isAuthenticated {
                // Verify current user exists
                if let _ = await authRepository.getCurrentUser() {
                    currentState = .authenticated
                } else {
                    currentState = .unauthenticated
                }
            } else {
                currentState = .unauthenticated
            }
        } catch {
            let errorMessage = errorHandler.handleError(error)
            currentState = .error(errorMessage)
        }
    }
    
    func signOut() async {
        do {
            try await authRepository.signOut()
            currentState = .unauthenticated
        } catch {
            let errorMessage = errorHandler.handleError(error)
            currentState = .error(errorMessage)
        }
    }
    
    func handleAuthenticationSuccess() {
        currentState = .authenticated
    }
}