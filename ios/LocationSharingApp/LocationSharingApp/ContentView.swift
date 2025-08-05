import SwiftUI

struct ContentView: View {
    @StateObject private var appStateManager = AppStateManager()
    @State private var showOnboarding = !OnboardingViewModel.hasCompletedOnboarding()
    
    var body: some View {
        Group {
            if showOnboarding {
                OnboardingView()
                    .onReceive(NotificationCenter.default.publisher(for: .onboardingCompleted)) { _ in
                        withAnimation(.easeInOut(duration: 0.5)) {
                            showOnboarding = false
                        }
                    }
            } else {
                switch appStateManager.currentState {
                case .loading:
                    LoadingView(message: "Initializing app...")
                        .accessibilityLabel("App is loading")
                case .unauthenticated:
                    LoginView(authRepository: DIContainer.shared.authRepository)
                case .authenticated:
                    MainAppView()
                case .error(let message):
                    ErrorView(message: message) {
                        Task {
                            await appStateManager.initialize()
                        }
                    }
                }
            }
        }
        .animation(.easeInOut(duration: 0.3), value: appStateManager.currentState)
        .onAppear {
            if !showOnboarding {
                Task {
                    await appStateManager.initialize()
                }
            }
        }
    }
}



struct ErrorView: View {
    let message: String
    let onRetry: () -> Void
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.orange)
            
            Text("Something went wrong")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button("Try Again") {
                onRetry()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

struct MainAppView: View {
    @StateObject private var mainViewModel = MainViewModel()
    
    var body: some View {
        TabView {
            MapView()
                .tabItem {
                    Image(systemName: "map")
                    Text("Map")
                }
            
            FriendsView()
                .tabItem {
                    Image(systemName: "person.2")
                    Text("Friends")
                }
            
            NotificationSettingsView()
                .tabItem {
                    Image(systemName: "gear")
                    Text("Settings")
                }
        }
        .onAppear {
            Task {
                await mainViewModel.initializeApp()
            }
        }
        .onDisappear {
            Task {
                await mainViewModel.cleanup()
            }
        }
    }
}

#Preview {
    ContentView()
}