package com.locationsharing.app.di

import com.locationsharing.app.data.friends.FriendsRepository
import com.locationsharing.app.data.location.EnhancedLocationService
import com.locationsharing.app.domain.usecase.GetNearbyFriendsUseCase
import com.locationsharing.app.ui.friends.components.NearbyPanelPerformanceMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    @Provides
    @Singleton
    fun provideGetNearbyFriendsUseCase(
        friendsRepository: FriendsRepository,
        locationService: EnhancedLocationService,
        performanceMonitor: NearbyPanelPerformanceMonitor,
        @IoDispatcher dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): GetNearbyFriendsUseCase {
        return GetNearbyFriendsUseCase(
            friendsRepository = friendsRepository,
            locationService = locationService,
            performanceMonitor = performanceMonitor,
            dispatcher = dispatcher
        )
    }
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher