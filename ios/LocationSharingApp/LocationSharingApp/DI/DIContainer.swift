import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseDatabase

class DIContainer {
    static let shared = DIContainer()
    
    private init() {}
    
    // MARK: - Firebase Services
    lazy var firebaseAuth: Auth = {
        return Auth.auth()
    }()
    
    lazy var firebaseFirestore: Firestore = {
        return Firestore.firestore()
    }()
    
    lazy var firebaseDatabase: Database = {
        return Database.database()
    }()
    
    // MARK: - Core Services
    lazy var errorHandler: ErrorHandler = {
        return ErrorHandler(
            networkErrorHandler: networkErrorHandler,
            userErrorMessageProvider: userErrorMessageProvider
        )
    }()
    
    lazy var networkErrorHandler: NetworkErrorHandler = {
        return NetworkErrorHandler()
    }()
    
    lazy var userErrorMessageProvider: UserErrorMessageProvider = {
        return UserErrorMessageProvider()
    }()
    
    lazy var connectivityMonitor: ConnectivityMonitor = {
        return ConnectivityMonitor()
    }()
    
    lazy var offlineQueueManager: OfflineQueueManager = {
        return OfflineQueueManager(connectivityMonitor: connectivityMonitor)
    }()
    
    lazy var cacheManager: CacheManager = {
        return CacheManager.shared
    }()
    
    // MARK: - Security Services
    lazy var secureStorage: SecureStorage = {
        return SecureStorage()
    }()
    
    lazy var secureTokenManager: SecureTokenManager = {
        return SecureTokenManager(secureStorage: secureStorage)
    }()
    
    // MARK: - Auth Dependencies
    lazy var jwtTokenManager: JwtTokenManager = {
        return JwtTokenManager(secureStorage: secureStorage)
    }()
    
    lazy var sessionManager: SessionManager = {
        return SessionManager(secureStorage: secureStorage, jwtTokenManager: jwtTokenManager)
    }()
    
    // MARK: - Repository Dependencies
    lazy var authRepository: AuthRepository = {
        return AuthRepositoryImpl(
            firebaseAuth: firebaseAuth,
            jwtTokenManager: jwtTokenManager,
            sessionManager: sessionManager
        )
    }()
    
    lazy var locationRepository: LocationRepository = {
        return LocationRepositoryImpl(
            firebaseAuth: firebaseAuth,
            firebaseDatabase: firebaseDatabase,
            locationCache: locationCache
        )
    }()
    
    lazy var friendsRepository: FriendsRepository = {
        return FriendsRepositoryImpl(
            firebaseAuth: firebaseAuth,
            firebaseFirestore: firebaseFirestore
        )
    }()
    
    // MARK: - Location Services
    lazy var locationService: LocationService = {
        return LocationServiceImpl(
            locationTracker: locationTracker,
            locationPermissionManager: locationPermissionManager
        )
    }()
    
    lazy var locationTracker: LocationTracker = {
        return LocationTracker()
    }()
    
    lazy var locationPermissionManager: LocationPermissionManager = {
        return LocationPermissionManager()
    }()
    
    lazy var locationCache: LocationCache = {
        return LocationCache()
    }()
    
    lazy var locationValidator: LocationValidator = {
        return LocationValidator()
    }()
    
    lazy var backgroundLocationManager: BackgroundLocationManager = {
        return BackgroundLocationManager()
    }()
    
    // MARK: - Location Sharing Services
    lazy var locationSharingService: LocationSharingService = {
        return LocationSharingService(
            firebaseAuth: firebaseAuth,
            firebaseDatabase: firebaseDatabase,
            locationValidator: locationValidator
        )
    }()
    
    lazy var locationSharingManager: LocationSharingManager = {
        return LocationSharingManager(friendsRepository: friendsRepository)
    }()
    
    // MARK: - Map Service Dependencies
    lazy var mapKitService: MapKitService = {
        return MapKitService()
    }()
    
    lazy var locationMarkerManager: LocationMarkerManager = {
        return LocationMarkerManager(mapService: mapKitService)
    }()
    
    lazy var realTimeLocationUpdateManager: RealTimeLocationUpdateManager = {
        return RealTimeLocationUpdateManager(locationMarkerManager: locationMarkerManager)
    }()
    
    // MARK: - Notification Services
    lazy var notificationService: NotificationService = {
        return NotificationServiceImpl(notificationHandler: notificationHandler)
    }()
    
    lazy var notificationHandler: NotificationHandler = {
        return NotificationHandler()
    }()
    

}