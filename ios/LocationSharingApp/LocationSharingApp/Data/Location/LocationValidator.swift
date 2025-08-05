import Foundation

/**
 * Validator for location data to ensure data integrity and security.
 */
class LocationValidator {
    
    // Valid latitude range: -90 to 90 degrees
    private static let minLatitude: Double = -90.0
    private static let maxLatitude: Double = 90.0
    
    // Valid longitude range: -180 to 180 degrees
    private static let minLongitude: Double = -180.0
    private static let maxLongitude: Double = 180.0
    
    // Maximum reasonable accuracy in meters (1km)
    private static let maxAccuracy: Float = 1000.0
    
    // Minimum accuracy (0 means perfect accuracy)
    private static let minAccuracy: Float = 0.0
    
    // Maximum age of location data in milliseconds (1 hour)
    private static let maxLocationAgeMs: Int64 = 60 * 60 * 1000
    
    // Minimum timestamp (year 2020)
    private static let minTimestamp: Int64 = 1577836800000 // Jan 1, 2020
    
    // Maximum altitude in meters (Mount Everest + buffer)
    private static let maxAltitude: Double = 10000.0
    
    // Minimum altitude (Dead Sea level)
    private static let minAltitude: Double = -500.0
    
    /**
     * Validates a location object and returns validation result.
     */
    func validateLocation(_ location: Location) -> ValidationResult {
        var errors: [String] = []
        
        // Validate latitude
        if !isValidLatitude(location.latitude) {
            errors.append("Invalid latitude: \(location.latitude). Must be between \(Self.minLatitude) and \(Self.maxLatitude)")
        }
        
        // Validate longitude
        if !isValidLongitude(location.longitude) {
            errors.append("Invalid longitude: \(location.longitude). Must be between \(Self.minLongitude) and \(Self.maxLongitude)")
        }
        
        // Validate accuracy
        if !isValidAccuracy(location.accuracy) {
            errors.append("Invalid accuracy: \(location.accuracy). Must be between \(Self.minAccuracy) and \(Self.maxAccuracy)")
        }
        
        // Validate timestamp
        if !isValidTimestamp(location.timestamp) {
            errors.append("Invalid timestamp: \(location.timestamp). Must be recent and not in the future")
        }
        
        // Validate altitude if present
        if let altitude = location.altitude {
            if !isValidAltitude(altitude) {
                errors.append("Invalid altitude: \(altitude). Must be between \(Self.minAltitude) and \(Self.maxAltitude)")
            }
        }
        
        // Check for coordinate validity (not null island)
        if isNullIsland(latitude: location.latitude, longitude: location.longitude) {
            errors.append("Invalid coordinates: appears to be null island (0,0)")
        }
        
        return ValidationResult(
            isValid: errors.isEmpty,
            errors: errors
        )
    }
    
    /**
     * Validates multiple locations in batch.
     */
    func validateLocations(_ locations: [Location]) -> BatchValidationResult {
        let results = locations.map { location in
            (location, validateLocation(location))
        }
        
        let validLocations = results.compactMap { result in
            result.1.isValid ? result.0 : nil
        }
        
        let invalidLocations = results.compactMap { result in
            !result.1.isValid ? InvalidLocationResult(location: result.0, errors: result.1.errors) : nil
        }
        
        return BatchValidationResult(
            validLocations: validLocations,
            invalidLocations: invalidLocations,
            totalCount: locations.count,
            validCount: validLocations.count,
            invalidCount: invalidLocations.count
        )
    }
    
    /**
     * Validates if coordinates represent a reasonable location.
     */
    func validateCoordinates(latitude: Double, longitude: Double) -> ValidationResult {
        var errors: [String] = []
        
        if !isValidLatitude(latitude) {
            errors.append("Invalid latitude: \(latitude)")
        }
        
        if !isValidLongitude(longitude) {
            errors.append("Invalid longitude: \(longitude)")
        }
        
        if isNullIsland(latitude: latitude, longitude: longitude) {
            errors.append("Coordinates appear to be null island (0,0)")
        }
        
        return ValidationResult(
            isValid: errors.isEmpty,
            errors: errors
        )
    }
    
    // MARK: - Private Validation Methods
    
    private func isValidLatitude(_ latitude: Double) -> Bool {
        return latitude.isFinite && latitude >= Self.minLatitude && latitude <= Self.maxLatitude
    }
    
    private func isValidLongitude(_ longitude: Double) -> Bool {
        return longitude.isFinite && longitude >= Self.minLongitude && longitude <= Self.maxLongitude
    }
    
    private func isValidAccuracy(_ accuracy: Float) -> Bool {
        return accuracy.isFinite && accuracy >= Self.minAccuracy && accuracy <= Self.maxAccuracy
    }
    
    private func isValidTimestamp(_ timestamp: Int64) -> Bool {
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        return timestamp >= Self.minTimestamp &&
               timestamp <= currentTime &&
               (currentTime - timestamp) <= Self.maxLocationAgeMs
    }
    
    private func isValidAltitude(_ altitude: Double) -> Bool {
        return altitude.isFinite && altitude >= Self.minAltitude && altitude <= Self.maxAltitude
    }
    
    private func isNullIsland(latitude: Double, longitude: Double) -> Bool {
        // Check if coordinates are exactly (0,0) or very close to it
        return abs(latitude) < 0.001 && abs(longitude) < 0.001
    }
}

// MARK: - Validation Result Types

/**
 * Result of location validation.
 */
struct ValidationResult {
    let isValid: Bool
    let errors: [String]
}

/**
 * Result of batch location validation.
 */
struct BatchValidationResult {
    let validLocations: [Location]
    let invalidLocations: [InvalidLocationResult]
    let totalCount: Int
    let validCount: Int
    let invalidCount: Int
    
    var validationRate: Double {
        return totalCount > 0 ? Double(validCount) / Double(totalCount) : 0.0
    }
}

/**
 * Represents an invalid location with its errors.
 */
struct InvalidLocationResult {
    let location: Location
    let errors: [String]
}