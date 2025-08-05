# Friend Request Backend Integration Summary

## Overview
Successfully implemented backend integration for sending friend requests in the FFinder app. The implementation includes Firebase Firestore integration, friend request status checking, and UI state management.

## Files Created/Modified

### Core Implementation Files

#### 1. FriendsRepository Interface & Implementation
**File:** `android/app/src/main/java/com/locationsharing/app/data/friends/FriendsRepository.kt`

**New Methods Added:**
- `checkFriendRequestStatus(toUserId: String): FriendRequestStatus` - Check current relationship status
- `cancelFriendRequest(toUserId: String): Result<Unit>` - Cancel a pending friend request

**Enhanced Methods:**
- `sendFriendRequest(toUserId: String, message: String?): Result<String>` - Enhanced with friendship validation

#### 2. InviteFriendsViewModel Updates
**File:** `android/app/src/main/java/com/locationsharing/app/ui/invite/InviteFriendsViewModel.kt`

**New Methods Added:**
- `checkFriendRequestStatuses(discoveredUsers: List<DiscoveredUser>)` - Check status for all discovered users
- `cancelFriendRequest(userId: String)` - Cancel a specific friend request

**Enhanced Methods:**
- `sendFriendRequests()` - Now uses actual FriendsRepository
- `sendSingleFriendRequest(userId: String)` - Enhanced with proper error handling
- `handleDiscoveryResult()` - Now checks friend request statuses after discovery

#### 3. UI State Updates
**Enhanced:** `InviteFriendsUiState` data class
- Added `currentProgress: String?` field for better progress tracking
- Updated progress display logic

### Test Files

#### 1. FriendsRepository Tests
**File:** `android/app/src/test/java/com/locationsharing/app/data/friends/FirebaseFriendsRepositoryTest.kt`

**New Test Cases:**
- `checkFriendRequestStatus should return ACCEPTED when users are friends`
- `checkFriendRequestStatus should return SENT when request was sent`
- `checkFriendRequestStatus should return RECEIVED when request was received`
- `checkFriendRequestStatus should return NONE when no relationship exists`
- `cancelFriendRequest should delete pending request`
- `cancelFriendRequest should fail when no pending request exists`
- `sendFriendRequest should fail when users are already friends`

#### 2. InviteFriendsViewModel Tests
**File:** `android/app/src/test/java/com/locationsharing/app/ui/invite/InviteFriendsViewModelTest.kt`

**Updated Tests:**
- Added FriendsRepository mock dependency
- Added tests for `sendSingleFriendRequest` and `cancelFriendRequest`

## Firebase Firestore Data Structure

### Friend Requests Collection: `friendRequests`
```json
{
  "fromUserId": "string",
  "toUserId": "string", 
  "fromUserName": "string",
  "fromUserAvatar": "string",
  "toUserName": "string",
  "toUserAvatar": "string",
  "status": "PENDING|ACCEPTED|DECLINED",
  "message": "string|null",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Users Collection: `users/{userId}/friends/{friendId}`
```json
{
  "userId": "string",
  "status": "ACCEPTED|BLOCKED",
  "createdAt": "timestamp"
}
```

## Friend Request Status Logic

### Status Checking Priority:
1. **ACCEPTED** - Users are already friends (exists in friends subcollection)
2. **BLOCKED** - User is blocked (status = "BLOCKED" in friends subcollection)
3. **SENT** - Current user sent a pending request to target user
4. **RECEIVED** - Current user received a pending request from target user
5. **DECLINED** - Previous request was declined
6. **NONE** - No relationship exists

### Status Transitions:
- `NONE` → `SENT` (via sendFriendRequest)
- `SENT` → `NONE` (via cancelFriendRequest)
- `RECEIVED` → `ACCEPTED` (via acceptFriendRequest)
- `RECEIVED` → `DECLINED` (via declineFriendRequest)

## UI Integration

### Button States Based on Friend Request Status:
- **NONE**: Show "Send Request" button (enabled)
- **SENT**: Show "Request Sent" button (disabled) with cancel option
- **RECEIVED**: Show "Accept" and "Decline" buttons
- **ACCEPTED**: Show "Already Friends" (disabled)
- **DECLINED**: Show "Request Declined" (disabled)
- **BLOCKED**: Hide user or show "Blocked" (disabled)

### Error Handling:
- Network errors are caught and displayed to user
- Invalid operations (e.g., sending request to existing friend) show appropriate messages
- Failed operations don't update UI state

## Security Considerations

### Firebase Security Rules Required:
```javascript
// Friend requests - users can create requests they send and read requests they receive
match /friendRequests/{requestId} {
  allow create: if request.auth != null && 
    request.auth.uid == resource.data.fromUserId;
  allow read: if request.auth != null && 
    (request.auth.uid == resource.data.fromUserId || 
     request.auth.uid == resource.data.toUserId);
  allow update: if request.auth != null && 
    request.auth.uid == resource.data.toUserId;
  allow delete: if request.auth != null && 
    request.auth.uid == resource.data.fromUserId;
}

// Friends subcollection - users can manage their own friends
match /users/{userId}/friends/{friendId} {
  allow read, write: if request.auth != null && 
    request.auth.uid == userId;
}
```

## Performance Optimizations

### Implemented:
- Batch status checking for multiple users
- Caching of friend request statuses in UI state
- Efficient Firestore queries with proper indexing

### Recommended:
- Add Firestore indexes for friend request queries:
  - `friendRequests` collection: `fromUserId`, `toUserId`, `status`
  - `users/{userId}/friends` subcollection: `status`, `createdAt`

## Testing Coverage

### Unit Tests:
- ✅ FriendsRepository methods (send, check status, cancel)
- ✅ InviteFriendsViewModel integration
- ✅ Error handling scenarios
- ✅ Edge cases (already friends, no pending request, etc.)

### Integration Tests Needed:
- Firebase emulator tests for real-time updates
- End-to-end friend request flow testing
- Offline/online synchronization testing

## Next Steps

1. **UI Updates**: Update InviteFriendsScreen to show appropriate button states
2. **Real-time Updates**: Implement listeners for friend request status changes
3. **Notifications**: Add push notifications for friend requests
4. **Firebase Rules**: Deploy the security rules to Firebase
5. **Performance**: Add Firestore indexes for optimal query performance

## Usage Example

```kotlin
// Send a friend request
val result = friendsRepository.sendFriendRequest("targetUserId", "Let's be friends!")
if (result.isSuccess) {
    // Update UI to show "Request Sent"
}

// Check friend request status
val status = friendsRepository.checkFriendRequestStatus("targetUserId")
when (status) {
    FriendRequestStatus.NONE -> showSendRequestButton()
    FriendRequestStatus.SENT -> showRequestSentButton()
    FriendRequestStatus.ACCEPTED -> showAlreadyFriendsButton()
    // ... handle other statuses
}

// Cancel a friend request
val cancelResult = friendsRepository.cancelFriendRequest("targetUserId")
if (cancelResult.isSuccess) {
    // Update UI to show "Send Request" again
}
```

This implementation provides a robust foundation for friend request functionality with proper error handling, status management, and Firebase integration.