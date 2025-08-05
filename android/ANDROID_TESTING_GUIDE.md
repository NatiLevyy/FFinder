# Android Testing Setup Guide

## Quick Start

### Prerequisites
1. **Java 8+** installed and configured
2. **Android SDK** installed (via Android Studio or standalone)
3. **ANDROID_HOME** environment variable set OR `local.properties` configured

### Running Tests

**From project root:**
```bash
# Windows
cd android && .\gradlew.bat testDebugUnitTest

# macOS/Linux
cd android && ./gradlew testDebugUnitTest
```

**From android directory:**
```bash
# Windows
.\gradlew.bat testDebugUnitTest

# macOS/Linux
./gradlew testDebugUnitTest
```

## Android SDK Setup

### Option 1: Environment Variable (Recommended for CI)
```bash
# Windows (Command Prompt)
set ANDROID_HOME=C:\Users\YourUsername\AppData\Local\Android\Sdk

# Windows (PowerShell)
$env:ANDROID_HOME="C:\Users\YourUsername\AppData\Local\Android\Sdk"

# macOS/Linux
export ANDROID_HOME=/Users/YourUsername/Library/Android/sdk
```

### Option 2: Local Properties File
Edit `android/local.properties`:
```properties
# Windows (use double backslashes)
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

# macOS
sdk.dir=/Users/YourUsername/Library/Android/sdk

# Linux
sdk.dir=/home/YourUsername/Android/Sdk
```

### Finding Your SDK Location
1. **Android Studio**: File → Settings → Appearance & Behavior → System Settings → Android SDK
2. **Common locations**:
   - Windows: `C:\Users\YourUsername\AppData\Local\Android\Sdk`
   - macOS: `/Users/YourUsername/Library/Android/sdk`
   - Linux: `/home/YourUsername/Android/Sdk`

## Test Commands

### Basic Testing
```bash
# Run all unit tests
.\gradlew.bat testDebugUnitTest

# Run with coverage
.\gradlew.bat testDebugUnitTestCoverage

# Run specific test class
.\gradlew.bat testDebugUnitTest --tests="*ExampleUnitTest*"

# Run tests matching pattern
.\gradlew.bat testDebugUnitTest --tests="*LocationForegroundServiceTest*"
```

### Advanced Testing
```bash
# Clean and test
.\gradlew.bat clean testDebugUnitTest

# Test with info logging
.\gradlew.bat testDebugUnitTest --info

# Test with debug logging
.\gradlew.bat testDebugUnitTest --debug

# Continue on test failures
.\gradlew.bat testDebugUnitTest --continue
```

### Instrumentation Tests (requires device/emulator)
```bash
# Run instrumentation tests
.\gradlew.bat connectedDebugAndroidTest

# Install and run on connected device
.\gradlew.bat connectedAndroidTest
```

## Test Framework Configuration

### Dependencies Included
- **JUnit 4**: Core testing framework
- **MockK**: Kotlin-friendly mocking library
- **Mockito**: Java mocking library with Kotlin support
- **Robolectric**: Android unit testing without device
- **Coroutines Test**: Testing coroutines and flows
- **Hilt Testing**: Dependency injection testing
- **AndroidX Test**: Android testing utilities

### Test Structure
```
android/app/src/test/java/com/locationsharing/app/
├── ExampleUnitTest.kt                    # Basic setup verification
├── data/
│   ├── auth/
│   │   ├── AuthRepositoryImplTest.kt     # Auth repository tests
│   │   ├── SessionManagerTest.kt         # Session management tests
│   │   └── JwtTokenManagerTest.kt        # Token management tests
│   └── location/
│       └── LocationTrackerTest.kt        # Location tracking tests
├── service/
│   └── LocationForegroundServiceTest.kt  # Background service tests
└── domain/
    └── model/
        ├── AuthErrorTest.kt              # Error model tests
        └── LocationTest.kt               # Location model tests
```

## Troubleshooting

### Common Issues

#### 1. SDK Location Not Found
**Error**: `SDK location not found. Define a valid SDK location...`

**Solutions**:
```bash
# Option A: Set environment variable
set ANDROID_HOME=C:\Users\YourUsername\AppData\Local\Android\Sdk

# Option B: Edit android/local.properties
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

# Option C: Install Android Studio
# Download from https://developer.android.com/studio
```

#### 2. Gradle Wrapper Issues
**Error**: `gradlew: command not found` or permission denied

**Solutions**:
```bash
# Windows: Use .bat extension
.\gradlew.bat testDebugUnitTest

# macOS/Linux: Make executable
chmod +x ./gradlew
./gradlew testDebugUnitTest
```

#### 3. Java Version Issues
**Error**: `Unsupported Java version` or compilation errors

**Solutions**:
```bash
# Check Java version (need Java 8+)
java -version

# Set JAVA_HOME if needed
set JAVA_HOME=C:\Program Files\Java\jdk-11.0.x
```

#### 4. Memory Issues
**Error**: `OutOfMemoryError` during tests

**Solutions**:
```bash
# Increase heap size
.\gradlew.bat testDebugUnitTest -Dorg.gradle.jvmargs="-Xmx2g"

# Or edit gradle.properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

#### 5. Test Failures
**Error**: Tests fail unexpectedly

**Solutions**:
```bash
# Run with more verbose output
.\gradlew.bat testDebugUnitTest --info --stacktrace

# Clean and retry
.\gradlew.bat clean testDebugUnitTest

# Run specific failing test
.\gradlew.bat testDebugUnitTest --tests="*SpecificTest*" --info
```

### Verification Commands

```bash
# Verify Gradle wrapper
.\gradlew.bat --version

# Verify project structure
.\gradlew.bat tasks --group="verification"

# Verify dependencies
.\gradlew.bat dependencies --configuration testImplementation

# Verify Android SDK
.\gradlew.bat androidDependencies
```

## CI/CD Configuration

### GitHub Actions Example
```yaml
name: Android Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
    - name: Run tests
      run: |
        cd android
        ./gradlew testDebugUnitTest
```

### Local CI Simulation
```bash
# Simulate CI environment
set ANDROID_HOME=
.\gradlew.bat clean testDebugUnitTest --no-daemon --stacktrace
```

## Performance Tips

### Faster Test Execution
```bash
# Use Gradle daemon (default)
.\gradlew.bat testDebugUnitTest

# Parallel execution
.\gradlew.bat testDebugUnitTest --parallel

# Build cache
.\gradlew.bat testDebugUnitTest --build-cache

# Configuration cache
.\gradlew.bat testDebugUnitTest --configuration-cache
```

### Test Selection
```bash
# Run only changed tests
.\gradlew.bat testDebugUnitTest --continuous

# Run tests for specific package
.\gradlew.bat testDebugUnitTest --tests="com.locationsharing.app.data.*"

# Skip slow tests (if tagged)
.\gradlew.bat testDebugUnitTest -PskipSlowTests
```

## Test Coverage

### Generate Coverage Report
```bash
# Generate coverage
.\gradlew.bat testDebugUnitTestCoverage

# View report
# Open: android/app/build/reports/coverage/test/debug/index.html
```

### Coverage Requirements
- **Minimum**: 80% line coverage
- **Critical paths**: 100% coverage (auth, location, security)
- **New code**: Must maintain or improve coverage

## IDE Integration

### Android Studio
1. Right-click test file → Run tests
2. View → Tool Windows → Test Results
3. Run → Edit Configurations → Add JUnit configuration

### IntelliJ IDEA
1. Right-click test directory → Run All Tests
2. View → Tool Windows → Run
3. Configure test runner in Settings → Build → Gradle

### VS Code
1. Install "Extension Pack for Java"
2. Install "Gradle for Java"
3. Use Command Palette → "Java: Run Tests"

This guide ensures you can run Android tests successfully in any environment!