#!/usr/bin/env pwsh

Write-Host "Testing Core Fixes Build" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan

Set-Location android

Write-Host "`nTesting compilation of core fixed files..." -ForegroundColor Yellow

# Test individual file compilation
$coreFiles = @(
    "app/src/main/java/com/locationsharing/app/navigation/Screen.kt",
    "app/src/main/java/com/locationsharing/app/navigation/NavigationAnalytics.kt", 
    "app/src/main/java/com/locationsharing/app/navigation/NavigationError.kt",
    "app/src/main/java/com/locationsharing/app/domain/usecase/ShareLocationUseCase.kt",
    "app/src/main/java/com/locationsharing/app/ui/map/integration/IntegratedMapScreen.kt"
)

foreach ($file in $coreFiles) {
    if (Test-Path $file) {
        Write-Host "✅ $file exists" -ForegroundColor Green
    } else {
        Write-Host "❌ $file missing" -ForegroundColor Red
    }
}

Write-Host "`nTesting Gradle build..." -ForegroundColor Yellow
try {
    $result = ./gradlew compileDebugKotlin --quiet 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Kotlin compilation successful" -ForegroundColor Green
    } else {
        Write-Host "❌ Kotlin compilation failed" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Build error: $_" -ForegroundColor Red
}

Set-Location ..
Write-Host "`nCore fixes build test complete!" -ForegroundColor Cyan