# FFinder Git Hooks Setup Script (PowerShell)
# Sets up Git hooks for enforcing FFinder commit policy (lint checks, unit tests, and commit message format)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    } else {
        $input | Write-Output
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Show-Header {
    Write-ColorOutput $Blue "=============================================="
    Write-ColorOutput $Blue "         FFinder Git Hooks Setup        "
    Write-ColorOutput $Blue "=============================================="
    Write-Output ""
}

function Setup-GitHooks {
    # Check if .git directory exists
    if (-not (Test-Path ".git")) {
        Write-ColorOutput $Red "Error: .git directory not found!"
        Write-ColorOutput $Yellow "Please run this script from the root of the Git repository."
        return $false
    }
    
    # Check if .git-hooks directory exists
    if (-not (Test-Path ".git-hooks")) {
        Write-ColorOutput $Red "Error: .git-hooks directory not found!"
        Write-ColorOutput $Yellow "Please make sure the .git-hooks directory exists."
        return $false
    }
    
    # Configure Git to use custom hooks directory
    git config core.hooksPath .git-hooks
    
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput $Red "Error: Failed to configure Git hooks path!"
        return $false
    }
    
    # Make hooks executable (for WSL/Git Bash compatibility)
    if (Test-Path "C:\Windows\System32\bash.exe") {
        bash -c "chmod +x .git-hooks/*"
    } else {
        Write-ColorOutput $Yellow "Warning: bash not found, skipping chmod. If using Git Bash/WSL, manually run: chmod +x .git-hooks/*"
    }
    
    # Commit signing setup removed as it's no longer required
    
    Write-ColorOutput $Green "Git hooks set up successfully!"
    return $true
}

# Main execution
Show-Header

$result = Setup-GitHooks

if ($result) {
    Write-ColorOutput $Green "\nFFinder commit policy is now enforced!\n"
    Write-ColorOutput $Blue "Remember:"
    Write-ColorOutput $Blue "- Commit messages must follow format: [Type] Description"
    Write-ColorOutput $Blue "- All lint checks and unit tests must pass"
    
    exit 0
} else {
    Write-ColorOutput $Red "\nFailed to set up FFinder commit policy!\n"
    exit 1
}