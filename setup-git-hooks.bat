@echo off
REM FFinder Git Hooks Setup Script (CMD/Batch)
REM Sets up Git hooks for enforcing FFinder commit policy (lint checks, unit tests, and commit message format)

echo ==============================================
echo         FFinder Git Hooks Setup        
echo ==============================================
echo.

REM Check if .git directory exists
if not exist ".git" (
    echo [ERROR] .git directory not found!
    echo Please run this script from the root of the Git repository.
    exit /b 1
)

REM Check if .git-hooks directory exists
if not exist ".git-hooks" (
    echo [ERROR] .git-hooks directory not found!
    echo Please make sure the .git-hooks directory exists.
    exit /b 1
)

REM Configure Git to use custom hooks directory
git config core.hooksPath .git-hooks

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to configure Git hooks path!
    exit /b 1
)

REM Make hooks executable (for WSL/Git Bash compatibility)
where bash >nul 2>&1
if %ERRORLEVEL% equ 0 (
    bash -c "chmod +x .git-hooks/*"
) else (
    echo [WARNING] bash not found, skipping chmod. If using Git Bash/WSL, manually run: chmod +x .git-hooks/*
)

REM Commit signing setup removed as it's no longer required

echo [SUCCESS] Git hooks set up successfully!

echo.
echo [INFO] FFinder commit policy is now enforced!
echo.
echo Remember:
echo - Commit messages must follow format: [Type] Description
echo - All lint checks and unit tests must pass

exit /b 0