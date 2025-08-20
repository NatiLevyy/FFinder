package com.locationsharing.app.di

import android.content.Context
import com.locationsharing.app.navigation.performance.NavigationCache
import com.locationsharing.app.navigation.performance.NavigationDestinationLoader
import com.locationsharing.app.navigation.performance.NavigationPerformanceMonitor
import com.locationsharing.app.navigation.performance.OptimizedButtonResponseManager
import com.locationsharing.app.ui.components.button.ButtonResponseManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for navigation performance optimization dependencies.
 * Provides performance monitoring, caching, and optimization components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationPerformanceModule {
    
    /**
     * Bind the optimized button response manager as the default implementation.
     */
    @Binds
    @Singleton
    abstract fun bindButtonResponseManager(
        optimizedButtonResponseManager: OptimizedButtonResponseManager
    ): ButtonResponseManager
    
    companion object {
        
        /**
         * Provide navigation performance monitor.
         */
        @Provides
        @Singleton
        fun provideNavigationPerformanceMonitor(
            @ApplicationContext context: Context
        ): NavigationPerformanceMonitor {
            return NavigationPerformanceMonitor(context)
        }
        
        /**
         * Provide navigation cache.
         */
        @Provides
        @Singleton
        fun provideNavigationCache(
            @ApplicationContext context: Context,
            performanceMonitor: NavigationPerformanceMonitor
        ): NavigationCache {
            return NavigationCache(context, performanceMonitor)
        }
        
        /**
         * Provide navigation destination loader.
         */
        @Provides
        @Singleton
        fun provideNavigationDestinationLoader(
            navigationCache: NavigationCache,
            performanceMonitor: NavigationPerformanceMonitor
        ): NavigationDestinationLoader {
            return NavigationDestinationLoader(navigationCache, performanceMonitor)
        }
        
        /**
         * Provide optimized button response manager.
         */
        @Provides
        @Singleton
        fun provideOptimizedButtonResponseManager(
            performanceMonitor: NavigationPerformanceMonitor
        ): OptimizedButtonResponseManager {
            return OptimizedButtonResponseManager(performanceMonitor)
        }
    }
}