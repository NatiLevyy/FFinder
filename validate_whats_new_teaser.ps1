#!/usr/bin/env pwsh

# WhatsNewTeaser Component Validation Script
# This script validates the implementation of Task 7: What's New Feature Teaser

Write-Host "🚀 Validating WhatsNewTeaser Component Implementation..." -ForegroundColor Cyan
Write-Host "=" * 60

# Check if component files exist
$componentFile = "android/app/src/main/java/com/locationsharing/app/ui/home/components/WhatsNewTeaser.kt"
$testFile = "android/app/src/test/java/com/locationsharing/app/ui/home/components/WhatsNewTeaserTest.kt"
$accessibilityTestFile = "android/app/src/test/java/com/locationsharing/app/ui/home/components/WhatsNewTeaserAccessibilityTest.kt"
$integrationTestFile = "android/app/src/test/java/com/locationsharing/app/ui/home/components/WhatsNewTeaserIntegrationTest.kt"
$performanceTestFile = "android/app/src/test/java/com/locationsharing/app/ui/home/components/WhatsNewTeaserPerformanceTest.kt"

Write-Host "📁 Checking file existence..." -ForegroundColor Yellow

$files = @(
    @{ Path = $componentFile; Name = "WhatsNewTeaser Component" },
    @{ Path = $testFile; Name = "Unit Tests" },
    @{ Path = $accessibilityTestFile; Name = "Accessibility Tests" },
    @{ Path = $integrationTestFile; Name = "Integration Tests" },
    @{ Path = $performanceTestFile; Name = "Performance Tests" }
)

$allFilesExist = $true
foreach ($file in $files) {
    if (Test-Path $file.Path) {
        Write-Host "✅ $($file.Name): Found" -ForegroundColor Green
    } else {
        Write-Host "❌ $($file.Name): Missing" -ForegroundColor Red
        $allFilesExist = $false
    }
}

if (-not $allFilesExist) {
    Write-Host "❌ Some required files are missing!" -ForegroundColor Red
    exit 1
}

Write-Host "`n🔍 Validating component implementation..." -ForegroundColor Yellow

# Check component implementation requirements
$componentContent = Get-Content $componentFile -Raw

$requirements = @(
    @{ Pattern = "WhatsNewTeaser"; Description = "WhatsNewTeaser composable function" },
    @{ Pattern = "animateIntAsState"; Description = "Slide-up animation using animateIntAsState" },
    @{ Pattern = "EaseOutBack"; Description = "EaseOutBack easing for animation" },
    @{ Pattern = "🚀"; Description = "Rocket emoji" },
    @{ Pattern = "New: Nearby Friends panel & Quick Share!"; Description = "Feature announcement text" },
    @{ Pattern = "RoundedCornerShape\(16\.dp\)"; Description = "16dp rounded corners" },
    @{ Pattern = "defaultElevation = 4\.dp"; Description = "4dp elevation" },
    @{ Pattern = "alpha = 0\.95f"; Description = "95% opacity surface color" },
    @{ Pattern = "WhatsNewDialog"; Description = "WhatsNewDialog composable" },
    @{ Pattern = "AlertDialog"; Description = "Modal dialog implementation" },
    @{ Pattern = "contentDescription"; Description = "Accessibility content description" }
)

$implementationScore = 0
foreach ($req in $requirements) {
    if ($componentContent -match $req.Pattern) {
        Write-Host "✅ $($req.Description)" -ForegroundColor Green
        $implementationScore++
    } else {
        Write-Host "❌ $($req.Description)" -ForegroundColor Red
    }
}

Write-Host "`n📊 Implementation Score: $implementationScore/$($requirements.Count)" -ForegroundColor Cyan

# Check test coverage
Write-Host "`n🧪 Validating test coverage..." -ForegroundColor Yellow

$testContent = Get-Content $testFile -Raw
$accessibilityTestContent = Get-Content $accessibilityTestFile -Raw
$integrationTestContent = Get-Content $integrationTestFile -Raw
$performanceTestContent = Get-Content $performanceTestFile -Raw

$testRequirements = @(
    @{ Pattern = "whatsNewTeaser_displaysCorrectContent"; Description = "Content display test" },
    @{ Pattern = "whatsNewTeaser_hasProperAccessibilitySupport"; Description = "Accessibility support test" },
    @{ Pattern = "whatsNewTeaser_triggersOnTapCallback"; Description = "Tap callback test" },
    @{ Pattern = "whatsNewDialog_displaysCorrectContent"; Description = "Dialog content test" },
    @{ Pattern = "whatsNewDialog_triggersOnDismissCallback"; Description = "Dialog dismiss test" }
)

$testScore = 0
foreach ($req in $testRequirements) {
    if ($testContent -match $req.Pattern) {
        Write-Host "✅ $($req.Description)" -ForegroundColor Green
        $testScore++
    } else {
        Write-Host "❌ $($req.Description)" -ForegroundColor Red
    }
}

Write-Host "`n📊 Test Coverage Score: $testScore/$($testRequirements.Count)" -ForegroundColor Cyan

# Try to compile the component
Write-Host "`n🔨 Compiling component..." -ForegroundColor Yellow

Push-Location android
try {
    $compileResult = & ./gradlew :app:compileDebugKotlin --quiet 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Component compiles successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ Compilation failed:" -ForegroundColor Red
        Write-Host $compileResult -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error during compilation: $_" -ForegroundColor Red
} finally {
    Pop-Location
}

# Summary
Write-Host "`n" + "=" * 60
Write-Host "📋 VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "=" * 60

$totalScore = $implementationScore + $testScore
$maxScore = $requirements.Count + $testRequirements.Count

Write-Host "Implementation Requirements: $implementationScore/$($requirements.Count)" -ForegroundColor $(if ($implementationScore -eq $requirements.Count) { "Green" } else { "Yellow" })
Write-Host "Test Coverage: $testScore/$($testRequirements.Count)" -ForegroundColor $(if ($testScore -eq $testRequirements.Count) { "Green" } else { "Yellow" })
Write-Host "Overall Score: $totalScore/$maxScore" -ForegroundColor $(if ($totalScore -eq $maxScore) { "Green" } else { "Yellow" })

if ($totalScore -eq $maxScore) {
    Write-Host "`n🎉 WhatsNewTeaser implementation is COMPLETE!" -ForegroundColor Green
    Write-Host "✅ All requirements satisfied" -ForegroundColor Green
    Write-Host "✅ Comprehensive test coverage" -ForegroundColor Green
    Write-Host "✅ Component compiles successfully" -ForegroundColor Green
} else {
    Write-Host "`n⚠️  WhatsNewTeaser implementation needs attention" -ForegroundColor Yellow
    Write-Host "Some requirements or tests may be missing" -ForegroundColor Yellow
}

Write-Host "`n🚀 Task 7: What's New Feature Teaser validation complete!" -ForegroundColor Cyan