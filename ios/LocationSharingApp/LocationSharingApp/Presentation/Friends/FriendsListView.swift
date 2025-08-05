import SwiftUI
import Combine

/**
 * Real-time friends list view with Firebase integration
 * Shows live status updates and smooth animations
 */
struct FriendsListView: View {
    @StateObject private var viewModel = FriendsListViewModel()
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()
                
                // Content
                if viewModel.isLoading {
                    LoadingStateView()
                } else if viewModel.friends.isEmpty {
                    EmptyFriendsStateView {
                        // Handle invite friends
                        viewModel.showInviteFriends()
                    }
                } else {
                    FriendsListContentView(
                        friends: viewModel.friends,
                        onlineFriends: viewModel.onlineFriends,
                        onFriendTap: { friend in
                            viewModel.selectFriend(friend)
                        }
                    )
                }
            }
            .navigationTitle("Friends")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Back") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.refreshFriends()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(viewModel.isRefreshing)
                }
            }
            .overlay(alignment: .bottomTrailing) {
                // Floating Action Button
                Button {
                    viewModel.showInviteFriends()
                } label: {
                    Image(systemName: "person.badge.plus")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 56, height: 56)
                        .background(Color.accentColor)
                        .clipShape(Circle())
                        .shadow(radius: 4)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
        .alert("Error", isPresented: .constant(viewModel.error != nil)) {
            Button("OK") {
                viewModel.clearError()
            }
        } message: {
            Text(viewModel.error ?? "")
        }
        .onAppear {
            viewModel.startRealTimeSync()
        }
        .onDisappear {
            viewModel.stopRealTimeSync()
        }
    }
}

/**
 * Loading state view with branded styling
 */
struct LoadingStateView: View {
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(Color.accentColor)
            
            Text("Loading friends...")
                .font(.body)
                .foregroundColor(.secondary)
        }
    }
}

/**
 * Main friends list content with real-time updates
 */
struct FriendsListContentView: View {
    let friends: [Friend]
    let onlineFriends: [Friend]
    let onFriendTap: (Friend) -> Void
    
    private var offlineFriends: [Friend] {
        friends.filter { !$0.isOnline }
    }
    
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                // Online friends section
                if !onlineFriends.isEmpty {
                    SectionHeaderView(
                        title: "Online",
                        count: onlineFriends.count,
                        isOnline: true
                    )
                    
                    ForEach(Array(onlineFriends.enumerated()), id: \.element.id) { index, friend in
                        FriendListItemView(
                            friend: friend,
                            index: index,
                            isOnline: true,
                            onTap: { onFriendTap(friend) }
                        )
                    }
                    
                    Spacer(minLength: 20)
                }
                
                // Offline friends section
                if !offlineFriends.isEmpty {
                    SectionHeaderView(
                        title: "Offline",
                        count: offlineFriends.count,
                        isOnline: false
                    )
                    
                    ForEach(Array(offlineFriends.enumerated()), id: \.element.id) { index, friend in
                        FriendListItemView(
                            friend: friend,
                            index: index,
                            isOnline: false,
                            onTap: { onFriendTap(friend) }
                        )
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
    }
}

/**
 * Section header with animated counter
 */
struct SectionHeaderView: View {
    let title: String
    let count: Int
    let isOnline: Bool
    
    @State private var pulseScale: CGFloat = 1.0
    
    var body: some View {
        HStack {
            Text(title)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.primary)
            
            if isOnline {
                Circle()
                    .fill(Color.green)
                    .frame(width: 8, height: 8)
                    .scaleEffect(pulseScale)
                    .onAppear {
                        withAnimation(
                            .easeInOut(duration: 1.0)
                            .repeatForever(autoreverses: true)
                        ) {
                            pulseScale = 1.2
                        }
                    }
            }
            
            Text("(\(count))")
                .font(.body)
                .foregroundColor(.secondary)
                .animation(.easeInOut, value: count)
            
            Spacer()
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 8)
    }
}

/**
 * Individual friend list item with staggered animations
 */
struct FriendListItemView: View {
    let friend: Friend
    let index: Int
    let isOnline: Bool
    let onTap: () -> Void
    
    @State private var hasAppeared = false
    @State private var pulseScale: CGFloat = 1.0
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 16) {
                // Friend avatar with status indicator
                ZStack {
                    AsyncImage(url: URL(string: friend.user.avatarUrl ?? "")) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .overlay(
                                Image(systemName: "person.fill")
                                    .foregroundColor(.gray)
                            )
                    }
                    .frame(width: 56, height: 56)
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(
                                LinearGradient(
                                    colors: [Color.accentColor, Color.accentColor.opacity(0.8)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 2
                            )
                    )
                    
                    // Online status indicator
                    if isOnline {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 16, height: 16)
                            .overlay(
                                Circle()
                                    .stroke(Color.white, lineWidth: 2)
                            )
                            .scaleEffect(pulseScale)
                            .offset(x: 20, y: -20)
                            .onAppear {
                                withAnimation(
                                    .easeInOut(duration: 2.0)
                                    .repeatForever(autoreverses: true)
                                ) {
                                    pulseScale = 1.2
                                }
                            }
                    }
                }
                
                // Friend info
                VStack(alignment: .leading, spacing: 4) {
                    Text(friend.user.name)
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(isOnline ? .primary : .secondary)
                        .lineLimit(1)
                    
                    Text(friend.statusText)
                        .font(.subheadline)
                        .foregroundColor(isOnline ? .green : .secondary)
                        .animation(.easeInOut, value: friend.statusText)
                }
                
                Spacer()
                
                // Location sharing indicator
                if friend.locationSharingEnabled {
                    Image(systemName: "location.fill")
                        .font(.caption)
                        .foregroundColor(.accentColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.accentColor.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
            .padding(16)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(hasAppeared ? 1.0 : 0.9)
        .opacity(hasAppeared ? 1.0 : 0.0)
        .onAppear {
            withAnimation(
                .spring(response: 0.6, dampingFraction: 0.8)
                .delay(Double(index) * 0.15) // Staggered animation
            ) {
                hasAppeared = true
            }
        }
    }
}

/**
 * Empty state with branded animation and invite CTA
 */
struct EmptyFriendsStateView: View {
    let onInviteTap: () -> Void
    
    @State private var hasAppeared = false
    @State private var floatingOffset1: CGFloat = 0
    @State private var floatingOffset2: CGFloat = 0
    
    var body: some View {
        VStack(spacing: 24) {
            // Animated floating elements
            ZStack {
                Text("📍")
                    .font(.system(size: 60))
                    .opacity(0.7)
                    .offset(y: floatingOffset1)
                
                Text("👥")
                    .font(.system(size: 40))
                    .opacity(0.5)
                    .offset(x: 30, y: floatingOffset2)
            }
            .frame(height: 120)
            .onAppear {
                withAnimation(
                    .easeInOut(duration: 3.0)
                    .repeatForever(autoreverses: true)
                ) {
                    floatingOffset1 = 20
                }
                
                withAnimation(
                    .easeInOut(duration: 2.5)
                    .repeatForever(autoreverses: true)
                ) {
                    floatingOffset2 = -10
                }
            }
            
            VStack(spacing: 8) {
                Text("No Friends Yet")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text("Invite friends to start sharing locations and see them on your map")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(nil)
            }
            
            Button(action: onInviteTap) {
                HStack {
                    Image(systemName: "person.badge.plus")
                    Text("Invite Friends")
                }
                .font(.headline)
                .foregroundColor(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(Color.accentColor)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(32)
        .scaleEffect(hasAppeared ? 1.0 : 0.8)
        .opacity(hasAppeared ? 1.0 : 0.0)
        .onAppear {
            withAnimation(
                .spring(response: 0.8, dampingFraction: 0.6)
                .delay(0.3)
            ) {
                hasAppeared = true
            }
        }
    }
}

// MARK: - Extensions

extension Friend {
    var isOnline: Bool {
        // Implement online status logic based on your Friend model
        return friendshipStatus == .accepted && locationSharingEnabled
    }
    
    var statusText: String {
        if isOnline {
            return "Online now"
        } else {
            return "Offline"
        }
    }
}

// MARK: - Preview

struct FriendsListView_Previews: PreviewProvider {
    static var previews: some View {
        FriendsListView()
    }
}