# Get SHA1 Fingerprint for Google Maps API Key Configuration
# This script displays the SHA1 fingerprints needed for Google Cloud Console

Write-Host "=== FFinder Google Maps API Key SHA1 Fingerprints ===" -ForegroundColor Green
Write-Host ""

# Debug keystore (default Android debug key)
Write-Host "DEBUG BUILD SHA1 FINGERPRINT:" -ForegroundColor Yellow
Write-Host "Use this for development/testing" -ForegroundColor Gray

$debugKeystore = "$env:USERPROFILE\.android\debug.keystore"
if (Test-Path $debugKeystore) {
    try {
        $debugSha1 = keytool -list -v -keystore $debugKeystore -alias androiddebugkey -storepass android -keypass android 2>$null | Select-String "SHA1:" | ForEach-Object { $_.ToString().Split(":")[1].Trim() }
        Write-Host $debugSha1 -ForegroundColor Cyan
        Write-Host ""
        
        Write-Host "Google Cloud Console Configuration:" -ForegroundColor Yellow
        Write-Host "Package Name: com.locationsharing.app" -ForegroundColor White
        Write-Host "SHA1: $debugSha1" -ForegroundColor White
        Write-Host ""
    } catch {
        Write-Host "Error reading debug keystore: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "Debug keystore not found at: $debugKeystore" -ForegroundColor Red
    Write-Host "Run an Android project first to generate the debug keystore" -ForegroundColor Gray
}

# Release keystore (if exists)
Write-Host "RELEASE BUILD SHA1 FINGERPRINT:" -ForegroundColor Yellow
Write-Host "Use this for production builds" -ForegroundColor Gray

$releaseKeystore = ".\app\release.keystore"
if (Test-Path $releaseKeystore) {
    Write-Host "Found release keystore. Please provide the alias and passwords:" -ForegroundColor Green
    $alias = Read-Host "Enter keystore alias"
    $storepass = Read-Host "Enter store password" -AsSecureString
    $keypass = Read-Host "Enter key password" -AsSecureString
    
    $storepassPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($storepass))
    $keypassPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keypass))
    
    try {
        $releaseSha1 = keytool -list -v -keystore $releaseKeystore -alias $alias -storepass $storepassPlain -keypass $keypassPlain 2>$null | Select-String "SHA1:" | ForEach-Object { $_.ToString().Split(":")[1].Trim() }
        Write-Host $releaseSha1 -ForegroundColor Cyan
        Write-Host ""
        
        Write-Host "Google Cloud Console Configuration:" -ForegroundColor Yellow
        Write-Host "Package Name: com.locationsharing.app" -ForegroundColor White
        Write-Host "SHA1: $releaseSha1" -ForegroundColor White
    } catch {
        Write-Host "Error reading release keystore: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "No release keystore found at: $releaseKeystore" -ForegroundColor Gray
    Write-Host "Create a release keystore for production builds" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== Next Steps ===" -ForegroundColor Green
Write-Host "1. Go to Google Cloud Console: https://console.cloud.google.com/apis/credentials" -ForegroundColor White
Write-Host "2. Select your API key for: AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ" -ForegroundColor White
Write-Host "3. Under 'Application restrictions', select 'Android apps'" -ForegroundColor White
Write-Host "4. Add the package name and SHA1 fingerprint shown above" -ForegroundColor White
Write-Host "5. Save the configuration" -ForegroundColor White
Write-Host ""
Write-Host "API Key: AIzaSyDynUiAXTN354tCutKG7dDc2iyOROLKuqQ" -ForegroundColor Cyan