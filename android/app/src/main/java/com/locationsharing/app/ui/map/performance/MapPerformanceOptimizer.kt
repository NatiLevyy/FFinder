package com.locationsharing.app.ui.map.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.model.LatLng
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.data.friends.Friend
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

/**
 * Performance optimizer for MapScreen with marker clustering, animation optimization,
 * and efficient state management
 * 
 * Implements task requirements:
 * - Implement marker clustering for large friend lists
 * - Optimize animation performance and memory usage
 * - Add proper lifecycle management for location updates
 * - Implement efficient state updates and recomposition
 */
@Singleton
class MapPerformanceOptimizer @Inject constructor() {
    
    companion object {
        private const val TAG = "MapPerformanceOptimizer"
        
        // Clustering thresholds
        private const val CLUSTERING_ZOOM_THRESHOLD = 12f
        private const val MAX_INDIVIDUAL_MARKERS = 25
        private const val CLUSTER_DISTANCE_THRESHOLD_METERS = 150.0
        
        // Performance thresholds
        private const val MAX_FRAME_TIME_MS = 16L // 60 FPS
        private const val MEMORY_WARNING_THRESHOLD_MB = 100L
        private const val ANIMATION_BATCH_SIZE = 10
        
        // Update throttling
        private const val MIN_UPDATE_INTERVAL_MS = 100L
        private const val MIN_MOVEMENT_THRESHOLD_METERS = 5.0
    }
    
    private var lastUpdateTime = 0L
    private var lastCameraPosition: LatLng? = null
    private var lastZoomLevel = 15f
    private var frameTimeHistory = mutableListOf<Long>()
    
    /**
     * Determine if marker clustering should be used based on zoom level and friend count
     */
    fun shouldUseMarkerClustering(zoomLevel: Float, friendCount: Int): Boolean {
        return zoomLevel < CLUSTERING_ZOOM_THRESHOLD || friendCount > MAX_INDIVIDUAL_MARKERS
    }
    
    /**
     * Create optimized clusters for friend markers
     */
    fun createOptimizedClusters(
        friends: List<Friend>,
        zoomLevel: Float,
        cameraCenter: LatLng
    ): List<FriendCluster> {
        val executionTime = measureTimeMillis {
            if (BuildConfig.DEBUG) {
                Timber.d("$TAG: Creating clusters for ${friends.size} friends at zoom $zoomLevel")
            }
        }
        
        val clusters = mutableListOf<FriendCluster>()
        val processedFriends = mutableSetOf<String>()
        val distanceThreshold = getClusterDistanceForZoom(zoomLevel)
        
        // Sort friends by distance from camera center for better clustering
        val sortedFriends = friends
            .mapNotNull { friend ->
                friend.getLatLng()?.let { location ->
                    friend to calculateDistance(cameraCenter, location)
                }
            }
            .sortedBy { it.second }
            .map { it.first }
        
        sortedFriends.forEach { friend ->
            if (friend.id in processedFriends) return@forEach
            
            val friendLocation = friend.getLatLng() ?: return@forEach
            val clusterFriends = mutableListOf(friend)
            processedFriends.add(friend.id)
            
            // Find nearby friends to cluster
            sortedFriends.forEach { otherFriend ->
                if (otherFriend.id in processedFriends) return@forEach
                
                val otherLocation = otherFriend.getLatLng() ?: return@forEach
                val distance = calculateDistance(friendLocation, otherLocation)
                
                if (distance <= distanceThreshold) {
                    clusterFriends.add(otherFriend)
                    processedFriends.add(otherFriend.id)
                }
            }
            
            clusters.add(
                FriendCluster(
                    id = generateClusterId(clusterFriends),
                    friends = clusterFriends,
                    center = calculateClusterCenter(clusterFriends),
                    onlineFriendsCount = clusterFriends.count { it.isOnline() },
                    isCluster = clusterFriends.size > 1
                )
            )
        }
        
        if (BuildConfig.DEBUG) {
            Timber.d("$TAG: Created ${clusters.size} clusters in ${executionTime}ms")
        }
        
        return clusters
    }
    
    /**
     * Check if location update should be throttled for performance
     */
    fun shouldThrottleLocationUpdate(
        newLocation: LatLng,
        lastLocation: LatLng?
    ): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Time-based throttling
        if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL_MS) {
            return true
        }
        
        // Distance-based throttling
        lastLocation?.let { last ->
            val distance = calculateDistance(last, newLocation)
            if (distance < MIN_MOVEMENT_THRESHOLD_METERS) {
                return true
            }
        }
        
        lastUpdateTime = currentTime
        return false
    }
    
    /**
     * Check if camera update should be throttled
     */
    fun shouldThrottleCameraUpdate(
        newPosition: LatLng,
        newZoom: Float
    ): Boolean {
        val positionChanged = lastCameraPosition?.let { last ->
            calculateDistance(last, newPosition) > 50.0 // 50 meters threshold
        } ?: true
        
        val zoomChanged = abs(newZoom - lastZoomLevel) > 0.5f
        
        if (positionChanged || zoomChanged) {
            lastCameraPosition = newPosition
            lastZoomLevel = newZoom
            return false
        }
        
        return true
    }
    
    /**
     * Monitor frame performance and provide optimization suggestions
     */
    fun trackFramePerformance(frameTimeMs: Long) {
        frameTimeHistory.add(frameTimeMs)
        
        // Keep only last 60 frames (1 second at 60 FPS)
        if (frameTimeHistory.size > 60) {
            frameTimeHistory.removeAt(0)
        }
        
        // Check for performance issues
        if (frameTimeMs > MAX_FRAME_TIME_MS) {
            if (BuildConfig.DEBUG) {
                Timber.w("$TAG: Frame time exceeded threshold: ${frameTimeMs}ms")
            }
        }
        
        // Calculate average FPS every 60 frames
        if (frameTimeHistory.size == 60) {
            val averageFrameTime = frameTimeHistory.average()
            val averageFps = 1000.0 / averageFrameTime
            
            if (BuildConfig.DEBUG) {
                Timber.d("$TAG: Average FPS: ${averageFps.toInt()}")
            }
            
            if (averageFps < 45.0) {
                Timber.w("$TAG: Low FPS detected: ${averageFps.toInt()}")
                suggestPerformanceOptimizations()
            }
        }
    }
    
    /**
     * Monitor memory usage and suggest optimizations
     */
    fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
        val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
        val memoryUsage = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt()
        
        if (usedMemory > MEMORY_WARNING_THRESHOLD_MB) {
            if (BuildConfig.DEBUG) {
                Timber.w("$TAG: High memory usage: ${usedMemory}MB (${memoryUsage}%)")
            }
            
            // Suggest garbage collection
            if (memoryUsage > 80) {
                System.gc()
                Timber.d("$TAG: Triggered garbage collection")
            }
        }
    }
    
    /**
     * Optimize animation batching for better performance
     */
    fun batchAnimations(animations: List<() -> Unit>) {
        animations.chunked(ANIMATION_BATCH_SIZE).forEach { batch ->
            batch.forEach { animation ->
                animation()
            }
            // Small delay between batches to prevent frame drops
            Thread.sleep(1)
        }
    }
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLng = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(point1.latitude)) * cos(Math.toRadians(point2.latitude)) *
                sin(dLng / 2) * sin(dLng / 2)
        
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * Get cluster distance threshold based on zoom level
     */
    private fun getClusterDistanceForZoom(zoomLevel: Float): Double {
        return when {
            zoomLevel < 8f -> 10000.0  // 10km
            zoomLevel < 10f -> 5000.0  // 5km
            zoomLevel < 12f -> 2000.0  // 2km
            zoomLevel < 14f -> 1000.0  // 1km
            zoomLevel < 16f -> 500.0   // 500m
            else -> CLUSTER_DISTANCE_THRESHOLD_METERS // 150m
        }
    }
    
    /**
     * Calculate cluster center point
     */
    private fun calculateClusterCenter(friends: List<Friend>): LatLng {
        val locations = friends.mapNotNull { it.getLatLng() }
        if (locations.isEmpty()) return LatLng(0.0, 0.0)
        
        val avgLat = locations.map { it.latitude }.average()
        val avgLng = locations.map { it.longitude }.average()
        
        return LatLng(avgLat, avgLng)
    }
    
    /**
     * Generate unique cluster ID
     */
    private fun generateClusterId(friends: List<Friend>): String {
        return if (friends.size == 1) {
            friends.first().id
        } else {
            "cluster_${friends.map { it.id }.sorted().joinToString("_").hashCode()}"
        }
    }
    
    /**
     * Suggest performance optimizations based on current metrics
     */
    private fun suggestPerformanceOptimizations() {
        if (BuildConfig.DEBUG) {
            Timber.i("$TAG: Performance optimization suggestions:")
            Timber.i("$TAG: - Consider reducing marker animation complexity")
            Timber.i("$TAG: - Enable marker clustering for better performance")
            Timber.i("$TAG: - Reduce location update frequency")
            Timber.i("$TAG: - Check for memory leaks in location listeners")
        }
    }
}

/**
 * Friend cluster data class for optimized rendering
 */
data class FriendCluster(
    val id: String,
    val friends: List<Friend>,
    val center: LatLng,
    val onlineFriendsCount: Int,
    val isCluster: Boolean
)

/**
 * Composable for lifecycle-aware performance monitoring
 */
@Composable
fun MapPerformanceMonitor(
    friendsCount: Int,
    onlineFriendsCount: Int,
    zoomLevel: Float,
    performanceOptimizer: MapPerformanceOptimizer
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isActive by remember { mutableStateOf(true) }
    
    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isActive = true
                    if (BuildConfig.DEBUG) {
                        Timber.d("MapPerformanceMonitor: Resumed monitoring")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isActive = false
                    if (BuildConfig.DEBUG) {
                        Timber.d("MapPerformanceMonitor: Paused monitoring")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    isActive = false
                    if (BuildConfig.DEBUG) {
                        Timber.d("MapPerformanceMonitor: Destroyed monitoring")
                    }
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Performance monitoring loop
    LaunchedEffect(isActive) {
        while (isActive) {
            val frameStartTime = System.currentTimeMillis()
            
            // Monitor memory usage every 5 seconds
            if (frameStartTime % 5000 < 100) {
                performanceOptimizer.checkMemoryUsage()
            }
            
            // Log performance metrics
            if (BuildConfig.DEBUG && frameStartTime % 10000 < 100) {
                Timber.d("MapPerformanceMonitor: Friends: $friendsCount ($onlineFriendsCount online), Zoom: $zoomLevel")
            }
            
            delay(100) // Check every 100ms
            
            val frameTime = System.currentTimeMillis() - frameStartTime
            performanceOptimizer.trackFramePerformance(frameTime)
        }
    }
}