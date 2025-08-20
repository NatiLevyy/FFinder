package com.locationsharing.app.ui.friends.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.locationsharing.app.R
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendRequest
import com.locationsharing.app.ui.theme.FFinderTheme

@Composable
fun FriendsHubScreen(
    viewModel: FriendsHubViewModel = hiltViewModel(),
    onNavigateToGlobalSearch: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    FriendsHubScreenContent(
        uiState = uiState,
        onApproveRequest = viewModel::approveRequest,
        onDeclineRequest = viewModel::declineRequest,
        onInviteFriends = viewModel::inviteViaShareIntent,
        onNavigateToGlobalSearch = onNavigateToGlobalSearch
    )
}

@Composable
fun FriendsHubScreenContent(
    uiState: FriendsHubUiState,
    onApproveRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onInviteFriends: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit
) {
    Scaffold {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(16.dp)
            ) {
                Text("Friends Hub", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Contacts on FFinder Section
                Text("Contacts on FFinder (${uiState.contactsOnFFinder.size})", style = MaterialTheme.typography.titleLarge)
                if (uiState.contactsOnFFinder.isEmpty()) {
                    Text("No contacts on FFinder yet.")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.contactsOnFFinder, key = { it.id }) {
                            FriendRow(friend = it)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Pending Requests Section
                Text("Pending Requests (${uiState.pendingRequests.size})", style = MaterialTheme.typography.titleLarge)
                if (uiState.pendingRequests.isEmpty()) {
                    Text("No pending requests.")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.pendingRequests, key = { it.id }) {
                            PendingRequestRow(
                                request = it,
                                onApprove = { onApproveRequest(it.id) },
                                onDecline = { onDeclineRequest(it.id) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // CTA Section
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = onInviteFriends) {
                        Text("Invite Friends")
                    }
                    OutlinedButton(onClick = onNavigateToGlobalSearch) {
                        Text("Global Search")
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRow(friend: Friend) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(text = friend.name, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun PendingRequestRow(request: FriendRequest, onApprove: () -> Unit, onDecline: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = request.fromUserName, modifier = Modifier.weight(1f))
            Button(onClick = onApprove) {
                Text("Approve")
            }
            OutlinedButton(onClick = onDecline) {
                Text("Decline")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendsHubScreenPreview() {
    FFinderTheme {
        FriendsHubScreenContent(
            uiState = FriendsHubUiState(
                contactsOnFFinder = listOf(Friend("1", "John Doe", "0.0", "0.0", ""))
            ),
            onApproveRequest = {},
            onDeclineRequest = {},
            onInviteFriends = {},
            onNavigateToGlobalSearch = {}
        )
    }
}