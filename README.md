# Location Sharing App

A cross-platform mobile application for real-time location sharing between friends, built with native Android (Kotlin) and iOS (Swift) implementations.

## Project Structure

### Android (Kotlin)
```
android/
├── app/
│   ├── build.gradle.kts          # App-level build configuration
│   ├── google-services.json      # Firebase configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # App permissions and configuration
│       ├── java/com/locationsharing/app/
│       │   ├── LocationSharingApplication.kt  # Hilt application class
│       │   ├── data/models/       # Data models (User, Location, Friend, etc.)
│       │   ├── domain/
│       │   │   ├── repository/    # Repository interfaces
│       │   │   └── service/       # Service interfaces
│       │   ├── di/               # Dependency injection modules
│       │   └── presentation/     # UI components
│       └── res/                  # Android resources
└── build.gradle.kts             # Project-level build configuration
```

### iOS (Swift)
```
ios/LocationSharingApp/
├── LocationSharingApp/
│   ├── LocationSharingApp.swift   # Main app entry point
│   ├── ContentView.swift          # Main content view
│   ├── Info.plist                # App configuration and permissions
│   ├── GoogleService-Info.plist  # Firebase configuration
│   ├── Models/                   # Data models (User, Location, Friend, etc.)
│   ├── Domain/
│   │   ├── Repositories/         # Repository protocols
│   │   └── Services/             # Service protocols
│   └── DI/                      # Dependency injection container
├── Package.swift                # Swift Package Manager dependencies
└── LocationSharingApp.xcodeproj/ # Xcode project file
```

## Core Interfaces

### Authentication
- `AuthRepository`: Handles user authentication and session management
- Supports email/password authentication via Firebase Auth
- JWT token management and refresh
- Comprehensive error handling with `AuthError` types
- Secure token storage using EncryptedSharedPreferences (Android) and Keychain (iOS)

### Location Services
- `LocationRepository`: Manages location data and backend synchronization
- `LocationService`: Platform-specific location tracking
- Real-time location updates every 5 seconds
- Background location tracking support

### Friends Management
- `FriendsRepository`: Handles friend relationships and requests
- Friend search, request sending/accepting, and removal
- Location sharing permission management

### Maps
- `MapService`: Interactive map display and marker management
- Google Maps SDK (Android) and MapKit (iOS)
- Real-time friend location visualization

## Dependencies

### Android
- **Hilt**: Dependency injection
- **Firebase**: Authentication, Firestore, Realtime Database, Cloud Messaging
- **Google Maps SDK**: Map display and location services
- **Coroutines**: Asynchronous programming

### iOS
- **Firebase iOS SDK**: Authentication, Firestore, Realtime Database, Cloud Messaging
- **MapKit**: Map display and location services
- **Combine**: Reactive programming
- **SwiftUI**: User interface framework

## Authentication Usage

### Android (Kotlin)
```kotlin
// Inject AuthRepository using Hilt
@Inject
lateinit var authRepository: AuthRepository

// Sign in user
val result = authRepository.signIn("user@example.com", "password123")
result.fold(
    onSuccess = { authResult ->
        val user = authResult.user
        val token = authResult.token
        // Handle successful authentication
    },
    onFailure = { error ->
        when (error) {
            is AuthError.InvalidCredentials -> // Handle invalid credentials
            is AuthError.NetworkError -> // Handle network error
            else -> // Handle other errors
        }
    }
)

// Check authentication status
val isAuthenticated = authRepository.isUserAuthenticated()

// Get current user
val currentUser = authRepository.getCurrentUser()
```

### iOS (Swift)
```swift
// Inject AuthRepository using dependency injection
let authRepository: AuthRepository

// Sign in user
let result = await authRepository.signIn(email: "user@example.com", password: "password123")
switch result {
case .success(let authResult):
    let user = authResult.user
    let token = authResult.token
    // Handle successful authentication
case .failure(let error):
    switch error {
    case .invalidCredentials:
        // Handle invalid credentials
    case .networkError:
        // Handle network error
    default:
        // Handle other errors
    }
}

// Check authentication status
let isAuthenticated = await authRepository.isUserAuthenticated()

// Get current user
let currentUser = await authRepository.getCurrentUser()
```

## Testing

### Running Android Tests
```bash
cd android
./gradlew test
```

### Running iOS Tests
```bash
cd ios/LocationSharingApp
swift test
```

### Test Coverage
- **AuthResult**: Data model validation and serialization
- **AuthError**: Error type handling and localization
- **JwtTokenManager**: Token storage, retrieval, and validation
- **AuthRepository**: Interface contract validation with mocking

## Configuration

### Firebase Setup
1. Replace placeholder `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) with your actual Firebase configuration files
2. Update the Google Maps API key in `android/app/src/main/res/values/strings.xml`

### Permissions
- **Android**: Location permissions (fine, coarse, background), internet, foreground service
- **iOS**: Location permissions (when in use, always), background app refresh

## Next Steps
This initial setup provides the foundation for the location sharing app. The next tasks will implement:
1. Authentication functionality
2. Location tracking services
3. Maps integration
4. Friends management system
5. Real-time location sharing
6. UI components and screens

## Requirements Addressed
- **1.1**: Authentication system foundation with Firebase Auth integration
- **1.2**: User data models and secure session management setup
- **1.3**: JWT token management interfaces and Firebase configuration#   F F i n d e r 
 
 