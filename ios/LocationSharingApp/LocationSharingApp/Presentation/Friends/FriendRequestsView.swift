import SwiftUI

/**
 * SwiftUI view for managing incoming friend requests.
 * 
 * This view displays pending friend requests and allows users
 * to accept or reject them.
 */
struct FriendRequestsView: View {
    @StateObject private var viewModel = FriendRequestsViewModel()
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        NavigationView {
            VStack {
                if viewModel.isLoading {
                    ProgressView("Loading friend requests...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.friendRequests.isEmpty {
                    VStack {
                        Image(systemName: "person.badge.plus")
                            .font(.system(size: 50))
                            .foregroundColor(.gray)
                        Text("No pending requests")
                            .font(.title2)
                            .foregroundColor(.gray)
                        Text("Friend requests will appear here")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(viewModel.friendRequests) { request in
                            FriendRequestRowView(
                                friendRequest: request,
                                onAccept: {
                                    viewModel.acceptFriendRequest(requestId: request.id)
                                },
                                onReject: {
                                    viewModel.rejectFriendRequest(requestId: request.id)
                                }
                            )
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("Friend Requests")
            .navigationBarItems(
                leading: Button("Close") {
                    presentationMode.wrappedValue.dismiss()
                }
            )
            .onAppear {
                viewModel.loadFriendRequests()
            }
        }
        .alert("Error", isPresented: .constant(viewModel.errorMessage != nil)) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .alert("Success", isPresented: .constant(viewModel.showSuccessMessage)) {
            Button("OK") {
                viewModel.clearSuccessMessage()
            }
        } message: {
            Text(viewModel.successMessage)
        }
    }
}

/**
 * Individual friend request row view component.
 */
struct FriendRequestRowView: View {
    let friendRequest: FriendRequest
    let onAccept: () -> Void
    let onReject: () -> Void
    
    private let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter
    }()
    
    var body: some View {
        HStack {
            // Profile image placeholder
            Circle()
                .fill(Color.gray.opacity(0.3))
                .frame(width: 50, height: 50)
                .overlay(
                    Image(systemName: "person.fill")
                        .foregroundColor(.gray)
                )
            
            VStack(alignment: .leading, spacing: 4) {
                // TODO: Load user details from fromUserId
                // For now, showing the user ID as placeholder
                Text("User ID: \(friendRequest.fromUserId)")
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text("Sent: \(dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(friendRequest.createdAt / 1000))))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(spacing: 8) {
                Button("Accept") {
                    onAccept()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color.green)
                .foregroundColor(.white)
                .cornerRadius(8)
                
                Button("Reject") {
                    onReject()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(Color.red)
                .foregroundColor(.white)
                .cornerRadius(8)
            }
        }
        .padding(.vertical, 8)
    }
}

struct FriendRequestsView_Previews: PreviewProvider {
    static var previews: some View {
        FriendRequestsView()
    }
}