import Foundation
import Combine

/**
 * Manages user session state throughout the application lifecycle.
 * Provides reactive access to current user information.
 */
@MainActor
class SessionManager: ObservableObject {
    
    // MARK: - Singleton
    
    static let shared = SessionManager()
    
    // MARK: - Published Properties
    
    @Published private(set) var currentUser: User?
    @Published private(set) var isAuthenticated: Bool = false
    
    // MARK: - Private Properties
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    
    private init() {
        // Update authentication state when user changes
        $currentUser
            .map { $0 != nil }
            .assign(to: \.isAuthenticated, on: self)
            .store(in: &cancellables)
    }
    
    // MARK: - Public Methods
    
    /**
     * Sets the current authenticated user.
     * 
     * - Parameter user: The authenticated user
     */
    func setCurrentUser(_ user: User) {
        currentUser = user
    }
    
    /**
     * Clears the current user session.
     */
    func clearCurrentUser() {
        currentUser = nil
    }
    
    /**
     * Gets the current user synchronously.
     * 
     * - Returns: The current user or nil if not authenticated
     */
    func getCurrentUserSync() -> User? {
        return currentUser
    }
    
    /**
     * Checks if a user is currently authenticated.
     * 
     * - Returns: True if authenticated, false otherwise
     */
    func isUserAuthenticated() -> Bool {
        return isAuthenticated && currentUser != nil
    }
    
    /**
     * Updates the current user information.
     * 
     * - Parameter user: The updated user information
     */
    func updateCurrentUser(_ user: User) {
        if currentUser?.id == user.id {
            currentUser = user
        }
    }
    
    // MARK: - Combine Publishers
    
    /**
     * Publisher for current user changes.
     */
    var currentUserPublisher: AnyPublisher<User?, Never> {
        $currentUser.eraseToAnyPublisher()
    }
    
    /**
     * Publisher for authentication state changes.
     */
    var isAuthenticatedPublisher: AnyPublisher<Bool, Never> {
        $isAuthenticated.eraseToAnyPublisher()
    }
}