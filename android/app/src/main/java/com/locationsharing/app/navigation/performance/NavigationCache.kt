package com.locationsharing.app.navigation.performance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches navigation state and destination data to improve performance.
 * Provides both memory and persistent caching capabilities.
 */
@Singleton
class NavigationCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: NavigationPerformanceMonitor
) {
    
    companion object {
        private const val TAG = "NavigationCache"
        private const val PREFS_NAME = "navigation_cache_prefs"
        private const val KEY_CACHE_DATA = "cache_data"
        private const val MAX_CACHE_SIZE = 10
        const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheMutex = Mutex()
    
    // Memory cache for fast access
    private val memoryCache = ConcurrentHashMap<String, CachedDestination>()
    private val stateCache = ConcurrentHashMap<String, NavigationStateCache>()
    
    /**
     * Cache a navigation destination.
     */
    suspend fun cacheDestination(route: String) {
        cacheMutex.withLock {
            val cachedDestination = CachedDestination(
                route = route,
                timestamp = System.currentTimeMillis(),
                accessCount = 1
            )
            
            memoryCache[route] = cachedDestination
            persistCache()
            
            performanceMonitor.recordCacheOperation("destination_cached", route)
            Timber.d(TAG, "Cached destination: $route")
        }
    }
    
    /**
     * Check if a destination is cached and still valid.
     */
    fun isDestinationCached(route: String): Boolean {
        val cached = memoryCache[route]
        return if (cached != null && !cached.isExpired()) {
            // Update access count
            memoryCache[route] = cached.copy(
                accessCount = cached.accessCount + 1,
                lastAccessed = System.currentTimeMillis()
            )
            performanceMonitor.recordCacheHit("destination", route)
            true
        } else {
            if (cached?.isExpired() == true) {
                memoryCache.remove(route)
                performanceMonitor.recordCacheExpiry("destination", route)
            }
            performanceMonitor.recordCacheMiss("destination", route)
            false
        }
    }
    
    /**
     * Cache navigation state for a specific screen.
     */
    suspend fun cacheNavigationState(route: String, state: Map<String, Any>) {
        cacheMutex.withLock {
            val stateCache = NavigationStateCache(
                route = route,
                state = state,
                timestamp = System.currentTimeMillis()
            )
            
            this.stateCache[route] = stateCache
            persistStateCache()
            
            performanceMonitor.recordCacheOperation("state_cached", route)
            Timber.d(TAG, "Cached navigation state for: $route")
        }
    }
    
    /**
     * Retrieve cached navigation state.
     */
    fun getCachedNavigationState(route: String): Map<String, Any>? {
        val cached = stateCache[route]
        return if (cached != null && !cached.isExpired()) {
            performanceMonitor.recordCacheHit("state", route)
            cached.state
        } else {
            if (cached?.isExpired() == true) {
                stateCache.remove(route)
                performanceMonitor.recordCacheExpiry("state", route)
            }
            performanceMonitor.recordCacheMiss("state", route)
            null
        }
    }
    
    /**
     * Preload cache from persistent storage.
     */
    suspend fun preloadCache() {
        cacheMutex.withLock {
            try {
                loadPersistedCache()
                loadPersistedStateCache()
                
                val cacheSize = memoryCache.size + stateCache.size
                performanceMonitor.recordCacheOperation("cache_preloaded", "total_items:$cacheSize")
                
                Timber.d(TAG, "Preloaded cache with ${memoryCache.size} destinations and ${stateCache.size} states")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to preload cache")
                performanceMonitor.recordCacheError("preload_failed", e.message ?: "unknown")
            }
        }
    }
    
    /**
     * Clear expired cache entries.
     */
    suspend fun clearOldCache() {
        cacheMutex.withLock {
            val currentTime = System.currentTimeMillis()
            var removedCount = 0
            
            // Remove expired destinations
            val expiredDestinations = memoryCache.filter { (_, cached) ->
                cached.isExpired()
            }.keys
            
            expiredDestinations.forEach { route ->
                memoryCache.remove(route)
                removedCount++
            }
            
            // Remove expired states
            val expiredStates = stateCache.filter { (_, cached) ->
                cached.isExpired()
            }.keys
            
            expiredStates.forEach { route ->
                stateCache.remove(route)
                removedCount++
            }
            
            // Remove least recently used items if cache is too large
            if (memoryCache.size > MAX_CACHE_SIZE) {
                val lruDestinations = memoryCache.toList()
                    .sortedBy { it.second.lastAccessed }
                    .take(memoryCache.size - MAX_CACHE_SIZE)
                
                lruDestinations.forEach { (route, _) ->
                    memoryCache.remove(route)
                    removedCount++
                }
            }
            
            if (removedCount > 0) {
                persistCache()
                persistStateCache()
                performanceMonitor.recordCacheOperation("cache_cleaned", "removed:$removedCount")
                Timber.d(TAG, "Cleaned cache, removed $removedCount entries")
            }
        }
    }
    
    /**
     * Get current cache size.
     */
    fun getCacheSize(): Int {
        return memoryCache.size + stateCache.size
    }
    
    /**
     * Get cache statistics.
     */
    fun getCacheStats(): NavigationCacheStats {
        val destinationStats = memoryCache.values.groupingBy { 
            if (it.isExpired()) "expired" else "valid" 
        }.eachCount()
        
        val stateStats = stateCache.values.groupingBy { 
            if (it.isExpired()) "expired" else "valid" 
        }.eachCount()
        
        return NavigationCacheStats(
            totalDestinations = memoryCache.size,
            validDestinations = destinationStats["valid"] ?: 0,
            expiredDestinations = destinationStats["expired"] ?: 0,
            totalStates = stateCache.size,
            validStates = stateStats["valid"] ?: 0,
            expiredStates = stateStats["expired"] ?: 0,
            memoryUsageKB = estimateMemoryUsage()
        )
    }
    
    /**
     * Clear all cache data.
     */
    suspend fun clearAllCache() {
        cacheMutex.withLock {
            memoryCache.clear()
            stateCache.clear()
            
            sharedPreferences.edit()
                .remove(KEY_CACHE_DATA)
                .remove("${KEY_CACHE_DATA}_states")
                .apply()
            
            performanceMonitor.recordCacheOperation("cache_cleared", "all")
            Timber.d(TAG, "Cleared all cache data")
        }
    }
    
    /**
     * Persist memory cache to SharedPreferences.
     */
    private fun persistCache() {
        try {
            val cacheData = CacheData(
                destinations = memoryCache.values.toList(),
                timestamp = System.currentTimeMillis()
            )
            
            val cacheJson = json.encodeToString(cacheData)
            sharedPreferences.edit()
                .putString(KEY_CACHE_DATA, cacheJson)
                .apply()
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist cache")
            performanceMonitor.recordCacheError("persist_failed", e.message ?: "unknown")
        }
    }
    
    /**
     * Load persisted cache from SharedPreferences.
     */
    private fun loadPersistedCache() {
        try {
            val cacheJson = sharedPreferences.getString(KEY_CACHE_DATA, null)
            if (cacheJson != null) {
                val cacheData = json.decodeFromString<CacheData>(cacheJson)
                
                cacheData.destinations.forEach { destination ->
                    if (!destination.isExpired()) {
                        memoryCache[destination.route] = destination
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load persisted cache")
            performanceMonitor.recordCacheError("load_failed", e.message ?: "unknown")
        }
    }
    
    /**
     * Persist state cache to SharedPreferences.
     */
    private fun persistStateCache() {
        try {
            val stateCacheData = StateCacheData(
                states = stateCache.values.toList(),
                timestamp = System.currentTimeMillis()
            )
            
            val cacheJson = json.encodeToString(stateCacheData)
            sharedPreferences.edit()
                .putString("${KEY_CACHE_DATA}_states", cacheJson)
                .apply()
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to persist state cache")
            performanceMonitor.recordCacheError("persist_state_failed", e.message ?: "unknown")
        }
    }
    
    /**
     * Load persisted state cache from SharedPreferences.
     */
    private fun loadPersistedStateCache() {
        try {
            val cacheJson = sharedPreferences.getString("${KEY_CACHE_DATA}_states", null)
            if (cacheJson != null) {
                val stateCacheData = json.decodeFromString<StateCacheData>(cacheJson)
                
                stateCacheData.states.forEach { stateCache ->
                    if (!stateCache.isExpired()) {
                        this.stateCache[stateCache.route] = stateCache
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load persisted state cache")
            performanceMonitor.recordCacheError("load_state_failed", e.message ?: "unknown")
        }
    }
    
    /**
     * Estimate memory usage of the cache in KB.
     */
    private fun estimateMemoryUsage(): Long {
        val destinationSize = memoryCache.size * 200L // Rough estimate per destination
        val stateSize = stateCache.values.sumOf { it.state.size * 100L } // Rough estimate per state entry
        return (destinationSize + stateSize) / 1024L
    }
}

/**
 * Represents a cached navigation destination.
 */
@Serializable
data class CachedDestination(
    val route: String,
    val timestamp: Long,
    val accessCount: Int,
    val lastAccessed: Long = timestamp
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > NavigationCache.CACHE_EXPIRY_MS
    }
}

/**
 * Represents cached navigation state for a screen.
 */
@Serializable
data class NavigationStateCache(
    val route: String,
    val state: Map<String, @Serializable(with = AnySerializer::class) Any>,
    val timestamp: Long
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > NavigationCache.CACHE_EXPIRY_MS
    }
}

/**
 * Container for persisted cache data.
 */
@Serializable
private data class CacheData(
    val destinations: List<CachedDestination>,
    val timestamp: Long
)

/**
 * Container for persisted state cache data.
 */
@Serializable
private data class StateCacheData(
    val states: List<NavigationStateCache>,
    val timestamp: Long
)

/**
 * Statistics about cache performance.
 */
data class NavigationCacheStats(
    val totalDestinations: Int,
    val validDestinations: Int,
    val expiredDestinations: Int,
    val totalStates: Int,
    val validStates: Int,
    val expiredStates: Int,
    val memoryUsageKB: Long
)

/**
 * Custom serializer for Any type in state cache.
 */
object AnySerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Any", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        return decoder.decodeString()
    }
}