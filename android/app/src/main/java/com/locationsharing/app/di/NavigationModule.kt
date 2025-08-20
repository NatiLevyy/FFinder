package com.locationsharing.app.di

import com.locationsharing.app.navigation.NavigationAnalytics
import com.locationsharing.app.navigation.NavigationAnalyticsImpl
import com.locationsharing.app.navigation.NavigationManager
import com.locationsharing.app.navigation.NavigationManagerImpl
import com.locationsharing.app.navigation.NavigationStateTracker
import com.locationsharing.app.navigation.NavigationStateTrackerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing navigation-related dependencies.
 * Provides NavigationManager, NavigationStateTracker, and NavigationAnalytics implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    
    /**
     * Provides NavigationManager implementation.
     */
    @Binds
    @Singleton
    abstract fun bindNavigationManager(
        navigationManagerImpl: NavigationManagerImpl
    ): NavigationManager
    
    /**
     * Provides NavigationStateTracker implementation.
     */
    @Binds
    @Singleton
    abstract fun bindNavigationStateTracker(
        navigationStateTrackerImpl: NavigationStateTrackerImpl
    ): NavigationStateTracker
    
    /**
     * Provides NavigationAnalytics implementation.
     */
    @Binds
    @Singleton
    abstract fun bindNavigationAnalytics(
        navigationAnalyticsImpl: NavigationAnalyticsImpl
    ): NavigationAnalytics

}