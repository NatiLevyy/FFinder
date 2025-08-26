package com.locationsharing.app.di

import com.locationsharing.app.navigation.AnalyticsManager
import com.locationsharing.app.navigation.ButtonAnalytics
import com.locationsharing.app.navigation.ButtonAnalyticsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing analytics dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    
    @Binds
    @Singleton
    abstract fun bindButtonAnalytics(
        buttonAnalyticsImpl: ButtonAnalyticsImpl
    ): ButtonAnalytics
}