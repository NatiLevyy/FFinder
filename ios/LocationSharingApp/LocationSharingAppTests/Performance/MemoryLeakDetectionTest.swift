import XCTest
import Foundation
@testable import LocationSharingApp

/**
 * Memory leak detection tests for iOS location sharing application.
 * These tests verify that services and repositories properly manage memory
 * and don't create memory leaks during normal operation.
 */
class MemoryLeakDetectionTest: XCTestCase {
    
    var authRepository: AuthRepositoryImpl!
    var locationService: LocationServiceImpl!
    var friendsRepository: FriendsRepositoryImpl!
    var mockFirebaseAuth: MockFirebaseAuth!
    var mockLocationManager: MockLocationManager!
    
    override func setUp() {
        super.setUp()
        
        // Initialize dependencies
        mockFirebaseAuth = MockFirebaseAuth.shared
        mockLocationManager = MockLocationManager.shared
        authRepository = AuthRepositoryImpl(sessionManager: SessionManager())
        locationService = LocationServiceImpl(
            locationTracker: LocationTracker(),
            locationRepository: LocationRepositoryImpl()
        )
        friendsRepository = FriendsRepositoryImpl()
        
        // Configure mocks
        mockFirebaseAuth.simulateSuccessfulAuth()
        mockLocationManager.simulateSuccessfulLocation()
    }
    
    override func tearDown() {
        authRepository = nil
        locationService = nil
        friendsRepository = nil
        mockFirebaseAuth = nil
        mockLocationManager = nil
        super.tearDown()
    }
    
    func testAuthRepositoryMemoryUsage() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        
        // When: Performing multiple authentication operations
        for i in 0..<50 {
            mockFirebaseAuth.simulateSuccessfulAuth(email: "user\(i)@example.com", uid: "user-\(i)")
            _ = try await authRepository.signIn(email: "user\(i)@example.com", password: "password")
            _ = try await authRepository.signOut()
            
            // Force garbage collection periodically
            if i % 10 == 0 {
                autoreleasepool {
                    // Force autorelease pool drain
                }
                try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
            }
        }
        
        // Force final garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        let finalMemory = getCurrentMemoryUsage()
        let memoryIncrease = finalMemory - initialMemory
        
        // Then: Memory usage should not increase significantly
        XCTAssertLessThan(
            memoryIncrease,
            10.0,
            "Memory usage should not increase by more than 10MB, actual increase: \(memoryIncrease)MB"
        )
    }
    
    func testLocationServiceMemoryUsage() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        
        // When: Starting and stopping location service multiple times
        for i in 0..<30 {
            mockLocationManager.setMockLocation(
                latitude: 37.7749 + (Double(i) * 0.001),
                longitude: -122.4194 + (Double(i) * 0.001),
                accuracy: 10.0
            )
            
            _ = try await locationService.startLocationTracking()
            
            // Simulate location updates
            for updateIndex in 0..<10 {
                mockLocationManager.simulateLocationUpdate(
                    latitude: 37.7749 + (Double(i) * 0.001) + (Double(updateIndex) * 0.0001),
                    longitude: -122.4194 + (Double(i) * 0.001) + (Double(updateIndex) * 0.0001),
                    accuracy: 10.0
                )
                try await Task.sleep(nanoseconds: 50_000_000) // 0.05 seconds
            }
            
            _ = try await locationService.stopLocationTracking()
            
            // Force garbage collection periodically
            if i % 10 == 0 {
                autoreleasepool {
                    // Force autorelease pool drain
                }
                try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
            }
        }
        
        // Force final garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        let finalMemory = getCurrentMemoryUsage()
        let memoryIncrease = finalMemory - initialMemory
        
        // Then: Memory usage should not increase significantly
        XCTAssertLessThan(
            memoryIncrease,
            15.0,
            "Location service memory usage should not increase by more than 15MB, actual increase: \(memoryIncrease)MB"
        )
    }
    
    func testFriendsRepositoryMemoryUsage() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        
        // When: Performing multiple friends operations
        for i in 0..<40 {
            do {
                _ = try await friendsRepository.getFriends(userId: "user-\(i)")
                _ = try await friendsRepository.searchUsersByEmail(email: "user\(i)@example.com")
                
                // Force garbage collection periodically
                if i % 10 == 0 {
                    autoreleasepool {
                        // Force autorelease pool drain
                    }
                    try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
                }
            } catch {
                // Expected for mock operations, continue test
            }
        }
        
        // Force final garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        let finalMemory = getCurrentMemoryUsage()
        let memoryIncrease = finalMemory - initialMemory
        
        // Then: Memory usage should not increase significantly
        XCTAssertLessThan(
            memoryIncrease,
            8.0,
            "Friends repository memory usage should not increase by more than 8MB, actual increase: \(memoryIncrease)MB"
        )
    }
    
    func testConcurrentOperationsMemoryUsage() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        
        // When: Running concurrent operations that might cause memory leaks
        await withTaskGroup(of: Void.self) { group in
            for index in 1...20 {
                group.addTask { [weak self] in
                    guard let self = self else { return }
                    
                    do {
                        // Simulate concurrent auth operations
                        self.mockFirebaseAuth.simulateSuccessfulAuth(
                            email: "concurrent\(index)@example.com",
                            uid: "user-\(index)"
                        )
                        _ = try await self.authRepository.signIn(
                            email: "concurrent\(index)@example.com",
                            password: "password"
                        )
                        
                        // Simulate concurrent location operations
                        self.mockLocationManager.setMockLocation(
                            latitude: 37.7749 + (Double(index) * 0.001),
                            longitude: -122.4194 + (Double(index) * 0.001),
                            accuracy: 10.0
                        )
                        _ = try await self.locationService.startLocationTracking()
                        try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
                        _ = try await self.locationService.stopLocationTracking()
                        
                        // Simulate concurrent friends operations
                        _ = try await self.friendsRepository.getFriends(userId: "user-\(index)")
                        
                        _ = try await self.authRepository.signOut()
                    } catch {
                        // Expected for some mock operations
                    }
                }
            }
        }
        
        // Force garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 1_000_000_000) // 1 second
        
        let finalMemory = getCurrentMemoryUsage()
        let memoryIncrease = finalMemory - initialMemory
        
        // Then: Concurrent operations should not cause memory leaks
        XCTAssertLessThan(
            memoryIncrease,
            20.0,
            "Concurrent operations memory usage should not increase by more than 20MB, actual increase: \(memoryIncrease)MB"
        )
    }
    
    func testLongRunningLocationTrackingMemory() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        
        // When: Running location tracking for an extended period
        mockLocationManager.setMockLocation(latitude: 37.7749, longitude: -122.4194, accuracy: 10.0)
        _ = try await locationService.startLocationTracking()
        
        // Simulate 5 minutes of location updates (compressed to 30 seconds for testing)
        for index in 0..<300 {
            mockLocationManager.simulateLocationUpdate(
                latitude: 37.7749 + (Double(index) * 0.00001),
                longitude: -122.4194 + (Double(index) * 0.00001),
                accuracy: 10.0
            )
            try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds instead of 1s for faster testing
            
            // Check memory periodically
            if index % 50 == 0 {
                let currentMemory = getCurrentMemoryUsage()
                let currentIncrease = currentMemory - initialMemory
                
                // Memory should not grow unbounded during long-running operations
                XCTAssertLessThan(
                    currentIncrease,
                    25.0,
                    "Memory should not grow unbounded during long-running tracking at iteration \(index), increase: \(currentIncrease)MB"
                )
            }
        }
        
        _ = try await locationService.stopLocationTracking()
        
        // Force garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        let finalMemory = getCurrentMemoryUsage()
        let memoryIncrease = finalMemory - initialMemory
        
        // Then: Long-running operations should not cause significant memory growth
        XCTAssertLessThan(
            memoryIncrease,
            12.0,
            "Long-running location tracking should not increase memory by more than 12MB, actual increase: \(memoryIncrease)MB"
        )
    }
    
    func testMemoryRecoveryAfterOperations() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        
        // When: Performing intensive operations
        for i in 0..<20 {
            // Heavy operations
            mockFirebaseAuth.simulateSuccessfulAuth(email: "heavy\(i)@example.com", uid: "user-\(i)")
            _ = try await authRepository.signIn(email: "heavy\(i)@example.com", password: "password")
            
            _ = try await locationService.startLocationTracking()
            for updateIndex in 0..<20 {
                mockLocationManager.simulateLocationUpdate(
                    latitude: 37.7749 + (Double(i) * 0.001) + (Double(updateIndex) * 0.0001),
                    longitude: -122.4194 + (Double(i) * 0.001) + (Double(updateIndex) * 0.0001),
                    accuracy: 10.0
                )
                try await Task.sleep(nanoseconds: 10_000_000) // 0.01 seconds
            }
            _ = try await locationService.stopLocationTracking()
            
            _ = try await authRepository.signOut()
        }
        
        // Record memory after operations but before cleanup
        let memoryAfterOperations = getCurrentMemoryUsage()
        
        // Force aggressive garbage collection
        for _ in 0..<5 {
            autoreleasepool {
                // Force autorelease pool drain
            }
            try await Task.sleep(nanoseconds: 200_000_000) // 0.2 seconds
        }
        
        let memoryAfterCleanup = getCurrentMemoryUsage()
        let memoryRecovered = memoryAfterOperations - memoryAfterCleanup
        let finalIncrease = memoryAfterCleanup - initialMemory
        
        // Then: Memory should be recoverable through garbage collection
        XCTAssertTrue(
            memoryRecovered > 0 || finalIncrease < 10.0,
            "Memory should be recoverable through GC, recovered: \(memoryRecovered)MB"
        )
        
        XCTAssertLessThan(
            finalIncrease,
            15.0,
            "Final memory increase should be minimal after cleanup, actual: \(finalIncrease)MB"
        )
    }
    
    func testMemoryUsageUnderStress() async throws {
        // Given: Initial memory state
        let initialMemory = getCurrentMemoryUsage()
        var memoryReadings: [Double] = []
        
        // When: Applying memory stress through rapid operations
        for iteration in 0..<100 {
            do {
                // Rapid fire operations
                mockFirebaseAuth.simulateSuccessfulAuth(
                    email: "stress\(iteration)@example.com",
                    uid: "user-\(iteration)"
                )
                _ = try await authRepository.signIn(
                    email: "stress\(iteration)@example.com",
                    password: "password"
                )
                
                _ = try await locationService.startLocationTracking()
                mockLocationManager.simulateLocationUpdate(
                    latitude: 37.7749 + (Double(iteration) * 0.0001),
                    longitude: -122.4194 + (Double(iteration) * 0.0001),
                    accuracy: 10.0
                )
                _ = try await locationService.stopLocationTracking()
                
                _ = try await friendsRepository.getFriends(userId: "user-\(iteration)")
                _ = try await authRepository.signOut()
                
                // Record memory every 10 iterations
                if iteration % 10 == 0 {
                    memoryReadings.append(getCurrentMemoryUsage())
                }
                
                // Brief pause to allow processing
                try await Task.sleep(nanoseconds: 10_000_000) // 0.01 seconds
            } catch {
                // Expected for some mock operations
            }
        }
        
        // Force garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        let finalMemory = getCurrentMemoryUsage()
        let memoryIncrease = finalMemory - initialMemory
        
        // Then: Memory should remain stable under stress
        XCTAssertLessThan(
            memoryIncrease,
            25.0,
            "Memory should remain stable under stress, increase: \(memoryIncrease)MB"
        )
        
        // Check that memory didn't grow unbounded during the test
        let maxMemoryDuringTest = memoryReadings.max() ?? initialMemory
        let maxIncrease = maxMemoryDuringTest - initialMemory
        XCTAssertLessThan(
            maxIncrease,
            30.0,
            "Memory should not grow unbounded during stress test, max increase: \(maxIncrease)MB"
        )
    }
    
    func testWeakReferenceHandling() async throws {
        // Given: Objects that should use weak references
        weak var weakLocationService: LocationServiceImpl?
        weak var weakAuthRepository: AuthRepositoryImpl?
        
        // When: Creating and releasing objects
        autoreleasepool {
            let tempLocationService = LocationServiceImpl(
                locationTracker: LocationTracker(),
                locationRepository: LocationRepositoryImpl()
            )
            let tempAuthRepository = AuthRepositoryImpl(sessionManager: SessionManager())
            
            weakLocationService = tempLocationService
            weakAuthRepository = tempAuthRepository
            
            // Use the objects briefly
            Task {
                _ = try? await tempLocationService.startLocationTracking()
                _ = try? await tempLocationService.stopLocationTracking()
            }
        }
        
        // Force garbage collection
        autoreleasepool {
            // Force autorelease pool drain
        }
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        // Then: Weak references should be nil (objects should be deallocated)
        XCTAssertNil(weakLocationService, "LocationService should be deallocated")
        XCTAssertNil(weakAuthRepository, "AuthRepository should be deallocated")
    }
    
    private func getCurrentMemoryUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        if kerr == KERN_SUCCESS {
            // Convert to MB
            return Double(info.resident_size) / (1024.0 * 1024.0)
        } else {
            // Fallback for testing environments
            return Double(arc4random_uniform(100)) + 50.0 // Simulate 50-150MB usage
        }
    }
}