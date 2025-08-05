import Foundation
import SwiftUI
import CoreLocation

@MainActor
class MainViewModel: ObservableObject {
    @Published var appState: AppState = .loading
    
    private let locationService = DIContainer.shared.locationService
    private let locationSharingManager = DIContainer.shared.locationSharingManager
    private let notificationService = DIContainer.shared.notificationService
    private let connectivityMonitor = DIContainer.shared.connectivityMonitor
    private let offlineQueueManager = DIContainer.shared.offlineQueueManager
    private let errorHandler = DIContainer.shared.errorHandler
    
    enum AppState {
        case loading
        case ready
        case error(String)
    }
    
    func initializeApp() async {
        appState = .loading
        
        do {
            // Initialize location services
            try await initializeLocationServices()
            
            // Initialize notification services
            await initializeNotificationServices()
            
            // Initialize offline support
            await initializeOfflineSupport()
            
            // Initialize location sharing
            await initializeLocationSharing()
            
            appState = .ready
        } catch {
            let errorMessage = errorHandler.handleError(error)
            appState = .error(errorMessage)
        }
    }
    
    private func initializeLocationServices() async throws {
        // Request location permissions
        let permissionStatus = await locationService.requestLocationPermission()
        
        guard permissionStatus == .authorized || permissionStatus == .authorizedWhenInUse else {
            throw LocationError.permissionDenied
        }
        
        // Start location tracking
        try await locationService.startLocationUpdates()
    }
    
    private func initializeNotificationServices() async {
        do {
            // Initialize notification service
            await notificationService.initialize()
            
            // Request notification permissions
            try await notificationService.requestNotificationPermission()
        } catch {
            // Notification failures shouldn't block app initialization
            _ = errorHandler.handleError(error)
        }
    }
    
    private func initializeOfflineSupport() async {
        do {
            await offlineQueueManager.initialize()
            
            // Start monitoring connectivity
            await connectivityMonitor.startMonitoring()
        } catch {
            // Offline support failures shouldn't block app initialization
            _ = errorHandler.handleError(error)
        }
    }
    
    private func initializeLocationSharing() async {
        do {
            await locationSharingManager.initialize()
        } catch {
            // Location sharing failures shouldn't block app initialization
            _ = errorHandler.handleError(error)
        }
    }
    
    func cleanup() async {
        do {
            // Stop location services
            await locationService.stopLocationUpdates()
            
            // Clean up location sharing
            await locationSharingManager.cleanup()
            
            // Process any remaining offline queue items
            await offlineQueueManager.processQueue()
            
            // Clean up notification service
            await notificationService.cleanup()
            
            // Stop connectivity monitoring
            await connectivityMonitor.stopMonitoring()
        } catch {
            _ = errorHandler.handleError(error)
        }
    }
}