import Foundation
import CoreLocation

/**
 * Represents a geographical location with coordinates and metadata.
 */
struct Location {
    /// The latitude coordinate in degrees
    let latitude: Double
    
    /// The longitude coordinate in degrees
    let longitude: Double
    
    /// The accuracy of the location in meters
    let accuracy: Double
    
    /// The timestamp when the location was recorded
    let timestamp: Date
    
    /// The altitude in meters above sea level (optional)
    let altitude: Double?
    
    /// The speed in meters per second (optional)
    let speed: Double?
    
    /// The bearing in degrees (optional)
    let bearing: Double?
    
    // MARK: - Constants
    
    static let invalidAccuracy: Double = -1.0
    static let minAccuracyThreshold: Double = 100.0 // meters
    static let highAccuracyThreshold: Double = 10.0 // meters
    
    // MARK: - Computed Properties
    
    /**
     * Checks if the location has acceptable accuracy for sharing.
     */
    var hasAcceptableAccuracy: Bool {
        return accuracy > 0 && accuracy <= Self.minAccuracyThreshold
    }
    
    /**
     * Checks if the location is considered high accuracy.
     */
    var isHighAccuracy: Bool {
        return accuracy > 0 && accuracy <= Self.highAccuracyThreshold
    }
    
    /**
     * Calculates the age of the location in seconds.
     */
    var age: TimeInterval {
        return Date().timeIntervalSince(timestamp)
    }
    
    /**
     * Checks if the location is considered fresh (less than 30 seconds old).
     */
    var isFresh: Bool {
        return age < 30.0 // 30 seconds
    }
    
    // MARK: - Initializers
    
    init(
        latitude: Double,
        longitude: Double,
        accuracy: Double,
        timestamp: Date = Date(),
        altitude: Double? = nil,
        speed: Double? = nil,
        bearing: Double? = nil
    ) {
        self.latitude = latitude
        self.longitude = longitude
        self.accuracy = accuracy
        self.timestamp = timestamp
        self.altitude = altitude
        self.speed = speed
        self.bearing = bearing
    }
    
    /**
     * Creates a Location from a CLLocation object.
     */
    init(from clLocation: CLLocation) {
        self.latitude = clLocation.coordinate.latitude
        self.longitude = clLocation.coordinate.longitude
        self.accuracy = clLocation.horizontalAccuracy
        self.timestamp = clLocation.timestamp
        self.altitude = clLocation.altitude
        self.speed = clLocation.speed >= 0 ? clLocation.speed : nil
        self.bearing = clLocation.course >= 0 ? clLocation.course : nil
    }
    
    // MARK: - Methods
    
    /**
     * Converts this Location to a CLLocation object.
     */
    func toCLLocation() -> CLLocation {
        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        let altitude = self.altitude ?? 0.0
        let horizontalAccuracy = accuracy
        let verticalAccuracy = accuracy
        let course = bearing ?? -1.0
        let speed = self.speed ?? -1.0
        
        return CLLocation(
            coordinate: coordinate,
            altitude: altitude,
            horizontalAccuracy: horizontalAccuracy,
            verticalAccuracy: verticalAccuracy,
            course: course,
            speed: speed,
            timestamp: timestamp
        )
    }
}

// MARK: - Equatable

extension Location: Equatable {
    static func == (lhs: Location, rhs: Location) -> Bool {
        return lhs.latitude == rhs.latitude &&
               lhs.longitude == rhs.longitude &&
               lhs.accuracy == rhs.accuracy &&
               lhs.timestamp == rhs.timestamp
    }
}

// MARK: - Codable

extension Location: Codable {
    enum CodingKeys: String, CodingKey {
        case latitude
        case longitude
        case accuracy
        case timestamp
        case altitude
        case speed
        case bearing
    }
}