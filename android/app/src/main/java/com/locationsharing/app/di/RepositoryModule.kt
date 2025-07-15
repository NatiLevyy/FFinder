package com.locationsharing.app.di

import com.locationsharing.app.domain.repository.AuthRepository
import com.locationsharing.app.domain.repository.LocationRepository
import com.locationsharing.app.domain.repository.FriendsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // Repository implementations will be provided here
    // These will be implemented in later tasks
}