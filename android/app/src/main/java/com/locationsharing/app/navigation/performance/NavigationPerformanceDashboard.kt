package com.locationsharing.app.navigation.performance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * Performance dashboard composable for monitoring navigation performance.
 * Shows real-time metrics, recommendations, and optimization status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationPerformanceDashboard(
    viewModel: NavigationPerformanceDashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val performanceMetrics by viewModel.performanceMetrics.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val cacheStats by viewModel.cacheStats.collectAsState()
    val buttonStats by viewModel.buttonStats.collectAsState()
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PerformanceOverviewCard(performanceMetrics)
        }
        
        item {
            NavigationMetricsCard(performanceMetrics)
        }
        
        item {
            CachePerformanceCard(cacheStats)
        }
        
        item {
            ButtonPerformanceCard(buttonStats)
        }
        
        if (recommendations.isNotEmpty()) {
            item {
                Text(
                    text = "Performance Recommendations",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(recommendations) { recommendation ->
                RecommendationCard(recommendation)
            }
        }
        
        item {
            PerformanceActionsCard(
                onResetMetrics = viewModel::resetMetrics,
                onOptimizeCache = viewModel::optimizeCache,
                onOptimizeButtons = viewModel::optimizeButtons
            )
        }
    }
}

@Composable
private fun PerformanceOverviewCard(
    metrics: NavigationPerformanceMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (metrics.performanceGrade) {
                PerformanceGrade.EXCELLENT -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                PerformanceGrade.GOOD -> Color(0xFF8BC34A).copy(alpha = 0.1f)
                PerformanceGrade.FAIR -> Color(0xFFFF9800).copy(alpha = 0.1f)
                PerformanceGrade.POOR -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Performance",
                    tint = when (metrics.performanceGrade) {
                        PerformanceGrade.EXCELLENT -> Color(0xFF4CAF50)
                        PerformanceGrade.GOOD -> Color(0xFF8BC34A)
                        PerformanceGrade.FAIR -> Color(0xFFFF9800)
                        PerformanceGrade.POOR -> Color(0xFFF44336)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Grade: ${metrics.performanceGrade.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = when (metrics.performanceGrade) {
                    PerformanceGrade.EXCELLENT -> Color(0xFF4CAF50)
                    PerformanceGrade.GOOD -> Color(0xFF8BC34A)
                    PerformanceGrade.FAIR -> Color(0xFFFF9800)
                    PerformanceGrade.POOR -> Color(0xFFF44336)
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Last updated: ${formatTimestamp(metrics.lastUpdated)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationMetricsCard(
    metrics: NavigationPerformanceMetrics,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Navigation Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricRow(
                label = "Average Navigation Time",
                value = "${metrics.averageNavigationTime.toInt()}ms",
                isGood = metrics.averageNavigationTime < 500
            )
            
            MetricRow(
                label = "Total Navigations",
                value = metrics.totalNavigations.toString(),
                isGood = true
            )
            
            MetricRow(
                label = "Cache Hit Rate",
                value = "${(metrics.cacheHitRate * 100).toInt()}%",
                isGood = metrics.cacheHitRate > 0.7
            )
            
            MetricRow(
                label = "Total Errors",
                value = metrics.totalErrors.toString(),
                isGood = metrics.totalErrors == 0L
            )
        }
    }
}

@Composable
private fun CachePerformanceCard(
    stats: NavigationCacheStats?,
    modifier: Modifier = Modifier
) {
    if (stats == null) return
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cache Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricRow(
                label = "Cached Destinations",
                value = "${stats.validDestinations}/${stats.totalDestinations}",
                isGood = stats.validDestinations > 0
            )
            
            MetricRow(
                label = "Cached States",
                value = "${stats.validStates}/${stats.totalStates}",
                isGood = stats.validStates > 0
            )
            
            MetricRow(
                label = "Memory Usage",
                value = "${stats.memoryUsageKB}KB",
                isGood = stats.memoryUsageKB < 500
            )
            
            if (stats.expiredDestinations > 0 || stats.expiredStates > 0) {
                MetricRow(
                    label = "Expired Entries",
                    value = "${stats.expiredDestinations + stats.expiredStates}",
                    isGood = false
                )
            }
        }
    }
}

@Composable
private fun ButtonPerformanceCard(
    stats: ButtonPerformanceStats?,
    modifier: Modifier = Modifier
) {
    if (stats == null) return
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Button Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricRow(
                label = "Total Buttons",
                value = stats.totalButtons.toString(),
                isGood = true
            )
            
            MetricRow(
                label = "Active Buttons",
                value = stats.activeButtons.toString(),
                isGood = stats.activeButtons > 0
            )
            
            MetricRow(
                label = "Preloaded Common Buttons",
                value = stats.commonButtonsPreloaded.toString(),
                isGood = stats.commonButtonsPreloaded > 0
            )
            
            if (stats.loadingButtons > 0) {
                MetricRow(
                    label = "Loading Buttons",
                    value = stats.loadingButtons.toString(),
                    isGood = stats.loadingButtons < 3
                )
            }
            
            if (stats.activeActionsCount > 0) {
                MetricRow(
                    label = "Active Actions",
                    value = stats.activeActionsCount.toString(),
                    isGood = stats.activeActionsCount < 3
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: PerformanceRecommendation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.priority) {
                Priority.CRITICAL -> Color(0xFFF44336).copy(alpha = 0.1f)
                Priority.HIGH -> Color(0xFFFF9800).copy(alpha = 0.1f)
                Priority.MEDIUM -> Color(0xFFFFEB3B).copy(alpha = 0.1f)
                Priority.LOW -> Color(0xFF2196F3).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recommendation.type.name.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (recommendation.priority == Priority.CRITICAL || recommendation.priority == Priority.HIGH) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = when (recommendation.priority) {
                            Priority.CRITICAL -> Color(0xFFF44336)
                            Priority.HIGH -> Color(0xFFFF9800)
                            else -> Color.Gray
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Action: ${recommendation.actionable}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PerformanceActionsCard(
    onResetMetrics: () -> Unit,
    onOptimizeCache: () -> Unit,
    onOptimizeButtons: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOptimizeCache,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Optimize Cache")
                }
                
                OutlinedButton(
                    onClick = onOptimizeButtons,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Optimize Buttons")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onResetMetrics,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Metrics")
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isGood) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    }
    
    Spacer(modifier = Modifier.height(4.dp))
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}