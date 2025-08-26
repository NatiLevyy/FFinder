package com.locationsharing.app.di

import com.locationsharing.app.data.contacts.ContactImportManagerImpl
import com.locationsharing.app.data.discovery.FirebaseUserDiscoveryService
import com.locationsharing.app.domain.repository.ContactImportManager
import com.locationsharing.app.domain.repository.UserDiscoveryService
// import dagger.Binds
// import dagger.Module
// import dagger.hilt.InstallIn
// import dagger.hilt.components.SingletonComponent
// import javax.inject.Singleton

/**
 * Hilt module for contact and discovery related dependencies
 * Provides contact import, user discovery, and friend matching functionality
 */
// @Module
// @InstallIn(SingletonComponent::class)
abstract class ContactModule {
    
    /**
     * Bind ContactImportManager implementation
     */
    // @Binds
    // @Singleton
    abstract fun bindContactImportManager(
        contactImportManagerImpl: ContactImportManagerImpl
    ): ContactImportManager
    
    /**
     * Bind UserDiscoveryService implementation
     */
    // @Binds
    // @Singleton
    abstract fun bindUserDiscoveryService(
        firebaseUserDiscoveryService: FirebaseUserDiscoveryService
    ): UserDiscoveryService
}