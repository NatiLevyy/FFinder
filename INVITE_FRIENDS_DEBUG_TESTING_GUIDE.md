# InviteFriendsScreen Debug Testing Guide

## Overview
This guide provides comprehensive testing instructions for the InviteFriendsScreen to ensure all functionality works correctly at runtime.

## Pre-Testing Setup

### 1. Enable Debug Mode
The `InviteFriendsViewModel` has a `DEBUG_MODE` flag set to `true`. This enables:
- Test user simulation methods
- Enhanced logging with emojis
- Debug state manipulation

### 2. Logging Setup
All key operations are logged with Timber using emoji prefixes:
- 🚀 Initialization/startup
- 📱 Permission requests
- 📥 Contact import
- 🔍 User discovery
- 👤 Friend requests
- 📤 Sharing/invitations
- ✅ Success operations
- ❌ Error conditions
- 🔄 State updates
- 🧪 Debug operations

## Testing Checklist

### 1. Permission Handling ✅

#### Test Case 1.1: Permission Denied
**Steps:**
1. Fresh app install or clear app data
2. Navigate to InviteFriendsScreen
3. Tap "Allow Access to Contacts"
4. Deny permission in system dialog

**Expected Behavior:**
- Screen shows permission request UI
- After denial, stays on permission screen
- Log shows: `❌ Permission denied by user`
- No crash or silent failure

#### Test Case 1.2: Permission Granted
**Steps:**
1. Fresh app install or clear app data
2. Navigate to InviteFriendsScreen
3. Tap "Allow Access to Contacts"
4. Grant permission in system dialog

**Expected Behavior:**
- Screen transitions to loading state
- Contact import begins automatically
- Log shows: `✅ Permission granted, starting import...`
- Progress indicator appears

### 2. Contact Import & User Discovery ✅

#### Test Case 2.1: Successful Import
**Steps:**
1. Ensure device has contacts
2. Grant contacts permission
3. Wait for import to complete

**Expected Behavior:**
- Loading screen with progress updates
- Log shows: `📊 Contact import progress: X/Y (Z%)`
- Transitions to discovery phase
- Log shows: `🔍 Found cached contacts, starting user discovery...`

#### Test Case 2.2: No Contacts Found
**Steps:**
1. Clear all contacts from device
2. Grant permission and import

**Expected Behavior:**
- Shows "No Contacts Found" screen
- "Try Again" button available
- Log shows: `📭 No cached contacts found`

#### Test Case 2.3: Network Error During Discovery
**Steps:**
1. Turn off internet connection
2. Grant permission and import contacts
3. Wait for discovery to fail

**Expected Behavior:**
- Error message about network connectivity
- Retry button available
- Log shows network error details

### 3. Friend Request Flow ✅

#### Test Case 3.1: Send Friend Request (Individual)
**Steps:**
1. Find a discovered user with status NONE
2. Tap "Add" button on user card

**Expected Behavior:**
- Button changes to show "Request Sent" or similar
- User card updates to show sent status
- Log shows: `✅ Friend request sent successfully to [Name]`
- No crash or silent failure

#### Test Case 3.2: Send Friend Request (Batch)
**Steps:**
1. Select multiple discovered users (tap to select)
2. Tap FAB with friend request icon
3. Confirm batch send

**Expected Behavior:**
- All selected users update to "Request Sent"
- Selection clears after sending
- Log shows success/failure for each user
- Progress indicator during operation

#### Test Case 3.3: Already Friends Status
**Steps:**
1. Find user with ACCEPTED status
2. Verify UI shows correct state

**Expected Behavior:**
- Shows "Already Friends" or checkmark
- No "Add" button available
- Green indicator or similar visual cue

#### Test Case 3.4: Request Already Sent
**Steps:**
1. Find user with SENT status
2. Verify UI shows correct state

**Expected Behavior:**
- Shows "Request Sent" status
- Button disabled or shows cancel option
- Appropriate visual indication

### 4. Invitation Flow ✅

#### Test Case 4.1: Single SMS Invitation
**Steps:**
1. Find non-user contact with phone number
2. Tap "Invite" button

**Expected Behavior:**
- SMS app opens with pre-filled message
- Message contains FFinder download link
- Log shows: `📲 Attempting SMS to [number]`

#### Test Case 4.2: Single General Invitation
**Steps:**
1. Find non-user contact without phone number
2. Tap "Invite" button

**Expected Behavior:**
- Share chooser opens
- Message contains FFinder invitation text
- Log shows: `📤 Using general share intent`

#### Test Case 4.3: Batch Invitations
**Steps:**
1. Select multiple non-user contacts
2. Tap FAB with share icon

**Expected Behavior:**
- Share chooser opens with batch message
- Selection clears after sharing
- Log shows: `🚀 Opening share chooser for X contacts`

### 5. Search & Filtering ✅

#### Test Case 5.1: Search Functionality
**Steps:**
1. Enter text in search field
2. Verify filtering works for both sections

**Expected Behavior:**
- Lists filter in real-time
- Shows both discovered users and non-users matching query
- Empty state if no matches

#### Test Case 5.2: Empty Search Results
**Steps:**
1. Search for non-existent contact name

**Expected Behavior:**
- Shows "No contacts found" message
- Displays searched query in message

### 6. Edge Cases & Error Handling ✅

#### Test Case 6.1: No Internet Connection
**Steps:**
1. Disable internet connection
2. Try to send friend request

**Expected Behavior:**
- Error message about connectivity
- Retry option available
- Log shows network error details

#### Test Case 6.2: Firebase Authentication Error
**Steps:**
1. Simulate auth failure (logout user)
2. Try to send friend request

**Expected Behavior:**
- Error message about authentication
- Appropriate fallback behavior
- Log shows auth error

#### Test Case 6.3: Malformed Contact Data
**Steps:**
1. Import contacts with unusual data
2. Verify app doesn't crash

**Expected Behavior:**
- Graceful handling of bad data
- No crashes or silent failures
- Appropriate error logging

### 7. Debug Features (DEBUG_MODE only) 🧪

#### Test Case 7.1: Add Test Users
**Steps:**
1. Call `viewModel.addTestDiscoveredUsers()` from debug menu
2. Verify test users appear

**Expected Behavior:**
- Test users with different statuses appear
- Each status displays correctly
- Log shows: `🧪 DEBUG: Adding test discovered users`

#### Test Case 7.2: Simulate Friend Request States
**Steps:**
1. Call `viewModel.simulateFriendRequestState(userId, status)`
2. Verify UI updates correctly

**Expected Behavior:**
- User status changes immediately
- UI reflects new status
- Log shows: `🧪 DEBUG: Simulating friend request status`

## Performance Testing

### Memory Usage
- Monitor memory usage during contact import
- Check for memory leaks after multiple operations
- Verify proper cleanup when leaving screen

### Battery Impact
- Test background operations (if any)
- Verify no unnecessary wake locks
- Check network request efficiency

### UI Responsiveness
- Ensure UI remains responsive during operations
- Test scroll performance with large contact lists
- Verify smooth animations

## Accessibility Testing

### Screen Reader Support
- Test with TalkBack enabled
- Verify all buttons have proper descriptions
- Check focus navigation

### High Contrast Mode
- Test with high contrast enabled
- Verify text remains readable
- Check color-dependent information

## Network Conditions Testing

### Slow Network
- Test with slow 3G connection
- Verify appropriate timeouts
- Check user feedback during delays

### Intermittent Connection
- Test with unstable connection
- Verify retry mechanisms work
- Check data consistency

## Device-Specific Testing

### Different Screen Sizes
- Test on phones and tablets
- Verify responsive layout
- Check text scaling

### Different Android Versions
- Test on Android 6.0+ (API 23+)
- Verify permission handling differences
- Check deprecated API usage

## Logging Analysis

### Key Log Messages to Monitor

#### Successful Flow:
```
🚀 InviteFriendsViewModel: Checking initial state...
📋 Initial state check: permission=true, cached=X contacts
🔍 Found cached contacts, starting user discovery...
📊 Contact import progress: X/Y (Z%)
✅ Contact import completed, handling result...
👤 Sending friend request to user: [userId]
✅ Friend request sent successfully to [Name]
```

#### Error Flow:
```
❌ Permission denied by user
❌ Error checking initial state
❌ Failed to send friend request to [Name]: [error]
❌ Exception sending friend request to user: [userId]
```

## Common Issues & Solutions

### Issue 1: Contacts Not Loading
**Symptoms:** Permission granted but no contacts appear
**Check:** 
- Device actually has contacts
- Contacts app permissions
- Log shows contact count

### Issue 2: Friend Requests Not Sending
**Symptoms:** Button tap but no status change
**Check:**
- Network connectivity
- Firebase authentication
- User ID validity
- Log error messages

### Issue 3: Share Intent Not Opening
**Symptoms:** Invite button tap but no share dialog
**Check:**
- Intent resolution
- Device has sharing apps
- Log shows intent launch

### Issue 4: UI Not Updating
**Symptoms:** Operations succeed but UI doesn't reflect changes
**Check:**
- State flow collection
- UI state updates in logs
- Compose recomposition

## Test Data Setup

### For Complete Testing:
1. **Device Contacts:** Add 10-20 test contacts with various data
2. **Firebase Users:** Create test accounts for friend request testing
3. **Network Conditions:** Use network simulation tools
4. **Different Devices:** Test on multiple form factors

## Automated Testing Integration

### Unit Tests
- Run existing ViewModel tests
- Verify mock interactions
- Check state transitions

### UI Tests
- Create Espresso tests for key flows
- Test permission handling
- Verify error states

### Integration Tests
- Test with Firebase emulator
- Verify end-to-end flows
- Check data consistency

## Success Criteria

The InviteFriendsScreen is considered fully functional when:

✅ **Permission Flow:** Handles grant/deny correctly without crashes
✅ **Contact Import:** Successfully imports and displays contacts
✅ **User Discovery:** Finds and categorizes users correctly
✅ **Friend Requests:** Sends requests and updates UI state
✅ **Invitations:** Opens appropriate sharing mechanisms
✅ **Error Handling:** Shows meaningful errors and recovery options
✅ **Search:** Filters contacts in real-time
✅ **Performance:** Remains responsive under normal loads
✅ **Accessibility:** Works with assistive technologies
✅ **Logging:** Provides comprehensive debugging information

## Post-Testing Actions

After successful testing:
1. Set `DEBUG_MODE = false` for production
2. Remove or comment out test methods
3. Verify logging level is appropriate for production
4. Update any discovered issues in bug tracker
5. Document any new edge cases found