package com.locationsharing.app.ui.friends.components

/**
 * Simple performance monitor implementation for testing/demo purposes
 * Provides no-op implementations when dependency injection is unavailable
 */
class SimplePerformanceMonitor : NearbyPanelPerformanceMonitor() {
    
    /**
     * Monitor distance calculation performance (no-op)
     */
    override fun monitorDistanceCalculation(friendCount: Int, operation: () -> Unit) {
        // Simply execute the operation without monitoring
        operation()
    }
    
    /**
     * Monitor LazyColumn scrolling performance (no-op)
     */
    override fun monitorScrollPerformance(friendCount: Int, scrollOperation: () -> Unit) {
        // Simply execute the operation without monitoring
        scrollOperation()
    }
    
    /**
     * Monitor memory usage (no-op)
     */
    override fun monitorMemoryUsage(operation: String) {
        // No-op for simple implementation
    }
    
    /**
     * Monitor search performance (no-op)
     */
    override fun monitorSearchPerformance(friendCount: Int, query: String, searchOperation: () -> Unit) {
        // Simply execute the operation without monitoring
        searchOperation()
    }
    
    /**
     * Monitor state preservation performance (no-op)
     */
    override fun monitorStatePreservation(operation: () -> Unit) {
        // Simply execute the operation without monitoring
        operation()
    }
    
    /**
     * Log performance summary (no-op)
     */
    override fun logPerformanceSummary(
        friendCount: Int,
        calculationTime: Long,
        renderTime: Long,
        memoryUsageMB: Long
    ) {
        // No-op for simple implementation
    }
    
    /**
     * Track throttling effectiveness (no-op)
     */
    override fun trackThrottlingEffectiveness(
        movementDistance: Double,
        timeSinceLastCalculation: Long,
        wasThrottled: Boolean
    ) {
        // No-op for simple implementation
    }
    
    /**
     * Monitor configuration change performance (no-op)
     */
    override fun monitorConfigurationChange(operation: () -> Unit) {
        // Simply execute the operation without monitoring
        operation()
    }
}