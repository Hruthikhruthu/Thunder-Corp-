# ============================================================
# ThunderCore ERP - LOCAL DEV MODE LAUNCHER (No Docker needed)
# Backend: Spring Boot + H2 in-memory DB on port 8080
# Frontend: Vite dev server on port 5173
# ============================================================

$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = "$projectPath\backend"
$frontendPath = "$projectPath\frontend"

Clear-Host
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  THUNDERCORE ERP - LOCAL DEV LAUNCHER" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Start Backend
Write-Host "[1/2] Starting Backend (Spring Boot + H2)..." -ForegroundColor Yellow
$backendProcess = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/k title ThunderCore-Backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev" `
    -WorkingDirectory $backendPath `
    -PassThru
Write-Host "  Backend PID: $($backendProcess.Id)" -ForegroundColor Green

# Wait for backend
Write-Host "  Waiting for backend to start..." -ForegroundColor Yellow
$started = $false
for ($i = 0; $i -lt 18; $i++) {
    Start-Sleep -Seconds 5
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($health.status -eq "UP") {
            Write-Host "  Backend is UP!" -ForegroundColor Green
            $started = $true
            break
        }
    } catch {}
    Write-Host "  Still waiting... ($([int](($i+1)*5))/90 sec)" -ForegroundColor Gray
}

# Start Frontend
Write-Host "[2/2] Starting Frontend (Vite)..." -ForegroundColor Yellow
$frontendProcess = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/k title ThunderCore-Frontend && npm run dev" `
    -WorkingDirectory $frontendPath `
    -PassThru
Write-Host "  Frontend PID: $($frontendProcess.Id)" -ForegroundColor Green
Start-Sleep -Seconds 5

# Final output
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  THUNDERCORE ERP IS RUNNING!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend:   http://localhost:5173" -ForegroundColor Cyan
Write-Host "  Backend:    http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Swagger:    http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "  H2 Console: http://localhost:8080/h2-console" -ForegroundColor Cyan
Write-Host ""
Write-Host "  LOGIN CREDENTIALS:" -ForegroundColor Yellow
Write-Host "  admin@thundercore.com   / Admin@123   (SUPER_ADMIN)" -ForegroundColor White
Write-Host "  manager@thundercore.com / Manager@123 (MANAGER)" -ForegroundColor White
Write-Host "  staff@thundercore.com   / Staff@123   (STAFF)" -ForegroundColor White
Write-Host ""
Start-Process "http://localhost:5173"
Write-Host ""
Write-Host "Press Enter to stop all services..." -ForegroundColor Red
Read-Host
Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $frontendProcess.Id -Force -ErrorAction SilentlyContinue
Write-Host "Services stopped." -ForegroundColor Green
