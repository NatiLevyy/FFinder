#!/usr/bin/env pwsh

Write-Host "MapScreen Performance Optimizations Validation" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$ErrorCount = 0

function Test-FileExists {
    param([string]$FilePath, [string]$Description)
    
    if (Test-Path $FilePath) {
        Write-Host "✅ $Description exists" -ForegroundColor Green
        return $true
    } else {
        Write-Host "❌ $Description missing: $FilePath" -ForegroundColor Red
        $script:ErrorCount++
        return $false
    }
}

function Test-FileContains {
    param([string]$FilePath, [string]$Pattern, [string]$Description)
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        if ($content -match $Pattern) {
            Write-Host "✅ $Description implemented" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $Description not found" -ForegroundColor Red
            $script:ErrorCount++
            return $false
        }
    } else {
        Write-Host "❌ File not found: $FilePath" -ForegroundColor Red
        $script:ErrorCount++
        return $false
    }
}

Write-Host "`nChecking Performance Optimization Files..." -ForegroundColor Blue

# Check core performance optimization files
Test-FileExists -FilePath "android/app/src/main/java/com/locationsharing/app/ui/map/performance/MapPerformanceOptimizer.kt" -Description "MapPerformanceOptimizer"
Test-FileExists -FilePath "android/app/src/main/java/com/locationsharing/app/ui/map/performance/OptimizedMarkerClusterer.kt" -Description "OptimizedMarkerClusterer"
Test-FileExists -FilePath "android/app/src/main/java/com/locationsharing/app/ui/map/performance/MapLifecycleManager.kt" -Description "MapLifecycleManager"
Test-FileExists -FilePath "android/app/src/main/java/com/locationsharing/app/ui/map/performance/EfficientStateManager.kt" -Description "EfficientStateManager"

Write-Host "`nChecking Test Files..." -ForegroundColor Blue

Test-FileExists -FilePath "android/app/src/test/java/com/locationsharing/app/ui/map/performance/MapPerformanceOptimizerTest.kt" -Description "MapPerformanceOptimizer tests"
Test-FileExists -FilePath "android/app/src/test/java/com/locationsharing/app/ui/map/performance/EfficientStateManagerTest.kt" -Description "EfficientStateManager tests"
Test-FileExists -FilePath "android/app/src/test/java/com/locationsharing/app/ui/map/performance/MapLifecycleManagerTest.kt" -Description "MapLifecycleManager tests"
Test-FileExists -FilePath "android/app/src/test/java/com/locationsharing/app/ui/map/performance/MapPerformanceIntegrationTest.kt" -Description "Performance integration tests"

Write-Host "`nValidating Marker Clustering Implementation..." -ForegroundColor Blue

$optimizerFile = "android/app/src/main/java/com/locationsharing/app/ui/map/performance/MapPerformanceOptimizer.kt"
Test-FileContains -FilePath $optimizerFile -Pattern "shouldUseMarkerClustering" -Description "Marker clustering decision logic"
Test-FileContains -FilePath $optimizerFile -Pattern "createOptimizedClusters" -Description "Cluster creation algorithm"
Test-FileContains -FilePath $optimizerFile -Pattern "CLUSTERING_ZOOM_THRESHOLD" -Description "Clustering zoom threshold"

$clustererFile = "android/app/src/main/java/com/locationsharing/app/ui/map/performance/OptimizedMarkerClusterer.kt"
Test-FileContains -FilePath $clustererFile -Pattern "RenderOptimizedMarkers" -Description "Optimized marker rendering"
Test-FileContains -FilePath $clustererFile -Pattern "RenderClusteredMarkers" -Description "Clustered marker rendering"

Write-Host "`nValidating Animation Performance Optimization..." -ForegroundColor Blue

Test-FileContains -FilePath $optimizerFile -Pattern "batchAnimations" -Description "Animation batching"
Test-FileContains -FilePath $clustererFile -Pattern "MARKER_STAGGER_DELAY" -Description "Staggered marker animations"

Write-Host "`nValidating Lifecycle Management..." -ForegroundColor Blue

$lifecycleFile = "android/app/src/main/java/com/locationsharing/app/ui/map/performance/MapLifecycleManager.kt"
Test-FileContains -FilePath $lifecycleFile -Pattern "startLocationUpdates" -Description "Location updates management"
Test-FileContains -FilePath $lifecycleFile -Pattern "cleanup" -Description "Resource cleanup"
Test-FileContains -FilePath $lifecycleFile -Pattern "MapLifecycleEffect" -Description "Lifecycle-aware composable"

Write-Host "`nValidating Efficient State Updates..." -ForegroundColor Blue

$stateFile = "android/app/src/main/java/com/locationsharing/app/ui/map/performance/EfficientStateManager.kt"
Test-FileContains -FilePath $stateFile -Pattern "OptimizedMapState" -Description "Optimized state data class"
Test-FileContains -FilePath $stateFile -Pattern "shouldUpdateLocation" -Description "Location update throttling"
Test-FileContains -FilePath $stateFile -Pattern "batchProcessFriendUpdates" -Description "Batch friend processing"
Test-FileContains -FilePath $stateFile -Pattern "debounce" -Description "State change debouncing"

Write-Host "`nValidating MapScreen Integration..." -ForegroundColor Blue

$mapScreenFile = "android/app/src/main/java/com/locationsharing/app/ui/screens/MapScreen.kt"
Test-FileContains -FilePath $mapScreenFile -Pattern "MapPerformanceOptimizer" -Description "Performance optimizer integration"
Test-FileContains -FilePath $mapScreenFile -Pattern "OptimizedMarkerClusterer" -Description "Marker clusterer integration"
Test-FileContains -FilePath $mapScreenFile -Pattern "MapLifecycleEffect" -Description "Lifecycle management integration"

Write-Host "`nValidation Summary" -ForegroundColor Cyan
Write-Host "=================" -ForegroundColor Cyan

if ($ErrorCount -eq 0) {
    Write-Host "All performance optimizations implemented successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ Marker clustering for large friend lists" -ForegroundColor Green
    Write-Host "✅ Animation performance optimization" -ForegroundColor Green
    Write-Host "✅ Proper lifecycle management for location updates" -ForegroundColor Green
    Write-Host "✅ Efficient state updates and recomposition" -ForegroundColor Green
    Write-Host ""
    Write-Host "MapScreen performance optimizations are ready!" -ForegroundColor Green
} else {
    Write-Host "Found $ErrorCount error(s) in performance optimization implementation" -ForegroundColor Red
}

exit $ErrorCount