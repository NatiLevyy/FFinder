import SwiftUI

/**
 * SwiftUI view for managing friends list and friend requests.
 * 
 * This view displays the user's friends list with search functionality,
 * location sharing controls, and navigation to friend request management.
 */
struct FriendsView: View {
    @StateObject private var viewModel = FriendsViewModel()
    @State private var searchText = ""
    @State private var showingAddFriend = false
    @State private var showingFriendRequests = false
    
    var body: some View {
        NavigationView {
            VStack {
                // Search bar
                SearchBar(text: $searchText, onSearchButtonClicked: {
                    viewModel.searchFriends(query: searchText)
                })
                .padding(.horizontal)
                
                // Friends list
                if viewModel.isLoading {
                    ProgressView("Loading friends...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.friends.isEmpty {
                    VStack {
                        Image(systemName: "person.2")
                            .font(.system(size: 50))
                            .foregroundColor(.gray)
                        Text("No friends yet")
                            .font(.title2)
                            .foregroundColor(.gray)
                        Text("Add friends to start sharing locations")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(viewModel.friends) { friend in
                            FriendRowView(
                                friend: friend,
                                onLocationSharingToggle: { enabled in
                                    viewModel.updateLocationSharing(friendId: friend.id, enabled: enabled)
                                },
                                onLocationSharingRequest: {
                                    viewModel.requestLocationSharing(friendId: friend.id)
                                },
                                onRemoveFriend: {
                                    viewModel.removeFriend(friendId: friend.id)
                                }
                            )
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("Friends")
            .navigationBarItems(
                leading: Button("Requests") {
                    showingFriendRequests = true
                },
                trailing: Button(action: {
                    showingAddFriend = true
                }) {
                    Image(systemName: "plus")
                }
            )
            .sheet(isPresented: $showingAddFriend) {
                AddFriendView()
            }
            .sheet(isPresented: $showingFriendRequests) {
                FriendRequestsView()
            }
            .onAppear {
                viewModel.loadFriends()
            }
            .onChange(of: searchText) { newValue in
                viewModel.searchFriends(query: newValue)
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
 * Individual friend row view component.
 */
struct FriendRowView: View {
    let friend: Friend
    let onLocationSharingToggle: (Bool) -> Void
    let onLocationSharingRequest: () -> Void
    let onRemoveFriend: () -> Void
    
    @State private var showingRemoveAlert = false
    @State private var showingLocationRequest = false
    
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
                Text(friend.user.displayName)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Text(friend.user.email)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text(locationStatusText)
                    .font(.caption2)
                    .foregroundColor(locationStatusColor)
                
                if friend.lastKnownLocation != nil {
                    Text("Location available")
                        .font(.caption2)
                        .foregroundColor(.blue)
                }
            }
            
            Spacer()
            
            VStack(spacing: 8) {
                Toggle("Share Location", isOn: .constant(friend.locationSharingEnabled))
                    .labelsHidden()
                    .disabled(friend.locationSharingPermission != .granted)
                    .onChange(of: friend.locationSharingEnabled) { enabled in
                        onLocationSharingToggle(enabled)
                    }
                
                // Location request button
                if friend.locationSharingPermission == .none {
                    Button("Request Location") {
                        onLocationSharingRequest()
                    }
                    .font(.caption2)
                    .foregroundColor(.blue)
                } else if friend.locationSharingPermission == .requested {
                    Text("Pending...")
                        .font(.caption2)
                        .foregroundColor(.orange)
                }
                
                Button("Remove") {
                    showingRemoveAlert = true
                }
                .font(.caption)
                .foregroundColor(.red)
            }
        }
        .padding(.vertical, 8)
        .alert("Remove Friend", isPresented: $showingRemoveAlert) {
            Button("Cancel", role: .cancel) { }
            Button("Remove", role: .destructive) {
                onRemoveFriend()
            }
        } message: {
            Text("Are you sure you want to remove \(friend.user.displayName) from your friends list?")
        }
    }
    
    private var locationStatusText: String {
        switch friend.locationSharingPermission {
        case .granted:
            return "Sharing location"
        case .requested:
            return "Location requested"
        case .denied:
            return "Location denied"
        case .none:
            return "Not sharing"
        }
    }
    
    private var locationStatusColor: Color {
        switch friend.locationSharingPermission {
        case .granted:
            return .green
        case .requested:
            return .orange
        case .denied:
            return .red
        case .none:
            return .gray
        }
    }
}

/**
 * Custom search bar component.
 */
struct SearchBar: View {
    @Binding var text: String
    let onSearchButtonClicked: () -> Void
    
    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search friends...", text: $text, onCommit: onSearchButtonClicked)
                .textFieldStyle(RoundedBorderTextFieldStyle())
            
            if !text.isEmpty {
                Button(action: {
                    text = ""
                }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(.horizontal)
    }
}

struct FriendsView_Previews: PreviewProvider {
    static var previews: some View {
        FriendsView()
    }
}