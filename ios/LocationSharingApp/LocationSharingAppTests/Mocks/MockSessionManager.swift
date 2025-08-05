import Foundation
import Combine
@testable import LocationSharingApp

@MainActor
class MockSessionManager: SessionManager {
    
    // MARK: - Method Call Tracking
    
    var setCurrentUserCalled = false
    var clearCurrentUserCalled = false
    var updateCurrentUserCalled = false
    
    // MARK: - Stored Values
    
    private var _currentUser: User?
    private var _isAuthenticated: Bool = false
    
    // MARK: - Publishers
    
    private let currentUserSubject = CurrentValueSubject<User?, Never>(nil)
    private let isAuthenticatedSubject = CurrentValueSubject<Bool, Never>(false)
    
    // MARK: - Overrides
    
    override var currentUser: User? {
        return _currentUser
    }
    
    override var isAuthenticated: Bool {
        return _isAuthenticated
    }
    
    override var currentUserPublisher: AnyPublisher<User?, Never> {
        return currentUserSubject.eraseToAnyPublisher()
    }
    
    override var isAuthenticatedPublisher: AnyPublisher<Bool, Never> {
        return isAuthenticatedSubject.eraseToAnyPublisher()
    }
    
    // MARK: - Mock Methods
    
    override func setCurrentUser(_ user: User) {
        setCurrentUserCalled = true
        _currentUser = user
        _isAuthenticated = true
        currentUserSubject.send(user)
        isAuthenticatedSubject.send(true)
    }
    
    override func clearCurrentUser() {
        clearCurrentUserCalled = true
        _currentUser = nil
        _isAuthenticated = false
        currentUserSubject.send(nil)
        isAuthenticatedSubject.send(false)
    }
    
    override func getCurrentUserSync() -> User? {
        return _currentUser
    }
    
    override func isUserAuthenticated() -> Bool {
        return _isAuthenticated && _currentUser != nil
    }
    
    override func updateCurrentUser(_ user: User) {
        updateCurrentUserCalled = true
        if _currentUser?.id == user.id {
            _currentUser = user
            currentUserSubject.send(user)
        }
    }
    
    // MARK: - Reset Methods
    
    func reset() {
        setCurrentUserCalled = false
        clearCurrentUserCalled = false
        updateCurrentUserCalled = false
        _currentUser = nil
        _isAuthenticated = false
        currentUserSubject.send(nil)
        isAuthenticatedSubject.send(false)
    }
}