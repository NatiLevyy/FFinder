import SwiftUI

struct LoadingView: View {
    let message: String
    
    init(message: String = "Loading...") {
        self.message = message
    }
    
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)
                .progressViewStyle(CircularProgressViewStyle(tint: .blue))
                .accessibilityLabel("Loading progress indicator")
            
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .accessibilityLabel("Loading message: \(message)")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Loading: \(message)")
    }
}

struct LoadingOverlay: View {
    let message: String
    let isVisible: Bool
    
    init(message: String = "Loading...", isVisible: Bool = true) {
        self.message = message
        self.isVisible = isVisible
    }
    
    var body: some View {
        if isVisible {
            ZStack {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.2)
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    
                    Text(message)
                        .font(.body)
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                }
                .padding(24)
                .background(Color.black.opacity(0.8))
                .cornerRadius(12)
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Loading overlay: \(message)")
            }
            .transition(.opacity)
            .animation(.easeInOut(duration: 0.3), value: isVisible)
        }
    }
}

#Preview {
    VStack {
        LoadingView(message: "Loading your location...")
        
        ZStack {
            Color.blue.ignoresSafeArea()
            LoadingOverlay(message: "Saving changes...")
        }
    }
}