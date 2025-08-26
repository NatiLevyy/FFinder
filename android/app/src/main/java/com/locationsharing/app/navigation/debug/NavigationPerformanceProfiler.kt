package com.locationsharing.app.navigation.debug

import androidx.compose.runtime.Stable
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Performance profiler for navigation operations that tracks timing,
 * memory usage, and performance bottlenecks.
 */
class NavigationPerformanceProfiler {
    
    private val _performanceData = MutableStateFlow(NavigationPerformanceData())
    val performanceData: StateFlow<NavigationPerformanceData> = _performanceData.asStateFlow()
    
    private val activeNavigations = mutableMapOf<String, NavigationProfileEntry>()
    private val completedNavigations = mutableListOf<NavigationProfileEntry>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Starts profiling a navigation operation.
     */
    fun startNavigationProfiling(
        navigationId: String,
        fromScreen: Screen?,
        toScreen: Screen,
        navigationType: NavigationType = NavigationType.STANDARD
    ) {
        val entry = NavigationProfileEntry(
            id = navigationId,
            fromScreen = fromScreen,
            toScreen = toScreen,
            navigationType = navigationType,
            startTime = System.currentTimeMillis(),
            startMemory = getCurrentMemoryUsage()
        )
        
        activeNavigations[navigationId] = entry
        updatePerformanceData()
    }
    
    /**
     * Ends profiling for a navigation operation.
     */
    fun endNavigationProfiling(
        navigationId: String,
        success: Boolean = true,
        error: String? = null
    ) {
        val entry = activeNavigations.remove(navigationId) ?: return
        
        val completedEntry = entry.copy(
            endTime = System.currentTimeMillis(),
            endMemory = getCurrentMemoryUsage(),
            success = success,
            error = error
        )
        
        completedNavigations.add(completedEntry)
        
        // Keep only recent entries to prevent memory issues
        if (completedNavigations.size > MAX_COMPLETED_NAVIGATIONS) {
            completedNavigations.removeAt(0)
        }
        
        updatePerformanceData()
    }
    
    /**
     * Records a memory snapshot for performance analysis.
     */
    fun recordMemorySnapshot(label: String = "Manual") {
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            label = label,
            totalMemory = Runtime.getRuntime().totalMemory(),
            freeMemory = Runtime.getRuntime().freeMemory(),
            maxMemory = Runtime.getRuntime().maxMemory()
        )
        
        memorySnapshots.add(snapshot)
        
        // Keep only recent snapshots
        if (memorySnapshots.size > MAX_MEMORY_SNAPSHOTS) {
            memorySnapshots.removeAt(0)
        }
        
        updatePerformanceData()
    }
    
    /**
     * Analyzes navigation performance and identifies bottlenecks.
     */
    fun analyzePerformance(): NavigationPerformanceAnalysis {
        val allNavigations = completedNavigations.filter { it.success }
        
        if (allNavigations.isEmpty()) {
            return NavigationPerformanceAnalysis()
        }
        
        val durations = allNavigations.map { it.duration }
        val memoryUsages = allNavigations.map { it.memoryUsed }
        
        val averageDuration = durations.average()
        val minDuration = durations.minOrNull() ?: 0L
        val maxDuration = durations.maxOrNull() ?: 0L
        val medianDuration = durations.sorted().let { sorted ->
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
        }
        
        val slowNavigations = allNavigations.filter { it.duration > SLOW_NAVIGATION_THRESHOLD }
        val fastNavigations = allNavigations.filter { it.duration < FAST_NAVIGATION_THRESHOLD }
        
        // Identify performance bottlenecks
        val bottlenecks = mutableListOf<PerformanceBottleneck>()
        
        // Check for consistently slow routes
        val routePerformance = allNavigations.groupBy { "${it.fromScreen?.route}->${it.toScreen.route}" }
        routePerformance.forEach { (route, navigations) ->
            val avgDuration = navigations.map { it.duration }.average()
            if (avgDuration > SLOW_NAVIGATION_THRESHOLD && navigations.size >= 3) {
                bottlenecks.add(
                    PerformanceBottleneck(
                        type = BottleneckType.SLOW_ROUTE,
                        description = "Route $route consistently slow (avg: ${avgDuration.toLong()}ms)",
                        severity = if (avgDuration > VERY_SLOW_NAVIGATION_THRESHOLD) 
                            BottleneckSeverity.HIGH else BottleneckSeverity.MEDIUM,
                        affectedNavigations = navigations.size
                    )
                )
            }
        }
        
        // Check for memory issues
        val highMemoryNavigations = allNavigations.filter { it.memoryUsed > HIGH_MEMORY_USAGE_THRESHOLD }
        if (highMemoryNavigations.isNotEmpty()) {
            bottlenecks.add(
                PerformanceBottleneck(
                    type = BottleneckType.HIGH_MEMORY_USAGE,
                    description = "High memory usage detected in ${highMemoryNavigations.size} navigations",
                    severity = BottleneckSeverity.MEDIUM,
                    affectedNavigations = highMemoryNavigations.size
                )
            )
        }
        
        // Check for timeout patterns
        val timeoutNavigations = completedNavigations.filter { !it.success && it.error?.contains("timeout", true) == true }
        if (timeoutNavigations.isNotEmpty()) {
            bottlenecks.add(
                PerformanceBottleneck(
                    type = BottleneckType.TIMEOUTS,
                    description = "Navigation timeouts detected (${timeoutNavigations.size} occurrences)",
                    severity = BottleneckSeverity.HIGH,
                    affectedNavigations = timeoutNavigations.size
                )
            )
        }
        
        return NavigationPerformanceAnalysis(
            totalNavigations = allNavigations.size,
            averageDuration = averageDuration.toLong(),
            minDuration = minDuration,
            maxDuration = maxDuration,
            medianDuration = medianDuration.toLong(),
            slowNavigations = slowNavigations.size,
            fastNavigations = fastNavigations.size,
            averageMemoryUsage = memoryUsages.average().toLong(),
            bottlenecks = bottlenecks,
            performanceGrade = calculatePerformanceGrade(averageDuration, bottlenecks.size)
        )
    }
    
    /**
     * Gets performance recommendations based on analysis.
     */
    fun getPerformanceRecommendations(): List<PerformanceRecommendation> {
        val analysis = analyzePerformance()
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        if (analysis.averageDuration > SLOW_NAVIGATION_THRESHOLD) {
            recommendations.add(
                PerformanceRecommendation(
                    priority = RecommendationPriority.HIGH,
                    title = "Optimize Navigation Speed",
                    description = "Average navigation time is ${analysis.averageDuration}ms. Consider optimizing screen initialization and reducing heavy operations during navigation.",
                    actionItems = listOf(
                        "Profile screen composition performance",
                        "Implement lazy loading for heavy components",
                        "Optimize ViewModel initialization",
                        "Consider using navigation caching"
                    )
                )
            )
        }
        
        if (analysis.averageMemoryUsage > HIGH_MEMORY_USAGE_THRESHOLD) {
            recommendations.add(
                PerformanceRecommendation(
                    priority = RecommendationPriority.MEDIUM,
                    title = "Reduce Memory Usage",
                    description = "High memory usage detected during navigation (avg: ${analysis.averageMemoryUsage}MB).",
                    actionItems = listOf(
                        "Review image loading and caching strategies",
                        "Implement proper cleanup in ViewModels",
                        "Consider using memory-efficient data structures",
                        "Profile memory allocations during navigation"
                    )
                )
            )
        }
        
        analysis.bottlenecks.forEach { bottleneck ->
            when (bottleneck.type) {
                BottleneckType.SLOW_ROUTE -> {
                    recommendations.add(
                        PerformanceRecommendation(
                            priority = if (bottleneck.severity == BottleneckSeverity.HIGH) 
                                RecommendationPriority.HIGH else RecommendationPriority.MEDIUM,
                            title = "Optimize Slow Route",
                            description = bottleneck.description,
                            actionItems = listOf(
                                "Profile the specific route for performance issues",
                                "Check for heavy operations in screen initialization",
                                "Consider preloading data for this route"
                            )
                        )
                    )
                }
                BottleneckType.TIMEOUTS -> {
                    recommendations.add(
                        PerformanceRecommendation(
                            priority = RecommendationPriority.HIGH,
                            title = "Fix Navigation Timeouts",
                            description = bottleneck.description,
                            actionItems = listOf(
                                "Investigate timeout causes",
                                "Implement proper error handling",
                                "Consider increasing timeout thresholds if appropriate",
                                "Add retry mechanisms for failed navigations"
                            )
                        )
                    )
                }
                BottleneckType.HIGH_MEMORY_USAGE -> {
                    recommendations.add(
                        PerformanceRecommendation(
                            priority = RecommendationPriority.MEDIUM,
                            title = "Address Memory Issues",
                            description = bottleneck.description,
                            actionItems = listOf(
                                "Profile memory usage during navigation",
                                "Implement proper resource cleanup",
                                "Consider using memory pools for frequent allocations"
                            )
                        )
                    )
                }
            }
        }
        
        return recommendations
    }
    
    /**
     * Clears all performance data.
     */
    fun clearPerformanceData() {
        activeNavigations.clear()
        completedNavigations.clear()
        memorySnapshots.clear()
        updatePerformanceData()
    }
    
    /**
     * Exports performance data for external analysis.
     */
    fun exportPerformanceData(): String {
        val analysis = analyzePerformance()
        
        return buildString {
            appendLine("=== Navigation Performance Report ===")
            appendLine("Generated: ${dateFormatter.format(Date())}")
            appendLine()
            
            appendLine("=== Summary ===")
            appendLine("Total Navigations: ${analysis.totalNavigations}")
            appendLine("Average Duration: ${analysis.averageDuration}ms")
            appendLine("Min Duration: ${analysis.minDuration}ms")
            appendLine("Max Duration: ${analysis.maxDuration}ms")
            appendLine("Median Duration: ${analysis.medianDuration}ms")
            appendLine("Performance Grade: ${analysis.performanceGrade}")
            appendLine()
            
            appendLine("=== Navigation Details ===")
            completedNavigations.takeLast(20).forEach { navigation ->
                appendLine("${navigation.fromScreen?.route ?: "Start"} -> ${navigation.toScreen.route}")
                appendLine("  Duration: ${navigation.duration}ms")
                appendLine("  Memory Used: ${navigation.memoryUsed}MB")
                appendLine("  Type: ${navigation.navigationType}")
                appendLine("  Success: ${navigation.success}")
                if (!navigation.success && navigation.error != null) {
                    appendLine("  Error: ${navigation.error}")
                }
                appendLine()
            }
            
            if (analysis.bottlenecks.isNotEmpty()) {
                appendLine("=== Performance Bottlenecks ===")
                analysis.bottlenecks.forEach { bottleneck ->
                    appendLine("${bottleneck.severity} - ${bottleneck.type}")
                    appendLine("  ${bottleneck.description}")
                    appendLine("  Affected Navigations: ${bottleneck.affectedNavigations}")
                    appendLine()
                }
            }
            
            val recommendations = getPerformanceRecommendations()
            if (recommendations.isNotEmpty()) {
                appendLine("=== Recommendations ===")
                recommendations.forEach { recommendation ->
                    appendLine("${recommendation.priority} - ${recommendation.title}")
                    appendLine("  ${recommendation.description}")
                    recommendation.actionItems.forEach { action ->
                        appendLine("  • $action")
                    }
                    appendLine()
                }
            }
        }
    }
    
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
    
    private fun updatePerformanceData() {
        val currentData = _performanceData.value
        val newData = currentData.copy(
            activeNavigations = activeNavigations.size,
            completedNavigations = completedNavigations.size,
            lastUpdated = System.currentTimeMillis(),
            recentNavigations = completedNavigations.takeLast(10)
        )
        _performanceData.value = newData
    }
    
    private fun calculatePerformanceGrade(averageDuration: Double, bottleneckCount: Int): PerformanceGrade {
        return when {
            averageDuration < FAST_NAVIGATION_THRESHOLD && bottleneckCount == 0 -> PerformanceGrade.EXCELLENT
            averageDuration < SLOW_NAVIGATION_THRESHOLD && bottleneckCount <= 1 -> PerformanceGrade.GOOD
            averageDuration < VERY_SLOW_NAVIGATION_THRESHOLD && bottleneckCount <= 2 -> PerformanceGrade.FAIR
            else -> PerformanceGrade.POOR
        }
    }
    
    companion object {
        private const val MAX_COMPLETED_NAVIGATIONS = 100
        private const val MAX_MEMORY_SNAPSHOTS = 50
        private const val FAST_NAVIGATION_THRESHOLD = 100L // 100ms
        private const val SLOW_NAVIGATION_THRESHOLD = 1000L // 1 second
        private const val VERY_SLOW_NAVIGATION_THRESHOLD = 3000L // 3 seconds
        private const val HIGH_MEMORY_USAGE_THRESHOLD = 50L // 50MB
    }
}

// Data classes for performance profiling

@Stable
data class NavigationProfileEntry(
    val id: String,
    val fromScreen: Screen?,
    val toScreen: Screen,
    val navigationType: NavigationType,
    val startTime: Long,
    val endTime: Long? = null,
    val startMemory: Long,
    val endMemory: Long? = null,
    val success: Boolean = true,
    val error: String? = null
) {
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime
    
    val memoryUsed: Long
        get() = max(0, (endMemory ?: getCurrentMemoryUsage()) - startMemory)
    
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
}

@Stable
data class MemorySnapshot(
    val timestamp: Long,
    val label: String,
    val totalMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long
) {
    val usedMemory: Long = totalMemory - freeMemory
    val usedMemoryMB: Long = usedMemory / (1024 * 1024)
}

@Stable
data class NavigationPerformanceData(
    val activeNavigations: Int = 0,
    val completedNavigations: Int = 0,
    val lastUpdated: Long = 0L,
    val recentNavigations: List<NavigationProfileEntry> = emptyList()
)

@Stable
data class NavigationPerformanceAnalysis(
    val totalNavigations: Int = 0,
    val averageDuration: Long = 0L,
    val minDuration: Long = 0L,
    val maxDuration: Long = 0L,
    val medianDuration: Long = 0L,
    val slowNavigations: Int = 0,
    val fastNavigations: Int = 0,
    val averageMemoryUsage: Long = 0L,
    val bottlenecks: List<PerformanceBottleneck> = emptyList(),
    val performanceGrade: PerformanceGrade = PerformanceGrade.GOOD
)

@Stable
data class PerformanceBottleneck(
    val type: BottleneckType,
    val description: String,
    val severity: BottleneckSeverity,
    val affectedNavigations: Int
)

@Stable
data class PerformanceRecommendation(
    val priority: RecommendationPriority,
    val title: String,
    val description: String,
    val actionItems: List<String>
)

enum class NavigationType {
    STANDARD,
    BACK_NAVIGATION,
    DEEP_LINK,
    REPLACE,
    POP_UP_TO
}

enum class PerformanceGrade {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

enum class BottleneckType {
    SLOW_ROUTE,
    HIGH_MEMORY_USAGE,
    TIMEOUTS
}

enum class BottleneckSeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH
}