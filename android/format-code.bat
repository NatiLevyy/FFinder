@echo off
REM FFinder Code Formatting Script (CMD/Batch)
REM Applies Kotlin best practices with Jetpack Compose support

setlocal enabledelayedexpansion

REM Parse command line arguments
set "CHECK_ONLY="
set "FORMAT_ONLY="
set "DETEKT_ONLY="
set "RUN_ALL="
set "VERBOSE="

:parse_args
if "%~1"=="" goto :args_done
if /i "%~1"=="--check" set "CHECK_ONLY=1"
if /i "%~1"=="-c" set "CHECK_ONLY=1"
if /i "%~1"=="--fix" set "FORMAT_ONLY=1"
if /i "%~1"=="-f" set "FORMAT_ONLY=1"
if /i "%~1"=="--detekt" set "DETEKT_ONLY=1"
if /i "%~1"=="-d" set "DETEKT_ONLY=1"
if /i "%~1"=="--all" set "RUN_ALL=1"
if /i "%~1"=="-a" set "RUN_ALL=1"
if /i "%~1"=="--verbose" set "VERBOSE=1"
if /i "%~1"=="-v" set "VERBOSE=1"
if /i "%~1"=="--help" goto :show_usage
if /i "%~1"=="-h" goto :show_usage
shift
goto :parse_args

:args_done

REM Show header
echo ==============================================
echo          FFinder Code Formatting Tool        
echo ==============================================
echo.

REM Check if no arguments provided
if not defined CHECK_ONLY if not defined FORMAT_ONLY if not defined DETEKT_ONLY if not defined RUN_ALL goto :show_usage

REM Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found in current directory
    echo Please run this script from the android/ directory
    exit /b 1
)

REM Initialize result tracking
set "KTLINT_CHECK_RESULT=0"
set "KTLINT_FORMAT_RESULT=0"
set "DETEKT_RESULT=0"
set "BUILD_RESULT=0"

if defined RUN_ALL (
    echo [INFO] Running complete FFinder code quality check...
    echo.
    
    REM 1. Format code first
    echo [INFO] Running ktlint format...
    call gradlew.bat ktlintFormat
    set "KTLINT_FORMAT_RESULT=!errorlevel!"
    
    if !KTLINT_FORMAT_RESULT! equ 0 (
        echo [SUCCESS] Code formatted successfully!
    ) else (
        echo [ERROR] Formatting failed!
    )
    echo.
    
    REM 2. Check formatting
    echo [INFO] Running ktlint check...
    call gradlew.bat ktlintCheck
    set "KTLINT_CHECK_RESULT=!errorlevel!"
    
    if !KTLINT_CHECK_RESULT! equ 0 (
        echo [SUCCESS] Ktlint check passed!
    ) else (
        echo [ERROR] Ktlint check failed!
    )
    echo.
    
    REM 3. Run static analysis
    echo [INFO] Running detekt static analysis...
    call gradlew.bat detekt
    set "DETEKT_RESULT=!errorlevel!"
    
    if !DETEKT_RESULT! equ 0 (
        echo [SUCCESS] Detekt analysis passed!
    ) else (
        echo [ERROR] Detekt analysis found issues!
        if exist "app\build\reports\detekt\detekt.html" (
            echo [INFO] Opening detekt report...
            start "" "app\build\reports\detekt\detekt.html"
        )
    )
    echo.
    
    REM 4. Verify build
    echo [INFO] Verifying build after formatting...
    call gradlew.bat compileDebugKotlin
    set "BUILD_RESULT=!errorlevel!"
    
    if !BUILD_RESULT! equ 0 (
        echo [SUCCESS] Build verification passed!
    ) else (
        echo [ERROR] Build verification failed!
    )
    echo.
    
) else if defined FORMAT_ONLY (
    echo [INFO] Running ktlint format...
    call gradlew.bat ktlintFormat
    set "KTLINT_FORMAT_RESULT=!errorlevel!"
    
    if !KTLINT_FORMAT_RESULT! equ 0 (
        echo [SUCCESS] Code formatted successfully!
    ) else (
        echo [ERROR] Formatting failed!
    )
    echo.
    
    REM Also run check after formatting
    echo [INFO] Running ktlint check...
    call gradlew.bat ktlintCheck
    set "KTLINT_CHECK_RESULT=!errorlevel!"
    
    if !KTLINT_CHECK_RESULT! equ 0 (
        echo [SUCCESS] Ktlint check passed!
    ) else (
        echo [ERROR] Ktlint check failed!
    )
    echo.
    
) else if defined CHECK_ONLY (
    echo [INFO] Running ktlint check...
    call gradlew.bat ktlintCheck
    set "KTLINT_CHECK_RESULT=!errorlevel!"
    
    if !KTLINT_CHECK_RESULT! equ 0 (
        echo [SUCCESS] Ktlint check passed!
    ) else (
        echo [ERROR] Ktlint check failed!
        echo [INFO] Run with --fix to automatically fix formatting issues
    )
    echo.
    
) else if defined DETEKT_ONLY (
    echo [INFO] Running detekt static analysis...
    call gradlew.bat detekt
    set "DETEKT_RESULT=!errorlevel!"
    
    if !DETEKT_RESULT! equ 0 (
        echo [SUCCESS] Detekt analysis passed!
    ) else (
        echo [ERROR] Detekt analysis found issues!
        if exist "app\build\reports\detekt\detekt.html" (
            echo [INFO] Opening detekt report...
            start "" "app\build\reports\detekt\detekt.html"
        )
    )
    echo.
)

REM Show summary
echo ==============================================
echo                  SUMMARY                      
echo ==============================================

set "ALL_PASSED=1"

if defined RUN_ALL (
    if !KTLINT_FORMAT_RESULT! equ 0 (
        echo [PASS] Ktlint Format: PASSED
    ) else (
        echo [FAIL] Ktlint Format: FAILED
        set "ALL_PASSED=0"
    )
    
    if !KTLINT_CHECK_RESULT! equ 0 (
        echo [PASS] Ktlint Check: PASSED
    ) else (
        echo [FAIL] Ktlint Check: FAILED
        set "ALL_PASSED=0"
    )
    
    if !DETEKT_RESULT! equ 0 (
        echo [PASS] Detekt Analysis: PASSED
    ) else (
        echo [FAIL] Detekt Analysis: FAILED
        set "ALL_PASSED=0"
    )
    
    if !BUILD_RESULT! equ 0 (
        echo [PASS] Build Verification: PASSED
    ) else (
        echo [FAIL] Build Verification: FAILED
        set "ALL_PASSED=0"
    )
) else if defined FORMAT_ONLY (
    if !KTLINT_FORMAT_RESULT! equ 0 (
        echo [PASS] Ktlint Format: PASSED
    ) else (
        echo [FAIL] Ktlint Format: FAILED
        set "ALL_PASSED=0"
    )
    
    if !KTLINT_CHECK_RESULT! equ 0 (
        echo [PASS] Ktlint Check: PASSED
    ) else (
        echo [FAIL] Ktlint Check: FAILED
        set "ALL_PASSED=0"
    )
) else if defined CHECK_ONLY (
    if !KTLINT_CHECK_RESULT! equ 0 (
        echo [PASS] Ktlint Check: PASSED
    ) else (
        echo [FAIL] Ktlint Check: FAILED
        set "ALL_PASSED=0"
    )
) else if defined DETEKT_ONLY (
    if !DETEKT_RESULT! equ 0 (
        echo [PASS] Detekt Analysis: PASSED
    ) else (
        echo [FAIL] Detekt Analysis: FAILED
        set "ALL_PASSED=0"
    )
)

echo.
if !ALL_PASSED! equ 1 (
    echo [SUCCESS] All checks passed! Your code follows FFinder standards.
) else (
    echo [WARNING] Some checks failed. Please review and fix the issues above.
)
echo.

REM Exit with appropriate code
if !ALL_PASSED! equ 1 (
    exit /b 0
) else (
    exit /b 1
)

:show_usage
echo Usage: format-code.bat [OPTIONS]
echo.
echo Options:
echo   --check, -c     Run ktlint check only (no formatting)
echo   --fix, -f       Run ktlint format to fix issues
echo   --detekt, -d    Run detekt static analysis
echo   --all, -a       Run all checks and formatting
echo   --verbose, -v   Show detailed output
echo   --help, -h      Show this help message
echo.
echo Examples:
echo   format-code.bat --all          # Run all tools
echo   format-code.bat --fix          # Format code only
echo   format-code.bat --check        # Check style only
echo   format-code.bat --detekt       # Run static analysis
echo.
exit /b 0