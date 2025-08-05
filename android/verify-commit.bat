@echo off
REM FFinder Commit Policy Verification Script (CMD/Batch)
REM Verifies commit message format, lint checks, and unit tests

setlocal enabledelayedexpansion

REM Parse command line arguments
set "COMMIT_MESSAGE="
set "SKIP_TESTS="
set "SKIP_LINT="
set "VERBOSE="

:parse_args
if "%~1"=="" goto :args_done
if /i "%~1"=="--message" set "COMMIT_MESSAGE=%~2" & shift & shift & goto :parse_args
if /i "%~1"=="-m" set "COMMIT_MESSAGE=%~2" & shift & shift & goto :parse_args
if /i "%~1"=="--skip-tests" set "SKIP_TESTS=1" & shift & goto :parse_args
if /i "%~1"=="-t" set "SKIP_TESTS=1" & shift & goto :parse_args
if /i "%~1"=="--skip-lint" set "SKIP_LINT=1" & shift & goto :parse_args
if /i "%~1"=="-l" set "SKIP_LINT=1" & shift & goto :parse_args
REM Removed skip-signing options as commit signing is no longer required
if /i "%~1"=="--verbose" set "VERBOSE=1" & shift & goto :parse_args
if /i "%~1"=="-v" set "VERBOSE=1" & shift & goto :parse_args
if /i "%~1"=="--help" goto :show_usage
if /i "%~1"=="-h" goto :show_usage
shift
goto :parse_args

:args_done

REM Show header
echo ==============================================
echo         FFinder Commit Policy Verifier        
echo ==============================================
echo.

REM Check if no arguments provided
if not defined COMMIT_MESSAGE if not defined SKIP_TESTS if not defined SKIP_LINT goto :show_usage

REM Initialize result tracking
set "ALL_PASSED=1"
set "MESSAGE_RESULT=0"
set "LINT_RESULT=0"
set "TESTS_RESULT=0"
set "SIGNING_RESULT=0"

REM Check commit message format if provided
if defined COMMIT_MESSAGE (
    echo [INFO] Checking commit message format...
    
    REM Valid commit types
    set "VALID_TYPES=Feature Fix Refactor Style Docs Test Chore Perf"
    
    REM Check if message follows [Type] Description format
    set "VALID_FORMAT=0"
    
    for %%t in (%VALID_TYPES%) do (
        echo !COMMIT_MESSAGE! | findstr /r /c:"^\[%%t\] .*" > nul
        if !errorlevel! equ 0 (
            set "VALID_FORMAT=1"
        )
    )
    
    if !VALID_FORMAT! equ 1 (
        echo [SUCCESS] Commit message format is valid!
        set "MESSAGE_RESULT=1"
    ) else (
        echo [ERROR] Invalid commit message format!
        echo [INFO] Commit message must follow format: [Type] Description
        echo [INFO] Valid types: %VALID_TYPES%
        echo [INFO] Example: [Feature] Add animated FAB for location sharing
        set "ALL_PASSED=0"
    )
    echo.
)

REM Check lint unless skipped
if not defined SKIP_LINT (
    echo [INFO] Running lint checks...
    
    REM Run format-code.bat with --check flag
    if exist "format-code.bat" (
        call format-code.bat --check
        set "LINT_EXIT_CODE=!errorlevel!"
        
        if !LINT_EXIT_CODE! equ 0 (
            echo [SUCCESS] Lint checks passed!
            set "LINT_RESULT=1"
        ) else (
            echo [ERROR] Lint checks failed!
            echo [INFO] Run format-code.bat --fix to fix formatting issues
            set "ALL_PASSED=0"
        )
    ) else (
        echo [ERROR] format-code.bat not found!
        echo [INFO] Make sure you're running this script from the android directory
        set "ALL_PASSED=0"
    )
    echo.
)

REM Check unit tests unless skipped
if not defined SKIP_TESTS (
    echo [INFO] Running unit tests...
    
    REM Run unit tests using gradlew
    if exist "gradlew.bat" (
        call gradlew.bat testDebugUnitTest
        set "TEST_EXIT_CODE=!errorlevel!"
        
        if !TEST_EXIT_CODE! equ 0 (
            echo [SUCCESS] Unit tests passed!
            set "TESTS_RESULT=1"
        ) else (
            echo [ERROR] Unit tests failed!
            echo [INFO] Fix failing tests before committing
            set "ALL_PASSED=0"
        )
    ) else (
        echo [ERROR] gradlew.bat not found!
        echo [INFO] Make sure you're running this script from the android directory
        set "ALL_PASSED=0"
    )
    echo.
)

REM Commit signing check removed as it's no longer required

REM Show summary
echo ==============================================
echo                  SUMMARY                      
echo ==============================================

if defined COMMIT_MESSAGE (
    if !MESSAGE_RESULT! equ 1 (
        echo [PASS] Commit Message Format: PASSED
    ) else (
        echo [FAIL] Commit Message Format: FAILED
    )
)

if not defined SKIP_LINT (
    if !LINT_RESULT! equ 1 (
        echo [PASS] Lint Checks: PASSED
    ) else (
        echo [FAIL] Lint Checks: FAILED
    )
)

if not defined SKIP_TESTS (
    if !TESTS_RESULT! equ 1 (
        echo [PASS] Unit Tests: PASSED
    ) else (
        echo [FAIL] Unit Tests: FAILED
    )
)

REM Commit signing result display removed as it's no longer required

echo.
if !ALL_PASSED! equ 1 (
    echo [SUCCESS] All checks passed! Your commit meets FFinder policy requirements.
) else (
    echo [WARNING] Some checks failed. Please fix the issues before committing.
)
echo.

REM Exit with appropriate code
if !ALL_PASSED! equ 1 (
    exit /b 0
) else (
    exit /b 1
)

:show_usage
echo Usage: verify-commit.bat [OPTIONS]
echo.
echo Options:
echo   --message, -m "message"  Commit message to verify
echo   --skip-tests, -t        Skip unit test verification
echo   --skip-lint, -l         Skip lint check verification
REM Removed skip-signing option from usage as commit signing is no longer required
echo   --verbose, -v           Show detailed output
echo   --help, -h              Show this help message
echo.
echo Examples:
echo   verify-commit.bat --message "[Feature] Add location sharing"
echo   verify-commit.bat --skip-tests -m "[Fix] Update README"
echo.
exit /b 0