# ============================================================
# ThunderCore ERP - Verify and Run
# ============================================================
# Builds/tests the project, then starts the backend and frontend.
# Usage:
#   .\verify-and-run.ps1
#   .\verify-and-run.ps1 -SkipInstall -SkipFrontendBuild -SkipBackendTests
#   .\verify-and-run.ps1 -Restart
# ============================================================

param(
    [switch]$SkipInstall,
    [switch]$SkipFrontendBuild,
    [switch]$SkipBackendTests,
    [switch]$Restart,
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [int]$StartupTimeoutSec = 150
)

$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$BackendPath = Join-Path $ProjectRoot "backend"
$FrontendPath = Join-Path $ProjectRoot "frontend"
$LogPath = Join-Path $ProjectRoot "ghost-logs"
$BackendLog = Join-Path $LogPath "backend-verify-and-run.log"
$FrontendLog = Join-Path $LogPath "frontend-verify-and-run.log"

New-Item -ItemType Directory -Force -Path $LogPath | Out-Null

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "=== $Message ===" -ForegroundColor Cyan
}

function Invoke-CheckedCommand {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$FilePath,
        [string[]]$Arguments
    )

    Write-Section $Title
    Push-Location $WorkingDirectory
    try {
        & $FilePath @Arguments
        $exitCode = $LASTEXITCODE
        if ($null -ne $exitCode -and $exitCode -ne 0) {
            throw "$Title failed with exit code $exitCode"
        }
    }
    finally {
        Pop-Location
    }
}

function Get-PortProcessIds {
    param([int]$Port)

    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
}

function Stop-PortListeners {
    param([int]$Port)

    $processIds = Get-PortProcessIds -Port $Port
    foreach ($processId in $processIds) {
        Write-Host "Stopping PID $processId on port $Port" -ForegroundColor Yellow
        Stop-Process -Id $processId -Force -ErrorAction Stop
    }
}

function Test-BackendHealth {
    param([int]$Port)

    try {
        $health = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 3 -ErrorAction Stop
        return $health.status -eq "UP"
    }
    catch {
        return $false
    }
}

function Wait-BackendHealth {
    param(
        [int]$Port,
        [int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        if (Test-BackendHealth -Port $Port) {
            return
        }
        Start-Sleep -Seconds 3
        Write-Host "Waiting for backend health..." -ForegroundColor DarkGray
    } while ((Get-Date) -lt $deadline)

    throw "Backend did not become healthy within $TimeoutSec seconds. Check $BackendLog"
}

function Test-Frontend {
    param([int]$Port)

    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        return $response.StatusCode -eq 200
    }
    catch {
        return $false
    }
}

function Wait-Frontend {
    param(
        [int]$Port,
        [int]$TimeoutSec
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        if (Test-Frontend -Port $Port) {
            return
        }
        Start-Sleep -Seconds 2
        Write-Host "Waiting for frontend..." -ForegroundColor DarkGray
    } while ((Get-Date) -lt $deadline)

    throw "Frontend did not respond within $TimeoutSec seconds. Check $FrontendLog"
}

function Start-LoggedPowerShell {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$Command,
        [string]$LogFile
    )

    $escapedWorkingDirectory = $WorkingDirectory.Replace("'", "''")
    $escapedLogFile = $LogFile.Replace("'", "''")
    $fullCommand = @"
Set-Location -LiteralPath '$escapedWorkingDirectory'
`$ErrorActionPreference = 'Stop'
$Command *> '$escapedLogFile'
"@
    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($fullCommand))

    return Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand) `
        -WindowStyle Hidden `
        -PassThru
}

if (-not (Test-Path $BackendPath)) {
    throw "Backend folder not found: $BackendPath"
}

if (-not (Test-Path $FrontendPath)) {
    throw "Frontend folder not found: $FrontendPath"
}

if ($Restart) {
    Write-Section "CLEARING PORTS"
    Stop-PortListeners -Port $BackendPort
    Stop-PortListeners -Port $FrontendPort
}

if (-not $SkipInstall) {
    Invoke-CheckedCommand -Title "FRONTEND INSTALL" -WorkingDirectory $FrontendPath -FilePath "npm" -Arguments @("install")
}

if (-not $SkipFrontendBuild) {
    Invoke-CheckedCommand -Title "FRONTEND BUILD" -WorkingDirectory $FrontendPath -FilePath "npm" -Arguments @("run", "build")
}

if (-not $SkipBackendTests) {
    Invoke-CheckedCommand -Title "BACKEND TESTS" -WorkingDirectory $BackendPath -FilePath "mvn" -Arguments @("test")
}

Write-Section "BACKEND START"
if (Test-BackendHealth -Port $BackendPort) {
    Write-Host "Backend already healthy on port $BackendPort" -ForegroundColor Green
}
else {
    $backendPortProcessIds = Get-PortProcessIds -Port $BackendPort
    if ($backendPortProcessIds) {
        throw "Port $BackendPort is already in use but backend health is not UP. Stop that process or run .\verify-and-run.ps1 -Restart"
    }

    $backendProcess = Start-LoggedPowerShell `
        -Title "ThunderCore Backend" `
        -WorkingDirectory $BackendPath `
        -Command "`$env:SPRING_PROFILES_ACTIVE = 'dev'; mvn spring-boot:run" `
        -LogFile $BackendLog

    Write-Host "Backend launcher PID: $($backendProcess.Id)" -ForegroundColor Green
    Write-Host "Backend log: $BackendLog" -ForegroundColor DarkGray
    Wait-BackendHealth -Port $BackendPort -TimeoutSec $StartupTimeoutSec
    Write-Host "Backend is UP on port $BackendPort" -ForegroundColor Green
}

Write-Section "FRONTEND START"
if (Test-Frontend -Port $FrontendPort) {
    Write-Host "Frontend already responding on port $FrontendPort" -ForegroundColor Green
}
else {
    $frontendPortProcessIds = Get-PortProcessIds -Port $FrontendPort
    if ($frontendPortProcessIds) {
        throw "Port $FrontendPort is already in use but frontend did not respond. Stop that process or run .\verify-and-run.ps1 -Restart"
    }

    $frontendProcess = Start-LoggedPowerShell `
        -Title "ThunderCore Frontend" `
        -WorkingDirectory $FrontendPath `
        -Command "node .\node_modules\vite\bin\vite.js --host 127.0.0.1 --port $FrontendPort --strictPort" `
        -LogFile $FrontendLog

    Write-Host "Frontend launcher PID: $($frontendProcess.Id)" -ForegroundColor Green
    Write-Host "Frontend log: $FrontendLog" -ForegroundColor DarkGray
    Wait-Frontend -Port $FrontendPort -TimeoutSec 60
    Write-Host "Frontend is responding on port $FrontendPort" -ForegroundColor Green
}

Write-Section "DONE"
Write-Host "Frontend: http://localhost:$FrontendPort" -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:$BackendPort" -ForegroundColor Cyan
Write-Host "Swagger:  http://localhost:$BackendPort/swagger-ui.html" -ForegroundColor Cyan
Write-Host ""
Write-Host "Admin:   admin@thundercore.com / Admin@123" -ForegroundColor Yellow
Write-Host "Manager: manager@thundercore.com / Manager@123" -ForegroundColor Yellow
Write-Host "Staff:   staff@thundercore.com / Staff@123" -ForegroundColor Yellow
