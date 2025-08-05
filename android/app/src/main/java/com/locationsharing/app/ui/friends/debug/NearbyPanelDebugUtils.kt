package com.locationsharing.app.ui.friends.debug

import android.location.Location
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.domain.model.NearbyFriend
import com.locationsharing.app.ui.friends.logging.NearbyPanelLogger
import kotlin.random.Random

/**
 * Debug utilities for the Friends Nearby Panel feature.
 * All utilities are guarded with BuildConfig.DEBUG to prevent inclusion in release builds.
 */
object NearbyPanelDebugUtils {
    
    /**
     * Creates mock nearby friends for testing purposes.
     * Only available in debug builds.
     */
    fun createMockNearbyFriends(count: Int = 5): List<NearbyFriend> {
        return if (BuildConfig.DEBUG) {
            val mockNames = listOf(
                "Alice Johnson", "Bob Smith", "Charlie Brown", "Diana Prince", "Eve Wilson",
                "Frank Miller", "Grace Lee", "Henry Davis", "Ivy Chen", "Jack Thompson"
            )
            
            (1..count).map { index ->
                val distance = Random.nextDouble(50.0, 5000.0) // 50m to 5km
                val isOnline = Random.nextBoolean()
                val lastUpdated = System.currentTimeMillis() - Random.nextLong(0, 3600000) // Up to 1 hour ago
                
                NearbyFriend(
                    id = "debug_friend_$index",
                    displayName = mockNames.getOrElse(index - 1) { "Debug Friend $index" },
                    avatarUrl = null,
                    distance = distance,
                    isOnline = isOnline,
                    lastUpdated = lastUpdated,
                    latLng = com.google.android.gms.maps.model.LatLng(
                        37.7749 + Random.nextDouble(-0.01, 0.01), // San Francisco area
                        -122.4194 + Random.nextDouble(-0.01, 0.01)
                    ),
                    location = createMockLocation(37.7749, -122.4194, distance)
                )
            }.sortedBy { it.distance }
        } else {
            emptyList()
        }
    }
    
    /**
     * Creates a mock location at specified distance from a base location.
     */
    private fun createMockLocation(baseLat: Double, baseLng: Double, distanceMeters: Double): Location {
        val location = Location("debug")
        
        // Simple approximation: 1 degree ≈ 111km
        val latOffset = (distanceMeters / 111000.0) * Random.nextDouble(-1.0, 1.0)
        val lngOffset = (distanceMeters / 111000.0) * Random.nextDouble(-1.0, 1.0)
        
        location.latitude = baseLat + latOffset
        location.longitude = baseLng + lngOffset
        location.accuracy = Random.nextFloat() * 10 + 5 // 5-15m accuracy
        location.time = System.currentTimeMillis()
        
        return location
    }
    
    /**
     * Validates friend list consistency for debugging.
     */
    fun validateFriendListConsistency(friends: List<NearbyFriend>): List<String> {
        if (!BuildConfig.DEBUG) return emptyList()
        
        val issues = mutableListOf<String>()
        
        // Check for duplicate IDs
        val duplicateIds = friends.groupBy { it.id }.filter { it.value.size > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            issues.add("Duplicate friend IDs found: $duplicateIds")
        }
        
        // Check distance sorting
        val distances = friends.map { it.distance }
        val sortedDistances = distances.sorted()
        if (distances != sortedDistances) {
            issues.add("Friends list is not sorted by distance")
        }
        
        // Check for invalid distances
        val invalidDistances = friends.filter { it.distance < 0 || it.distance > 20000000 } // > 20,000km
        if (invalidDistances.isNotEmpty()) {
            issues.add("Invalid distances found: ${invalidDistances.map { "${it.displayName}: ${it.distance}m" }}")
        }
        
        // Check for very old timestamps
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val oldUpdates = friends.filter { it.lastUpdated < oneWeekAgo }
        if (oldUpdates.isNotEmpty()) {
            issues.add("Friends with very old updates (>1 week): ${oldUpdates.map { it.displayName }}")
        }
        
        if (issues.isNotEmpty()) {
            NearbyPanelLogger.logWarning(
                "validateFriendListConsistency",
                "Consistency issues found",
                mapOf("issues" to issues.size, "totalFriends" to friends.size)
            )
            issues.forEach { issue ->
                NearbyPanelLogger.logWarning("validateFriendListConsistency", issue)
            }
        }
        
        return issues
    }
    
    /**
     * Simulates distance calculation performance under load.
     */
    fun simulateDistanceCalculationLoad(friendCount: Int, iterations: Int = 100) {
        if (!BuildConfig.DEBUG) return
        
        NearbyPanelLogger.logMemoryUsage("simulateDistanceCalculationLoad_start")
        
        val mockFriends = createMockNearbyFriends(friendCount)
        val userLocation = Location("debug").apply {
            latitude = 37.7749
            longitude = -122.4194
        }
        
        repeat(iterations) { iteration ->
            NearbyPanelLogger.measureDistanceCalculation(
                "simulateLoad_iteration_$iteration",
                friendCount
            ) {
                mockFriends.forEach { friend ->
                    friend.location?.let { friendLocation ->
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            userLocation.latitude,
                            userLocation.longitude,
                            friendLocation.latitude,
                            friendLocation.longitude,
                            results
                        )
                    }
                }
            }
        }
        
        NearbyPanelLogger.logMemoryUsage("simulateDistanceCalculationLoad_end")
    }
    
    /**
     * Logs detailed system information for debugging.
     */
    fun logSystemInfo() {
        if (!BuildConfig.DEBUG) return
        
        val runtime = Runtime.getRuntime()
        val systemInfo = mapOf(
            "availableProcessors" to runtime.availableProcessors(),
            "maxMemoryMB" to runtime.maxMemory() / 1024 / 1024,
            "totalMemoryMB" to runtime.totalMemory() / 1024 / 1024,
            "freeMemoryMB" to runtime.freeMemory() / 1024 / 1024,
            "androidVersion" to android.os.Build.VERSION.SDK_INT,
            "buildType" to BuildConfig.BUILD_TYPE
        )
        
        NearbyPanelLogger.logWarning(
            "systemInfo",
            "System information for debugging",
            systemInfo
        )
    }
    
    /**
     * Creates a debug report of the current nearby panel state.
     */
    fun generateDebugReport(
        friends: List<NearbyFriend>,
        searchQuery: String,
        isPanelOpen: Boolean,
        userLocation: Location?
    ): String {
        if (!BuildConfig.DEBUG) return "Debug reports only available in debug builds"
        
        val report = buildString {
            appendLine("=== Nearby Panel Debug Report ===")
            appendLine("Timestamp: ${System.currentTimeMillis()}")
            appendLine("Panel Open: $isPanelOpen")
            appendLine("Search Query: '$searchQuery'")
            appendLine("User Location: ${userLocation?.let { "${it.latitude}, ${it.longitude}" } ?: "null"}")
            appendLine("Friends Count: ${friends.size}")
            appendLine()
            
            if (friends.isNotEmpty()) {
                appendLine("Friends List:")
                friends.forEachIndexed { index, friend ->
                    appendLine("  [$index] ${friend.displayName}")
                    appendLine("    Distance: ${friend.formattedDistance} (${friend.distance}m)")
                    appendLine("    Status: ${if (friend.isOnline) "Online" else "Offline"}")
                    appendLine("    Last Updated: ${friend.lastUpdated}")
                    appendLine("    Location: ${friend.location?.let { "${it.latitude}, ${it.longitude}" } ?: "null"}")
                }
            } else {
                appendLine("No friends in list")
            }
            
            appendLine()
            val issues = validateFriendListConsistency(friends)
            if (issues.isNotEmpty()) {
                appendLine("Consistency Issues:")
                issues.forEach { issue ->
                    appendLine("  - $issue")
                }
            } else {
                appendLine("No consistency issues found")
            }
            
            appendLine("=== End Debug Report ===")
        }
        
        NearbyPanelLogger.logWarning("generateDebugReport", "Debug report generated", mapOf("reportLength" to report.length))
        
        return report
    }
    
    /**
     * Measures memory allocation during friend list operations.
     */
    fun measureMemoryAllocation(operation: String, block: () -> Unit) {
        if (!BuildConfig.DEBUG) {
            block()
            return
        }
        
        val runtime = Runtime.getRuntime()
        
        // Force garbage collection to get accurate baseline
        System.gc()
        Thread.sleep(100)
        
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        block()
        
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryDiff = memoryAfter - memoryBefore
        
        NearbyPanelLogger.logWarning(
            "measureMemoryAllocation",
            "$operation memory allocation",
            mapOf(
                "beforeMB" to memoryBefore / 1024 / 1024,
                "afterMB" to memoryAfter / 1024 / 1024,
                "allocatedMB" to memoryDiff / 1024 / 1024
            )
        )
    }
}