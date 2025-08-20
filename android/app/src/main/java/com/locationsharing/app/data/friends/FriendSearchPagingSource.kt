package com.locationsharing.app.data.friends

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * PagingSource for searching friends globally in Firestore
 * Implements pagination for friend search results
 */
class FriendSearchPagingSource(
    private val firestore: FirebaseFirestore,
    private val query: String
) : PagingSource<QuerySnapshot, Friend>() {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PAGE_SIZE = 30
    }
    
    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Friend> {
        return try {
            if (query.isBlank()) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }
            
            // Build Firestore query for searching users by keywords
            var firestoreQuery: Query = firestore.collection(USERS_COLLECTION)
                .whereArrayContains("keywords", query)
                .limit(PAGE_SIZE.toLong())
            
            // Add pagination cursor if available
            params.key?.let { snapshot ->
                if (!snapshot.isEmpty) {
                    firestoreQuery = firestoreQuery.startAfter(snapshot.documents.last())
                }
            }
            
            // Execute query
            val snapshot = firestoreQuery.get().await()
            
            // Convert documents to Friend objects
            val friends = snapshot.documents.mapNotNull { document ->
                try {
                    Friend.fromDocument(document)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse friend document: ${document.id}")
                    null
                }
            }
            
            Timber.d("Friend search loaded ${friends.size} results for query: '$query'")
            
            LoadResult.Page(
                data = friends,
                prevKey = null, // Only forward pagination
                nextKey = if (snapshot.size() < PAGE_SIZE) null else snapshot
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading friend search results for query: '$query'")
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<QuerySnapshot, Friend>): QuerySnapshot? {
        // Return null to always start from the beginning on refresh
        return null
    }
}