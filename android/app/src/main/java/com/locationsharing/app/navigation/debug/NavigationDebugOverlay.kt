package com.locationsharing.app.navigation.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.locationsharing.app.BuildConfig
import com.locationsharing.app.navigation.NavigationState
import com.locationsharing.app.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced debug overlay for navigation system that provides comprehensive debugging tools
 * including state inspection, performance profiling, error simulation, and debugging utilities.
 * Only available in debug builds.
 */
@Composable
fun NavigationDebugOverlay(
    navigationState: NavigationState,
    debugInfo: NavigationDebugInfo,
    stateInspector: NavigationStateInspector,
    performanceProfiler: NavigationPerformanceProfiler,
    errorSimulator: NavigationErrorSimulator,
    debugUtils: NavigationDebugUtils,
    onToggleErrorSimulation: () -> Unit,
    onClearHistory: () -> Unit,
    onSimulateTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.DEBUG) return
    
    var isOverlayVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Debug toggle button
        FloatingActionButton(
            onClick = { isOverlayVisible = !isOverlayVisible },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp),
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        ) {
            Icon(
                imageVector = if (isOverlayVisible) Icons.Default.VisibilityOff else Icons.Default.BugReport,
                contentDescription = if (isOverlayVisible) "Hide Debug Overlay" else "Show Debug Overlay",
                tint = Color.White
            )
        }
        
        // Debug overlay content
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            if (isExpanded) {
                NavigationDebugDialog(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    stateInspector = stateInspector,
                    performanceProfiler = performanceProfiler,
                    errorSimulator = errorSimulator,
                    debugUtils = debugUtils,
                    onDismiss = { isExpanded = false },
                    onToggleErrorSimulation = onToggleErrorSimulation,
                    onClearHistory = onClearHistory,
                    onSimulateTimeout = onSimulateTimeout
                )
            } else {
                NavigationDebugMiniOverlay(
                    navigationState = navigationState,
                    debugInfo = debugInfo,
                    onExpand = { isExpanded = true }
                )
            }
        }
    }
}

@Composable
private fun NavigationDebugMiniOverlay(
    navigationState: NavigationState,
    debugInfo: NavigationDebugInfo,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onExpand() }
            .widthIn(max = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Navigation Debug",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Current: ${navigationState.currentScreen.title}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                text = "History: ${navigationState.navigationHistory.size}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                text = "Errors: ${debugInfo.errorCount}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (debugInfo.errorCount > 0) MaterialTheme.colorScheme.error else Color.Unspecified
            )
            
            if (navigationState.isNavigating) {
                Text(
                    text = "⏳ Navigating...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NavigationDebugDialog(
    navigationState: NavigationState,
    debugInfo: NavigationDebugInfo,
    stateInspector: NavigationStateInspector,
    performanceProfiler: NavigationPerformanceProfiler,
    errorSimulator: NavigationErrorSimulator,
    debugUtils: NavigationDebugUtils,
    onDismiss: () -> Unit,
    onToggleErrorSimulation: () -> Unit,
    onClearHistory: () -> Unit,
    onSimulateTimeout: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Navigation Debug Console",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Debug Console"
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current State Section
                    item {
                        NavigationStateSection(navigationState = navigationState)
                    }
                    
                    // State Inspection Section
                    item {
                        StateInspectionSection(stateInspector = stateInspector)
                    }
                    
                    // Performance Metrics Section
                    item {
                        PerformanceMetricsSection(debugInfo = debugInfo)
                    }
                    
                    // Performance Profiling Section
                    item {
                        PerformanceProfilingSection(performanceProfiler = performanceProfiler)
                    }
                    
                    // Navigation History Section
                    item {
                        NavigationHistorySection(
                            history = navigationState.navigationHistory,
                            onClearHistory = onClearHistory
                        )
                    }
                    
                    // Error Log Section
                    item {
                        ErrorLogSection(errors = debugInfo.recentErrors)
                    }
                    
                    // Error Simulation Section
                    item {
                        ErrorSimulationSection(errorSimulator = errorSimulator)
                    }
                    
                    // Debug Controls Section
                    item {
                        DebugControlsSection(
                            isErrorSimulationEnabled = debugInfo.isErrorSimulationEnabled,
                            onToggleErrorSimulation = onToggleErrorSimulation,
                            onSimulateTimeout = onSimulateTimeout
                        )
                    }
                    
                    // Debug Utils Section
                    item {
                        DebugUtilsSection(debugUtils = debugUtils)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationStateSection(navigationState: NavigationState) {
    DebugSection(title = "Current State") {
        DebugInfoRow("Current Screen", navigationState.currentScreen.title)
        DebugInfoRow("Route", navigationState.currentScreen.route)
        DebugInfoRow("Can Navigate Back", navigationState.canNavigateBack.toString())
        DebugInfoRow("Is Navigating", navigationState.isNavigating.toString())
        DebugInfoRow("History Size", navigationState.navigationHistory.size.toString())
    }
}

@Composable
private fun PerformanceMetricsSection(debugInfo: NavigationDebugInfo) {
    DebugSection(title = "Performance Metrics") {
        DebugInfoRow("Avg Navigation Time", "${debugInfo.averageNavigationTime}ms")
        DebugInfoRow("Total Navigations", debugInfo.totalNavigations.toString())
        DebugInfoRow("Success Rate", "${debugInfo.successRate}%")
        DebugInfoRow("Memory Usage", "${debugInfo.memoryUsage}MB")
    }
}

@Composable
private fun NavigationHistorySection(
    history: List<Screen>,
    onClearHistory: () -> Unit
) {
    DebugSection(
        title = "Navigation History",
        action = {
            TextButton(onClick = onClearHistory) {
                Text("Clear")
            }
        }
    ) {
        if (history.isEmpty()) {
            Text(
                text = "No navigation history",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            history.takeLast(5).forEachIndexed { index, screen ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${history.size - index}. ${screen.title}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = screen.route,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (history.size > 5) {
                Text(
                    text = "... and ${history.size - 5} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorLogSection(errors: List<NavigationDebugError>) {
    DebugSection(title = "Recent Errors") {
        if (errors.isEmpty()) {
            Text(
                text = "No recent errors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            errors.take(3).forEach { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = error.type,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error.message,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = error.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugControlsSection(
    isErrorSimulationEnabled: Boolean,
    onToggleErrorSimulation: () -> Unit,
    onSimulateTimeout: () -> Unit
) {
    DebugSection(title = "Debug Controls") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = onToggleErrorSimulation,
                label = { Text("Error Simulation") },
                selected = isErrorSimulationEnabled,
                modifier = Modifier.weight(1f)
            )
            
            OutlinedButton(
                onClick = onSimulateTimeout,
                modifier = Modifier.weight(1f)
            ) {
                Text("Simulate Timeout")
            }
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                action?.invoke()
            }
            
            content()
        }
    }
}

@Composable
private fun StateInspectionSection(stateInspector: NavigationStateInspector) {
    val inspectionData by stateInspector.inspectionData.collectAsState()
    
    DebugSection(
        title = "State Inspection",
        action = {
            TextButton(onClick = { stateInspector.clearInspectionData() }) {
                Text("Clear")
            }
        }
    ) {
        if (inspectionData.timestamp.isNotEmpty()) {
            DebugInfoRow("Last Inspection", inspectionData.timestamp)
            DebugInfoRow("Current Route", inspectionData.currentRoute ?: "None")
            DebugInfoRow("Back Stack Size", inspectionData.backStackSize.toString())
            DebugInfoRow("Route Valid", inspectionData.routeValidation.isValid.toString())
            
            if (inspectionData.routeValidation.issues.isNotEmpty()) {
                Text(
                    text = "Issues: ${inspectionData.routeValidation.issues.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Text(
                text = "No inspection data available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PerformanceProfilingSection(performanceProfiler: NavigationPerformanceProfiler) {
    val performanceData by performanceProfiler.performanceData.collectAsState()
    
    DebugSection(
        title = "Performance Profiling",
        action = {
            TextButton(onClick = { performanceProfiler.clearPerformanceData() }) {
                Text("Clear")
            }
        }
    ) {
        DebugInfoRow("Active Navigations", performanceData.activeNavigations.toString())
        DebugInfoRow("Completed Navigations", performanceData.completedNavigations.toString())
        
        if (performanceData.recentNavigations.isNotEmpty()) {
            Text(
                text = "Recent Navigations:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            
            performanceData.recentNavigations.take(3).forEach { navigation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${navigation.fromScreen?.route ?: "Start"} → ${navigation.toScreen.route}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${navigation.duration}ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (navigation.duration > 1000) 
                            MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorSimulationSection(errorSimulator: NavigationErrorSimulator) {
    val simulationState by errorSimulator.simulationState.collectAsState()
    
    DebugSection(
        title = "Error Simulation",
        action = {
            TextButton(onClick = { errorSimulator.clearSimulationHistory() }) {
                Text("Clear History")
            }
        }
    ) {
        DebugInfoRow("Simulation Enabled", simulationState.isEnabled.toString())
        DebugInfoRow("Active Scenarios", simulationState.activeScenarios.size.toString())
        DebugInfoRow("Simulated Errors", simulationState.simulatedErrors.size.toString())
        
        if (simulationState.simulatedErrors.isNotEmpty()) {
            Text(
                text = "Recent Simulated Errors:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            
            simulationState.simulatedErrors.takeLast(3).forEach { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = error.scenarioName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${error.fromScreen ?: "Start"} → ${error.toScreen}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = error.errorType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Error scenario controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    if (simulationState.isEnabled) {
                        errorSimulator.disableErrorSimulation()
                    } else {
                        errorSimulator.enableErrorSimulation(
                            ErrorSimulationConfig(
                                enabledScenarios = listOf("timeout", "invalid_route"),
                                randomErrorProbability = 0.1f
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (simulationState.isEnabled) "Disable" else "Enable")
            }
            
            OutlinedButton(
                onClick = { 
                    // Simulate a random error scenario
                    val scenarios = errorSimulator.getAvailableScenarios()
                    if (scenarios.isNotEmpty()) {
                        val randomScenario = scenarios.random()
                        // This would need to be handled in a coroutine scope
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Simulate Error")
            }
        }
    }
}

@Composable
private fun DebugUtilsSection(debugUtils: NavigationDebugUtils) {
    val debugState by debugUtils.debugState.collectAsState()
    
    DebugSection(
        title = "Debug Utilities",
        action = {
            TextButton(onClick = { debugUtils.clearDebugData() }) {
                Text("Clear Logs")
            }
        }
    ) {
        DebugInfoRow("Debug Mode", debugState.isDebugging.toString())
        DebugInfoRow("Total Logs", debugState.totalLogs.toString())
        DebugInfoRow("Recent Errors", debugState.recentErrorCount.toString())
        
        if (debugState.lastActivity > 0) {
            DebugInfoRow(
                "Last Activity", 
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(debugState.lastActivity))
            )
        }
        
        // Debug action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    debugUtils.logDebug(
                        DebugLogLevel.INFO,
                        "Manual debug log entry",
                        "NavigationDebugOverlay"
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Log Test")
            }
            
            OutlinedButton(
                onClick = { 
                    // This would need context to export data
                    debugUtils.logDebug(
                        DebugLogLevel.INFO,
                        "Export requested from debug overlay",
                        "NavigationDebugOverlay"
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export Data")
            }
        }
    }
}

@Composable
private fun DebugInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}