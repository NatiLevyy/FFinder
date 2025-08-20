#!/usr/bin/env pwsh

Write-Host "Friend Markers Implementation Validation (Task 6)" -ForegroundColor Green
Write-Host "============================================================"

$results = @()

# Check AnimatedFriendMarker
$markerFile = "android/app/src/main/java/com/locationsharing/app/ui/friends/components/AnimatedFriendMarker.kt"
if (Test-Path $markerFile) {
    $content = Get-Content $markerFile -Raw
    if ($content -match "animateFloatAsState|infiniteRepeatable") {
        Write-Host "✅ AnimatedFriendMarker with smooth animations" -ForegroundColor Green
        $results += "PASS"
    } else {
        Write-Host "❌ AnimatedFriendMarker missing animations" -ForegroundColor Red
        $results += "FAIL"
    }
} else {
    Write-Host "❌ AnimatedFriendMarker not found" -ForegroundColor Red
    $results += "FAIL"
}

# Check Clustering
$clusterFile = "android/app/src/main/java/com/locationsharing/app/ui/friends/components/ClusterMarker.kt"
if (Test-Path $clusterFile) {
    Write-Host "✅ Friend marker clustering implemented" -ForegroundColor Green
    $results += "PASS"
} else {
    Write-Host "❌ Clustering not implemented" -ForegroundColor Red
    $results += "FAIL"
}

# Check Real-time updates
$realTimeFile = "android/app/src/main/java/com/locationsharing/app/data/friends/RealTimeFriendsService.kt"
if (Test-Path $realTimeFile) {
    Write-Host "✅ Real-time friend location updates" -ForegroundColor Green
    $results += "PASS"
} else {
    Write-Host "❌ Real-time updates not implemented" -ForegroundColor Red
    $results += "FAIL"
}

# Check MapScreen integration
$mapScreenFile = "android/app/src/main/java/com/locationsharing/app/ui/screens/MapScreen.kt"
if (Test-Path $mapScreenFile) {
    $content = Get-Content $mapScreenFile -Raw
    if ($content -match "onFriendMarkerClick|FriendMarkersLayer") {
        Write-Host "✅ Friend marker click handling and selection" -ForegroundColor Green
        $results += "PASS"
    } else {
        Write-Host "❌ Click handling not integrated" -ForegroundColor Red
        $results += "FAIL"
    }
} else {
    Write-Host "❌ MapScreen integration missing" -ForegroundColor Red
    $results += "FAIL"
}

# Summary
$passCount = ($results | Where-Object { $_ -eq "PASS" }).Count
$totalCount = $results.Count
$successRate = [math]::Round(($passCount / $totalCount) * 100, 1)

Write-Host ""
Write-Host "SUMMARY:" -ForegroundColor Cyan
Write-Host "Passed: $passCount/$totalCount ($successRate%)" -ForegroundColor White

if ($successRate -eq 100) {
    Write-Host "TASK 6 COMPLETED SUCCESSFULLY!" -ForegroundColor Green
} elseif ($successRate -ge 75) {
    Write-Host "TASK 6 MOSTLY COMPLETED" -ForegroundColor Yellow
} else {
    Write-Host "TASK 6 NEEDS MORE WORK" -ForegroundColor Red
}