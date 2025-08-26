package com.locationsharing.app.ui.friends.hub

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationsharing.app.data.discovery.UserDiscoveryService
import com.locationsharing.app.data.friends.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsHubViewModel @Inject constructor(
    private val userDiscoveryService: UserDiscoveryService,
    private val friendsRepository: FriendsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState = combine(
        friendsRepository.getFriends(),
        friendsRepository.getFriendRequests()
    ) { friends, friendRequests ->
        FriendsHubUiState(
            contactsOnFFinder = friends,
            pendingRequests = friendRequests,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FriendsHubUiState()
    )

    fun approveRequest(id: String) {
        viewModelScope.launch {
            friendsRepository.acceptFriendRequest(id)
        }
    }

    fun declineRequest(id: String) {
        viewModelScope.launch {
            friendsRepository.declineFriendRequest(id)
        }
    }

    fun inviteViaShareIntent() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out FFinder!") // Customize the invite message
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}