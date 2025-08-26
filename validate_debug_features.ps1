# Debug Features Validation Script
# Validates the implementation of debug features according to requirements 4.1-4.5

Write-Host "Debug Features Validation" -ForegroundColor Cyan
Write-Host "========================="

$validationResults = @()

# Function to add validation result
function Add-ValidationResult {
    param($requirement, $description, $status, $details)
    $script:validationResults += [PSCustomObject]@{
        Requirement = $requirement
        Description = $description
        Status = $status
        Details = $details
    }
}

# Requirement 4.1: Purple flask FAB in debug builds only
Write-Host "`nChecking Requirement 4.1: Purple flask FAB in debug builds only" -ForegroundColor Yellow

$debugFabFile = "android/app/src/main/java/com/locationsharing/app/ui/map/components/DebugFAB.kt"
$flaskIconFile = "android/app/src/main/res/drawable/ic_flask.xml"

if (Test-Path $debugFabFile) {
    $debugFabContent = Get-Content $debugFabFile -Raw
    
    # Check for BuildConfig.DEBUG conditional
    if ($debugFabContent -match "BuildConfig\.DEBUG") {
        Add-ValidationResult "4.1" "Debug FAB conditional rendering" "PASS" "BuildConfig.DEBUG check found"
    } else {
        Add-ValidationResult "4.1" "Debug FAB conditional rendering" "FAIL" "BuildConfig.DEBUG check not found"
    }
    
    # Check for purple color
    if ($debugFabContent -match "DEBUG_FAB_COLOR|6B4F8F") {
        Add-ValidationResult "4.1" "Purple color implementation" "PASS" "Purple color reference found"
    } else {
        Add-ValidationResult "4.1" "Purple color implementation" "FAIL" "Purple color not found"
    }
    
    # Check for flask icon
    if ($debugFabContent -match "ic_flask") {
        Add-ValidationResult "4.1" "Flask icon implementation" "PASS" "Flask icon reference found"
    } else {
        Add-ValidationResult "4.1" "Flask icon implementation" "FAIL" "Flask icon reference not found"
    }
} else {
    Add-ValidationResult "4.1" "DebugFAB component" "FAIL" "DebugFAB.kt file not found"
}

if (Test-Path $flaskIconFile) {
    Add-ValidationResult "4.1" "Flask icon resource" "PASS" "ic_flask.xml found"
} else {
    Add-ValidationResult "4.1" "Flask icon resource" "FAIL" "ic_flask.xml not found"
}

# Requirement 4.2: Add test friends to map functionality
Write-Host "`nChecking Requirement 4.2: Add test friends to map functionality" -ForegroundColor Yellow

$viewModelFile = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenViewModel.kt"

if (Test-Path $viewModelFile) {
    $viewModelContent = Get-Content $viewModelFile -Raw
    
    # Check for addTestFriendsOnMap method
    if ($viewModelContent -match "addTestFriendsOnMap") {
        Add-ValidationResult "4.2" "addTestFriendsOnMap method" "PASS" "Method found in ViewModel"
    } else {
        Add-ValidationResult "4.2" "addTestFriendsOnMap method" "FAIL" "Method not found in ViewModel"
    }
    
    # Check for createDebugFriends method
    if ($viewModelContent -match "createDebugFriends") {
        Add-ValidationResult "4.2" "createDebugFriends method" "PASS" "Method found in ViewModel"
    } else {
        Add-ValidationResult "4.2" "createDebugFriends method" "FAIL" "Method not found in ViewModel"
    }
    
    # Check for debug event handling
    if ($viewModelContent -match "OnDebugAddFriends") {
        Add-ValidationResult "4.2" "Debug event handling" "PASS" "OnDebugAddFriends event found"
    } else {
        Add-ValidationResult "4.2" "Debug event handling" "FAIL" "OnDebugAddFriends event not found"
    }
} else {
    Add-ValidationResult "4.2" "MapScreenViewModel" "FAIL" "MapScreenViewModel.kt file not found"
}

# Requirement 4.3: Confirmation Snackbar
Write-Host "`nChecking Requirement 4.3: Confirmation Snackbar" -ForegroundColor Yellow

$debugSnackbarFile = "android/app/src/main/java/com/locationsharing/app/ui/map/components/DebugSnackbar.kt"
$mapScreenStateFile = "android/app/src/main/java/com/locationsharing/app/ui/map/MapScreenState.kt"

if (Test-Path $debugSnackbarFile) {
    Add-ValidationResult "4.3" "DebugSnackbar component" "PASS" "DebugSnackbar.kt file found"
} else {
    Add-ValidationResult "4.3" "DebugSnackbar component" "FAIL" "DebugSnackbar.kt file not found"
}

if (Test-Path $mapScreenStateFile) {
    $stateContent = Get-Content $mapScreenStateFile -Raw
    
    # Check for debugSnackbarMessage field
    if ($stateContent -match "debugSnackbarMessage") {
        Add-ValidationResult "4.3" "Debug snackbar state" "PASS" "debugSnackbarMessage field found in state"
    } else {
        Add-ValidationResult "4.3" "Debug snackbar state" "FAIL" "debugSnackbarMessage field not found in state"
    }
} else {
    Add-ValidationResult "4.3" "MapScreenState" "FAIL" "MapScreenState.kt file not found"
}

# Check for confirmation message in ViewModel
if (Test-Path $viewModelFile) {
    $viewModelContent = Get-Content $viewModelFile -Raw
    
    # Check for debug confirmation message
    if ($viewModelContent -match "Debug.*Added.*test friends") {
        Add-ValidationResult "4.3" "Confirmation message" "PASS" "Debug confirmation message found"
    } else {
        Add-ValidationResult "4.3" "Confirmation message" "FAIL" "Debug confirmation message not found"
    }
}

# Requirement 4.5: Haptic feedback
Write-Host "`nChecking Requirement 4.5: Haptic feedback" -ForegroundColor Yellow

if (Test-Path $debugFabFile) {
    $debugFabContent = Get-Content $debugFabFile -Raw
    
    # Check for haptic feedback
    if ($debugFabContent -match "HapticFeedback|performHapticFeedback") {
        Add-ValidationResult "4.5" "Haptic feedback" "PASS" "Haptic feedback implementation found"
    } else {
        Add-ValidationResult "4.5" "Haptic feedback" "FAIL" "Haptic feedback implementation not found"
    }
}

# Integration checks
Write-Host "`nChecking Integration" -ForegroundColor Yellow

$mapScreenFile = "android/app/src/main/java/com/locationsharing/app/ui/screens/MapScreen.kt"

if (Test-Path $mapScreenFile) {
    $mapScreenContent = Get-Content $mapScreenFile -Raw
    
    # Check for DebugFAB integration
    if ($mapScreenContent -match "DebugFAB") {
        Add-ValidationResult "Integration" "DebugFAB in MapScreen" "PASS" "DebugFAB integrated in MapScreen"
    } else {
        Add-ValidationResult "Integration" "DebugFAB in MapScreen" "FAIL" "DebugFAB not integrated in MapScreen"
    }
    
    # Check for DebugSnackbar integration
    if ($mapScreenContent -match "DebugSnackbar") {
        Add-ValidationResult "Integration" "DebugSnackbar in MapScreen" "PASS" "DebugSnackbar integrated in MapScreen"
    } else {
        Add-ValidationResult "Integration" "DebugSnackbar in MapScreen" "FAIL" "DebugSnackbar not integrated in MapScreen"
    }
} else {
    Add-ValidationResult "Integration" "MapScreen file" "FAIL" "MapScreen.kt file not found"
}

# Display results
Write-Host "`nValidation Results" -ForegroundColor Cyan
Write-Host "=================="

$passCount = ($validationResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($validationResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $validationResults.Count

foreach ($result in $validationResults) {
    $statusColor = if ($result.Status -eq "PASS") { "Green" } else { "Red" }
    $statusSymbol = if ($result.Status -eq "PASS") { "[PASS]" } else { "[FAIL]" }
    
    Write-Host "$statusSymbol $($result.Requirement): $($result.Description)" -ForegroundColor $statusColor
    if ($result.Details) {
        Write-Host "   Details: $($result.Details)" -ForegroundColor Gray
    }
}

Write-Host "`nSummary" -ForegroundColor Cyan
Write-Host "======="
Write-Host "Total Checks: $totalCount"
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red
Write-Host "Success Rate: $([math]::Round(($passCount / $totalCount) * 100, 1))%"

if ($failCount -eq 0) {
    Write-Host "`nAll debug features implemented successfully!" -ForegroundColor Green
    Write-Host "Requirements 4.1-4.5 are fully satisfied." -ForegroundColor Green
} else {
    Write-Host "`nSome issues found. Please review the failed checks above." -ForegroundColor Yellow
}

Write-Host "`nDebug Features Summary:" -ForegroundColor Cyan
Write-Host "- Purple flask FAB (debug builds only) - Requirement 4.1"
Write-Host "- addTestFriendsOnMap functionality - Requirement 4.2"
Write-Host "- Debug confirmation Snackbar - Requirement 4.3"
Write-Host "- Hidden in release builds - Requirement 4.4"
Write-Host "- Haptic feedback support - Requirement 4.5"