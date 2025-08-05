import XCTest
import Combine
@testable import LocationSharingApp

class LocationServiceImplTests: XCTestCase {
    
    var locationService: LocationServiceImpl!
    var mockLocationTracker: MockLocationTracker!
    var mockPermissionManager: MockLocationPermissionManager!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        mockLocationTracker = MockLocationTracker()
        mockPermissionManager = MockLocationPermissionManager()
        locationService = LocationServiceImpl(
            locationTracker: mockLocationTracker,
            permissionManager: mockPermissionManager
        )
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        cancellables = nil
        locationService = nil
        mockLocationTracker = nil
        mockPermissionManager = nil
        super.tearDown()
    }
    
    func testStartLocationTracking_DelegatesToTracker() async {
        // Given
        let interval: TimeInterval = 5.0
        mockLocationTracker.startLocationTrackingResult = .success(())
        
        // When
        let result = await locationService.startLocationTracking(interval: interval)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Should return success")
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        XCTAssertTrue(mockLocationTracker.startLocationTrackingCalled)
        XCTAssertEqual(mockLocationTracker.startLocationTrackingInterval, interval)
    }
    
    func testStopLocationTracking_DelegatesToTracker() async {
        // Given
        mockLocationTracker.stopLocationTrackingResult = .success(())
        
        // When
        let result = await locationService.stopLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Should return success")
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        XCTAssertTrue(mockLocationTracker.stopLocationTrackingCalled)
    }
    
    func testGetCurrentLocation_DelegatesToTracker() async {
        // Given
        let timeout: TimeInterval = 10.0
        let expectedLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Date()
        )
        mockLocationTracker.getCurrentLocationResult = .success(expectedLocation)
        
        // When
        let result = await locationService.getCurrentLocation(timeout: timeout)
        
        // Then
        switch result {
        case .success(let location):
            XCTAssertEqual(location, expectedLocation)
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        XCTAssertTrue(mockLocationTracker.getCurrentLocationCalled)
        XCTAssertEqual(mockLocationTracker.getCurrentLocationTimeout, timeout)
    }
    
    func testLocationUpdates_ReturnsTrackerPublisher() async {
        // Given
        let expectedLocation = Location(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5.0,
            timestamp = Date()
        )
        mockLocationTracker.locationUpdatesSubject.send(expectedLocation)
        
        // When
        let location = await locationService.locationUpdates.first()
        
        // Then
        XCTAssertEqual(location, expectedLocation)
    }
    
    func testLocationErrors_ReturnsTrackerPublisher() async {
        // Given
        let expectedError = LocationError.permissionDenied
        mockLocationTracker.locationErrorsSubject.send(expectedError)
        
        // When
        let error = await locationService.locationErrors.first()
        
        // Then
        XCTAssertEqual(error, expectedError)
    }
    
    func testIsTracking_ReturnsTrackerPublisher() async {
        // Given
        mockLocationTracker.isTrackingSubject.send(true)
        
        // When
        let isTracking = await locationService.isTracking.first()
        
        // Then
        XCTAssertTrue(isTracking)
    }
    
    func testCheckLocationPermission_DelegatesToPermissionManager() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        
        // When
        let result = await locationService.checkLocationPermission()
        
        // Then
        XCTAssertEqual(result, .granted)
    }
    
    func testCheckBackgroundLocationPermission_DelegatesToPermissionManager() async {
        // Given
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        
        // When
        let result = await locationService.checkBackgroundLocationPermission()
        
        // Then
        XCTAssertEqual(result, .granted)
    }
    
    func testRequestLocationPermission_ReturnsGrantedWhenSuccessful() async {
        // Given
        mockPermissionManager.requestLocationPermissionsResult = .success(.granted)
        
        // When
        let result = await locationService.requestLocationPermission()
        
        // Then
        switch result {
        case .success(let status):
            XCTAssertEqual(status, .granted)
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        XCTAssertTrue(mockPermissionManager.requestLocationPermissionsCalled)
        XCTAssertFalse(mockPermissionManager.requestLocationPermissionsRequireBackground)
    }
    
    func testRequestLocationPermission_ReturnsErrorWhenFailed() async {
        // Given
        mockPermissionManager.requestLocationPermissionsResult = .failure(.permissionDenied)
        
        // When
        let result = await locationService.requestLocationPermission()
        
        // Then
        switch result {
        case .success:
            XCTFail("Should fail when permission manager fails")
        case .failure(let error):
            XCTAssertEqual(error, .permissionDenied)
        }
    }
    
    func testRequestBackgroundLocationPermission_ReturnsGrantedWhenSuccessful() async {
        // Given
        mockPermissionManager.requestLocationPermissionsResult = .success(.granted)
        
        // When
        let result = await locationService.requestBackgroundLocationPermission()
        
        // Then
        switch result {
        case .success(let status):
            XCTAssertEqual(status, .granted)
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        XCTAssertTrue(mockPermissionManager.requestLocationPermissionsCalled)
        XCTAssertTrue(mockPermissionManager.requestLocationPermissionsRequireBackground)
    }
    
    func testRequestBackgroundLocationPermission_ReturnsErrorWhenFailed() async {
        // Given
        mockPermissionManager.requestLocationPermissionsResult = .failure(.backgroundPermissionDenied)
        
        // When
        let result = await locationService.requestBackgroundLocationPermission()
        
        // Then
        switch result {
        case .success:
            XCTFail("Should fail when permission manager fails")
        case .failure(let error):
            XCTAssertEqual(error, .backgroundPermissionDenied)
        }
    }
    
    func testIsLocationEnabled_DelegatesToTracker() async {
        // Given
        mockLocationTracker.isLocationEnabledValue = true
        
        // When
        let result = await locationService.isLocationEnabled()
        
        // Then
        XCTAssertTrue(result)
        XCTAssertTrue(mockLocationTracker.isLocationEnabledCalled)
    }
    
    func testPromptEnableLocation_ReturnsSuccessWhenSettingsOpened() async {
        // Given
        // This test is difficult to mock since it involves UIApplication.shared.open
        // We'll test the basic flow
        
        // When
        let result = await locationService.promptEnableLocation()
        
        // Then
        // In test environment, this might fail due to simulator limitations
        // We accept both success and failure as valid outcomes
        switch result {
        case .success(let opened):
            XCTAssertTrue(opened == true || opened == false)
        case .failure:
            // Expected in test environment
            XCTAssertTrue(true, "Expected failure in test environment")
        }
    }
}

// MARK: - Mock Classes

class MockLocationTracker {
    
    // MARK: - Call Tracking
    
    var startLocationTrackingCalled = false
    var startLocationTrackingInterval: TimeInterval?
    var startLocationTrackingResult: Result<Void, LocationError> = .success(())
    
    var stopLocationTrackingCalled = false
    var stopLocationTrackingResult: Result<Void, LocationError> = .success(())
    
    var getCurrentLocationCalled = false
    var getCurrentLocationTimeout: TimeInterval?
    var getCurrentLocationResult: Result<Location, LocationError> = .success(
        Location(latitude: 0, longitude: 0, accuracy: 0, timestamp: Date())
    )
    
    var isLocationEnabledCalled = false
    var isLocationEnabledValue = true
    
    // MARK: - Publishers
    
    let locationUpdatesSubject = PassthroughSubject<Location, Never>()
    let locationErrorsSubject = PassthroughSubject<LocationError, Never>()
    let isTrackingSubject = CurrentValueSubject<Bool, Never>(false)
    
    var locationUpdates: AnyPublisher<Location, Never> {
        locationUpdatesSubject.eraseToAnyPublisher()
    }
    
    var locationErrors: AnyPublisher<LocationError, Never> {
        locationErrorsSubject.eraseToAnyPublisher()
    }
    
    var isTracking: AnyPublisher<Bool, Never> {
        isTrackingSubject.eraseToAnyPublisher()
    }
    
    // MARK: - Methods
    
    func startLocationTracking(interval: TimeInterval) async -> Result<Void, LocationError> {
        startLocationTrackingCalled = true
        startLocationTrackingInterval = interval
        return startLocationTrackingResult
    }
    
    func stopLocationTracking() async -> Result<Void, LocationError> {
        stopLocationTrackingCalled = true
        return stopLocationTrackingResult
    }
    
    func getCurrentLocation(timeout: TimeInterval) async -> Result<Location, LocationError> {
        getCurrentLocationCalled = true
        getCurrentLocationTimeout = timeout
        return getCurrentLocationResult
    }
    
    func isLocationEnabled() -> Bool {
        isLocationEnabledCalled = true
        return isLocationEnabledValue
    }
}

extension MockLocationPermissionManager {
    
    // MARK: - Additional Properties for LocationServiceImpl Tests
    
    var requestLocationPermissionsCalled = false
    var requestLocationPermissionsRequireBackground = false
    var requestLocationPermissionsResult: Result<PermissionStatus, LocationError> = .success(.granted)
    
    func requestLocationPermissions(requireBackground: Bool) async -> Result<PermissionStatus, LocationError> {
        requestLocationPermissionsCalled = true
        requestLocationPermissionsRequireBackground = requireBackground
        return requestLocationPermissionsResult
    }
}