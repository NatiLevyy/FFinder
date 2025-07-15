package com.locationsharing.app.data.models

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val status: RequestStatus,
    val createdAt: Long,
    val respondedAt: Long? = null
)

enum class RequestStatus {
    PENDING, ACCEPTED, REJECTED, EXPIRED
}