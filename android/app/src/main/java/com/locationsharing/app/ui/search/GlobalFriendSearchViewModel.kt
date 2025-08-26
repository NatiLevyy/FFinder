package com.locationsharing.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.locationsharing.app.data.friends.Friend
import com.locationsharing.app.data.friends.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for global friend search functionality
 * Handles search queries with debouncing and pagination
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class GlobalFriendSearchViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    /**
     * Paginated search results with debouncing and caching
     */
    val searchResults: Flow<PagingData<Friend>> = _searchQuery
        .debounce(300) // Debounce for 300ms as specified
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                Timber.d("Search query is blank, returning empty results")
                flowOf(PagingData.empty())
            } else {
                Timber.d("Searching for friends with query: '$query'")
                _isSearching.value = true
                try {
                    friendsRepository.searchFriends(query)
                } catch (e: Exception) {
                    Timber.e(e, "Error searching friends")
                    flowOf(PagingData.empty())
                } finally {
                    _isSearching.value = false
                }
            }
        }
        .cachedIn(viewModelScope) // Cache results in viewModelScope as specified
    
    /**
     * Update the search query
     */
    fun updateSearchQuery(query: String) {
        Timber.d("Search query updated: '$query'")
        _searchQuery.value = query
    }
    
    /**
     * Clear the search query and results
     */
    fun clearSearch() {
        Timber.d("Clearing search")
        _searchQuery.value = ""
        _isSearching.value = false
    }
    
    /**
     * Handle friend selection - this will be called when user taps on a search result
     */
    fun onFriendSelected(friend: Friend) {
        Timber.d("Friend selected: ${friend.name} (${friend.id})")
        // The actual navigation and map zoom will be handled by the composable
    }
}