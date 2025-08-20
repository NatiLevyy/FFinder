#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Validates the location sharing status management implementation
.DESCRIPTION
    This script validates that task 8 (location sharing status management) has been properly implemented
    according to requirements 3.3, 5.6, 5.7
#>

param(
    [switch]$Verbose = $false
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "Validating Location Sharing Status Management Implementation" -ForegroundColor Cyan
Write-Host ("=" * 70)

# Define validation results
$script:validationResults = @()

function Add-ValidationResult {
    param(
        [string]$Test,
        [bool]$Passed,
        [string]$Details = ""
    )
    
    $script:validationResults += [PSCustomObject]@{
        Test = $Test
        Passed = $Passed
        Details = $Details
    }
    
    $status = if ($Passed) { "PASS" } else { "FAIL" }
    $color = if ($Passed) { "Green" } else { "Red" }
    
    Write-Host "$status - $Test" -ForegroundColor $color
    if ($Details -and ($Verbose -or -not $Passed)) {
        Write-Host "    $Details" -ForegroundColor Gray
    }
}

# Test 1: LocationSharingService exists and has required methods
Write-Host "`nTesting LocationSharingService Implementation..." -ForegroundColor Yellow

$locationSharingServicePath = "android/app/src/main/java/com/locationsharing/app/data/location/LocationSharingService.kt"
if (Test-Path $locationSharingServicePath) {
    $serviceContent = Get-Content $locationSharingServicePath -Raw
    
    # Check for required methods
    $requiredMethods = @(
        "startLocationSharing",
        "stopLocationSharing", 
        "toggleLocationSharing",
        "retryLocationSharing",
        "getStatusText",
        "isLocationSharingActive",
        "canToggleSharing"
    )
    
    $methodsFound = 0
    foreach ($method in $requiredMethods) {
        if ($serviceContent -match "fun $method") {
            $methodsFound++
        }
    }
    
    Add-ValidationResult "LocationSharingService has all required methods" ($methodsFound -eq $requiredMethods.Count) "Found $methodsFound of $($requiredMethods.Count) methods"
    
    # Check for state management
    $hasStateFlow = $serviceContent -match "StateFlow.*LocationSharingState"
    Add-ValidationResult "LocationSharingService has state management" $hasStateFlow "StateFlow for LocationSharingState"
    
    # Check for notifications
    $hasNotifications = $serviceContent -match "StateFlow.*LocationSharingNotification"
    Add-ValidationResult "LocationSharingService has notification system" $hasNotifications "StateFlow for notifications"
    
    # Check for error handling
    $hasErrorHandling = $serviceContent -match "handleSharingError"
    Add-ValidationResult "LocationSharingService has error handling" $hasErrorHandling "Error handling method present"
    
} else {
    Add-ValidationResult "LocationSharingService file exists" $false "File not found: $locationSharingServicePath"
}

# Test 2: FriendsRepository has location sharing methods
Write-Host "`nTesting FriendsRepository Integration..." -ForegroundColor Yellow

$friendsRepoPath = "android/app/src/main/java/com/locationsharing/app/data/friends/FriendsRepository.kt"
if (Test-Path $friendsRepoPath) {
    $repoContent = Get-Content $friendsRepoPath -Raw
    
    $hasStartMethod = $repoContent -match "suspend fun startLocationSharing"
    Add-ValidationResult "FriendsRepository has startLocationSharing method" $hasStartMethod "Interface method defined"
    
    $hasStopMethod = $repoContent -match "suspend fun stopLocationSharing"
    Add-ValidationResult "FriendsRepository has stopLocationSharing method" $hasStopMethod "Interface method defined"
    
} else {
    Add-ValidationResult "FriendsRepository file exists" $false "File not found: $friendsRepoPath"
}

# Test 3: MapScreenViewModel integration
Write-Host "`nTesting MapScreenViewModel Integration..." -ForegroundColor Yellow

$viewModelPath = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenViewModel.kt"
if (Test-Path $viewModelPath) {
    $viewModelContent = Get-Content $viewModelPath -Raw
    
    # Check for LocationSharingService injection
    $hasServiceInjection = $viewModelContent -match "locationSharingService.*LocationSharingService"
    Add-ValidationResult "MapScreenViewModel injects LocationSharingService" $hasServiceInjection "Service dependency injected"
    
    # Check for state observation
    $hasStateObservation = $viewModelContent -match "observeLocationSharingState"
    Add-ValidationResult "MapScreenViewModel observes location sharing state" $hasStateObservation "State observation method present"
    
    # Check for event handlers
    $hasStartHandler = $viewModelContent -match "handleStartLocationSharing"
    Add-ValidationResult "MapScreenViewModel has start sharing handler" $hasStartHandler "Start handler method present"
    
    $hasStopHandler = $viewModelContent -match "handleStopLocationSharing"
    Add-ValidationResult "MapScreenViewModel has stop sharing handler" $hasStopHandler "Stop handler method present"
    
    # Check for service method calls
    $callsService = $viewModelContent -match "locationSharingService\.(start|stop|toggle)"
    Add-ValidationResult "MapScreenViewModel calls LocationSharingService methods" $callsService "Service method calls present"
    
} else {
    Add-ValidationResult "MapScreenViewModel file exists" $false "File not found: $viewModelPath"
}

# Test 4: MapScreenState integration
Write-Host "`nTesting MapScreenState Integration..." -ForegroundColor Yellow

$statePath = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenState.kt"
if (Test-Path $statePath) {
    $stateContent = Get-Content $statePath -Raw
    
    # Check for location sharing fields
    $hasActiveField = $stateContent -match "isLocationSharingActive.*Boolean"
    Add-ValidationResult "MapScreenState has isLocationSharingActive field" $hasActiveField "Boolean field for sharing status"
    
    $hasErrorField = $stateContent -match "locationSharingError.*String"
    Add-ValidationResult "MapScreenState has locationSharingError field" $hasErrorField "String field for sharing errors"
    
    # Check for status text method
    $hasStatusText = $stateContent -match "locationSharingStatusText.*String"
    Add-ValidationResult "MapScreenState has status text property" $hasStatusText "Status text property present"
    
    # Check for coordinates text
    $hasCoordinatesText = $stateContent -match "coordinatesText.*String"
    Add-ValidationResult "MapScreenState has coordinates text property" $hasCoordinatesText "Coordinates text property present"
    
} else {
    Add-ValidationResult "MapScreenState file exists" $false "File not found: $statePath"
}

# Test 5: Event handling
Write-Host "`nTesting Event Handling..." -ForegroundColor Yellow

$eventPath = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenEvent.kt"
if (Test-Path $eventPath) {
    $eventContent = Get-Content $eventPath -Raw
    
    # Check for location sharing events
    $hasQuickShareEvent = $eventContent -match "OnQuickShare"
    Add-ValidationResult "MapScreenEvent has OnQuickShare event" $hasQuickShareEvent "Quick share event defined"
    
    $hasStartEvent = $eventContent -match "OnStartLocationSharing"
    Add-ValidationResult "MapScreenEvent has OnStartLocationSharing event" $hasStartEvent "Start sharing event defined"
    
    $hasStopEvent = $eventContent -match "OnStopLocationSharing"
    Add-ValidationResult "MapScreenEvent has OnStopLocationSharing event" $hasStopEvent "Stop sharing event defined"
    
    $hasStatusSheetEvents = $eventContent -match "OnStatusSheet(Show|Dismiss)"
    Add-ValidationResult "MapScreenEvent has status sheet events" $hasStatusSheetEvents "Status sheet events defined"
    
} else {
    Add-ValidationResult "MapScreenEvent file exists" $false "File not found: $eventPath"
}

# Test 6: Compilation check
Write-Host "`nTesting Compilation..." -ForegroundColor Yellow

try {
    Push-Location "android"
    $compileResult = & ./gradlew compileDebugKotlin --quiet 2>&1
    $compileSuccess = $LASTEXITCODE -eq 0
    Add-ValidationResult "Code compiles successfully" $compileSuccess "Kotlin compilation check"
} catch {
    Add-ValidationResult "Code compiles successfully" $false "Compilation error: $($_.Exception.Message)"
} finally {
    Pop-Location
}

# Test 7: Test files exist
Write-Host "`nTesting Test Coverage..." -ForegroundColor Yellow

$serviceTestPath = "android/app/src/test/java/com/locationsharing/app/data/location/LocationSharingServiceTest.kt"
$hasServiceTests = Test-Path $serviceTestPath
Add-ValidationResult "LocationSharingService unit tests exist" $hasServiceTests "Test file: $serviceTestPath"

$viewModelTestPath = "android/app/src/test/java/com/locationsharing/app/ui/map/MapScreenViewModelLocationSharingTest.kt"
$hasViewModelTests = Test-Path $viewModelTestPath
Add-ValidationResult "MapScreenViewModel location sharing tests exist" $hasViewModelTests "Test file: $viewModelTestPath"

# Summary
Write-Host "`nValidation Summary" -ForegroundColor Cyan
Write-Host ("=" * 50)

$totalTests = $script:validationResults.Count
$passedTests = ($script:validationResults | Where-Object { $_.Passed }).Count
$failedTests = $totalTests - $passedTests

Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $failedTests" -ForegroundColor Red

$successRate = [math]::Round(($passedTests / $totalTests) * 100, 1)
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } else { "Yellow" })

# Requirements validation
Write-Host "`nRequirements Validation" -ForegroundColor Cyan
Write-Host ("=" * 30)

$requirements = @(
    @{ Id = "3.3"; Description = "Location sharing toggle functionality"; Met = $hasServiceInjection -and $hasStartHandler -and $hasStopHandler }
    @{ Id = "5.6"; Description = "Location sharing status management"; Met = $hasActiveField -and $hasErrorField -and $hasStatusText }
    @{ Id = "5.7"; Description = "Status change notifications and feedback"; Met = $hasNotifications -and $hasErrorHandling }
)

foreach ($req in $requirements) {
    $status = if ($req.Met) { "MET" } else { "NOT MET" }
    $color = if ($req.Met) { "Green" } else { "Red" }
    Write-Host "$status - Requirement $($req.Id): $($req.Description)" -ForegroundColor $color
}

# Final result
if ($failedTests -eq 0) {
    Write-Host "`nAll validations passed! Location sharing status management is properly implemented." -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nSome validations failed. Please review the implementation." -ForegroundColor Yellow
    
    if ($Verbose) {
        Write-Host "`nFailed Tests:" -ForegroundColor Red
        $script:validationResults | Where-Object { -not $_.Passed } | ForEach-Object {
            Write-Host "  - $($_.Test): $($_.Details)" -ForegroundColor Red
        }
    }
    
    exit 1
}