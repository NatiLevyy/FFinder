package com.locationsharing.app.di

import com.locationsharing.app.data.friends.FirebaseFriendsRepository
import com.locationsharing.app.data.friends.FriendsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindFriendsRepository(
        firebaseFriendsRepository: FirebaseFriendsRepository
    ): FriendsRepository
}