# =========================================================
# ThunderCore ERP - One-Click Ghost Level Starter
# =========================================================

$ROOT = $PSScriptRoot
$BACKEND = "$ROOT\backend"
$FRONTEND = "$ROOT\frontend"

Write-Host ""
Write-Host "=========================================================" -ForegroundColor Magenta
Write-Host "         THUNDERCORE ERP - GHOST LAUNCHER               " -ForegroundColor Magenta
Write-Host "=========================================================" -ForegroundColor Magenta

# STEP 1: Kill old processes on ports 8080, 5173, 5174
Write-Host "`n[1/4] Clearing ports..." -ForegroundColor Cyan
foreach ($port in @(8080, 5173, 5174)) {
    $pids = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($p in $pids) {
        Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
        Write-Host "  Stopped PID $p on port $port" -ForegroundColor Yellow
    }
}
Start-Sleep -Seconds 2
Write-Host "  Ports cleared." -ForegroundColor Green

# STEP 2: Check JAR exists, build if missing
Write-Host "`n[2/4] Checking backend JAR..." -ForegroundColor Cyan
$JAR = "$BACKEND\target\erp-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $JAR)) {
    Write-Host "  JAR not found. Building now..." -ForegroundColor Yellow
    Push-Location $BACKEND
    mvn clean package -DskipTests -q 2>&1 | Where-Object { $_ -notmatch "^WARNING" }
    Pop-Location
} else {
    Write-Host "  JAR found at $JAR" -ForegroundColor Green
}

# STEP 3: Start Backend
Write-Host "`n[3/4] Starting Backend on port 8080..." -ForegroundColor Cyan
Start-Process -FilePath "java" `
    -ArgumentList "-jar", $JAR, "--spring.profiles.active=dev" `
    -WorkingDirectory $BACKEND `
    -WindowStyle Minimized
Write-Host "  Backend process launched. Waiting 30 seconds for startup..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Check if backend actually started
$backendUp = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($backendUp) {
    Write-Host "  Backend ONLINE on :8080" -ForegroundColor Green
} else {
    Write-Host "  Backend still starting or failed. Check logs." -ForegroundColor Red
}

# STEP 4: Start Frontend
Write-Host "`n[4/4] Starting Frontend on port 5173..." -ForegroundColor Cyan
Start-Process -FilePath "cmd" `
    -ArgumentList "/c", "cd /d `"$FRONTEND`" && npm run dev" `
    -WindowStyle Minimized
Start-Sleep -Seconds 6

$frontendUp = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
if ($frontendUp) {
    Write-Host "  Frontend ONLINE on :5173" -ForegroundColor Green
} else {
    Write-Host "  Frontend starting..." -ForegroundColor Yellow
}

# Final Summary
Write-Host ""
Write-Host "=========================================================" -ForegroundColor Magenta
Write-Host "           ALL SYSTEMS LAUNCHING                        " -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "  App URL  : http://localhost:5173" -ForegroundColor Cyan
Write-Host "  Swagger  : http://localhost:8080/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "  H2 DB    : http://localhost:8080/h2-console" -ForegroundColor Cyan
Write-Host "  Login    : admin@thundercore.com / Admin@123" -ForegroundColor Yellow
Write-Host ""

# Open browser
Start-Process "http://localhost:5173"
