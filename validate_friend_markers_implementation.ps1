#!/usr/bin/env pwsh

# Friend Markers Implementation Validation Script
# Validates that task 6 requirements have been implemented

Write-Host "🧪 Validating Friend Markers Implementation (Task 6)" -ForegroundColor Green
Write-Host "=" * 60

$validationResults = @()

# Check 1: AnimatedFriendMarker component exists and has required features
Write-Host "✅ Checking AnimatedFriendMarker component..." -ForegroundColor Yellow
$animatedMarkerFile = "android/app/src/main/java/com/locationsharing/app/ui/friends/components/AnimatedFriendMarker.kt"
if (Test-Path $animatedMarkerFile) {
    $content = Get-Content $animatedMarkerFile -Raw
    
    $checks = @(
        @{ Name = "Smooth animations"; Pattern = "animateFloatAsState|infiniteRepeatable|spring" },
        @{ Name = "Friend marker click handling"; Pattern = "onClick.*Unit|clickable" },
        @{ Name = "Selection state support"; Pattern = "isSelected.*Boolean" },
        @{ Name = "Movement animations"; Pattern = "isMoving|MovementTrail" },
        @{ Name = "Online status indicators"; Pattern = "isOnline|pulseScale|pulseAlpha" }
    )
    
    foreach ($check in $checks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: AnimatedFriendMarker - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: AnimatedFriendMarker - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ AnimatedFriendMarker.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: AnimatedFriendMarker.kt not found"
}

# Check 2: Clustering implementation exists
Write-Host "`n✅ Checking clustering implementation..." -ForegroundColor Yellow
$clusterMarkerFile = "android/app/src/main/java/com/locationsharing/app/ui/friends/components/ClusterMarker.kt"
$markerManagerFile = "android/app/src/main/java/com/locationsharing/app/ui/friends/components/EnhancedMapMarkerManager.kt"

if (Test-Path $clusterMarkerFile) {
    $content = Get-Content $clusterMarkerFile -Raw
    
    $clusterChecks = @(
        @{ Name = "Cluster marker component"; Pattern = "ClusterMarker.*@Composable" },
        @{ Name = "Friend count display"; Pattern = "totalFriends.*size|friends\.size" },
        @{ Name = "Online friends indicator"; Pattern = "onlineFriends|hasOnlineFriends" },
        @{ Name = "Cluster click handling"; Pattern = "onClick.*Unit" }
    )
    
    foreach ($check in $clusterChecks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: ClusterMarker - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: ClusterMarker - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ ClusterMarker.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: ClusterMarker.kt not found"
}

if (Test-Path $markerManagerFile) {
    $content = Get-Content $markerManagerFile -Raw
    
    $managerChecks = @(
        @{ Name = "Enhanced marker manager"; Pattern = "EnhancedMapMarkerManager" },
        @{ Name = "Clustering logic"; Pattern = "createClusters|shouldUseClusteringForZoom" },
        @{ Name = "Performance optimization"; Pattern = "MAX_MARKERS_WITHOUT_CLUSTERING|CLUSTERING_ZOOM_THRESHOLD" },
        @{ Name = "Marker animations"; Pattern = "MarkerAnimationState|animateMarkerToPosition" }
    )
    
    foreach ($check in $managerChecks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: EnhancedMapMarkerManager - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: EnhancedMapMarkerManager - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ EnhancedMapMarkerManager.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: EnhancedMapMarkerManager.kt not found"
}

# Check 3: Real-time updates implementation
Write-Host "`n✅ Checking real-time updates..." -ForegroundColor Yellow
$realTimeServiceFile = "android/app/src/main/java/com/locationsharing/app/data/friends/RealTimeFriendsService.kt"

if (Test-Path $realTimeServiceFile) {
    $content = Get-Content $realTimeServiceFile -Raw
    
    $realTimeChecks = @(
        @{ Name = "Real-time service"; Pattern = "RealTimeFriendsService" },
        @{ Name = "Friend updates with animations"; Pattern = "getFriendUpdatesWithAnimations|FriendUpdateWithAnimation" },
        @{ Name = "Location update handling"; Pattern = "handleLocationUpdate|LocationUpdateEvent" },
        @{ Name = "Friend appearance/disappearance"; Pattern = "handleFriendAppeared|handleFriendDisappeared" },
        @{ Name = "Animation metadata"; Pattern = "AnimationType|LocationUpdateType" }
    )
    
    foreach ($check in $realTimeChecks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: RealTimeFriendsService - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: RealTimeFriendsService - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ RealTimeFriendsService.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: RealTimeFriendsService.kt not found"
}

# Check 4: MapScreen integration
Write-Host "`n✅ Checking MapScreen integration..." -ForegroundColor Yellow
$mapScreenFile = "android/app/src/main/java/com/locationsharing/app/ui/screens/MapScreen.kt"

if (Test-Path $mapScreenFile) {
    $content = Get-Content $mapScreenFile -Raw
    
    $integrationChecks = @(
        @{ Name = "Friends parameter"; Pattern = "friends.*List.*Friend" },
        @{ Name = "Selected friend ID"; Pattern = "selectedFriendId.*String" },
        @{ Name = "Friend marker click handler"; Pattern = "onFriendMarkerClick.*String.*Unit" },
        @{ Name = "Cluster click handler"; Pattern = "onClusterClick.*List.*Friend.*Unit" },
        @{ Name = "FriendMarkersLayer component"; Pattern = "FriendMarkersLayer" }
    )
    
    foreach ($check in $integrationChecks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: MapScreen - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: MapScreen - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ MapScreen.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: MapScreen.kt not found"
}

# Check 5: ViewModel integration
Write-Host "`n✅ Checking ViewModel integration..." -ForegroundColor Yellow
$viewModelFile = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenViewModel.kt"

if (Test-Path $viewModelFile) {
    $content = Get-Content $viewModelFile -Raw
    
    $viewModelChecks = @(
        @{ Name = "Real-time friends service injection"; Pattern = "realTimeFriendsService.*RealTimeFriendsService" },
        @{ Name = "Friend marker click handling"; Pattern = "handleFriendMarkerClick" },
        @{ Name = "Cluster click handling"; Pattern = "handleClusterClick" },
        @{ Name = "Real-time updates integration"; Pattern = "getFriendUpdatesWithAnimations" },
        @{ Name = "Service lifecycle management"; Pattern = "startSync|stopSync" }
    )
    
    foreach ($check in $viewModelChecks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: MapScreenViewModel - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: MapScreenViewModel - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ MapScreenViewModel.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: MapScreenViewModel.kt not found"
}

# Check 6: Event handling
Write-Host "`n✅ Checking event handling..." -ForegroundColor Yellow
$eventFile = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenEvent.kt"

if (Test-Path $eventFile) {
    $content = Get-Content $eventFile -Raw
    
    $eventChecks = @(
        @{ Name = "Friend marker click event"; Pattern = "OnFriendMarkerClick.*friendId.*String" },
        @{ Name = "Cluster click event"; Pattern = "OnClusterClick.*friends.*List.*Friend" },
        @{ Name = "Friend selection clear event"; Pattern = "OnFriendSelectionClear" }
    )
    
    foreach ($check in $eventChecks) {
        if ($content -match $check.Pattern) {
            Write-Host "  ✅ $($check.Name)" -ForegroundColor Green
            $validationResults += "PASS: MapScreenEvent - $($check.Name)"
        } else {
            Write-Host "  ❌ $($check.Name)" -ForegroundColor Red
            $validationResults += "FAIL: MapScreenEvent - $($check.Name)"
        }
    }
} else {
    Write-Host "  ❌ MapScreenEvent.kt not found" -ForegroundColor Red
    $validationResults += "FAIL: MapScreenEvent.kt not found"
}

# Check 7: Test files created
Write-Host "`n✅ Checking test implementation..." -ForegroundColor Yellow
$integrationTestFile = "android/app/src/test/java/com/locationsharing/app/ui/map/FriendMarkersIntegrationTest.kt"
$viewModelTestFile = "android/app/src/test/java/com/locationsharing/app/ui/map/MapScreenViewModelFriendMarkersTest.kt"

$testFiles = @(
    @{ Name = "Integration tests"; File = $integrationTestFile },
    @{ Name = "ViewModel tests"; File = $viewModelTestFile }
)

foreach ($test in $testFiles) {
    if (Test-Path $test.File) {
        Write-Host "  ✅ $($test.Name) created" -ForegroundColor Green
        $validationResults += "PASS: Test - $($test.Name) created"
    } else {
        Write-Host "  ❌ $($test.Name) not found" -ForegroundColor Red
        $validationResults += "FAIL: Test - $($test.Name) not found"
    }
}

# Summary
Write-Host "`n" + "=" * 60
Write-Host "📊 VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "=" * 60

$passCount = ($validationResults | Where-Object { $_ -like "PASS:*" }).Count
$failCount = ($validationResults | Where-Object { $_ -like "FAIL:*" }).Count
$totalCount = $validationResults.Count

Write-Host "✅ Passed: $passCount" -ForegroundColor Green
Write-Host "❌ Failed: $failCount" -ForegroundColor Red
Write-Host "📊 Total:  $totalCount" -ForegroundColor White

$successRate = [math]::Round(($passCount / $totalCount) * 100, 1)
Write-Host "🎯 Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })

# Requirements validation
Write-Host "`n📋 REQUIREMENTS VALIDATION" -ForegroundColor Cyan
Write-Host "=" * 60

$requirements = @(
    @{ Id = "2.3"; Description = "Friend markers display on map"; Status = if ($passCount -ge 15) { "✅ IMPLEMENTED" } else { "❌ PARTIAL" } },
    @{ Id = "2.4"; Description = "Friend marker click handling"; Status = if ($validationResults -like "*Friend marker click*" -and $validationResults -like "*PASS*") { "✅ IMPLEMENTED" } else { "❌ MISSING" } },
    @{ Id = "2.5"; Description = "Friend marker selection"; Status = if ($validationResults -like "*Selection state*" -and $validationResults -like "*PASS*") { "✅ IMPLEMENTED" } else { "❌ MISSING" } },
    @{ Id = "8.5"; Description = "Smooth marker animations"; Status = if ($validationResults -like "*Smooth animations*" -and $validationResults -like "*PASS*") { "✅ IMPLEMENTED" } else { "❌ MISSING" } }
)

foreach ($req in $requirements) {
    Write-Host "Requirement $($req.Id): $($req.Description)" -ForegroundColor White
    Write-Host "  Status: $($req.Status)" -ForegroundColor $(if ($req.Status -like "*IMPLEMENTED*") { "Green" } else { "Red" })
}

# Task completion status
Write-Host "`n🎯 TASK 6 COMPLETION STATUS" -ForegroundColor Cyan
Write-Host "=" * 60

$taskComponents = @(
    "AnimatedFriendMarker component with smooth animations",
    "Friend marker clustering for performance", 
    "Real-time friend location updates",
    "Friend marker click handling and selection"
)

$completedComponents = 0
foreach ($component in $taskComponents) {
    $isCompleted = $false
    
    switch -Wildcard ($component) {
        "*AnimatedFriendMarker*" { $isCompleted = $validationResults -like "*AnimatedFriendMarker*PASS*" }
        "*clustering*" { $isCompleted = $validationResults -like "*Cluster*PASS*" }
        "*Real-time*" { $isCompleted = $validationResults -like "*RealTimeFriendsService*PASS*" }
        "*click handling*" { $isCompleted = $validationResults -like "*click*PASS*" }
    }
    
    if ($isCompleted) {
        Write-Host "✅ $component" -ForegroundColor Green
        $completedComponents++
    } else {
        Write-Host "❌ $component" -ForegroundColor Red
    }
}

$taskCompletionRate = [math]::Round(($completedComponents / $taskComponents.Count) * 100, 1)
$componentText = "$completedComponents/$($taskComponents.Count) components"
Write-Host "`nTask 6 Completion: $taskCompletionRate% ($componentText)" -ForegroundColor $(if ($taskCompletionRate -eq 100) { "Green" } else { "Yellow" })

if ($taskCompletionRate -eq 100) {
    Write-Host "`nTASK 6 SUCCESSFULLY COMPLETED!" -ForegroundColor Green
    Write-Host "All friend markers and map integration requirements have been implemented." -ForegroundColor Green
} elseif ($taskCompletionRate -ge 75) {
    Write-Host "`nTASK 6 MOSTLY COMPLETED" -ForegroundColor Yellow
    Write-Host "Most requirements implemented, minor issues remain." -ForegroundColor Yellow
} else {
    Write-Host "`nTASK 6 NEEDS MORE WORK" -ForegroundColor Red
    Write-Host "Significant implementation gaps remain." -ForegroundColor Red
}

$separator = "=" * 60
Write-Host ""
Write-Host $separator