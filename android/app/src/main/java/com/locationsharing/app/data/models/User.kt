package com.locationsharing.app.data.models

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val profileImageUrl: String? = null,
    val createdAt: Long,
    val lastActiveAt: Long
)