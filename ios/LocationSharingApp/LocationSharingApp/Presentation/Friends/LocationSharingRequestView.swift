import SwiftUI

/**
 * SwiftUI view for handling location sharing requests from friends.
 * 
 * This view displays incoming location sharing requests and allows
 * the user to accept or deny them with appropriate feedback.
 */
struct LocationSharingRequestView: View {
    let friendId: String
    let friendName: String
    @StateObject private var viewModel = LocationSharingRequestViewModel()
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        VStack(spacing: 24) {
            // Header icon
            Image(systemName: "location.circle.fill")
                .font(.system(size: 80))
                .foregroundColor(.blue)
                .padding(.top, 32)
            
            // Title
            Text("Location Sharing Request")
                .font(.title)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
            
            // Request message
            Text("\(friendName) wants to share locations with you")
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            // Description
            Text("If you accept, \(friendName) will be able to see your location and you'll be able to see theirs.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Spacer()
            
            // Action buttons
            HStack(spacing: 16) {
                Button("Deny") {
                    viewModel.denyLocationSharingRequest(friendId: friendId)
                }
                .buttonStyle(SecondaryButtonStyle())
                .disabled(viewModel.isLoading)
                
                Button("Accept") {
                    viewModel.acceptLocationSharingRequest(friendId: friendId)
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(viewModel.isLoading)
            }
            .padding(.horizontal)
            
            // Cancel button
            Button("Cancel") {
                presentationMode.wrappedValue.dismiss()
            }
            .foregroundColor(.secondary)
            .disabled(viewModel.isLoading)
            
            // Loading indicator
            if viewModel.isLoading {
                ProgressView()
                    .padding()
            }
            
            Spacer()
        }
        .padding()
        .alert("Error", isPresented: .constant(viewModel.errorMessage != nil)) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .onChange(of: viewModel.requestProcessed) { processed in
            if processed {
                presentationMode.wrappedValue.dismiss()
            }
        }
    }
}

/**
 * Primary button style for accept actions.
 */
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.white)
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.green)
            .cornerRadius(8)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}

/**
 * Secondary button style for deny actions.
 */
struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.red)
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.clear)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.red, lineWidth: 1)
            )
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}

struct LocationSharingRequestView_Previews: PreviewProvider {
    static var previews: some View {
        LocationSharingRequestView(friendId: "123", friendName: "John Doe")
    }
}