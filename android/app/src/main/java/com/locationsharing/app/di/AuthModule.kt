package com.locationsharing.app.di

import com.locationsharing.app.data.auth.AuthManager
import com.locationsharing.app.data.auth.PhoneLinker
import com.locationsharing.app.data.discovery.UserDiscoveryService
import com.locationsharing.app.data.location.LocationSharingRepository
import com.locationsharing.app.data.user.UserProfileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    // No need for provides methods since all classes are annotated with @Singleton and @Inject
    // Hilt will automatically provide them
}