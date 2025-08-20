package com.locationsharing.app.di

import com.locationsharing.app.ui.components.feedback.HapticFeedbackManager
import com.locationsharing.app.ui.components.feedback.HapticFeedbackManagerImpl
import com.locationsharing.app.ui.components.feedback.VisualFeedbackManager
import com.locationsharing.app.ui.components.feedback.VisualFeedbackManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for visual feedback components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VisualFeedbackModule {
    
    @Binds
    @Singleton
    abstract fun bindVisualFeedbackManager(
        visualFeedbackManagerImpl: VisualFeedbackManagerImpl
    ): VisualFeedbackManager
    
    @Binds
    @Singleton
    abstract fun bindHapticFeedbackManager(
        hapticFeedbackManagerImpl: HapticFeedbackManagerImpl
    ): HapticFeedbackManager
}