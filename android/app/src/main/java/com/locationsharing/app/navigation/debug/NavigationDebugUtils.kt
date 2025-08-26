package com.locationsharing.app.navigation.debug

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.navigation.NavigationError
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class providing debugging tools and helpers for navigation issues.
 * Only available in debug builds.
 */
class NavigationDebugUtils {
    
    private val _debugState = MutableStateFlow(NavigationDebugState())
    val debugState: StateFlow<NavigationDebugState> = _debugState.asStateFlow()
    
    private val debugLogs = mutableListOf<NavigationDebugLog>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * Logs a navigation debug message.
     */
    fun logDebug(
        level: DebugLogLevel,
        message: String,
        tag: String = "NavigationDebug",
        throwable: Throwable? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        if (!BuildConfig.DEBUG) return
        
        val logEntry = NavigationDebugLog(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            metadata = metadata
        )
        
        debugLogs.add(logEntry)
        
        // Keep only recent logs to prevent memory issues
        if (debugLogs.size > MAX_DEBUG_LOGS) {
            debugLogs.removeAt(0)
        }
        
        // Also log to Android Log for immediate visibility
        when (level) {
            DebugLogLevel.VERBOSE -> android.util.Log.v(tag, message, throwable)
            DebugLogLevel.DEBUG -> android.util.Log.d(tag, message, throwable)
            DebugLogLevel.INFO -> android.util.Log.i(tag, message, throwable)
            DebugLogLevel.WARNING -> android.util.Log.w(tag, message, throwable)
            DebugLogLevel.ERROR -> android.util.Log.e(tag, message, throwable)
        }
        
        updateDebugState()
    }
    
    /**
     * Validates the current navigation state and reports issues.
     */
    fun validateNavigationState(
        navController: NavController?,
        navigationState: NavigationState
    ): NavigationValidationResult {
        val issues = mutableListOf<NavigationIssue>()
        val warnings = mutableListOf<NavigationWarning>()
        
        // Check navigation controller validity
        if (navController == null) {
            issues.add(
                NavigationIssue(
                    severity = IssueSeverity.CRITICAL,
                    type = IssueType.CONTROLLER_NULL,
                    description = "Navigation controller is null",
                    recommendation = "Ensure navigation controller is properly initialized"
                )
            )
        } else {
            // Check back stack consistency
            val backStackSize = navController.currentBackStack.value.size
            val historySize = navigationState.navigationHistory.size
            
            if (backStackSize != historySize + 1) { // +1 for current destination
                warnings.add(
                    NavigationWarning(
                        type = WarningType.BACK_STACK_INCONSISTENCY,
                        description = "Back stack size ($backStackSize) doesn't match navigation history size ($historySize)",
                        recommendation = "Check navigation state tracking implementation"
                    )
                )
            }
            
            // Check current destination consistency
            val currentDestination = navController.currentDestination
            if (currentDestination?.route != navigationState.currentScreen.route) {
                issues.add(
                    NavigationIssue(
                        severity = IssueSeverity.HIGH,
                        type = IssueType.STATE_INCONSISTENCY,
                        description = "Current destination (${currentDestination?.route}) doesn't match navigation state (${navigationState.currentScreen.route})",
                        recommendation = "Synchronize navigation controller with navigation state"
                    )
                )
            }
        }
        
        // Check for navigation loops
        val recentHistory = navigationState.navigationHistory.takeLast(10)
        val screenCounts = recentHistory.groupBy { it }.mapValues { it.value.size }
        screenCounts.forEach { (screen, count) ->
            if (count > 3) {
                warnings.add(
                    NavigationWarning(
                        type = WarningType.POTENTIAL_LOOP,
                        description = "Screen ${screen.title} appears $count times in recent history",
                        recommendation = "Check for navigation loops or excessive back-and-forth navigation"
                    )
                )
            }
        }
        
        // Check navigation state flags
        if (navigationState.isNavigating && navigationState.navigationHistory.isEmpty()) {
            warnings.add(
                NavigationWarning(
                    type = WarningType.INCONSISTENT_FLAGS,
                    description = "Navigation is marked as in progress but no history exists",
                    recommendation = "Verify navigation state flag management"
                )
            )
        }
        
        return NavigationValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings,
            validationTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Diagnoses navigation performance issues.
     */
    fun diagnosePerformanceIssues(
        performanceData: NavigationPerformanceData,
        recentNavigations: List<NavigationProfileEntry>
    ): NavigationPerformanceDiagnosis {
        val issues = mutableListOf<PerformanceIssue>()
        val recommendations = mutableListOf<String>()
        
        // Analyze navigation times
        val slowNavigations = recentNavigations.filter { it.duration > SLOW_NAVIGATION_THRESHOLD }
        if (slowNavigations.isNotEmpty()) {
            issues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.SLOW_NAVIGATION,
                    severity = if (slowNavigations.size > recentNavigations.size / 2) 
                        PerformanceIssueSeverity.HIGH else PerformanceIssueSeverity.MEDIUM,
                    description = "${slowNavigations.size} out of ${recentNavigations.size} recent navigations are slow (>${SLOW_NAVIGATION_THRESHOLD}ms)",
                    affectedRoutes = slowNavigations.map { "${it.fromScreen?.route}->${it.toScreen.route}" }.distinct()
                )
            )
            recommendations.add("Profile slow navigation routes for performance bottlenecks")
        }
        
        // Analyze memory usage
        val highMemoryNavigations = recentNavigations.filter { it.memoryUsed > HIGH_MEMORY_THRESHOLD }
        if (highMemoryNavigations.isNotEmpty()) {
            issues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.HIGH_MEMORY_USAGE,
                    severity = PerformanceIssueSeverity.MEDIUM,
                    description = "${highMemoryNavigations.size} navigations used excessive memory (>${HIGH_MEMORY_THRESHOLD}MB)",
                    affectedRoutes = highMemoryNavigations.map { "${it.fromScreen?.route}->${it.toScreen.route}" }.distinct()
                )
            )
            recommendations.add("Investigate memory leaks and optimize resource usage")
        }
        
        // Check for failed navigations
        val failedNavigations = recentNavigations.filter { !it.success }
        if (failedNavigations.isNotEmpty()) {
            issues.add(
                PerformanceIssue(
                    type = PerformanceIssueType.NAVIGATION_FAILURES,
                    severity = PerformanceIssueSeverity.HIGH,
                    description = "${failedNavigations.size} navigation failures detected",
                    affectedRoutes = failedNavigations.map { "${it.fromScreen?.route}->${it.toScreen.route}" }.distinct()
                )
            )
            recommendations.add("Investigate and fix navigation failure causes")
        }
        
        return NavigationPerformanceDiagnosis(
            overallHealth = calculateOverallHealth(issues),
            issues = issues,
            recommendations = recommendations,
            diagnosisTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generates a comprehensive debug report.
     */
    fun generateDebugReport(
        navigationState: NavigationState,
        performanceData: NavigationPerformanceData,
        validationResult: NavigationValidationResult,
        performanceDiagnosis: NavigationPerformanceDiagnosis
    ): String {
        return buildString {
            appendLine("=== Navigation Debug Report ===")
            appendLine("Generated: ${dateFormatter.format(Date())}")
            appendLine("Build: ${BuildConfig.BUILD_TYPE}")
            appendLine()
            
            appendLine("=== Navigation State ===")
            appendLine("Current Screen: ${navigationState.currentScreen.title} (${navigationState.currentScreen.route})")
            appendLine("Can Navigate Back: ${navigationState.canNavigateBack}")
            appendLine("Is Navigating: ${navigationState.isNavigating}")
            appendLine("History Size: ${navigationState.navigationHistory.size}")
            appendLine()
            
            appendLine("=== Validation Results ===")
            appendLine("Valid: ${validationResult.isValid}")
            if (validationResult.issues.isNotEmpty()) {
                appendLine("Issues:")
                validationResult.issues.forEach { issue ->
                    appendLine("  ${issue.severity} - ${issue.type}: ${issue.description}")
                    appendLine("    Recommendation: ${issue.recommendation}")
                }
            }
            if (validationResult.warnings.isNotEmpty()) {
                appendLine("Warnings:")
                validationResult.warnings.forEach { warning ->
                    appendLine("  ${warning.type}: ${warning.description}")
                    appendLine("    Recommendation: ${warning.recommendation}")
                }
            }
            appendLine()
            
            appendLine("=== Performance Diagnosis ===")
            appendLine("Overall Health: ${performanceDiagnosis.overallHealth}")
            if (performanceDiagnosis.issues.isNotEmpty()) {
                appendLine("Performance Issues:")
                performanceDiagnosis.issues.forEach { issue ->
                    appendLine("  ${issue.severity} - ${issue.type}: ${issue.description}")
                    if (issue.affectedRoutes.isNotEmpty()) {
                        appendLine("    Affected Routes: ${issue.affectedRoutes.joinToString(", ")}")
                    }
                }
            }
            if (performanceDiagnosis.recommendations.isNotEmpty()) {
                appendLine("Recommendations:")
                performanceDiagnosis.recommendations.forEach { recommendation ->
                    appendLine("  • $recommendation")
                }
            }
            appendLine()
            
            appendLine("=== Recent Debug Logs ===")
            debugLogs.takeLast(20).forEach { log ->
                appendLine("${dateFormatter.format(Date(log.timestamp))} [${log.level}] ${log.tag}: ${log.message}")
                if (log.throwable != null) {
                    appendLine("  Exception: ${log.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Exports debug data to a file for sharing.
     */
    fun exportDebugData(
        context: Context,
        navigationState: NavigationState,
        performanceData: NavigationPerformanceData
    ): File? {
        if (!BuildConfig.DEBUG) return null
        
        try {
            val fileName = "navigation_debug_${System.currentTimeMillis()}.txt"
            val file = File(context.cacheDir, fileName)
            
            val validationResult = validateNavigationState(null, navigationState)
            val performanceDiagnosis = diagnosePerformanceIssues(performanceData, emptyList())
            
            FileWriter(file).use { writer ->
                writer.write(generateDebugReport(navigationState, performanceData, validationResult, performanceDiagnosis))
            }
            
            logDebug(DebugLogLevel.INFO, "Debug data exported to ${file.absolutePath}")
            return file
            
        } catch (e: Exception) {
            logDebug(DebugLogLevel.ERROR, "Failed to export debug data", throwable = e)
            return null
        }
    }
    
    /**
     * Shares debug data via system share intent.
     */
    fun shareDebugData(context: Context, debugFile: File) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(debugFile))
                putExtra(Intent.EXTRA_SUBJECT, "Navigation Debug Report")
                putExtra(Intent.EXTRA_TEXT, "Navigation debug report generated at ${Date()}")
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Debug Report"))
            
        } catch (e: Exception) {
            logDebug(DebugLogLevel.ERROR, "Failed to share debug data", throwable = e)
        }
    }
    
    /**
     * Clears all debug data.
     */
    fun clearDebugData() {
        debugLogs.clear()
        updateDebugState()
    }
    
    /**
     * Gets debug statistics.
     */
    fun getDebugStatistics(): NavigationDebugStatistics {
        return NavigationDebugStatistics(
            totalLogs = debugLogs.size,
            logsByLevel = debugLogs.groupBy { it.level }.mapValues { it.value.size },
            recentErrorCount = debugLogs.count { 
                it.level == DebugLogLevel.ERROR && 
                System.currentTimeMillis() - it.timestamp < RECENT_ERROR_WINDOW 
            },
            lastLogTimestamp = debugLogs.maxOfOrNull { it.timestamp }
        )
    }
    
    private fun updateDebugState() {
        val statistics = getDebugStatistics()
        _debugState.value = NavigationDebugState(
            isDebugging = BuildConfig.DEBUG,
            totalLogs = statistics.totalLogs,
            recentErrorCount = statistics.recentErrorCount,
            lastActivity = statistics.lastLogTimestamp ?: 0L
        )
    }
    
    private fun calculateOverallHealth(issues: List<PerformanceIssue>): PerformanceHealth {
        val highSeverityCount = issues.count { it.severity == PerformanceIssueSeverity.HIGH }
        val mediumSeverityCount = issues.count { it.severity == PerformanceIssueSeverity.MEDIUM }
        
        return when {
            highSeverityCount > 0 -> PerformanceHealth.POOR
            mediumSeverityCount > 2 -> PerformanceHealth.FAIR
            mediumSeverityCount > 0 -> PerformanceHealth.GOOD
            else -> PerformanceHealth.EXCELLENT
        }
    }
    
    companion object {
        private const val MAX_DEBUG_LOGS = 500
        private const val SLOW_NAVIGATION_THRESHOLD = 1000L // 1 second
        private const val HIGH_MEMORY_THRESHOLD = 50L // 50MB
        private const val RECENT_ERROR_WINDOW = 5 * 60 * 1000L // 5 minutes
    }
}

// Data classes for debug utilities

@Stable
data class NavigationDebugState(
    val isDebugging: Boolean = false,
    val totalLogs: Int = 0,
    val recentErrorCount: Int = 0,
    val lastActivity: Long = 0L
)

@Stable
data class NavigationDebugLog(
    val timestamp: Long,
    val level: DebugLogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
)

@Stable
data class NavigationValidationResult(
    val isValid: Boolean,
    val issues: List<NavigationIssue>,
    val warnings: List<NavigationWarning>,
    val validationTimestamp: Long
)

@Stable
data class NavigationIssue(
    val severity: IssueSeverity,
    val type: IssueType,
    val description: String,
    val recommendation: String
)

@Stable
data class NavigationWarning(
    val type: WarningType,
    val description: String,
    val recommendation: String
)

@Stable
data class NavigationPerformanceDiagnosis(
    val overallHealth: PerformanceHealth,
    val issues: List<PerformanceIssue>,
    val recommendations: List<String>,
    val diagnosisTimestamp: Long
)

@Stable
data class PerformanceIssue(
    val type: PerformanceIssueType,
    val severity: PerformanceIssueSeverity,
    val description: String,
    val affectedRoutes: List<String> = emptyList()
)

@Stable
data class NavigationDebugStatistics(
    val totalLogs: Int,
    val logsByLevel: Map<DebugLogLevel, Int>,
    val recentErrorCount: Int,
    val lastLogTimestamp: Long?
)

// Enums for debug utilities

enum class DebugLogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class IssueType {
    CONTROLLER_NULL,
    STATE_INCONSISTENCY,
    ROUTE_INVALID,
    MEMORY_LEAK,
    PERFORMANCE_DEGRADATION
}

enum class WarningType {
    BACK_STACK_INCONSISTENCY,
    POTENTIAL_LOOP,
    INCONSISTENT_FLAGS,
    DEPRECATED_USAGE,
    SUBOPTIMAL_PATTERN
}

enum class PerformanceHealth {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

enum class PerformanceIssueType {
    SLOW_NAVIGATION,
    HIGH_MEMORY_USAGE,
    NAVIGATION_FAILURES,
    EXCESSIVE_RECOMPOSITION
}

enum class PerformanceIssueSeverity {
    LOW,
    MEDIUM,
    HIGH
}