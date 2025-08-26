package com.locationsharing.app.di

import com.locationsharing.app.ui.home.components.LocationProvider
import com.locationsharing.app.ui.home.components.LocationProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for location-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    
    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        locationProviderImpl: LocationProviderImpl
    ): LocationProvider
}