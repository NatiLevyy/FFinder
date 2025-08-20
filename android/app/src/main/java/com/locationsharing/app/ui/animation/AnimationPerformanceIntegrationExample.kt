package com.locationsharing.app.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import timber.log.Timber

/**
 * Integration example demonstrating comprehensive animation performance optimization.
 * 
 * This example shows how to use all Task 13 features together:
 * - AnimationPerformanceOptimizer for real-time monitoring
 * - AnimationLifecycleManager for proper cleanup
 * - OptimizedRecomposition for efficient updates
 * - ReducedMotionAlternatives for accessibility
 * - MemoryUsageMonitor for resource tracking
 * 
 * This serves as a practical reference for implementing performance-optimized animations
 * throughout the FFinder application.
 */

/**
 * Main example composable that demonstrates integrated animation performance optimization.
 */
@Composable
fun AnimationPerformanceIntegrationExample(
    modifier: Modifier = Modifier
) {
    var performanceMetrics by remember { mutableStateOf<AnimationPerformanceMetrics?>(null) }
    var memoryMetrics by remember { mutableStateOf<AnimationMemoryMetrics?>(null) }
    var showPerformancePanel by remember { mutableStateOf(false) }
    
    // Main performance optimization wrapper
    AnimationPerformanceOptimizer(
        isEnabled = true,
        onPerformanceIssue = { issue ->
            Timber.w("Animation performance issue detected: $issue")
        },
        onPerformanceUpdate = { metrics ->
            performanceMetrics = metrics
        }
    ) { optimizationConfig ->
        
        // Memory monitoring wrapper
        AnimationMemoryMonitor(
            onMemoryUpdate = { metrics ->
                memoryMetrics = metrics
            },
            onMemoryWarning = { analysis ->
                Timber.w("Memory warning: ${analysis.warnings}")
                if (analysis.overallHealth == MemoryHealth.CRITICAL) {
                    showPerformancePanel = true
                }
            }
        ) { memoryMonitor ->
            
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Performance status header
                PerformanceStatusHeader(
                    performanceMetrics = performanceMetrics,
                    memoryMetrics = memoryMetrics,
                    optimizationConfig = optimizationConfig,
                    onTogglePanel = { showPerformancePanel = !showPerformancePanel }
                )
                
                // Animated content examples
                AnimatedContentExamples(
                    optimizationConfig = optimizationConfig,
                    memoryMonitor = memoryMonitor
                )
                
                // Performance panel (if enabled)
                if (showPerformancePanel) {
                    PerformancePanel(
                        performanceMetrics = performanceMetrics,
                        memoryMetrics = memoryMetrics,
                        optimizationConfig = optimizationConfig,
                        onDismiss = { showPerformancePanel = false }
                    )
                }
            }
        }
    }
}

/**
 * Performance status header showing current optimization state.
 */
@Composable
private fun PerformanceStatusHeader(
    performanceMetrics: AnimationPerformanceMetrics?,
    memoryMetrics: AnimationMemoryMetrics?,
    optimizationConfig: AnimationOptimizationConfig,
    onTogglePanel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Animation Performance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = onTogglePanel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (optimizationConfig.animationQuality) {
                            AnimationQuality.FULL -> Color.Green
                            AnimationQuality.REDUCED -> Color.Yellow
                            AnimationQuality.MINIMAL -> Color(0xFFFFA500)
                            AnimationQuality.DISABLED -> Color.Red
                        }
                    )
                ) {
                    Text("${optimizationConfig.animationQuality}")
                }
            }
            
            // Performance indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PerformanceIndicator(
                    label = "FPS",
                    value = performanceMetrics?.averageFps?.toInt()?.toString() ?: "--",
                    isGood = (performanceMetrics?.averageFps ?: 0.0) >= 55.0
                )
                
                PerformanceIndicator(
                    label = "Memory",
                    value = "${memoryMetrics?.usedMemoryMb ?: 0}MB",
                    isGood = (memoryMetrics?.memoryUsagePercentage ?: 0f) < 75f
                )
                
                PerformanceIndicator(
                    label = "Battery",
                    value = "${performanceMetrics?.batteryLevel ?: 0}%",
                    isGood = (performanceMetrics?.batteryLevel ?: 0) > 30
                )
                
                PerformanceIndicator(
                    label = "Thermal",
                    value = performanceMetrics?.thermalState?.name?.take(4) ?: "OK",
                    isGood = performanceMetrics?.thermalState == ThermalState.NONE
                )
            }
        }
    }
}

/**
 * Individual performance indicator component.
 */
@Composable
private fun PerformanceIndicator(
    label: String,
    value: String,
    isGood: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isGood) Color.Green else Color.Red
        )
    }
}

/**
 * Examples of animated content using performance optimization features.
 */
@Composable
private fun AnimatedContentExamples(
    optimizationConfig: AnimationOptimizationConfig,
    memoryMonitor: AnimationMemoryMonitor
) {
    val lifecycleManager = rememberAnimationLifecycleManager()
    
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            OptimizedFloatAnimationExample(
                optimizationConfig = optimizationConfig,
                lifecycleManager = lifecycleManager
            )
        }
        
        item {
            AccessibleAnimationExample(optimizationConfig = optimizationConfig)
        }
        
        item {
            MemoryTrackedAnimationExample(
                memoryMonitor = memoryMonitor,
                optimizationConfig = optimizationConfig
            )
        }
        
        item {
            InfiniteAnimationExample(
                optimizationConfig = optimizationConfig,
                lifecycleManager = lifecycleManager
            )
        }
    }
}

/**
 * Example of optimized float animation with stable parameters.
 */
@Composable
private fun OptimizedFloatAnimationExample(
    optimizationConfig: AnimationOptimizationConfig,
    lifecycleManager: AnimationLifecycleManager
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val animationConfig = rememberOptimizedAnimationConfig(
        quality = optimizationConfig.animationQuality,
        baseConfig = OptimizedAnimationSpecs.rememberMediumAnimation(
            quality = optimizationConfig.animationQuality
        )
    )
    
    val scale by rememberOptimizedFloatAnimation(
        targetValue = if (isExpanded) 1.2f else 1.0f,
        config = animationConfig,
        label = "ScaleAnimation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .trackMemoryUsage(
                componentName = "OptimizedFloatAnimation",
                estimatedMemoryMb = 1L
            ),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Optimized Float Animation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Quality: ${optimizationConfig.animationQuality}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Duration: ${(animationConfig.duration * optimizationConfig.durationMultiplier).toInt()}ms",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Example of accessible animation with reduced motion alternatives.
 */
@Composable
private fun AccessibleAnimationExample(
    optimizationConfig: AnimationOptimizationConfig
) {
    var isVisible by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Accessible Animation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { isVisible = !isVisible }
            ) {
                Text(if (isVisible) "Hide" else "Show")
            }
            
            AccessibleFadeTransition(
                visible = isVisible,
                label = "AccessibleFadeExample"
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                )
            }
        }
    }
}

/**
 * Example of memory-tracked animation component.
 */
@Composable
private fun MemoryTrackedAnimationExample(
    memoryMonitor: AnimationMemoryMonitor,
    optimizationConfig: AnimationOptimizationConfig
) {
    var animationCount by remember { mutableStateOf(1) }
    
    val memoryAwareConfig = rememberMemoryAwareAnimationConfig(
        baseConfig = StableAnimationConfig(
            duration = 1000,
            easing = FastOutSlowInEasing
        )
    )
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Memory-Tracked Animation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { animationCount = maxOf(1, animationCount - 1) }
                ) {
                    Text("-")
                }
                
                Text(
                    text = "Animations: $animationCount",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                
                Button(
                    onClick = { animationCount = minOf(10, animationCount + 1) }
                ) {
                    Text("+")
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(animationCount) { index ->
                    val component = rememberAnimationComponentWithMemoryTracking(
                        component = "AnimatedBox$index",
                        estimatedMemoryMb = 2L,
                        monitor = memoryMonitor
                    )
                    
                    AnimatedBox(
                        index = index,
                        config = memoryAwareConfig
                    )
                }
            }
        }
    }
}

/**
 * Individual animated box component.
 */
@Composable
private fun AnimatedBox(
    index: Int,
    config: StableAnimationConfig
) {
    val infiniteTransition = rememberManagedInfiniteTransition(
        label = "AnimatedBox$index"
    )
    
    val color by infiniteTransition?.animateColor(
        initialValue = Color.Blue,
        targetValue = Color.Red,
        animationSpec = infiniteRepeatable(
            animation = tween(config.duration, easing = config.easing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorAnimation$index"
    ) ?: remember { mutableStateOf(Color.Blue) }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, CircleShape)
            .clip(CircleShape)
    )
}

/**
 * Example of infinite animation with lifecycle management.
 */
@Composable
private fun InfiniteAnimationExample(
    optimizationConfig: AnimationOptimizationConfig,
    lifecycleManager: AnimationLifecycleManager
) {
    var isEnabled by remember { mutableStateOf(true) }
    
    val infiniteTransition = rememberAccessibleInfiniteTransition(
        label = "InfiniteExample"
    )
    
    val rotation by infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 * optimizationConfig.durationMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAnimation"
    ) ?: remember { mutableStateOf(0f) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Infinite Animation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it }
            )
            
            if (isEnabled && infiniteTransition != null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .rotate(rotation)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Performance panel showing detailed metrics and controls.
 */
@Composable
private fun PerformancePanel(
    performanceMetrics: AnimationPerformanceMetrics?,
    memoryMetrics: AnimationMemoryMetrics?,
    optimizationConfig: AnimationOptimizationConfig,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Text("✕")
                }
            }
            
            Divider()
            
            // Performance metrics
            performanceMetrics?.let { metrics ->
                PerformanceMetricsSection(metrics)
            }
            
            Divider()
            
            // Memory metrics
            memoryMetrics?.let { metrics ->
                MemoryMetricsSection(metrics)
            }
            
            Divider()
            
            // Optimization settings
            OptimizationSettingsSection(optimizationConfig)
        }
    }
}

/**
 * Performance metrics section in the panel.
 */
@Composable
private fun PerformanceMetricsSection(
    metrics: AnimationPerformanceMetrics
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Performance Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        MetricRow("Average FPS", "${metrics.averageFps.toInt()}")
        MetricRow("Dropped Frames", "${metrics.droppedFramePercentage.toInt()}%")
        MetricRow("Jitter", "${metrics.jitterMs.toInt()}ms")
        MetricRow("Grade", metrics.performanceGrade.name)
    }
}

/**
 * Memory metrics section in the panel.
 */
@Composable
private fun MemoryMetricsSection(
    metrics: AnimationMemoryMetrics
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Memory Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        MetricRow("Used Memory", "${metrics.usedMemoryMb}MB")
        MetricRow("Animation Memory", "${metrics.animationMemoryMb}MB")
        MetricRow("Usage", "${metrics.memoryUsagePercentage.toInt()}%")
        MetricRow("Pressure", metrics.memoryPressureLevel.name)
    }
}

/**
 * Optimization settings section in the panel.
 */
@Composable
private fun OptimizationSettingsSection(
    config: AnimationOptimizationConfig
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Optimization Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        MetricRow("Quality", config.animationQuality.name)
        MetricRow("Duration Multiplier", "${config.durationMultiplier}x")
        MetricRow("Max Concurrent", "${config.maxConcurrentAnimations}")
        MetricRow("Hardware Accel", if (config.useHardwareAcceleration) "ON" else "OFF")
    }
}

/**
 * Individual metric row component.
 */
@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}