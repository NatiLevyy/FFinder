#!/usr/bin/env pwsh

Write-Host "🔍 Verifying Task 2: FriendsPanelScaffold Integration" -ForegroundColor Cyan
Write-Host "=" * 60

# Test compilation
Write-Host "✅ Test 1: Compilation Check" -ForegroundColor Green
Set-Location android
./gradlew :app:compileDebugKotlin --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✓ Code compiles successfully" -ForegroundColor Green
} else {
    Write-Host "   ✗ Compilation failed" -ForegroundColor Red
}
Set-Location ..

# Check FriendsToggleFAB updates
Write-Host "✅ Test 2: FriendsToggleFAB Updates" -ForegroundColor Green
$fabContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/friends/components/FriendsToggleFAB.kt" -Raw

if ($fabContent -match "isPanelOpen: Boolean = false") {
    Write-Host "   ✓ isPanelOpen parameter added" -ForegroundColor Green
} else {
    Write-Host "   ✗ isPanelOpen parameter missing" -ForegroundColor Red
}

if ($fabContent -match "if \(isPanelOpen\)") {
    Write-Host "   ✓ Content description logic updated" -ForegroundColor Green
} else {
    Write-Host "   ✗ Content description logic not updated" -ForegroundColor Red
}

# Check FriendsPanelScaffold integration
Write-Host "✅ Test 3: FriendsPanelScaffold Integration" -ForegroundColor Green
$scaffoldContent = Get-Content "android/app/src/main/java/com/locationsharing/app/ui/friends/components/FriendsPanelScaffold.kt" -Raw

if ($scaffoldContent -match "isPanelOpen = uiState\.isPanelOpen") {
    Write-Host "   ✓ isPanelOpen parameter passed" -ForegroundColor Green
} else {
    Write-Host "   ✗ isPanelOpen parameter not passed" -ForegroundColor Red
}

if ($scaffoldContent -match "friendCount = uiState\.friends\.size") {
    Write-Host "   ✓ Friend count passed from uiState" -ForegroundColor Green
} else {
    Write-Host "   ✗ Friend count not passed correctly" -ForegroundColor Red
}

if ($scaffoldContent -match "\.align\(Alignment\.TopEnd\)") {
    Write-Host "   ✓ FAB positioned in top-right corner" -ForegroundColor Green
} else {
    Write-Host "   ✗ FAB positioning incorrect" -ForegroundColor Red
}

if ($scaffoldContent -match "onEvent\(NearbyPanelEvent\.TogglePanel\)") {
    Write-Host "   ✓ onClick behavior maintained" -ForegroundColor Green
} else {
    Write-Host "   ✗ onClick behavior not correct" -ForegroundColor Red
}

Write-Host ""
Write-Host "🎉 Task 2 verification complete!" -ForegroundColor Green