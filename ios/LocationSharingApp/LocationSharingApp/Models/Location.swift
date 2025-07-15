import Foundation
import CoreLocation

struct Location: Codable {
    let latitude: Double
    let longitude: Double
    let accuracy: Float
    let timestamp: Int64
    let altitude: Double?
    
    init(latitude: Double, longitude: Double, accuracy: Float, timestamp: Int64, altitude: Double? = nil) {
        self.latitude = latitude
        self.longitude = longitude
        self.accuracy = accuracy
        self.timestamp = timestamp
        self.altitude = altitude
    }
    
    init(from clLocation: CLLocation) {
        self.latitude = clLocation.coordinate.latitude
        self.longitude = clLocation.coordinate.longitude
        self.accuracy = Float(clLocation.horizontalAccuracy)
        self.timestamp = Int64(clLocation.timestamp.timeIntervalSince1970 * 1000)
        self.altitude = clLocation.altitude
    }
    
    var clLocation: CLLocation {
        return CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            altitude: altitude ?? 0,
            horizontalAccuracy: Double(accuracy),
            verticalAccuracy: 0,
            timestamp: Date(timeIntervalSince1970: Double(timestamp) / 1000)
        )
    }
}