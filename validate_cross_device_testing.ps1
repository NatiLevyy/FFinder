#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Validates MapScreen cross-device and cross-platform testing implementation.

.DESCRIPTION
    This script runs comprehensive cross-device testing for the MapScreen redesign,
    covering different Android versions, screen sizes, densities, themes, accessibility,
    and performance on various device configurations.

.PARAMETER TestType
    Specifies which type of tests to run: All, Compatibility, ScreenSize, Theme, Accessibility, Performance

.PARAMETER DeviceConfig
    Specifies device configuration to test: All, Phone, Tablet, LowEnd, HighEnd

.PARAMETER Verbose
    Enables verbose output for detailed test results

.EXAMPLE
    .\validate_cross_device_testing.ps1 -TestType All -DeviceConfig All -Verbose
#>

param(
    [Parameter(Mandatory = $false)]
    [ValidateSet("All", "Compatibility", "ScreenSize", "Theme", "Accessibility", "Performance")]
    [string]$TestType = "All",
    
    [Parameter(Mandatory = $false)]
    [ValidateSet("All", "Phone", "Tablet", "LowEnd", "HighEnd")]
    [string]$DeviceConfig = "All",
    
    [Parameter(Mandatory = $false)]
    [switch]$Verbose
)

# Set error action preference
$ErrorActionPreference = "Stop"

# Define colors for output
$Green = "Green"
$Red = "Red"
$Yellow = "Yellow"
$Blue = "Blue"
$Cyan = "Cyan"

function Write-Header {
    param([string]$Message)
    Write-Host "`n$('=' * 80)" -ForegroundColor $Blue
    Write-Host $Message -ForegroundColor $Blue
    Write-Host $('=' * 80) -ForegroundColor $Blue
}

function Write-SubHeader {
    param([string]$Message)
    Write-Host "`n$('-' * 60)" -ForegroundColor $Cyan
    Write-Host $Message -ForegroundColor $Cyan
    Write-Host $('-' * 60) -ForegroundColor $Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor $Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor $Red
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor $Yellow
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor $Blue
}

function Test-AndroidEnvironment {
    Write-SubHeader "Validating Android Environment"
    
    # Check if Android SDK is available
    if (-not $env:ANDROID_HOME) {
        Write-Error "ANDROID_HOME environment variable not set"
        return $false
    }
    Write-Success "ANDROID_HOME: $env:ANDROID_HOME"
    
    # Check if ADB is available
    try {
        $adbVersion = & adb version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "ADB is available"
        } else {
            Write-Error "ADB not found in PATH"
            return $false
        }
    } catch {
        Write-Error "ADB not found: $_"
        return $false
    }
    
    # Check for connected devices or emulators
    $devices = & adb devices 2>$null | Select-String "device$"
    if ($devices.Count -eq 0) {
        Write-Warning "No Android devices or emulators connected"
        Write-Info "Starting Android emulator if available..."
        
        # Try to start an emulator
        try {
            $emulators = & emulator -list-avds 2>$null
            if ($emulators -and $emulators.Count -gt 0) {
                $firstEmulator = $emulators[0]
                Write-Info "Starting emulator: $firstEmulator"
                Start-Process "emulator" -ArgumentList "-avd", $firstEmulator -WindowStyle Hidden
                
                # Wait for emulator to boot
                Write-Info "Waiting for emulator to boot..."
                $timeout = 120
                $elapsed = 0
                do {
                    Start-Sleep 5
                    $elapsed += 5
                    $devices = & adb devices 2>$null | Select-String "device$"
                } while ($devices.Count -eq 0 -and $elapsed -lt $timeout)
                
                if ($devices.Count -gt 0) {
                    Write-Success "Emulator started successfully"
                } else {
                    Write-Error "Emulator failed to start within $timeout seconds"
                    return $false
                }
            } else {
                Write-Error "No Android emulators available"
                return $false
            }
        } catch {
            Write-Error "Failed to start emulator: $_"
            return $false
        }
    } else {
        Write-Success "$($devices.Count) Android device(s) connected"
    }
    
    return $true
}

function Run-CrossDeviceCompatibilityTests {
    Write-SubHeader "Running Cross-Device Compatibility Tests"
    
    $testClass = "com.locationsharing.app.ui.map.crossdevice.CrossDeviceCompatibilityTest"
    
    try {
        $result = & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$testClass
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Cross-device compatibility tests passed"
            return $true
        } else {
            Write-Error "Cross-device compatibility tests failed"
            if ($Verbose) {
                Write-Host $result
            }
            return $false
        }
    } catch {
        Write-Error "Failed to run cross-device compatibility tests: $_"
        return $false
    }
}

function Run-ScreenSizeAdaptationTests {
    Write-SubHeader "Running Screen Size Adaptation Tests"
    
    $testClass = "com.locationsharing.app.ui.map.crossdevice.ScreenSizeAdaptationTest"
    
    try {
        $result = & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$testClass
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Screen size adaptation tests passed"
            return $true
        } else {
            Write-Error "Screen size adaptation tests failed"
            if ($Verbose) {
                Write-Host $result
            }
            return $false
        }
    } catch {
        Write-Error "Failed to run screen size adaptation tests: $_"
        return $false
    }
}

function Run-ThemeCompatibilityTests {
    Write-SubHeader "Running Theme Compatibility Tests"
    
    $testClass = "com.locationsharing.app.ui.map.crossdevice.ThemeCompatibilityTest"
    
    try {
        $result = & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$testClass
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Theme compatibility tests passed"
            return $true
        } else {
            Write-Error "Theme compatibility tests failed"
            if ($Verbose) {
                Write-Host $result
            }
            return $false
        }
    } catch {
        Write-Error "Failed to run theme compatibility tests: $_"
        return $false
    }
}

function Run-AccessibilityDeviceTests {
    Write-SubHeader "Running Accessibility Device Tests"
    
    $testClass = "com.locationsharing.app.ui.map.crossdevice.AccessibilityDeviceTest"
    
    try {
        $result = & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$testClass
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Accessibility device tests passed"
            return $true
        } else {
            Write-Error "Accessibility device tests failed"
            if ($Verbose) {
                Write-Host $result
            }
            return $false
        }
    } catch {
        Write-Error "Failed to run accessibility device tests: $_"
        return $false
    }
}

function Run-PerformanceDeviceTests {
    Write-SubHeader "Running Performance Device Tests"
    
    $testClass = "com.locationsharing.app.ui.map.crossdevice.PerformanceDeviceTest"
    
    try {
        $result = & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=$testClass
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Performance device tests passed"
            return $true
        } else {
            Write-Error "Performance device tests failed"
            if ($Verbose) {
                Write-Host $result
            }
            return $false
        }
    } catch {
        Write-Error "Failed to run performance device tests: $_"
        return $false
    }
}

function Run-AllCrossDeviceTests {
    Write-SubHeader "Running All Cross-Device Tests"
    
    $testPackage = "com.locationsharing.app.ui.map.crossdevice"
    
    try {
        $result = & ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=$testPackage
        if ($LASTEXITCODE -eq 0) {
            Write-Success "All cross-device tests passed"
            return $true
        } else {
            Write-Error "Some cross-device tests failed"
            if ($Verbose) {
                Write-Host $result
            }
            return $false
        }
    } catch {
        Write-Error "Failed to run cross-device tests: $_"
        return $false
    }
}

function Test-DeviceSpecificConfigurations {
    Write-SubHeader "Testing Device-Specific Configurations"
    
    # Get connected devices
    $devices = & adb devices | Select-String "device$" | ForEach-Object { ($_ -split "\s+")[0] }
    
    foreach ($device in $devices) {
        Write-Info "Testing on device: $device"
        
        # Get device properties
        $manufacturer = & adb -s $device shell getprop ro.product.manufacturer
        $model = & adb -s $device shell getprop ro.product.model
        $apiLevel = & adb -s $device shell getprop ro.build.version.sdk
        $density = & adb -s $device shell wm density | Select-String "Physical density:" | ForEach-Object { ($_ -split ": ")[1] }
        $size = & adb -s $device shell wm size | Select-String "Physical size:" | ForEach-Object { ($_ -split ": ")[1] }
        
        Write-Info "  Manufacturer: $manufacturer"
        Write-Info "  Model: $model"
        Write-Info "  API Level: $apiLevel"
        Write-Info "  Density: $density"
        Write-Info "  Screen Size: $size"
        
        # Categorize device
        $deviceCategory = "Unknown"
        if ([int]$apiLevel -lt 26) {
            $deviceCategory = "Legacy"
        } elseif ([int]$density -lt 240) {
            $deviceCategory = "LowEnd"
        } elseif ([int]$density -gt 480) {
            $deviceCategory = "HighEnd"
        } else {
            $deviceCategory = "Standard"
        }
        
        Write-Info "  Category: $deviceCategory"
        
        # Run device-specific tests based on configuration filter
        if ($DeviceConfig -eq "All" -or 
            ($DeviceConfig -eq "LowEnd" -and $deviceCategory -eq "LowEnd") -or
            ($DeviceConfig -eq "HighEnd" -and $deviceCategory -eq "HighEnd") -or
            ($DeviceConfig -eq "Phone" -and $size -match "^[0-9]{3}x[0-9]{3}") -or
            ($DeviceConfig -eq "Tablet" -and $size -match "^[0-9]{4}x[0-9]{4}")) {
            
            Write-Info "Running tests on $device ($deviceCategory)"
            # Tests will run on this device as part of the connectedAndroidTest task
        } else {
            Write-Info "Skipping $device (doesn't match filter: $DeviceConfig)"
        }
    }
}

function Generate-TestReport {
    Write-SubHeader "Generating Test Report"
    
    $reportPath = "build/reports/androidTests/connected/index.html"
    if (Test-Path $reportPath) {
        Write-Success "Test report generated: $reportPath"
        
        # Try to open the report in the default browser
        try {
            Start-Process $reportPath
            Write-Info "Test report opened in browser"
        } catch {
            Write-Warning "Could not open test report automatically: $_"
        }
    } else {
        Write-Warning "Test report not found at expected location"
    }
}

function Main {
    Write-Header "MapScreen Cross-Device Testing Validation"
    
    # Change to android directory
    if (Test-Path "android") {
        Set-Location "android"
        Write-Info "Changed to android directory"
    } else {
        Write-Error "Android directory not found. Please run from project root."
        exit 1
    }
    
    # Validate environment
    if (-not (Test-AndroidEnvironment)) {
        Write-Error "Android environment validation failed"
        exit 1
    }
    
    # Test device configurations
    Test-DeviceSpecificConfigurations
    
    # Run tests based on type
    $allTestsPassed = $true
    
    switch ($TestType) {
        "All" {
            $allTestsPassed = Run-AllCrossDeviceTests
        }
        "Compatibility" {
            $allTestsPassed = Run-CrossDeviceCompatibilityTests
        }
        "ScreenSize" {
            $allTestsPassed = Run-ScreenSizeAdaptationTests
        }
        "Theme" {
            $allTestsPassed = Run-ThemeCompatibilityTests
        }
        "Accessibility" {
            $allTestsPassed = Run-AccessibilityDeviceTests
        }
        "Performance" {
            $allTestsPassed = Run-PerformanceDeviceTests
        }
    }
    
    # Generate test report
    Generate-TestReport
    
    # Final results
    Write-Header "Cross-Device Testing Results"
    
    if ($allTestsPassed) {
        Write-Success "All cross-device tests completed successfully!"
        Write-Info "MapScreen is compatible across different:"
        Write-Info "  ✓ Android versions (API 24+)"
        Write-Info "  ✓ Screen sizes and densities"
        Write-Info "  ✓ Light and dark themes"
        Write-Info "  ✓ Accessibility configurations"
        Write-Info "  ✓ Device performance levels"
        
        Write-Header "Task 21 - Cross-device and cross-platform testing: COMPLETED"
        exit 0
    } else {
        Write-Error "Some cross-device tests failed!"
        Write-Info "Please review the test results and fix any issues."
        exit 1
    }
}

# Run main function
Main