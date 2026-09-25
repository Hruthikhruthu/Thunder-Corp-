# ============================================================
# ThunderCore ERP - Docker + WSL2 Complete Fix Script
# RUN THIS AS ADMINISTRATOR (Right-click -> Run with PowerShell as Admin)
# ============================================================

# Check admin
if (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]"Administrator")) {
    Write-Host "ERROR: This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Right-click the script and select 'Run with PowerShell as Administrator'" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Clear-Host
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  DOCKER + WSL2 COMPLETE FIX SCRIPT" -ForegroundColor Cyan
Write-Host "  Running as Administrator" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# PHASE 1: Kill everything
Write-Host "[1/9] Killing Docker and WSL processes..." -ForegroundColor Yellow
$procs = @("Docker Desktop","com.docker.backend","com.docker.build","dockerd","wslhost","wslservice","vmmem")
foreach ($p in $procs) {
    Get-Process -Name $p -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2
Write-Host "  Done" -ForegroundColor Green

# PHASE 2: Stop WSL service
Write-Host "[2/9] Stopping WSL service..." -ForegroundColor Yellow
Stop-Service -Name "WSLService" -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3
Write-Host "  Done" -ForegroundColor Green

# PHASE 3: Remove broken docker-desktop distro from registry
Write-Host "[3/9] Removing broken docker-desktop WSL distro from registry..." -ForegroundColor Yellow
$lxssPath = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Lxss"
Get-ChildItem $lxssPath -ErrorAction SilentlyContinue | ForEach-Object {
    $name = (Get-ItemProperty $_.PSPath -ErrorAction SilentlyContinue).DistributionName
    if ($name -eq "docker-desktop" -or $name -eq "docker-desktop-data") {
        Remove-Item $_.PSPath -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  Removed: $name" -ForegroundColor Green
    }
}
Write-Host "  Done" -ForegroundColor Green

# PHASE 4: Delete corrupted Docker WSL VHDX
Write-Host "[4/9] Deleting corrupted Docker WSL data..." -ForegroundColor Yellow
$dockerWslMain = "C:\Users\$env:USERNAME\AppData\Local\Docker\wsl\main"
$dockerWslData = "C:\Users\$env:USERNAME\AppData\Local\Docker\wsl\data"
Remove-Item -Path $dockerWslMain -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path $dockerWslData -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $dockerWslMain -Force | Out-Null
New-Item -ItemType Directory -Path $dockerWslData -Force | Out-Null
Write-Host "  Done" -ForegroundColor Green

# PHASE 5: Clear Docker lock files
Write-Host "[5/9] Clearing Docker lock files..." -ForegroundColor Yellow
@(
    "C:\Users\$env:USERNAME\AppData\Local\Docker\backend.lock",
    "C:\Users\$env:USERNAME\AppData\Local\Docker\frontend.lock",
    "C:\Users\$env:USERNAME\AppData\Local\Docker\launcher.lock"
) | ForEach-Object { Remove-Item $_ -Force -ErrorAction SilentlyContinue }
Write-Host "  Done" -ForegroundColor Green

# PHASE 6: Enable required Windows features
Write-Host "[6/9] Checking and enabling Windows features..." -ForegroundColor Yellow
$features = @(
    "Microsoft-Windows-Subsystem-Linux",
    "VirtualMachinePlatform",
    "HypervisorPlatform",
    "Containers"
)
$needsReboot = $false
foreach ($feature in $features) {
    $info = dism /online /get-featureinfo /featurename:$feature 2>&1
    $state = ($info | Select-String "State :").ToString().Trim()
    if ($state -match "Disabled") {
        Write-Host "  Enabling: $feature" -ForegroundColor Yellow
        dism /online /enable-feature /featurename:$feature /all /norestart 2>&1 | Out-Null
        Write-Host "  Enabled: $feature (reboot required)" -ForegroundColor Green
        $needsReboot = $true
    } else {
        Write-Host "  Already enabled: $feature" -ForegroundColor Green
    }
}

# PHASE 7: Reset network stack
Write-Host "[7/9] Resetting network stack..." -ForegroundColor Yellow
netsh winsock reset | Out-Null
netsh int ip reset | Out-Null
Write-Host "  Done" -ForegroundColor Green

# PHASE 8: Update WSL
Write-Host "[8/9] Updating WSL kernel..." -ForegroundColor Yellow
wsl --update 2>&1 | Out-Null
wsl --set-default-version 2 2>&1 | Out-Null
Write-Host "  Done" -ForegroundColor Green

# PHASE 9: Start WSL service
Write-Host "[9/9] Starting WSL service..." -ForegroundColor Yellow
Start-Service -Name "WSLService" -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
$svcStatus = (Get-Service -Name "WSLService" -ErrorAction SilentlyContinue).Status
Write-Host "  WSLService status: $svcStatus" -ForegroundColor Green

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  FIX COMPLETE!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

if ($needsReboot) {
    Write-Host "IMPORTANT: Windows features were enabled." -ForegroundColor Yellow
    Write-Host "You MUST restart your computer before Docker will work." -ForegroundColor Red
    Write-Host ""
}

Write-Host "NEXT STEPS:" -ForegroundColor Cyan
Write-Host "1. $(if ($needsReboot) {'RESTART your computer first'} else {'Open Docker Desktop'})" -ForegroundColor White
Write-Host "2. Wait 2-3 minutes for Docker Desktop to initialize" -ForegroundColor White
Write-Host "3. Run: docker compose up -d --build" -ForegroundColor White
Write-Host "   from: C:\Users\DELL\Downloads\thunder core project\thunder core project\thunder core project\thundercore-erp" -ForegroundColor White
Write-Host ""
Write-Host "If Docker STILL fails after restart:" -ForegroundColor Yellow
Write-Host "  Your Dell Precision 5560 may have Intel VT-x disabled in BIOS." -ForegroundColor White
Write-Host "  Steps to enable:" -ForegroundColor White
Write-Host "  1. Restart computer" -ForegroundColor White
Write-Host "  2. Press F2 during Dell logo to enter BIOS" -ForegroundColor White
Write-Host "  3. Go to: Virtualization Support -> Virtualization" -ForegroundColor White
Write-Host "  4. Enable: Intel Virtualization Technology (VT-x)" -ForegroundColor White
Write-Host "  5. Enable: VT for Direct I/O (VT-d)" -ForegroundColor White
Write-Host "  6. Save and Exit (F10)" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to exit"
