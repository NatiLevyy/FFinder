import SwiftUI

/**
 * SwiftUI view for managing notification settings and preferences
 */
struct NotificationSettingsView: View {
    @StateObject private var viewModel = NotificationSettingsViewModel()
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        NavigationView {
            List {
                // Permission Status Section
                Section {
                    HStack {
                        Image(systemName: viewModel.hasPermission ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(viewModel.hasPermission ? .green : .red)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Notification Permission")
                                .font(.headline)
                            Text(viewModel.hasPermission ? "Notifications are enabled" : "Notifications are disabled")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        if !viewModel.hasPermission {
                            Button("Enable") {
                                Task {
                                    await viewModel.requestPermission()
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .controlSize(.small)
                        }
                    }
                    .padding(.vertical, 4)
                } header: {
                    Text("Permission Status")
                }
                
                // Notification Types Section
                Section {
                    NotificationToggleRow(
                        title: "Friend Requests",
                        description: "Get notified when someone sends you a friend request",
                        isEnabled: $viewModel.friendRequestEnabled
                    )
                    
                    NotificationToggleRow(
                        title: "Friend Request Accepted",
                        description: "Get notified when someone accepts your friend request",
                        isEnabled: $viewModel.friendRequestAcceptedEnabled
                    )
                    
                    NotificationToggleRow(
                        title: "Location Sharing Requests",
                        description: "Get notified when someone wants to see your location",
                        isEnabled: $viewModel.locationSharingRequestEnabled
                    )
                    
                    NotificationToggleRow(
                        title: "Location Sharing Granted",
                        description: "Get notified when someone allows you to see their location",
                        isEnabled: $viewModel.locationSharingGrantedEnabled
                    )
                    
                    NotificationToggleRow(
                        title: "Location Sharing Revoked",
                        description: "Get notified when someone stops sharing their location with you",
                        isEnabled: $viewModel.locationSharingRevokedEnabled
                    )
                } header: {
                    Text("Notification Types")
                } footer: {
                    if !viewModel.hasPermission {
                        Text("Enable notification permission to configure these settings")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                // Test Notification Section
                Section {
                    Button(action: {
                        Task {
                            await viewModel.sendTestNotification()
                        }
                    }) {
                        HStack {
                            Image(systemName: "bell.badge")
                                .foregroundColor(.blue)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Send Test Notification")
                                    .font(.headline)
                                Text("Verify your notification settings are working correctly")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                        }
                        .padding(.vertical, 4)
                    }
                    .disabled(!viewModel.hasPermission || viewModel.isLoading)
                } header: {
                    Text("Test Notifications")
                }
            }
            .navigationTitle("Notification Settings")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
            .disabled(!viewModel.hasPermission)
            .overlay {
                if viewModel.isLoading {
                    ProgressView("Loading...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.black.opacity(0.3))
                }
            }
            .alert("Error", isPresented: .constant(viewModel.errorMessage != nil)) {
                Button("OK") {
                    viewModel.clearError()
                }
            } message: {
                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                }
            }
            .alert("Success", isPresented: .constant(viewModel.successMessage != nil)) {
                Button("OK") {
                    viewModel.clearSuccess()
                }
            } message: {
                if let successMessage = viewModel.successMessage {
                    Text(successMessage)
                }
            }
        }
        .task {
            await viewModel.loadSettings()
        }
    }
}

/**
 * Reusable row component for notification toggle settings
 */
struct NotificationToggleRow: View {
    let title: String
    let description: String
    @Binding var isEnabled: Bool
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Toggle("", isOn: $isEnabled)
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    NotificationSettingsView()
}