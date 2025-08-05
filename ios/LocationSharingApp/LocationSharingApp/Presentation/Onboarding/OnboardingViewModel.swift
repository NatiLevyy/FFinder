import Foundation
import SwiftUI

@MainActor
class OnboardingViewModel: ObservableObject {
    @Published var onboardingState: OnboardingState = .inProgress
    
    private let errorHandler = DIContainer.shared.errorHandler
    
    enum OnboardingState {
        case inProgress
        case completed
        case error(String)
    }
    
    func completeOnboarding() async {
        do {
            // Mark onboarding as completed in UserDefaults
            UserDefaults.standard.set(true, forKey: "hasCompletedOnboarding")
            onboardingState = .completed
            
            // Post notification to update UI
            NotificationCenter.default.post(name: .onboardingCompleted, object: nil)
        } catch {
            let errorMessage = errorHandler.handleError(error)
            onboardingState = .error(errorMessage)
        }
    }
    
    static func hasCompletedOnboarding() -> Bool {
        return UserDefaults.standard.bool(forKey: "hasCompletedOnboarding")
    }
}