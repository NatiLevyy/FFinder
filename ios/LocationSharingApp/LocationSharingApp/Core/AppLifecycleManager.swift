import Foundation
import SwiftUI

@MainActor
class AppLifecycleManager: ObservableObject {
    @Published var isAppActive: Bool = true
    
    private let locationService = DIContainer.shared.locationService
    private let locationSharingManager = DIContainer.shared.locationSharingManager
    private let offlineQueueManager = DIContainer.shared.offlineQueueManager
    private let cacheManager = DIContainer.shared.cacheManager
    private let backgroundLocationManager = DIContainer.shared.backgroundLocationManager
    private let errorHandler = DIContainer.shared.errorHandler
    
    init() {
        initializeServices()
    }
    
    private func initializeServices() {
        Task {
            do {
                // Initialize cache manager
                await cacheManager.initialize()
                
                // Initialize offline queue manager
                await offlineQueueManager.initialize()
                
            } catch {
                _ = errorHandler.handleError(error)
            }
        }
    }
    
    func handleAppWillEnterForeground() async {
        isAppActive = true
        
        do {
            // Resume location sharing if user was authenticated
            await locationSharingManager.resumeLocationSharing()
            
            // Process offline queue
            await offlineQueueManager.processQueue()
            
            // Resume foreground location updates
            try await locationService.resumeLocationUpdates()
            
        } catch {
            _ = errorHandler.handleError(error)
        }
    }
    
    func handleAppDidEnterBackground() async {
        isAppActive = false
        
        do {
            // Optimize for background operation
            await locationSharingManager.optimizeForBackground()
            
            // Start background location tracking if permissions allow
            await backgroundLocationManager.startBackgroundLocationTracking()
            
            // Clean up unnecessary cache
            await cacheManager.cleanupExpiredEntries()
            
            // Pause foreground location updates
            await locationService.pauseLocationUpdates()
            
        } catch {
            _ = errorHandler.handleError(error)
        }
    }
    
    func handleAppWillTerminate() async {
        do {
            // Stop location services
            await locationService.stopLocationUpdates()
            
            // Clean up location sharing
            await locationSharingManager.cleanup()
            
            // Process any remaining offline queue items
            await offlineQueueManager.processQueue()
            
            // Clean up cache
            await cacheManager.cleanup()
            
            // Stop background location tracking
            await backgroundLocationManager.stopBackgroundLocationTracking()
            
        } catch {
            _ = errorHandler.handleError(error)
        }
    }
    
    func handleMemoryWarning() async {
        do {
            // Clear non-essential cache
            await cacheManager.clearNonEssentialCache()
            
            // Pause non-essential services
            await locationSharingManager.pauseNonEssentialServices()
            
            // Optimize memory usage
            await optimizeMemoryUsage()
            
        } catch {
            _ = errorHandler.handleError(error)
        }
    }
    
    private func optimizeMemoryUsage() async {
        // Release any large objects or caches that can be recreated
        await cacheManager.trimMemory(.critical)
        
        // Reduce location update frequency temporarily
        await locationService.reduceUpdateFrequency()
    }
    
    func cleanup() async {
        await handleAppWillTerminate()
    }
}