import SwiftUI

struct OnboardingView: View {
    @StateObject private var onboardingViewModel = OnboardingViewModel()
    @State private var currentPage = 0
    
    private let onboardingPages = [
        OnboardingPage(
            title: "Share Your Location",
            description: "Connect with friends and family by sharing your real-time location safely and securely.",
            imageName: "location.circle.fill",
            color: .blue
        ),
        OnboardingPage(
            title: "Stay Connected",
            description: "See where your friends are on an interactive map and get notified when they're nearby.",
            imageName: "person.2.circle.fill",
            color: .green
        ),
        OnboardingPage(
            title: "Privacy First",
            description: "You control who can see your location and when. Your privacy is our priority.",
            imageName: "lock.shield.fill",
            color: .purple
        ),
        OnboardingPage(
            title: "Get Started",
            description: "Ready to start sharing? Create your account and connect with friends today!",
            imageName: "checkmark.circle.fill",
            color: .orange
        )
    ]
    
    var body: some View {
        VStack(spacing: 0) {
            // Page Content
            TabView(selection: $currentPage) {
                ForEach(0..<onboardingPages.count, id: \.self) { index in
                    OnboardingPageView(page: onboardingPages[index])
                        .tag(index)
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentPage)
            
            // Page Indicators
            HStack(spacing: 8) {
                ForEach(0..<onboardingPages.count, id: \.self) { index in
                    Circle()
                        .fill(index == currentPage ? Color.blue : Color.gray.opacity(0.3))
                        .frame(width: index == currentPage ? 12 : 8, height: index == currentPage ? 12 : 8)
                        .animation(.easeInOut, value: currentPage)
                        .accessibilityLabel("Page \(index + 1) of \(onboardingPages.count)")
                }
            }
            .padding(.vertical, 20)
            
            // Navigation Buttons
            HStack(spacing: 16) {
                if currentPage < onboardingPages.count - 1 {
                    Button("Skip") {
                        Task {
                            await onboardingViewModel.completeOnboarding()
                        }
                    }
                    .foregroundColor(.secondary)
                    .accessibilityLabel("Skip onboarding")
                    
                    Spacer()
                    
                    Button("Next") {
                        withAnimation {
                            currentPage += 1
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .accessibilityLabel("Next page")
                } else {
                    Button("Get Started") {
                        Task {
                            await onboardingViewModel.completeOnboarding()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel("Get started with location sharing")
                }
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 32)
        }
        .background(Color(.systemBackground))
    }
}

struct OnboardingPageView: View {
    let page: OnboardingPage
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            // Image
            Image(systemName: page.imageName)
                .font(.system(size: 120))
                .foregroundColor(page.color)
                .accessibilityLabel(page.title)
            
            VStack(spacing: 16) {
                // Title
                Text(page.title)
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
                
                // Description
                Text(page.description)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(nil)
                    .padding(.horizontal, 32)
            }
            
            Spacer()
        }
        .padding(.horizontal, 32)
    }
}

struct OnboardingPage {
    let title: String
    let description: String
    let imageName: String
    let color: Color
}

#Preview {
    OnboardingView()
}